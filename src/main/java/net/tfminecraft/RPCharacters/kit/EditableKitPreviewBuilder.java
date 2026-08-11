package net.tfminecraft.RPCharacters.kit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Loaders.KitLoader;

/**
 * Resolve editable kit.yml lines into catalog rows with optional ItemStack preview.
 * Call on the main thread (TLibs / Bukkit ItemMeta are not async-safe).
 */
public final class EditableKitPreviewBuilder {

	private EditableKitPreviewBuilder() {}

	public static final class Preview {
		private final String displayName;
		private final List<String> lore;
		private final String material;
		private final Integer customModelData;

		public Preview(
				String displayName,
				List<String> lore,
				String material,
				Integer customModelData
		) {
			this.displayName = displayName != null ? displayName : "";
			this.lore = lore != null
					? Collections.unmodifiableList(new ArrayList<>(lore))
					: List.of();
			this.material = material != null ? material : "";
			this.customModelData = customModelData;
		}

		public String getDisplayName() {
			return displayName;
		}

		public List<String> getLore() {
			return lore;
		}

		public String getMaterial() {
			return material;
		}

		public Integer getCustomModelData() {
			return customModelData;
		}
	}

	public static final class Row {
		private final String grantKitId;
		private final String kitKey;
		private final String path;
		private final int amount;
		private final String skinPng;
		private final String baseSet;
		private final Preview preview;

		public Row(
				String grantKitId,
				String kitKey,
				String path,
				int amount,
				String skinPng,
				String baseSet,
				Preview preview
		) {
			this.grantKitId = grantKitId != null ? grantKitId : "";
			this.kitKey = kitKey;
			this.path = path;
			this.amount = amount;
			this.skinPng = skinPng != null ? skinPng : "";
			this.baseSet = baseSet != null ? baseSet : "";
			this.preview = preview;
		}

		public String getGrantKitId() {
			return grantKitId;
		}

		public String getKitKey() {
			return kitKey;
		}

		public String getPath() {
			return path;
		}

		public int getAmount() {
			return amount;
		}

		public String getSkinPng() {
			return skinPng;
		}

		public String getBaseSet() {
			return baseSet;
		}

		public Preview getPreview() {
			return preview;
		}
	}

	public static List<Row> build() {
		List<Row> out = new ArrayList<>();
		for (KitDefinition kit : KitLoader.getKits().values()) {
			if (kit == null) {
				continue;
			}
			for (KitItemDefinition def : kit.getItems()) {
				if (def == null || !def.isEditable()) {
					continue;
				}
				KitEditableSpec editable = def.getEditable();
				String path = def.getPath() != null ? def.getPath().trim() : "";
				if (path.isEmpty()) {
					continue;
				}
				out.add(new Row(
						kit.getId(),
						kitKeyFromPath(path),
						path,
						def.getAmount(),
						editable.getSkinPng(),
						editable.getBaseSet(),
						resolvePreview(path)
				));
			}
		}
		return out;
	}

	public static String kitKeyFromPath(String path) {
		int dot = path.lastIndexOf('.');
		String segment = dot >= 0 ? path.substring(dot + 1) : path;
		return segment.toLowerCase(Locale.ROOT);
	}

	private static Preview resolvePreview(String path) {
		ItemStack stack;
		try {
			stack = TLibs.getItemAPI().getCreator().getItemFromPath(path);
		} catch (Exception e) {
			RPCharacters.plugin.getLogger().warning(
					"[editable-kit] preview path '" + path + "' threw: " + e.getMessage()
			);
			return null;
		}
		if (stack == null || stack.getType().isAir()) {
			RPCharacters.plugin.getLogger().warning(
					"[editable-kit] preview path '" + path + "' unresolved (null/air) — omitting preview."
			);
			return null;
		}

		String displayName = strip(StringFormatter.getName(stack));
		List<String> lore = List.of();
		ItemMeta meta = stack.getItemMeta();
		if (meta != null && meta.hasLore() && meta.getLore() != null) {
			List<String> stripped = new ArrayList<>();
			for (String line : meta.getLore()) {
				stripped.add(strip(line));
			}
			lore = stripped;
		}
		Integer cmd = null;
		if (meta != null && meta.hasCustomModelData()) {
			cmd = Integer.valueOf(meta.getCustomModelData());
		}
		return new Preview(
				displayName,
				lore,
				stack.getType().name(),
				cmd
		);
	}

	private static String strip(String raw) {
		if (raw == null) {
			return "";
		}
		return ChatColor.stripColor(raw);
	}
}
