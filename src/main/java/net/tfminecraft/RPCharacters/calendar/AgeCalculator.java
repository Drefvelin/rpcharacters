package net.tfminecraft.RPCharacters.calendar;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadLocalRandom;

import net.tfminecraft.RPCharacters.Cache;

public final class AgeCalculator {

	private AgeCalculator() {}

	public static int computeAgeYears(String birthdayIso, LocalDate now) {
		LocalDate birthday = FantasyCalendar.fromIso(birthdayIso);
		if (birthday == null || now == null) {
			return 0;
		}
		int years = Period.between(birthday, now).getYears();
		return Math.max(0, years);
	}

	public static String formatAge(String birthdayIso) {
		if (birthdayIso == null || birthdayIso.isBlank()) {
			return Cache.calendarAgeUnsetLabel;
		}
		int age = computeAgeYears(birthdayIso, FantasyCalendar.getCurrentDate());
		return Integer.toString(age);
	}

	public static String birthdayFromAge(int ageYears, LocalDate now) {
		if (ageYears < 0) {
			return null;
		}
		if (now == null) {
			now = FantasyCalendar.getCurrentDate();
		}
		LocalDate earliest = now.minusYears(ageYears + 1L).plusDays(1);
		LocalDate latest = now.minusYears(ageYears);
		long daySpan = ChronoUnit.DAYS.between(earliest, latest);
		if (daySpan < 0) {
			return FantasyCalendar.toIso(latest);
		}
		int offset = daySpan == 0 ? 0 : ThreadLocalRandom.current().nextInt((int) daySpan + 1);
		LocalDate birthday = earliest.plusDays(offset);
		return FantasyCalendar.toIso(birthday);
	}
}
