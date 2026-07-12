package net.tfminecraft.RPCharacters.Managers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.enums.ClueAddResult;

public class ClueInputManager implements Listener {

	private static final Map<Player, String> pendingCharacterId = new HashMap<>();
	private static final Set<UUID> skipConversationTracking = ConcurrentHashMap.newKeySet();

	public static boolean consumeConversationSkip(UUID playerId) {
		return skipConversationTracking.remove(playerId);
	}

	public static void beginInput(Player player, String characterId) {
		pendingCharacterId.put(player, characterId);
		player.closeInventory();
		player.sendMessage("§eType your clue in chat.");
		player.sendMessage("§7Clues must be between §e" + Cache.clueMinLength + "§7 and §e" + Cache.clueMaxLength + "§7 characters.");
	}

	public static boolean isPending(Player player) {
		return pendingCharacterId.containsKey(player);
	}

	public static void cancel(Player player) {
		pendingCharacterId.remove(player);
	}

	@EventHandler
	public void onChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		if (!pendingCharacterId.containsKey(player)) return;
		if (CreationManager.activeCreators.containsKey(player)) return;

		skipConversationTracking.add(player.getUniqueId());
		event.setCancelled(true);
		String message = event.getMessage();
		String characterId = pendingCharacterId.remove(player);

		Bukkit.getScheduler().runTask(RPCharacters.plugin, () -> handleClueInput(player, characterId, message));
	}

	private void handleClueInput(Player player, String characterId, String message) {
		PlayerData pd = PlayerManager.get(player);
		if (pd == null) return;

		RPCharacter character = pd.getCharacterById(characterId);
		if (character == null) {
			player.sendMessage("§cCould not find that character.");
			return;
		}
		if (!character.getOwner().equals(player)) {
			player.sendMessage("§cYou can only add clues to your own characters.");
			return;
		}

		ClueAddResult result = character.addPlayerClue(message);
		if (result != ClueAddResult.SUCCESS) {
			player.sendMessage(character.getClueAddErrorMessage(result));
			pendingCharacterId.put(player, characterId);
			return;
		}

		RPCharacters.getPlayerManager().savePlayer(player);
		RPCharacters.getPlayerManager().reevaluateFreeze(player);

		InventoryManager inv = new InventoryManager();
		inv.cluesView(player, character);
		player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		pendingCharacterId.remove(event.getPlayer());
	}
}
