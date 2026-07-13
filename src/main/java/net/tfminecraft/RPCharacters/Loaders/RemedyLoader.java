package net.tfminecraft.RPCharacters.Loaders;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import me.Plugins.TLibs.Interface.LoaderInterface;
import me.Plugins.TLibs.TLibs;
import net.tfminecraft.RPCharacters.Objects.RemedyDefinition;
import net.tfminecraft.RPCharacters.RPCharacters;

public final class RemedyLoader implements LoaderInterface {

	private static final Map<String, RemedyDefinition> remedies = new HashMap<>();

	@Override
	public void load(File configFile) {
		FileConfiguration config = new YamlConfiguration();
		try {
			config.load(configFile);
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}

		remedies.clear();
		loadRemedies(config);
	}

	private static void loadRemedies(FileConfiguration config) {
		if (!config.isConfigurationSection("remedies")) {
			return;
		}

		ConfigurationSection section = config.getConfigurationSection("remedies");
		for (String key : section.getKeys(false)) {
			ConfigurationSection remedySection = section.getConfigurationSection(key);
			if (remedySection == null) {
				continue;
			}
			RemedyDefinition definition = new RemedyDefinition(key, remedySection);
			if (definition.getItem() == null || definition.getItem().isBlank()) {
				RPCharacters.plugin.getLogger().warning("Remedy '" + key + "' has no item path — skipped.");
				continue;
			}
			if (definition.getTraits().isEmpty()) {
				RPCharacters.plugin.getLogger().warning("Remedy '" + key + "' has no trait(s) — skipped.");
				continue;
			}
			remedies.put(key.toLowerCase(Locale.ROOT), definition);
		}
	}

	public static List<RemedyDefinition> resolveAll(ItemStack item) {
		if (item == null || item.getType().isAir()) {
			return List.of();
		}
		List<RemedyDefinition> matches = new ArrayList<>();
		for (RemedyDefinition remedy : remedies.values()) {
			if (TLibs.getItemAPI().getChecker().checkItemWithPath(item, remedy.getItem())) {
				matches.add(remedy);
			}
		}
		return matches;
	}
}
