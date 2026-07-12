package net.tfminecraft.RPCharacters.Loaders;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.Plugins.TLibs.Interface.LoaderInterface;
import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Objects.PermissionGroupDefinition;

public final class PermissionGroupsLoader implements LoaderInterface {

	@Override
	public void load(File configFile) {
		FileConfiguration config = new YamlConfiguration();
		try {
			config.load(configFile);
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}

		Map<String, Integer> defaults = new HashMap<>();
		if (config.isConfigurationSection("defaults")) {
			ConfigurationSection section = config.getConfigurationSection("defaults");
			for (String key : section.getKeys(false)) {
				defaults.put(key, section.getInt(key));
			}
		}
		Cache.permissionGroupDefaults = defaults;

		List<PermissionGroupDefinition> groups = new ArrayList<>();
		if (config.isConfigurationSection("groups")) {
			ConfigurationSection groupsSection = config.getConfigurationSection("groups");
			for (String id : groupsSection.getKeys(false)) {
				ConfigurationSection groupSection = groupsSection.getConfigurationSection(id);
				if (groupSection == null) {
					continue;
				}
				String permission = groupSection.getString("permission", "");
				Map<String, Integer> perks = new HashMap<>();
				for (String key : groupSection.getKeys(false)) {
					if ("permission".equals(key)) {
						continue;
					}
					perks.put(key, groupSection.getInt(key));
				}
				groups.add(new PermissionGroupDefinition(id, permission, perks));
			}
		}
		Cache.permissionGroups = groups;
	}
}
