package net.tfminecraft.RPCharacters.professions;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import net.tfminecraft.RPCharacters.Utils.RPTexts;

public final class ProfessionItemFactory {
	private ProfessionItemFactory() {}

	public static ItemStack fromConfig(ConfigurationSection section) {
		if (section == null) {
			return new ItemStack(Material.BARRIER);
		}
		Material material = Material.valueOf(section.getString("material", "BARRIER").toUpperCase());
		ItemStack item = new ItemStack(material, 1);
		ItemMeta meta = item.getItemMeta();
		if (section.contains("name")) {
			meta.setDisplayName(formatItemText(section.getString("name")));
		}
		if (section.contains("model_data")) {
			meta.setCustomModelData(section.getInt("model_data"));
		}
		if (section.contains("enchants")) {
			for (String enchantSpec : section.getStringList("enchants")) {
				String[] parts = enchantSpec.split("\\.");
				meta.addEnchant(Enchantment.getByKey(NamespacedKey.minecraft(parts[0])),
						Integer.parseInt(parts[1]), true);
			}
		}
		if (section.getBoolean("hide_enchants", false)) {
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		}
		List<String> lore = new ArrayList<>();
		for (String line : section.getStringList("lore")) {
			lore.add(formatItemText(line));
		}
		if (!lore.isEmpty()) {
			meta.setLore(lore);
		}
		item.setItemMeta(meta);
		return item;
	}

	private static String formatItemText(String raw) {
		if (raw == null || raw.isEmpty()) {
			return raw;
		}
		return RPTexts.formatGui(raw);
	}
}
