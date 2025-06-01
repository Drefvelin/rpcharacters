package net.tfminecraft.RPCharacters.Objects.Trait;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

public class Trait {
	private String id;
	private String name;
	private List<String> desc = new ArrayList<String>();
	
	private TraitData data;
	
	public Trait(String key, ConfigurationSection config) {
		this.id = key;
		this.name = config.getString("name");
		this.desc = config.getStringList("description");
		this.data = new TraitData(config);
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

	public TraitData getTraitData() {
		return data;
	}
	
	
}
