package net.tfminecraft.RPCharacters.Managers;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Creation.CharacterCreation;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Utils.RPTexts;
import net.tfminecraft.RPCharacters.enums.ClueAddResult;
import net.tfminecraft.RPCharacters.enums.CreationGuiContext;

public class ClueInputManager implements Listener {

	private static final Map<Player, String> pendingCharacterId = new HashMap<>();
	private static final Set<Player> creationSummaryClueInput = ConcurrentHashMap.newKeySet();
	private static final Set<UUID> skipConversationTracking = ConcurrentHashMap.newKeySet();

	public static boolean consumeConversationSkip(UUID playerId) {
		return skipConversationTracking.remove(playerId);
	}

	public static void beginInput(Player player, String characterId) {
		beginInput(player, characterId, false);
	}

	public static void beginInput(Player player, String characterId, boolean fromCreationSummary) {
		pendingCharacterId.put(player, characterId);
		if (fromCreationSummary) {
			creationSummaryClueInput.add(player);
		}
		player.closeInventory();
		RPTexts.send(player, RPTexts.WARN + "Type your clue in chat.");
		RPTexts.send(player, RPTexts.MUTED + "Clues must be between " + RPTexts.WARN + Cache.clueMinLength
				+ RPTexts.MUTED + " and " + RPTexts.WARN + Cache.clueMaxLength + RPTexts.MUTED + " characters.");
	}

	public static String getPendingCharacterId(Player player) {
		return pendingCharacterId.get(player);
	}

	public static boolean isPending(Player player) {
		return pendingCharacterId.containsKey(player);
	}

	public static void cancel(Player player) {
		pendingCharacterId.remove(player);
		creationSummaryClueInput.remove(player);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		if (!pendingCharacterId.containsKey(player)) {
			return;
		}
		if (CreationManager.activeCreators.containsKey(player)) {
			if (!creationSummaryClueInput.contains(player)) {
				return;
			}
			creationSummaryClueInput.remove(player);
		}

		skipConversationTracking.add(player.getUniqueId());
		event.setCancelled(true);
		String message = event.getMessage();
		String characterId = pendingCharacterId.remove(player);

		Bukkit.getScheduler().runTask(RPCharacters.plugin, () -> handleClueInput(player, characterId, message));
	}

	private static void handleClueInput(Player player, String characterId, String message) {
		RPCharacter character = CreationManager.resolveCharacter(player, characterId);
		if (character == null) {
			PlayerData pd = PlayerManager.get(player);
			if (pd != null) {
				character = pd.getCharacterById(characterId);
			}
		}
		if (character == null) {
			RPTexts.send(player, RPTexts.ERROR + "Could not find that character.");
			return;
		}
		if (!character.getOwner().equals(player)) {
			RPTexts.send(player, RPTexts.ERROR + "You can only add clues to your own characters.");
			return;
		}

		ClueAddResult result = character.addPlayerClue(message);
		if (result != ClueAddResult.SUCCESS) {
			RPTexts.send(player, character.getClueAddErrorMessage(result));
			pendingCharacterId.put(player, characterId);
			if (CreationManager.isDraftCharacter(player, characterId)) {
				creationSummaryClueInput.add(player);
			}
			return;
		}

		boolean draft = CreationManager.isDraftCharacter(player, characterId);
		CharacterCreation cc = CreationManager.activeCreators.get(player);
		boolean editing = cc != null && cc.isEditing();
		if (!draft) {
			RPCharacters.getPlayerManager().savePlayer(player);
			RPCharacters.getPlayerManager().reevaluateFreeze(player);
		} else if (editing) {
			cc.persistEdits();
		}

		InventoryManager inv = new InventoryManager();
		if (cc != null && (draft || editing)) {
			CreationGuiContext context = editing ? CreationGuiContext.EDIT_SUMMARY : CreationGuiContext.CREATION_SUMMARY;
			inv.cluesView(player, character, context, cc);
		} else {
			inv.cluesView(player, character);
		}
		player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		pendingCharacterId.remove(player);
		creationSummaryClueInput.remove(player);
	}
}
