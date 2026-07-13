package net.tfminecraft.RPCharacters.professions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.inventory.ItemStack;

public final class ProfessionDefinition {
	private final String id;
	private final String name;
	private final ItemStack menuItem;
	private final List<ProfessionUpgradeDefinition> upgrades;

	public ProfessionDefinition(String id, String name, ItemStack menuItem,
			List<ProfessionUpgradeDefinition> upgrades) {
		this.id = id;
		this.name = name;
		this.menuItem = menuItem;
		this.upgrades = upgrades != null ? upgrades : new ArrayList<>();
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public ItemStack getMenuItem() {
		return menuItem.clone();
	}

	public List<ProfessionUpgradeDefinition> getUpgrades() {
		return Collections.unmodifiableList(upgrades);
	}
}
