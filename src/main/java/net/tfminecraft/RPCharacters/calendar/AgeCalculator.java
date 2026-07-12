package net.tfminecraft.RPCharacters.calendar;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import net.tfminecraft.RPCharacters.Cache;

public final class AgeCalculator {

	private AgeCalculator() {}

	public static double computeAgeYears(String birthdayIso, LocalDate now) {
		LocalDate birthday = FantasyCalendar.fromIso(birthdayIso);
		if (birthday == null || now == null) {
			return 0;
		}
		long days = ChronoUnit.DAYS.between(birthday, now);
		if (days < 0) {
			days = 0;
		}
		double daysPerYear = Cache.calendarDaysPerYear > 0 ? Cache.calendarDaysPerYear : 365.25;
		return days / daysPerYear;
	}

	public static String formatAge(String birthdayIso) {
		if (birthdayIso == null || birthdayIso.isBlank()) {
			return Cache.calendarAgeUnsetLabel;
		}
		double age = computeAgeYears(birthdayIso, FantasyCalendar.getCurrentDate());
		return formatAgeValue(age);
	}

	public static String birthdayFromAge(double ageYears, LocalDate now) {
		if (now == null) {
			now = FantasyCalendar.getCurrentDate();
		}
		double daysPerYear = Cache.calendarDaysPerYear > 0 ? Cache.calendarDaysPerYear : 365.25;
		long daysToSubtract = Math.round(ageYears * daysPerYear);
		LocalDate birthday = now.minusDays(daysToSubtract);
		return FantasyCalendar.toIso(birthday);
	}

	private static String formatAgeValue(double age) {
		int decimals = Math.max(0, Cache.calendarAgeDecimalPlaces);
		if (decimals == 0) {
			return Integer.toString((int) Math.round(age));
		}
		String pattern = "%." + decimals + "f";
		String formatted = String.format(Locale.US, pattern, age);
		if (decimals > 0) {
			formatted = stripTrailingZeros(formatted);
		}
		return formatted;
	}

	private static String stripTrailingZeros(String value) {
		if (!value.contains(".")) {
			return value;
		}
		while (value.endsWith("0")) {
			value = value.substring(0, value.length() - 1);
		}
		if (value.endsWith(".")) {
			value = value.substring(0, value.length() - 1);
		}
		return value;
	}
}
