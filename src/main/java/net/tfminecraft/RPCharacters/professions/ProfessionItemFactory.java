package net.tfminecraft.RPCharacters.professions;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.Plugins.TLibs.TLibs;
import net.tfminecraft.RPCharacters.Utils.RPTexts;

public final class ProfessionItemFactory {
	private ProfessionItemFactory() {}

	public static ItemStack fromNode(Object node) {
		if (node == null) {
			return new ItemStack(Material.BARRIER);
		}
		if (node instanceof String path) {
			ItemStack item = fromPath(path);
			return item != null ? item : new ItemStack(Material.BARRIER);
		}
		if (node instanceof ConfigurationSection section) {
			return fromConfig(section);
		}
		return new ItemStack(Material.BARRIER);
	}

	public static ItemStack fromConfig(ConfigurationSection section) {
		if (section == null) {
			return new ItemStack(Material.BARRIER);
		}
		ItemStack item = resolveBase(section);
		ItemMeta meta = item.getItemMeta();
		if (meta == null) {
			return item;
		}
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

	private static ItemStack resolveBase(ConfigurationSection section) {
		String path = section.getString("item");
		if (path == null || path.isBlank()) {
			path = section.getString("path");
		}
		Material fallbackMaterial = null;
		if (section.contains("material")) {
			try {
				fallbackMaterial = Material.valueOf(section.getString("material", "BARRIER").toUpperCase());
			} catch (IllegalArgumentException ignored) {
				fallbackMaterial = Material.BARRIER;
			}
		}
		if (path != null && !path.isBlank()) {
			ItemStack fromPath = fromPath(path);
			if (fromPath != null) {
				return fromPath;
			}
			if (fallbackMaterial != null) {
				return new ItemStack(fallbackMaterial, 1);
			}
			Bukkit.getLogger().warning("[RPCharacters] Profession item path unresolved: " + path);
			return new ItemStack(Material.BARRIER);
		}
		if (fallbackMaterial != null) {
			return new ItemStack(fallbackMaterial, 1);
		}
		return new ItemStack(Material.BARRIER);
	}

	private static ItemStack fromPath(String ref) {
		String normalized = normalizePath(ref);
		if (normalized.isBlank()) {
			return null;
		}
		try {
			ItemStack built = TLibs.getItemAPI().getCreator().getItemFromPath(normalized);
			if (built == null || built.getType() == Material.AIR) {
				return null;
			}
			return built.clone();
		} catch (Exception ex) {
			return null;
		}
	}

	private static String normalizePath(String ref) {
		if (ref == null || ref.isBlank()) {
			return "";
		}
		String trimmed = ref.trim();
		if (trimmed.toLowerCase().startsWith("vanilla.")) {
			return "v." + trimmed.substring("vanilla.".length()).toLowerCase();
		}
		return trimmed;
	}

	private static String formatItemText(String raw) {
		if (raw == null || raw.isEmpty()) {
			return raw;
		}
		return RPTexts.formatGui(raw);
	}
}
