package net.tfminecraft.RPCharacters.persona;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Utils.ClueFormatter;
import net.tfminecraft.RPCharacters.Utils.RPTexts;

public final class DescriptionValidator {

	private DescriptionValidator() {}

	public static String validate(String description) {
		if (description == null) {
			return RPTexts.format(RPTexts.ERROR + "Description cannot be empty.");
		}
		String plain = ClueFormatter.stripColor(description).trim();
		if (plain.isEmpty()) {
			return RPTexts.format(RPTexts.ERROR + "Description cannot be empty.");
		}
		int length = plain.length();
		if (length < Cache.characterDescriptionMinLength) {
			return RPTexts.format(RPTexts.ERROR + "Description must be at least "
					+ Cache.characterDescriptionMinLength + " characters.");
		}
		if (length > Cache.characterDescriptionMaxLength) {
			return RPTexts.format(RPTexts.ERROR + "Description cannot exceed "
					+ Cache.characterDescriptionMaxLength + " characters.");
		}
		return null;
	}
}
