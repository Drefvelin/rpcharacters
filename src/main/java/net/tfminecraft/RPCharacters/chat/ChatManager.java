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

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Loaders.ChatLoader;
import net.tfminecraft.RPCharacters.Managers.ClueInputManager;
import net.tfminecraft.RPCharacters.Managers.CreationManager;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Utils.ClueFormatter;
import net.tfminecraft.RPCharacters.identity.DisplayIdentityService;
import net.tfminecraft.RPCharacters.identity.MaskService;

public final class ChatManager implements Listener {

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
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

		ChatChannel channel = ChatLoader.getDefaultChannel();
		if (channel == null) {
			RPCharacters.plugin.getLogger().warning("Default chat channel not loaded — check chat.yml");
			return;
		}

		PlayerData data = PlayerManager.get(player);
		RPCharacter character = data != null ? data.getActiveCharacter() : null;
		if (channel.requiresActiveCharacter() && character == null) {
			Bukkit.getScheduler().runTask(RPCharacters.plugin, () -> player.sendMessage(formatNoCharacterMessage()));
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
			return;
		}

		if (!channel.getUsePermission().isBlank() && !player.hasPermission(channel.getUsePermission())) {
			player.sendMessage("§cYou do not have permission to use this chat channel.");
			return;
		}

		PlayerData data = PlayerManager.get(player);
		RPCharacter character = data != null ? data.getActiveCharacter() : null;
		if (channel.requiresActiveCharacter() && character == null) {
			player.sendMessage(formatNoCharacterMessage());
			return;
		}

		String message = sanitizeMessage(player, channel, rawMessage);
		if (message.isEmpty()) {
			return;
		}

		if (!player.hasPermission(Cache.chatBypassCooldownPermission)
				&& ChatCooldownManager.get().isOnCooldown(player, channel.getId(), channel.getCooldownSeconds())) {
			int remaining = ChatCooldownManager.get().getRemainingSeconds(player, channel.getId());
			player.sendMessage("§cPlease wait §e" + remaining + "§c more second(s) before using this channel again.");
			return;
		}

		Set<Player> recipients = buildRecipients(player, channel);
		if (recipients.isEmpty()) {
			player.sendMessage("§cNo one can hear you in this channel.");
			return;
		}

		String displayName = DisplayIdentityService.resolveDisplay(player);
		boolean masked = MaskService.isMasked(player);

		CharacterChatEvent chatEvent = new CharacterChatEvent(
				player,
				character,
				channel.getId(),
				message,
				displayName,
				recipients,
				masked,
				wasCommand);
		Bukkit.getPluginManager().callEvent(chatEvent);
		if (chatEvent.isCancelled()) {
			return;
		}

		String formatted = ChatFormatter.format(channel, player, chatEvent.getDisplayName(), chatEvent.getMessage());
		if (formatted.isEmpty()) {
			return;
		}

		for (Player recipient : chatEvent.getRecipients()) {
			if (recipient != null && recipient.isOnline()) {
				recipient.sendMessage(formatted);
			}
		}

		if (!player.hasPermission(Cache.chatBypassCooldownPermission)) {
			ChatCooldownManager.get().applyCooldown(player, channel.getId(), channel.getCooldownSeconds());
		}
	}

	private static String formatNoCharacterMessage() {
		return StringFormatter.formatHex(Cache.chatNoCharacterMessage.replace('&', '\u00A7'));
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
		if (canReceive(sender, readPerm)) {
			recipients.add(sender);
		}
		if (channel.isGlobal()) {
			for (Player target : Bukkit.getOnlinePlayers()) {
				if (!target.equals(sender) && canReceive(target, readPerm)) {
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
