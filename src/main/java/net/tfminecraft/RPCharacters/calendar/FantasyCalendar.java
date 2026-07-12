package net.tfminecraft.RPCharacters.calendar;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import net.tfminecraft.RPCharacters.Cache;

public final class FantasyCalendar {

	private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_LOCAL_DATE;

	private FantasyCalendar() {}

	public static LocalDate getCurrentDate() {
		return toFantasyDate(LocalDate.now());
	}

	public static int getCurrentFantasyYear() {
		return toFantasyYear(LocalDate.now().getYear());
	}

	public static int toFantasyYear(int irlYear) {
		return Cache.calendarBaseFantasyYear + (irlYear - Cache.calendarBaseIrlYear);
	}

	public static LocalDate toFantasyDate(LocalDate realDate) {
		if (realDate == null) {
			return null;
		}
		int fantasyYear = toFantasyYear(realDate.getYear());
		return LocalDate.of(fantasyYear, realDate.getMonth(), realDate.getDayOfMonth());
	}

	public static String toIso(LocalDate fantasyDate) {
		if (fantasyDate == null) {
			return null;
		}
		return fantasyDate.format(ISO);
	}

	public static LocalDate fromIso(String iso) {
		if (iso == null || iso.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(iso.trim(), ISO);
		} catch (DateTimeParseException ex) {
			return null;
		}
	}

	public static String formatFantasyYear(long timestamp) {
		int irlYear = Instant.ofEpochMilli(timestamp)
				.atZone(ZoneId.systemDefault())
				.getYear();
		int fantasyYear = toFantasyYear(irlYear);
		if (Cache.calendarEra == null || Cache.calendarEra.isBlank()) {
			return Integer.toString(fantasyYear);
		}
		return fantasyYear + " " + Cache.calendarEra;
	}

	public static String formatDate(long timestamp) {
		var zonedDateTime = Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault());
		int day = zonedDateTime.getDayOfMonth();
		int month = zonedDateTime.getMonthValue();
		String fantasyYear = formatFantasyYear(timestamp);
		return String.format("%02d/%02d/%s", day, month, fantasyYear);
	}
}
