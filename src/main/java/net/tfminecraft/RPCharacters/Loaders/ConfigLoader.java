package net.tfminecraft.RPCharacters.Loaders;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.Plugins.TLibs.Interface.LoaderInterface;
import net.tfminecraft.RPCharacters.Cache;

public class ConfigLoader implements LoaderInterface{

	@Override
	public void load(File configFile) {
		FileConfiguration config = new YamlConfiguration();
        try {
        	config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        if(config.contains("attributes")) {
        	Cache.attributes = config.getStringList("attributes");
        }
        if(config.contains("professions")) {
        	Cache.professions = config.getStringList("professions");
        }
        if(config.contains("editable-trait-types")) {
        	Cache.editableTraits = config.getStringList("editable-trait-types");
        }
        Cache.backgroundTraitTypes = config.getStringList("background-trait-types");
        Cache.requireCharacter = config.getBoolean("require-character", false);
        Cache.startingProfessionFactor = config.getInt("base-profession-factor", -15);
	}

}
