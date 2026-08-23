package net.tfminecraft.RPCharacters.prosthetics;

import java.util.List;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.RPCharacters.Loaders.ProstheticLoader;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.ProstheticReplacement;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.Utils.RPTexts;
import net.tfminecraft.RPCharacters.Utils.TraitChangeService;

public final class ProstheticInstallListener implements Listener {

	private enum InstallAction {
		INSTALL,
		UPGRADE,
		MAX_TIER,
		NONE
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		Action action = event.getAction();
		if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		if (event.getHand() != EquipmentSlot.HAND) {
			return;
		}

		ItemStack item = event.getItem();
		List<ProstheticReplacement> replacements = ProstheticLoader.resolveForItem(item);
		if (replacements.isEmpty()) {
			return;
		}

		event.setCancelled(true);

		Player player = event.getPlayer();
		PlayerData pd = PlayerManager.get(player);
		if (pd == null || !pd.hasActiveCharacter()) {
			RPTexts.send(player, RPTexts.ERROR + "You need an active character to install prosthetics.");
			return;
		}

		RPCharacter character = pd.getActiveCharacter();
		boolean sawMaxTier = false;
		for (ProstheticReplacement replacement : replacements) {
			InstallAction resolved = resolveAction(character, replacement);
			switch (resolved) {
				case INSTALL -> {
					String tierId = replacement.getTierId(0);
					if (tierId != null && TraitChangeService.replaceInjuryWithProsthetic(player, character,
							replacement.getPermanentInjuryId(), tierId)) {
						player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
						return;
					}
				}
				case UPGRADE -> {
					Trait current = findOwnedProsthetic(character, replacement);
					String nextTierId = current != null ? replacement.getNextTierId(current.getId()) : null;
					if (nextTierId != null && TraitChangeService.upgradeProsthetic(player, character,
							current.getId(), nextTierId)) {
						player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.2f);
						return;
					}
				}
				case MAX_TIER -> sawMaxTier = true;
				case NONE -> {
				}
			}
		}

		if (sawMaxTier) {
			RPTexts.send(player, RPTexts.ERROR + "You already have the best prosthetic for that injury.");
			return;
		}

		RPTexts.send(player, RPTexts.MUTED + "No injury on this character can use that item.");
	}

	private static InstallAction resolveAction(RPCharacter character, ProstheticReplacement replacement) {
		Trait injury = findOwnedTrait(character, replacement.getPermanentInjuryId());
		Trait prosthetic = findOwnedProsthetic(character, replacement);

		if (injury != null && prosthetic == null) {
			return replacement.getTierId(0) != null ? InstallAction.INSTALL : InstallAction.NONE;
		}

		if (prosthetic != null) {
			if (replacement.getNextTierId(prosthetic.getId()) != null) {
				return InstallAction.UPGRADE;
			}
			return InstallAction.MAX_TIER;
		}

		return InstallAction.NONE;
	}

	private static Trait findOwnedTrait(RPCharacter character, String traitId) {
		if (traitId == null || traitId.isBlank()) {
			return null;
		}
		for (Trait trait : character.getTraits()) {
			if (trait.getId().equalsIgnoreCase(traitId)) {
				return trait;
			}
		}
		return null;
	}

	private static Trait findOwnedProsthetic(RPCharacter character, ProstheticReplacement replacement) {
		Trait best = null;
		int bestIndex = -1;
		for (Trait trait : character.getTraits()) {
			int index = replacement.getTierIndex(trait.getId());
			if (index > bestIndex) {
				bestIndex = index;
				best = trait;
			}
		}
		return best;
	}
}
