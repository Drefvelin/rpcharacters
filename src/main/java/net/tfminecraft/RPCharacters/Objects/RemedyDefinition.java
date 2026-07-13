package net.tfminecraft.RPCharacters.Objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

public final class RemedyDefinition {

	private final String id;
	private final String item;
	private final List<String> traits;

	public RemedyDefinition(String id, ConfigurationSection config) {
		this.id = id;
		this.item = config.getString("item", "");
		List<String> traitList = new ArrayList<>(config.getStringList("traits"));
		if (traitList.isEmpty()) {
			String trait = config.getString("trait", "");
			if (trait != null && !trait.isBlank()) {
				traitList.add(trait);
			}
		}
		this.traits = Collections.unmodifiableList(traitList);
	}

	public String getId() {
		return id;
	}

	public String getItem() {
		return item;
	}

	public List<String> getTraits() {
		return traits;
	}

	/** @deprecated use {@link #getTraits()} */
	@Deprecated
	public String getTrait() {
		return traits.isEmpty() ? "" : traits.get(0);
	}
}
