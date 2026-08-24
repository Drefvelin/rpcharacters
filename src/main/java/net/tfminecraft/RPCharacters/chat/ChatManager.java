package net.tfminecraft.RPCharacters.chat;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Loaders.ChatLoader;
import net.tfminecraft.RPCharacters.Managers.ClueInputManager;
import net.tfminecraft.RPCharacters.Managers.CreationManager;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Utils.ClueFormatter;
import net.tfminecraft.RPCharacters.Utils.RPTexts;
import net.tfminecraft.RPCharacters.identity.DisplayIdentityService;
import net.tfminecraft.RPCharacters.identity.MaskService;
import net.tfminecraft.RPCharacters.chat.smart.SmartMessageService;
import net.tfminecraft.RPCharacters.chat.smart.SmartMessageSettings;
import net.tfminecraft.RPCharacters.Loaders.SmartMessageLoader;
import net.tfminecraft.RPCharacters.speechbubble.SpeechBubbleDebug;

public final class ChatManager implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlainChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		if (shouldSkipIngest(player)) {
			return;
		}

		String message = event.getMessage();
		if (message == null || message.isBlank()) {
			return;
		}
		if (message.stripLeading().startsWith("/")) {
			return;
		}

		event.setCancelled(true);

		ChatChannel channel = ChatChannelPreferenceManager.get().getActiveChannel(player);
		if (channel == null) {
			RPCharacters.plugin.getLogger().warning("Default chat channel not loaded — check chat.yml");
			return;
		}

		PlayerData data = PlayerManager.get(player);
		RPCharacter character = data != null ? data.getActiveCharacter() : null;
		if (channel.requiresActiveCharacter() && character == null) {
			Bukkit.getScheduler().runTask(RPCharacters.plugin, () -> RPTexts.send(player, Cache.chatNoCharacterMessage));
			return;
		}

		Bukkit.getScheduler().runTask(RPCharacters.plugin,
				() -> dispatch(player, channel, message, false));
	}

	private static boolean shouldSkipIngest(Player player) {
		if (player == null) {
			return true;
		}
		if (CreationManager.activeCreators.containsKey(player)) {
			return true;
		}
		return ClueInputManager.isPending(player);
	}

	public static void dispatch(Player player, ChatChannel channel, String rawMessage, boolean wasCommand) {
		if (player == null || channel == null || rawMessage == null || rawMessage.isBlank()) {
			if (SpeechBubbleDebug.isEnabled()) {
				SpeechBubbleDebug.logSkip("chat-dispatch", "null player/channel/message");
			}
			return;
		}

		if (SpeechBubbleDebug.isEnabled() && channel.hasSpeechBubble()) {
			SpeechBubbleDebug.log("chat-dispatch",
					"player=" + player.getName()
							+ ", channel=" + channel.getId()
							+ ", bubble=true"
							+ ", wasCommand=" + wasCommand
							+ ", raw=" + rawMessage);
		}

		if (!ChatChannelPreferenceManager.get().isChannelVisible(player, channel.getId())) {
			if (SpeechBubbleDebug.isEnabled() && channel.hasSpeechBubble()) {
				SpeechBubbleDebug.logSkip("chat-dispatch", "channel toggled off for sender");
			}
			RPTexts.send(player, Cache.chatChannelCantUseWhenToggledOffMessage);
			return;
		}

		if (!channel.getUsePermission().isBlank() && !player.hasPermission(channel.getUsePermission())) {
			if (SpeechBubbleDebug.isEnabled() && channel.hasSpeechBubble()) {
				SpeechBubbleDebug.logSkip("chat-dispatch", "missing use permission " + channel.getUsePermission());
			}
			RPTexts.send(player, RPTexts.ERROR + "You do not have permission to use this chat channel.");
			return;
		}

		PlayerData data = PlayerManager.get(player);
		RPCharacter character = data != null ? data.getActiveCharacter() : null;
		if (channel.requiresActiveCharacter() && character == null) {
			if (SpeechBubbleDebug.isEnabled() && channel.hasSpeechBubble()) {
				SpeechBubbleDebug.logSkip("chat-dispatch", "no active character");
			}
			RPTexts.send(player, Cache.chatNoCharacterMessage);
			return;
		}

		String message = sanitizeMessage(player, channel, rawMessage);
		if (message.isEmpty()) {
			if (SpeechBubbleDebug.isEnabled() && channel.hasSpeechBubble()) {
				SpeechBubbleDebug.logSkip("chat-dispatch", "message empty after sanitize");
			}
			return;
		}

		if (!player.hasPermission(Cache.chatBypassCooldownPermission)
				&& ChatCooldownManager.get().isOnCooldown(player, channel.getId(), channel.getCooldownSeconds())) {
			if (SpeechBubbleDebug.isEnabled() && channel.hasSpeechBubble()) {
				SpeechBubbleDebug.logSkip("chat-dispatch", "on cooldown");
			}
			int remaining = ChatCooldownManager.get().getRemainingSeconds(player, channel.getId());
			RPTexts.send(player, RPTexts.ERROR + "Wait " + RPTexts.WARN + remaining + RPTexts.ERROR + "s.");
			return;
		}

		Set<Player> recipients = buildRecipients(player, channel);

		boolean wearingMask = MaskService.isMasked(player);
		String displayName = channel.isMasked() && wearingMask
			? MaskService.getMaskedLabel()
			: DisplayIdentityService.resolveDisplayUnmasked(player);

		if (SpeechBubbleDebug.isEnabled() && channel.hasSpeechBubble()) {
			SpeechBubbleDebug.log("chat-dispatch", "firing CharacterChatEvent, recipients=" + recipients.size());
		}

		CharacterChatEvent chatEvent = new CharacterChatEvent(
				player,
				character,
				channel.getId(),
				message,
				displayName,
				recipients,
				wearingMask,
				wasCommand);
		Bukkit.getPluginManager().callEvent(chatEvent);
		if (chatEvent.isCancelled()) {
			if (SpeechBubbleDebug.isEnabled() && channel.hasSpeechBubble()) {
				SpeechBubbleDebug.logSkip("chat-dispatch", "CharacterChatEvent cancelled by another plugin");
			}
			return;
		}

		RPCharacters.plugin.getLogger().info(
				player.getName() + " in " + channel.getId() + ": " + chatEvent.getMessage());

		SmartMessageSettings smartSettings = SmartMessageLoader.getSettings();
		if (smartSettings.isEnabled() && channel.isSmartMessages() && !channel.isGlobal()) {
			SmartMessageService.deliver(chatEvent, channel, player);
		} else {
			if (recipients.isEmpty()) {
				RPTexts.send(player, RPTexts.ERROR + "No one can hear you in this channel.");
				return;
			}

			String formatted = ChatFormatter.format(channel, player, chatEvent.getDisplayName(), chatEvent.getMessage());
			if (formatted.isEmpty()) {
				return;
			}

			for (Player recipient : chatEvent.getRecipients()) {
				if (recipient != null && recipient.isOnline()
						&& ChatChannelPreferenceManager.get().isChannelVisible(recipient, channel.getId())) {
					RPTexts.send(recipient, formatted);
				}
			}
		}

		if (!player.hasPermission(Cache.chatBypassCooldownPermission)) {
			ChatCooldownManager.get().applyCooldown(player, channel.getId(), channel.getCooldownSeconds());
		}
	}

	private static String sanitizeMessage(Player player, ChatChannel channel, String rawMessage) {
		String trimmed = rawMessage.trim();
		if (trimmed.isEmpty()) {
			return "";
		}
		String colorPerm = channel.getColorCodePermission();
		if (colorPerm != null && !colorPerm.isBlank() && player.hasPermission(colorPerm)) {
			return trimmed;
		}
		return ClueFormatter.stripColor(trimmed);
	}

	private static Set<Player> buildRecipients(Player sender, ChatChannel channel) {
		Set<Player> recipients = new HashSet<>();
		String readPerm = channel.getReadPermission();
		String channelId = channel.getId();
		if (canReceive(sender, readPerm)
				&& ChatChannelPreferenceManager.get().isChannelVisible(sender, channelId)) {
			recipients.add(sender);
		}
		if (channel.isGlobal()) {
			for (Player target : Bukkit.getOnlinePlayers()) {
				if (!target.equals(sender) && canReceive(target, readPerm)
						&& ChatChannelPreferenceManager.get().isChannelVisible(target, channelId)) {
					recipients.add(target);
				}
			}
			return recipients;
		}

		Location origin = sender.getLocation();
		double rangeSq = (double) channel.getRange() * channel.getRange();
		for (Player target : sender.getWorld().getPlayers()) {
			if (target.equals(sender) || !canReceive(target, readPerm)) {
				continue;
			}
			if (!ChatChannelPreferenceManager.get().isChannelVisible(target, channelId)) {
				continue;
			}
			if (target.getLocation().distanceSquared(origin) <= rangeSq) {
				recipients.add(target);
			}
		}
		return recipients;
	}

	private static boolean canReceive(Player player, String readPerm) {
		if (player == null) {
			return false;
		}
		return readPerm == null || readPerm.isBlank() || player.hasPermission(readPerm);
	}
}
