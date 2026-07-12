package net.tfminecraft.RPCharacters.Utils;

import org.bukkit.entity.Player;

import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;

public final class PlaytimeGate {

	private PlaytimeGate() {}

	public static boolean canSelectTrait(Player player, Trait trait) {
		if (player == null || trait == null) {
			return false;
		}
		int required = trait.getTraitData().getRequiredAccountPlaytimeSeconds();
		if (required <= 0) {
			return true;
		}
		PlayerData data = PlayerManager.get(player);
		if (data == null) {
			return false;
		}
		return data.getAccountPlaytimeSeconds() >= required;
	}

	public static String denialMessage(Player player, Trait trait) {
		int required = trait.getTraitData().getRequiredAccountPlaytimeSeconds();
		PlayerData data = PlayerManager.get(player);
		int have = data != null ? data.getAccountPlaytimeSeconds() : 0;
		int remaining = Math.max(0, required - have);
		return "§cYou need §e" + PlaytimeFormatter.formatHoursRemaining(remaining)
				+ " §cmore playtime to select this trait.";
	}
}
