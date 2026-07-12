package net.tfminecraft.RPCharacters.Loaders;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.Plugins.TLibs.Interface.LoaderInterface;
import net.tfminecraft.RPCharacters.Cache;

public final class CalendarLoader implements LoaderInterface {

	@Override
	public void load(File configFile) {
		FileConfiguration config = new YamlConfiguration();
		try {
			config.load(configFile);
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}

		String startingYear = config.getString("starting-year", "372 AE");
		String[] parts = startingYear.trim().split("\\s+", 2);
		try {
			Cache.calendarBaseFantasyYear = Integer.parseInt(parts[0]);
		} catch (NumberFormatException ex) {
			Cache.calendarBaseFantasyYear = 372;
		}
		Cache.calendarEra = parts.length > 1 ? parts[1] : "";

		Cache.calendarBaseIrlYear = config.getInt("base-irl-year", Cache.calendarBaseIrlYear);

		if (config.isConfigurationSection("age")) {
			Cache.calendarDaysPerYear = config.getDouble("age.days-per-year", Cache.calendarDaysPerYear);
			Cache.calendarAgeDecimalPlaces = Math.max(0, config.getInt("age.decimal-places", Cache.calendarAgeDecimalPlaces));
			Cache.calendarAgeMinimum = Math.max(0, config.getInt("age.minimum", Cache.calendarAgeMinimum));
			Cache.calendarAgeUnsetLabel = config.getString("age.unset-label", Cache.calendarAgeUnsetLabel);
		}
	}
}
