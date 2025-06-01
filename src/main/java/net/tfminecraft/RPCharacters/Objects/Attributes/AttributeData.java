package net.tfminecraft.RPCharacters.Objects.Attributes;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

import net.tfminecraft.RPCharacters.Cache;

public class AttributeData {
	private List<AttributeModifier> modifiers = new ArrayList<>();
	
	public AttributeData(ConfigurationSection config) {
		if(config.contains("attribute-modifiers")) {
			for(String s : config.getStringList("attribute-modifiers")) {
				modifiers.add(new AttributeModifier(s));
			}
		}
	}
	public AttributeData() {
		if(Cache.attributes.size() > 0) {
			for(String s : Cache.attributes) {
				modifiers.add(new AttributeModifier(s, 0));
			}
		}
	}
	
	public boolean hasModifiers() {
		if(modifiers.size() > 0) return true;
		return false;
	}
	
	public List<AttributeModifier> getModifiers() {
		return modifiers;
	}

	public boolean hasModifier(AttributeModifier m) {
		for(AttributeModifier mod : modifiers) {
			if(mod.getType().equalsIgnoreCase(m.getType())) return true;
		}
		return false;
	}
	
	private void modify(AttributeModifier m) {
		for(AttributeModifier mod : modifiers) {
			if(mod.getType().equalsIgnoreCase(m.getType())) {
				mod.add(m.getAmount());
			}
		}
	}
	
	public int getAmount(AttributeModifier m) {
		for(AttributeModifier mod : modifiers) {
			if(mod.getType().equalsIgnoreCase(m.getType())) return mod.getAmount();
		}
		return 0;
	}
	
	public void mergeFrom(AttributeData data) {
		for(AttributeModifier m : data.getModifiers()) {
			addModifier(m);
		}
	}
	
	public void addModifier(AttributeModifier m) {
		if(hasModifier(m)) {
			modify(m);
		} else {
			modifiers.add(new AttributeModifier(m.getType(), m.getAmount()));
		}
	}
	public void mergeFromReverse(AttributeData data) {
		for(AttributeModifier m : data.getModifiers()) {
			addModifier(new AttributeModifier(m.getType(), m.getAmount()*-1));
		}
	}
}
