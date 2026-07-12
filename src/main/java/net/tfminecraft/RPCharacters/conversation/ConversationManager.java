package net.tfminecraft.RPCharacters.conversation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Loaders.ConversationChannelLoader;
import net.tfminecraft.RPCharacters.Managers.ClueInputManager;
import net.tfminecraft.RPCharacters.Managers.CreationManager;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;

import net.tfminecraft.RPCharacters.Utils.MaskChecker;

public class ConversationManager implements Listener {

	private static final Map<String, PendingConversation> pending = new HashMap<>();

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		if (player.getGameMode() != GameMode.SURVIVAL) {
			return;
		}

		String raw = event.getMessage().stripLeading();
		if (!raw.startsWith("/")) {
			return;
		}

		String withoutSlash = raw.substring(1);
		int space = withoutSlash.indexOf(' ');
		String label = (space < 0 ? withoutSlash : withoutSlash.substring(0, space)).toLowerCase(Locale.ROOT);
		String message = space < 0 ? "" : withoutSlash.substring(space + 1).stripLeading();
		if (message.isEmpty()) {
			return;
		}

		String channelKey = ConversationChannelLoader.resolveChannelFromCommand(label);
		if (channelKey == null) {
			return;
		}

		ConversationChannel channel = ConversationChannelLoader.getChannel(channelKey);
		if (channel == null) {
			return;
		}

		recordChannelMessage(player, channel);
	}

	/**
	 * Plain chat (no command) is the default rp channel in OpenRP.
	 * ignoreCancelled=false so we still count when OpenRP cancels and re-broadcasts.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void onChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		if (player.getGameMode() != GameMode.SURVIVAL) {
			return;
		}
		if (CreationManager.activeCreators.containsKey(player)) {
			return;
		}
		if (ClueInputManager.consumeConversationSkip(player.getUniqueId())) {
			return;
		}
		if (MaskChecker.isWearingMask(player)) {
			return;
		}

		String message = event.getMessage();
		if (message == null || message.isBlank()) {
			return;
		}
		if (message.stripLeading().startsWith("/")) {
			return;
		}

		ConversationChannel channel = ConversationChannelLoader.getDefaultChannel();
		if (channel == null) {
			return;
		}

		Bukkit.getScheduler().runTask(RPCharacters.plugin, () -> recordChannelMessage(player, channel));
	}

	public static void recordChannelMessage(Player speaker, ConversationChannel channel) {
		if (speaker == null || channel == null) {
			return;
		}
		if (MaskChecker.isWearingMask(speaker)) {
			return;
		}

		PlayerData speakerData = PlayerManager.get(speaker);
		if (speakerData == null || !speakerData.hasActiveCharacter()) {
			return;
		}

		RPCharacter speakerCharacter = speakerData.getActiveCharacter();
		if (speakerCharacter == null) {
			return;
		}

		long now = System.currentTimeMillis();
		pruneExpired(now);

		Location origin = speaker.getLocation();
		int replyRange = channel.getRange();

		processReplies(speaker, speakerCharacter, origin, replyRange, now);

		int outboundRange = channel.getRange();
		double rangeSq = (double) outboundRange * outboundRange;
		long expiresAt = now + (Cache.conversationReplyTimeoutSeconds * 1000L);

		for (Player target : speaker.getWorld().getPlayers()) {
			if (target.equals(speaker) || target.getGameMode() != GameMode.SURVIVAL) {
				continue;
			}
			if (MaskChecker.isWearingMask(target)) {
				continue;
			}
			if (target.getLocation().distanceSquared(origin) > rangeSq) {
				continue;
			}

			PlayerData listenerData = PlayerManager.get(target);
			if (listenerData == null || !listenerData.hasActiveCharacter()) {
				continue;
			}
			RPCharacter listenerCharacter = listenerData.getActiveCharacter();
			if (listenerCharacter == null) {
				continue;
			}

			String key = PendingConversation.key(listenerCharacter.getId(), speakerCharacter.getId());
			pending.put(key, new PendingConversation(
					speakerCharacter.getId(),
					listenerCharacter.getId(),
					speaker.getUniqueId(),
					expiresAt));
		}
	}

	private static void processReplies(Player replier, RPCharacter replierCharacter, Location replyOrigin,
			int replyRange, long now) {
		double rangeSq = (double) replyRange * replyRange;
		Iterator<Map.Entry<String, PendingConversation>> iterator = pending.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<String, PendingConversation> entry = iterator.next();
			PendingConversation pendingConversation = entry.getValue();

			if (!replierCharacter.getId().equals(pendingConversation.getListenerCharacterId())) {
				continue;
			}
			if (pendingConversation.isExpired(now)) {
				iterator.remove();
				continue;
			}

			Player speakerPlayer = Bukkit.getPlayer(pendingConversation.getSpeakerPlayerId());
			if (speakerPlayer == null || !speakerPlayer.isOnline()) {
				continue;
			}
			if (MaskChecker.isWearingMask(speakerPlayer)) {
				continue;
			}
			if (!speakerPlayer.getWorld().equals(replyOrigin.getWorld())) {
				continue;
			}
			if (speakerPlayer.getLocation().distanceSquared(replyOrigin) > rangeSq) {
				continue;
			}

			PlayerData speakerData = PlayerManager.get(speakerPlayer);
			if (speakerData == null || !speakerData.hasActiveCharacter()) {
				continue;
			}
			RPCharacter speakerCharacter = speakerData.getActiveCharacter();
			if (speakerCharacter == null
					|| !speakerCharacter.getId().equals(pendingConversation.getSpeakerCharacterId())) {
				continue;
			}

			if (!speakerCharacter.canCountConversationWith(replierCharacter, now)) {
				continue;
			}

			speakerCharacter.recordConversationWith(replierCharacter, now);
			replierCharacter.recordConversationWith(speakerCharacter, now);
			iterator.remove();
		}
	}

	private static void pruneExpired(long now) {
		pending.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
	}

	public static List<Map.Entry<String, Integer>> getTopPartners(RPCharacter character, int limit) {
		if (character == null || limit <= 0) {
			return List.of();
		}
		List<Map.Entry<String, Integer>> sorted = new ArrayList<>(character.getConversationCounts().entrySet());
		sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));
		if (sorted.size() <= limit) {
			return sorted;
		}
		return sorted.subList(0, limit);
	}
}
