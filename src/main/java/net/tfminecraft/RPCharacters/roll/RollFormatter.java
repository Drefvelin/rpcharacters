package net.tfminecraft.RPCharacters.roll;

import org.bukkit.entity.Player;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.identity.DisplayIdentityService;

public final class RollFormatter {

	private RollFormatter() {}

	public static String format(Player player, int roll, int max, int modifier) {
		if (player == null) {
			return "";
		}
		String template = Cache.rollBroadcastText;
		if (template == null || template.isEmpty()) {
			return "";
		}
		String modifierText = formatModifier(modifier);
		String withTokens = template
				.replace("{display}", DisplayIdentityService.resolveDisplay(player))
				.replace("{player}", player.getName())
				.replace("{roll}", Integer.toString(roll))
				.replace("{max}", Integer.toString(max))
				.replace("{modifier}", modifierText);
		return StringFormatter.formatHex(withTokens.replace('&', '\u00A7'));
	}

	private static String formatModifier(int modifier) {
		if (modifier > 0) {
			return " +" + modifier;
		}
		if (modifier < 0) {
			return " " + modifier;
		}
		return "";
	}
}
