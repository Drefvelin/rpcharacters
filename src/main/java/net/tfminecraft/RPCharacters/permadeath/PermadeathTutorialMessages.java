package net.tfminecraft.RPCharacters.permadeath;

import org.bukkit.entity.Player;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.tfminecraft.RPCharacters.Loaders.PermadeathZoneLoader;
import net.tfminecraft.RPCharacters.Utils.RPTexts;

public final class PermadeathTutorialMessages {

	private static final String GOT_IT_LABEL = "[Got It]";
	private static final int SEPARATOR_WIDTH = 40;

	private PermadeathTutorialMessages() {
	}

	public static void send(Player player) {
		RPTexts.send(player, RPTexts.separator());

		for (String line : PermadeathZoneLoader.getTutorialLines()) {
			player.sendMessage(formatTutorialLine(line));
		}

		int sideDashes = (SEPARATOR_WIDTH - GOT_IT_LABEL.length()) / 2;
		String dashes = "-".repeat(sideDashes);

		TextComponent left = new TextComponent(dashes);
		left.setColor(ChatColor.GRAY);

		TextComponent gotIt = new TextComponent(GOT_IT_LABEL);
		gotIt.setColor(ChatColor.GREEN);
		gotIt.setBold(true);
		gotIt.setUnderlined(true);
		gotIt.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/rpcharacter dismisspdwarning"));
		gotIt.setHoverEvent(new HoverEvent(
				HoverEvent.Action.SHOW_TEXT,
				new ComponentBuilder("Click to dismiss")
						.color(ChatColor.GREEN)
						.bold(true)
						.append("\n")
						.color(ChatColor.GRAY)
						.italic(true)
						.append("You won't see this tutorial again.")
						.create()));

		TextComponent right = new TextComponent(dashes);
		right.setColor(ChatColor.GRAY);

		TextComponent row = new TextComponent("");
		row.addExtra(left);
		row.addExtra(gotIt);
		row.addExtra(right);
		player.spigot().sendMessage(row);
	}

	private static String formatTutorialLine(String line) {
		if (line == null || line.isEmpty()) {
			return line;
		}
		return StringFormatter.formatHex(line.replace('&', '\u00A7'));
	}
}
