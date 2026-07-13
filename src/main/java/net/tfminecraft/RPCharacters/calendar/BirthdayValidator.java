package net.tfminecraft.RPCharacters.calendar;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Races.Race;
import net.tfminecraft.RPCharacters.Utils.RPTexts;

public final class BirthdayValidator {

	private BirthdayValidator() {}

	public static String validateForCharacter(RPCharacter character, String birthdayIso) {
		if (character == null) {
			return RPTexts.format(RPTexts.ERROR + "No character found.");
		}
		if (birthdayIso == null || birthdayIso.isBlank()) {
			return RPTexts.format(RPTexts.ERROR + "Invalid birthday.");
		}
		Race race = character.getRace();
		if (race == null) {
			return RPTexts.format(RPTexts.ERROR + "Select a race before setting a birthday.");
		}
		int age = AgeCalculator.computeAgeYears(birthdayIso, FantasyCalendar.getCurrentDate());
		int ageMin = Cache.calendarAgeMinimum;
		int ageMax = race.getAgeMax();
		if (age < ageMin || age > ageMax) {
			return RPTexts.format(RPTexts.ERROR + "Age must be between " + RPTexts.WARN + ageMin
					+ " " + RPTexts.ERROR + "and " + RPTexts.WARN + ageMax + " " + RPTexts.ERROR + "for your race.");
		}
		return null;
	}
}
