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
import net.tfminecraft.RPCharacters.Creation.Stage;

public class StageLoader implements LoaderInterface{
	
	public static List<Stage> oList = new ArrayList<>();
	
	public static List<Stage> getNew(){
		List<Stage> newList = new ArrayList<>();
		for(Stage s : oList) {
			newList.add(Stage.another(s));
		}
		return newList;
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
			Stage o = Stage.create(key, config.getConfigurationSection(key));
			oList.add(o);
		}
	}
}
