package net.tfminecraft.RPCharacters.identity;

import org.bukkit.entity.Player;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.calendar.AgeCalculator;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Races.Race;
import net.tfminecraft.RPCharacters.Utils.ClueFormatter;

public final class PersonaService {

	private PersonaService() {}

	public static boolean hasActiveCharacter(Player player) {
		if (player == null) {
			return false;
		}
		PlayerData data = PlayerManager.get(player);
		return data != null && data.hasActiveCharacter();
	}

	public static RPCharacter getActiveCharacter(Player player) {
		if (player == null) {
			return null;
		}
		PlayerData data = PlayerManager.get(player);
		return data != null ? data.getActiveCharacter() : null;
	}

	public static String resolveRace(Player player) {
		RPCharacter character = getActiveCharacter(player);
		return resolveRace(character);
	}

	public static String resolveRace(RPCharacter character) {
		if (character == null || character.getRace() == null) {
			return "";
		}
		return ClueFormatter.stripColor(character.getRace().getName());
	}

	public static String resolveGender(Player player) {
		RPCharacter character = getActiveCharacter(player);
		return resolveGender(character);
	}

	public static String resolveGender(RPCharacter character) {
		if (character == null) {
			return Cache.personaGenderDefault;
		}
		String gender = character.getGender();
		if (gender == null || gender.isBlank()) {
			return Cache.personaGenderDefault;
		}
		return gender;
	}

	public static String resolveAge(Player player) {
		return resolveAge(getActiveCharacter(player));
	}

	public static String resolveAge(RPCharacter character) {
		if (character == null || character.getBirthday() == null || character.getBirthday().isBlank()) {
			return Cache.calendarAgeUnsetLabel;
		}
		return AgeCalculator.formatAge(character.getBirthday());
	}

	public static String resolveDescription(Player player) {
		RPCharacter character = getActiveCharacter(player);
		return resolveDescription(character);
	}

	public static String resolveDescription(RPCharacter character) {
		if (character == null) {
			return "";
		}
		String custom = character.getPersonaDescription();
		if (custom != null && !custom.isBlank()) {
			return custom;
		}
		return applyDescriptionTemplate(character);
	}

	private static String applyDescriptionTemplate(RPCharacter character) {
		String template = Cache.personaDescriptionDefaultTemplate;
		if (template == null || template.isBlank()) {
			return "";
		}
		Race race = character.getRace();
		String raceName = race != null ? ClueFormatter.stripColor(race.getName()) : "";
		String articleSuffix = startsWithVowel(raceName) ? "n" : "";
		String continent = Cache.continent != null ? Cache.continent : "";
		return template
				.replace("{n}", articleSuffix)
				.replace("{race}", raceName)
				.replace("{continent}", continent);
	}

	private static boolean startsWithVowel(String word) {
		if (word == null || word.isEmpty()) {
			return false;
		}
		char c = Character.toLowerCase(word.charAt(0));
		return c == 'a' || c == 'e' || c == 'i' || c == 'o' || c == 'u';
	}
}
