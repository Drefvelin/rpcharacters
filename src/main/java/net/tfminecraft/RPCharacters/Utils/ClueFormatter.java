package net.tfminecraft.RPCharacters.Utils;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;

import net.tfminecraft.RPCharacters.Cache;

public final class ClueFormatter {

	private ClueFormatter() {}

	/** Visible characters per lore line — matches typical SimpleFactions / GUI item lore width. */
	public static final int LORE_LINE_LENGTH = 32;

	public static String stripColor(String input) {
		if (input == null) return "";
		return ChatColor.stripColor(input).trim();
	}

	public static int plainLength(String input) {
		return stripColor(input).length();
	}

	public static String format(String input) {
		String plain = stripColor(input);
		if (plain.isEmpty()) return "§7";
		return "§7" + plain;
	}

	public static String validate(String input) {
		String plain = stripColor(input);
		if (plain.length() < Cache.clueMinLength) {
			return "§cClue must be at least " + Cache.clueMinLength + " characters.";
		}
		if (plain.length() > Cache.clueMaxLength) {
			return "§cClue cannot exceed " + Cache.clueMaxLength + " characters.";
		}
		return null;
	}

	public static String lengthRangeMessage() {
		return "§cClue must be between " + Cache.clueMinLength + " and " + Cache.clueMaxLength + " characters.";
	}

	/**
	 * Splits clue text into lore lines at word boundaries without exceeding {@link #LORE_LINE_LENGTH} visible characters.
	 */
	public static List<String> wrapLore(String formattedClue) {
		return wrapLore(formattedClue, LORE_LINE_LENGTH);
	}

	/**
	 * Splits clue text into lore lines at word boundaries without exceeding {@code maxLineLength} visible characters.
	 */
	public static List<String> wrapLore(String formattedClue, int maxLineLength) {
		List<String> lines = new ArrayList<>();
		String plain = stripColor(formattedClue);
		if (plain.isEmpty()) {
			lines.add("§7");
			return lines;
		}

		int lineLength = Math.max(1, maxLineLength);
		String[] words = plain.split("\\s+");
		StringBuilder current = new StringBuilder();

		for (String word : words) {
			if (word.isEmpty()) continue;
			if (current.length() == 0) {
				current.append(word);
				continue;
			}
			if (current.length() + 1 + word.length() <= lineLength) {
				current.append(' ').append(word);
			} else {
				lines.add("§7" + current);
				current = new StringBuilder(word);
			}
		}
		if (current.length() > 0) {
			lines.add("§7" + current);
		}
		return lines;
	}
}
