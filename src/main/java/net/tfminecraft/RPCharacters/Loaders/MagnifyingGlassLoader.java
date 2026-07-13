package net.tfminecraft.RPCharacters.Loaders;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import me.Plugins.TLibs.Interface.LoaderInterface;
import me.Plugins.TLibs.TLibs;
import net.tfminecraft.RPCharacters.Objects.MagnifyingGlassDefinition;
import net.tfminecraft.RPCharacters.RPCharacters;

public final class MagnifyingGlassLoader implements LoaderInterface {

	private static final Map<String, MagnifyingGlassDefinition> glasses = new HashMap<>();

	@Override
	public void load(File configFile) {
		FileConfiguration config = new YamlConfiguration();
		try {
			config.load(configFile);
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}

		glasses.clear();
		if (!config.isConfigurationSection("magnifying-glasses")) {
			return;
		}
		ConfigurationSection section = config.getConfigurationSection("magnifying-glasses");
		for (String key : section.getKeys(false)) {
			ConfigurationSection glassSection = section.getConfigurationSection(key);
			if (glassSection == null) continue;
			MagnifyingGlassDefinition definition = new MagnifyingGlassDefinition(key, glassSection);
			if (definition.getItem() == null || definition.getItem().isBlank()) {
				RPCharacters.plugin.getLogger().warning("Magnifying glass '" + key + "' has no item path — skipped.");
				continue;
			}
			glasses.put(key.toLowerCase(Locale.ROOT), definition);
		}
	}

	public static MagnifyingGlassDefinition resolve(ItemStack item) {
		if (item == null || item.getType().isAir()) {
			return null;
		}
		for (MagnifyingGlassDefinition glass : glasses.values()) {
			if (TLibs.getItemAPI().getChecker().checkItemWithPath(item, glass.getItem())) {
				return glass;
			}
		}
		return null;
	}
}
