package net.tfminecraft.RPCharacters.chat.smart;

import org.bukkit.entity.Player;

import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.conversation.ConversationManager;

public final class PlaceholderSuppressionResolver {

	private PlaceholderSuppressionResolver() {}

	public static boolean shouldSuppress(Player listener, SmartMessageSettings settings) {
		if (!settings.isPlaceholderSuppressionEnabled()) {
			return false;
		}
		if (listener == null) {
			return false;
		}
		PlayerData data = PlayerManager.get(listener);
		if (data == null || !data.hasActiveCharacter()) {
			return false;
		}
		RPCharacter character = data.getActiveCharacter();
		if (character == null) {
			return false;
		}
		return ConversationManager.isInActiveConversation(character, System.currentTimeMillis());
	}

	public static int activeSessionCount(Player listener) {
		if (listener == null) {
			return 0;
		}
		PlayerData data = PlayerManager.get(listener);
		if (data == null || !data.hasActiveCharacter()) {
			return 0;
		}
		RPCharacter character = data.getActiveCharacter();
		if (character == null) {
			return 0;
		}
		return ConversationManager.countActiveSessions(character, System.currentTimeMillis());
	}
}
