package net.tfminecraft.RPCharacters.Creation.Stages;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import net.tfminecraft.RPCharacters.Creation.CharacterCreation;
import net.tfminecraft.RPCharacters.Creation.Stage;
import net.tfminecraft.RPCharacters.Managers.InventoryManager;

public class SummaryStage extends Stage {
	private final Map<String, String> entries = new LinkedHashMap<>();

	public SummaryStage(Stage s, ConfigurationSection config) {
		copyBaseFields(s);
		ConfigurationSection entriesSection = config.getConfigurationSection("entries");
		if (entriesSection != null) {
			for (String key : entriesSection.getKeys(false)) {
				entries.put(key, entriesSection.getString(key));
			}
		}
	}

	public SummaryStage(SummaryStage another) {
		copyBaseFields(another);
		entries.putAll(another.entries);
	}

	public Map<String, String> getEntries() {
		return entries;
	}

	public void execute(Player p, CharacterCreation cc) {
		if (cc.isCancelled()) {
			return;
		}
		InventoryManager inv = new InventoryManager();
		inv.creationSummaryView(p, cc, this);
	}
}
