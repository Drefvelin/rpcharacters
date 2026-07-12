package net.tfminecraft.RPCharacters.persona;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Utils.ClueFormatter;

public final class DescriptionValidator {

	private DescriptionValidator() {}

	public static String validate(String description) {
		if (description == null) {
			return "§cDescription cannot be empty.";
		}
		String plain = ClueFormatter.stripColor(description).trim();
		if (plain.isEmpty()) {
			return "§cDescription cannot be empty.";
		}
		int length = plain.length();
		if (length < Cache.characterDescriptionMinLength) {
			return "§cDescription must be at least " + Cache.characterDescriptionMinLength + " characters.";
		}
		if (length > Cache.characterDescriptionMaxLength) {
			return "§cDescription cannot exceed " + Cache.characterDescriptionMaxLength + " characters.";
		}
		return null;
	}
}
