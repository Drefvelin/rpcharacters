package net.tfminecraft.RPCharacters.grave;

import org.bukkit.entity.Player;

import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.Objects.Trait.TraitData;

public final class GraveLootRules {

	private static final String ADMIN_PERMISSION = "rpchar.grave.admin";

	private GraveLootRules() {}

	public static boolean canRecover(Player player, Grave grave) {
		if (player == null || grave == null) {
			return false;
		}
		return grave.isOwner(player.getUniqueId()) || player.hasPermission(ADMIN_PERMISSION);
	}

	public static boolean canSteal(Player player, Grave grave) {
		if (player == null || grave == null || canRecover(player, grave)) {
			return false;
		}
		if (grave.getKiller() == null || !grave.getKiller().equals(player.getUniqueId())) {
			return false;
		}
		return hasCanLootGravesTrait(player);
	}

	public static boolean hasCanLootGravesTrait(Player player) {
		PlayerData data = PlayerManager.get(player);
		if (data == null || !data.hasActiveCharacter()) {
			return false;
		}
		RPCharacter character = data.getActiveCharacter();
		if (character == null) {
			return false;
		}
		for (Trait trait : character.getTraits()) {
			if (trait == null) {
				continue;
			}
			TraitData traitData = trait.getTraitData();
			if (traitData != null && traitData.canLootGraves()) {
				return true;
			}
		}
		return false;
	}
}
