package net.tfminecraft.RPCharacters.grave;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.tfminecraft.RPCharacters.identity.DisplayIdentityService;

public final class GraveHologramTexts {

	private GraveHologramTexts() {}

	public static List<String> baseLines(Grave grave) {
		List<String> lines = new ArrayList<>();
		if (grave == null) {
			return lines;
		}
		Player owner = Bukkit.getPlayer(grave.getOwner());
		String name = owner != null ? characterOrName(owner) : offlineName(grave.getOwner());
		lines.add(name);
		if (GraveLoader.isHologramShowKiller() && grave.getKiller() != null) {
			Player killer = Bukkit.getPlayer(grave.getKiller());
			String killerLine = killer != null ? characterOrName(killer) : offlineName(grave.getKiller());
			if (killerLine != null && !killerLine.isBlank()) {
				lines.add(killerLine);
			}
		}
		return lines;
	}

	public static List<String> linesForViewer(Player viewer, Grave grave) {
		List<String> lines = new ArrayList<>(baseLines(grave));
		if (viewer != null && GraveLootRules.canSteal(viewer, grave)) {
			lines.add(formatMessage(GraveLoader.getMessageRobHint()));
		}
		return lines;
	}

	private static String formatMessage(String message) {
		if (message == null || message.isBlank()) {
			return "";
		}
		return StringFormatter.formatHex(message.replace('&', '\u00A7'));
	}

	private static String characterOrName(Player player) {
		if (player == null) {
			return "Unknown";
		}
		String character = DisplayIdentityService.resolveCharacterName(player);
		if (character != null && !character.isBlank()) {
			return character;
		}
		return player.getName() != null ? player.getName() : "Unknown";
	}

	private static String offlineName(java.util.UUID id) {
		if (id == null) {
			return "Unknown";
		}
		String name = Bukkit.getOfflinePlayer(id).getName();
		return name != null && !name.isBlank() ? name : "Unknown";
	}
}
