package net.tfminecraft.RPCharacters.persona;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Utils.ClueFormatter;
import net.tfminecraft.RPCharacters.Utils.RPTexts;

public final class AliasValidator {

	private AliasValidator() {}

	public static String validate(String alias) {
		if (alias == null) {
			return RPTexts.format(RPTexts.ERROR + "Alias cannot be empty.");
		}
		String plain = ClueFormatter.stripColor(alias);
		if (plain.isEmpty()) {
			return RPTexts.format(RPTexts.ERROR + "Alias cannot be empty.");
		}
		String lengthError = validateLength(plain, "Alias");
		if (lengthError != null) {
			return lengthError;
		}
		String allowed = Cache.personaAliasAllowedChars;
		for (int i = 0; i < plain.length(); i++) {
			char c = plain.charAt(i);
			if (!isAllowed(c, allowed)) {
				return RPTexts.format(RPTexts.ERROR + "Alias contains disallowed character: "
						+ RPTexts.WARN + c);
			}
		}
		return null;
	}

	public static String validateCharacterName(String name) {
		if (name == null || name.isBlank()) {
			return RPTexts.format(RPTexts.ERROR + "Name cannot be empty.");
		}
		String trimmed = name.trim();
		if (containsColourCodes(trimmed)) {
			return RPTexts.format(RPTexts.ERROR + "Colour codes are not allowed in character names. Use "
					+ RPTexts.COMMAND + "/char namecolour " + RPTexts.ERROR + "for display colour.");
		}
		return validateLength(trimmed, "Name");
	}

	private static boolean containsColourCodes(String input) {
		if (input == null) {
			return false;
		}
		String stripped = ClueFormatter.stripColor(input);
		if (!stripped.equals(input)) {
			return true;
		}
		return input.indexOf('&') >= 0 || input.indexOf('§') >= 0
				|| input.matches("(?i).*(#[a-f0-9]{6}).*");
	}

	public static String validateLength(String plain, String label) {
		int length = plain.length();
		if (length < Cache.personaDisplayNameMinLength) {
			return RPTexts.format(RPTexts.ERROR + label + " must be at least "
					+ Cache.personaDisplayNameMinLength + " characters.");
		}
		if (length > Cache.personaDisplayNameMaxLength) {
			return RPTexts.format(RPTexts.ERROR + label + " cannot exceed "
					+ Cache.personaDisplayNameMaxLength + " characters.");
		}
		return null;
	}

	private static boolean isAllowed(char c, String allowed) {
		if (allowed == null) {
			return false;
		}
		for (int i = 0; i < allowed.length(); i++) {
			char allowedChar = allowed.charAt(i);
			if (Character.isLetter(c) && Character.isLetter(allowedChar)) {
				if (Character.toLowerCase(c) == Character.toLowerCase(allowedChar)) {
					return true;
				}
			} else if (c == allowedChar) {
				return true;
			}
		}
		return false;
	}
}
