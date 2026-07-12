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

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Loaders.ChatLoader;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.chat.CharacterChatEvent;
import net.tfminecraft.RPCharacters.chat.ChatChannel;
import net.tfminecraft.RPCharacters.identity.MaskService;

public class ConversationManager implements Listener {

	private static final Map<String, PendingConversation> pending = new HashMap<>();

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onCharacterChat(CharacterChatEvent event) {
		String channelId = event.getChannel();
		if (channelId == null) {
			return;
		}
		String normalized = channelId.toLowerCase(Locale.ROOT);
		if (normalized.equals("looc") || normalized.equals("ooc")) {
			return;
		}
		if (event.isMasked()) {
			return;
		}

		Player player = event.getSender();
		if (player == null || player.getGameMode() != GameMode.SURVIVAL) {
			return;
		}

		ChatChannel channel = ChatLoader.getChannel(channelId);
		if (channel == null) {
			return;
		}

		recordChannelMessage(player, channel);
	}

	public static void recordChannelMessage(Player speaker, ChatChannel channel) {
		if (speaker == null || channel == null) {
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
		if (outboundRange <= 0) {
			return;
		}
		double rangeSq = (double) outboundRange * outboundRange;
		long expiresAt = now + (Cache.conversationReplyTimeoutSeconds * 1000L);

		for (Player target : speaker.getWorld().getPlayers()) {
			if (target.equals(speaker) || target.getGameMode() != GameMode.SURVIVAL) {
				continue;
			}
			if (MaskService.isMasked(target)) {
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
		if (replyRange <= 0) {
			return;
		}
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
			if (MaskService.isMasked(speakerPlayer)) {
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
