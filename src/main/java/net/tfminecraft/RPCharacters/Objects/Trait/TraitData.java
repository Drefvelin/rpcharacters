package net.tfminecraft.RPCharacters.Objects.Trait;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

import net.tfminecraft.RPCharacters.Creation.Dependency;
import net.tfminecraft.RPCharacters.Objects.Attributes.AttributeData;

public class TraitData {
	private int cost;
	private List<String> exclusive = new ArrayList<>();
	private String key;
	
	private Dependency dependency;
	private AttributeData data;
	
	public TraitData(ConfigurationSection config) {
		key = config.getString("key");
		if(config.contains("cost")) {
			cost = config.getInt("cost");
		} else {
			cost = 0;
		}
		if(config.contains("mutually-exclusive")) {
			exclusive = config.getStringList("mutually-exclusive");
		}
		if(config.contains("dependency")) {
			dependency = new Dependency(config.getConfigurationSection("dependency"));
		}
		data = new AttributeData(config);
	}
	
	public boolean hasDependency() {
		if(dependency != null) return true;
		return false;
	}
	
	public Dependency getDependency() {
		return dependency;
	}

	public void setDependency(Dependency dependency) {
		this.dependency = dependency;
	}
	public String getKey() {
		return key;
	}
	public boolean hasCost() {
		if(cost != 0) return true;
		return false;
	}

	public int getCost() {
		return cost;
	}
	public boolean hasExclusives() {
		if(exclusive.size() > 0) return true;
		return false;
	}

	public List<String> getExclusive() {
		return exclusive;
	}
	
	public boolean isExclusive(String s) {
		for(String e : exclusive) {
			if(e.equalsIgnoreCase(s)) return true;
		}
		return false;
	}

	public AttributeData getAttributeData() {
		return data;
	}
	
	
}
