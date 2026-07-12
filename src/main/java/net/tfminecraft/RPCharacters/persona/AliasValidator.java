package net.tfminecraft.RPCharacters.persona;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Utils.ClueFormatter;

public final class AliasValidator {

	private AliasValidator() {}

	public static String validate(String alias) {
		if (alias == null) {
			return "§cAlias cannot be empty.";
		}
		String plain = ClueFormatter.stripColor(alias);
		if (plain.isEmpty()) {
			return "§cAlias cannot be empty.";
		}
		String lengthError = validateLength(plain, "Alias");
		if (lengthError != null) {
			return lengthError;
		}
		String allowed = Cache.personaAliasAllowedChars;
		for (int i = 0; i < plain.length(); i++) {
			char c = plain.charAt(i);
			if (!isAllowed(c, allowed)) {
				return "§cAlias contains disallowed character: §e" + c;
			}
		}
		return null;
	}

	public static String validateCharacterName(String name) {
		if (name == null || name.isBlank()) {
			return "§cName cannot be empty.";
		}
		return validateLength(name.trim(), "Name");
	}

	public static String validateLength(String plain, String label) {
		int length = plain.length();
		if (length < Cache.personaDisplayNameMinLength) {
			return "§c" + label + " must be at least " + Cache.personaDisplayNameMinLength + " characters.";
		}
		if (length > Cache.personaDisplayNameMaxLength) {
			return "§c" + label + " cannot exceed " + Cache.personaDisplayNameMaxLength + " characters.";
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
