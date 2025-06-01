package net.tfminecraft.RPCharacters.Creation;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;

public class Dependency {
	private String type;
	private String mode;
	private List<String> dependencies = new ArrayList<>();
	
	public Dependency(ConfigurationSection config) {
		this.type = config.getString("type");
		this.mode = config.getString("mode");
		this.dependencies = config.getStringList("depends-on");
	}
	
	public String getType() {
		return type;
	}
	public String getMode() {
		return mode;
	}
	public List<String> getDependencies() {
		return dependencies;
	}
	
	public boolean check(RPCharacter c) {
		if(type.equalsIgnoreCase("trait")) {
			if(mode.equalsIgnoreCase("all")) {
				for(String s : dependencies) {
					boolean found = false;
					for(Trait t : c.getTraits()) {
						if(t.getId().equalsIgnoreCase(s)) found = true;
					}
					if(!found) return false;
				}
				return true;
			}
			if(mode.equalsIgnoreCase("one-or-more")) {
				for(String s : dependencies) {
					for(Trait t : c.getTraits()) {
						if(t.getId().equalsIgnoreCase(s)) return true;
					}
				}
			}
		} else if(type.equalsIgnoreCase("race")) {
			for(String s : dependencies) {
				if(c.getRace().getId().equalsIgnoreCase(s)) return true;
			}
		}
		return false;
	}
}
