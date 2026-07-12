package net.tfminecraft.RPCharacters.Objects.Races;

import org.bukkit.configuration.ConfigurationSection;

import net.tfminecraft.RPCharacters.Objects.Attributes.AttributeData;

public class RaceData {
	
	private String key;
	
	private AttributeData data;
	private int ageMax = 100;

	public RaceData(ConfigurationSection config) {
		this.key = config.getString("key");
		this.data = new AttributeData(config);
		this.ageMax = config.getInt("age-max", 100);
	}
	
	public String getKey() {
		return key;
	}
	
	public AttributeData getAttributeData() {
		return data;
	}

	public int getAgeMax() {
		return ageMax;
	}

}
