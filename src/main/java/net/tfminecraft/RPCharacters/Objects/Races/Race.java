package net.tfminecraft.RPCharacters.Objects.Races;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

public class Race {
	
	private String id;
	private String name;
	private List<String> desc = new ArrayList<String>();
	private boolean isShown;
	
	private RaceData data;

	public Race(String key, ConfigurationSection config) {
		this.id = key;
		this.name = config.getString("name");
		this.desc = config.getStringList("description");
		this.isShown = config.getBoolean("shown");
		this.data = new RaceData(config);
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public List<String> getDesc() {
		return desc;
	}

	public boolean isShown() {
		return isShown;
	}

	public RaceData getRaceData() {
		return data;
	}

}
