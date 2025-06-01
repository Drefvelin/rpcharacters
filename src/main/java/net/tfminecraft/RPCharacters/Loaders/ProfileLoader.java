package net.tfminecraft.RPCharacters.Loaders;

import java.io.File;
import java.io.IOException;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.Plugins.TLibs.Interface.LoaderInterface;
import net.tfminecraft.RPCharacters.Cache;

public class ProfileLoader implements LoaderInterface{
	@Override
	public void load(File configFile) {
		FileConfiguration config = new YamlConfiguration();
        try {
        	config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        Cache.maxAlive = config.getInt("max-alive-characters");
        Cache.switchCooldown = config.getInt("character-switch-cooldown");
        Cache.characterSlots = config.getIntegerList("character-slots");
        Cache.deadSlot = config.getInt("dead-slot");
	}
}
