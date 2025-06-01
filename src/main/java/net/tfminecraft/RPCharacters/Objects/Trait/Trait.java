package net.tfminecraft.RPCharacters.Objects.Trait;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public class Trait {
	private String id;
	private String name;
	private List<String> desc = new ArrayList<String>();
	
	private TraitData data;
	
	public Trait(String key, ConfigurationSection config) {
		this.id = key;
		this.name = StringFormatter.formatHex(config.getString("name"));
		for(String s : config.getStringList("description")) {
			desc.add(StringFormatter.formatHex(s));
		}
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
