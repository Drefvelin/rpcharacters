package net.tfminecraft.RPCharacters.Loaders;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.Plugins.TLibs.Interface.LoaderInterface;
import net.tfminecraft.RPCharacters.Objects.Races.Race;

public class RaceLoader implements LoaderInterface{
	
	public static List<Race> oList = new ArrayList<>();
	
	public static List<Race> get(){
		return oList;
	}
	
	@Override
	public void load(File configFile) {
		
		FileConfiguration config = new YamlConfiguration();
        try {
        	config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        Set<String> set = config.getKeys(false);

		List<String> list = new ArrayList<String>(set);
		
		for(String key : list) {
			Race o = new Race(key, config.getConfigurationSection(key));
			oList.add(o);
		}
	}

	public static Race getByString(String id) {
		for(Race r : oList) {
			if(r.getId().equalsIgnoreCase(id)) return r;
		}
		return null;
	}

}
