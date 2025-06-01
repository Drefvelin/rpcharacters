package net.tfminecraft.RPCharacters.Creation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
		return checkExclude(c, "noneTrait");
	}
	public boolean checkExclude(RPCharacter c, String id) {
		if (type.equalsIgnoreCase("trait")) {
			Set<String> traitIds = c.getTraits().stream()
									.map(Trait::getId)
									.filter(traitId -> !traitId.equalsIgnoreCase(id))
									.collect(Collectors.toSet());

			if (mode.equalsIgnoreCase("all")) {
				for (String s : dependencies) {
					if (!traitIds.contains(s)) return false;
				}
				return true;
			}
			if (mode.equalsIgnoreCase("one-or-more")) {
				for (String s : dependencies) {
					if(traitIds.contains(s)) return true;
				}
				return false;
			}
		} else if (type.equalsIgnoreCase("race")) {
			for (String s : dependencies) {
				if (c.getRace().getId().equalsIgnoreCase(s)) return true;
			}
		}
		return false;
	}
	@Override
	public String toString() {
		return "Dependency{" +
				"type='" + type + '\'' +
				", mode='" + mode + '\'' +
				", dependencies=" + dependencies +
				'}';
	}
}
