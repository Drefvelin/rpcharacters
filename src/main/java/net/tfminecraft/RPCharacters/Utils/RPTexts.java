package net.tfminecraft.RPCharacters.Utils;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public final class RPTexts {

	public static final String SUCCESS = "#87d65c";
	public static final String ERROR = "#d65c5c";
	public static final String WARN = "#d6cf69";
	public static final String ACCENT = "#c9a24f";
	public static final String INFO = "#56ccf2";
	public static final String COMMAND = "#32ed73";
	public static final String MUTED = "§7";
	public static final String RESET = "§r";
	public static final String WHITE = "§f";

	private RPTexts() {
	}

	public static String format(String raw) {
		if (raw == null || raw.isEmpty()) {
			return raw;
		}
		return StringFormatter.formatHex(raw.replace('&', '\u00A7'));
	}

	public static void send(CommandSender sender, String raw) {
		sender.sendMessage(format(raw));
	}

	public static void title(Player player, String title, String subtitle) {
		player.sendTitle(format(title), format(subtitle), 10, 60, 20);
	}

	public static void title(Player player, String title, String subtitle, int fadeIn, int stay, int fadeOut) {
		player.sendTitle(format(title), format(subtitle), fadeIn, stay, fadeOut);
	}

	/** Title visible for ~20 seconds (400 ticks stay). */
	public static void longTitle(Player player, String title, String subtitle) {
		title(player, title, subtitle, 10, 400, 20);
	}

	public static String separator() {
		return MUTED + "----------------------------------------";
	}

	public static String enterTitle(String zoneName) {
		return MUTED + "Now entering " + WARN + zoneName;
	}

	public static String leaveTitle(String zoneName) {
		return MUTED + "Now leaving " + WARN + zoneName;
	}

	public static String prefix(String label) {
		return ACCENT + "[RPCharacters] " + MUTED;
	}

	public static void sendPrefixed(CommandSender sender, String raw) {
		send(sender, prefix("") + raw);
	}

	/** Lore spacer that resets inherited formatting (avoids default purple italic). */
	public static String spacer() {
		return format(RESET + MUTED + " ");
	}

	/** Muted bullet line for plain text. */
	public static String bullet(String plainText) {
		return format(RESET + MUTED + "- " + RESET + plainText);
	}

	/** Muted bullet prefix before text that is already colour-formatted. */
	public static String bulletFormatted(String formattedText) {
		return format(RESET + MUTED + "- ") + formattedText;
	}

	public static String mutedParenthetical(String plainText) {
		return format(" " + MUTED + "(" + plainText + ")");
	}

	public static String joinFormatted(java.util.List<String> formattedParts, String separator) {
		if (formattedParts == null || formattedParts.isEmpty()) {
			return format(MUTED + "Not selected");
		}
		String formattedSeparator = format(RESET + separator);
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < formattedParts.size(); i++) {
			if (i > 0) {
				builder.append(formattedSeparator);
			}
			builder.append(formattedParts.get(i));
		}
		return builder.toString();
	}

	/** Ensures plain lore text always has an explicit colour (defaults to muted gray). */
	public static String lore(String plainText) {
		return format(RESET + MUTED + plainText);
	}

	public static String labeled(String label, String formattedSuffix) {
		return format(RESET + MUTED + label) + formattedSuffix;
	}
}
