package net.tfminecraft.RPCharacters.kit;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import io.lumine.mythic.lib.api.item.NBTItem;
import me.Plugins.TLibs.TLibs;
import me.Plugins.TLibs.Objects.API.SubAPI.ArmorMerger;
import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;

/**
 * Rebuild starter kit editable items: MI base → optional AS skin → append custom lore.
 */
public final class KitCustomiseApplyService {

	public static final String PDC_KEY = "kit_customise";

	private KitCustomiseApplyService() {}

	public static void applyStoredForPlayer(Player player, RPCharacter character) {
		if (player == null || character == null) {
			return;
		}
		for (KitCustomiseData data : character.getKitCustomisations().values()) {
			if (data == null || data.getKitKey().isBlank()) {
				continue;
			}
			applyToInventory(player, data);
		}
	}

	public static boolean applyToInventory(Player player, KitCustomiseData data) {
		if (player == null || data == null) {
			return false;
		}
		ItemStack built = buildStack(data);
		if (built == null || built.getType().isAir()) {
			RPCharacters.plugin.getLogger().warning(
					"[kit-customise] could not build stack for " + data.getKitKey()
			);
			return false;
		}
		String itemId = itemIdFromPath(data.getPath());
		if (itemId.isBlank()) {
			itemId = data.getKitKey().toUpperCase();
		}
		PlayerInventory inv = player.getInventory();
		boolean replaced = false;
		ItemStack[] contents = inv.getContents();
		for (int i = 0; i < contents.length; i++) {
			ItemStack slot = contents[i];
			if (matchesKitItem(slot, itemId, data.getKitKey())) {
				built.setAmount(Math.max(1, slot.getAmount()));
				inv.setItem(i, built.clone());
				replaced = true;
			}
		}
		ItemStack off = inv.getItemInOffHand();
		if (matchesKitItem(off, itemId, data.getKitKey())) {
			built.setAmount(Math.max(1, off.getAmount()));
			inv.setItemInOffHand(built.clone());
			replaced = true;
		}
		return replaced;
	}

	public static ItemStack buildStack(KitCustomiseData data) {
		if (data == null) {
			return null;
		}
		String path = data.getPath();
		if (path == null || path.isBlank()) {
			path = "m.tools.IRON_HUNTING_KNIFE";
		}
		ItemStack stack;
		try {
			stack = TLibs.getItemAPI().getCreator().getItemFromPath(path);
		} catch (Exception e) {
			RPCharacters.plugin.getLogger().warning(
					"[kit-customise] path '" + path + "' threw: " + e.getMessage()
			);
			return null;
		}
		if (stack == null || stack.getType().isAir()) {
			return null;
		}
		stack = stack.clone();

		String slug = data.getSkinSlug();
		String colouredName = formatDisplayName(data);
		if (slug != null && !slug.isBlank()) {
			String ns = data.getIaNamespace();
			if (ns == null || ns.isBlank()) {
				ns = "tfmc_submissions";
			}
			String iaPath = "ia." + ns + ":" + slug;
			Optional<String> name = Optional.empty();
			if (colouredName != null && !colouredName.isBlank()) {
				name = Optional.of(colouredName);
			}
			try {
				ArmorMerger merger = TLibs.getItemAPI().getArmorMerger();
				stack = merger.merge(stack, name, iaPath);
			} catch (Exception e) {
				RPCharacters.plugin.getLogger().warning(
						"[kit-customise] skin merge failed for " + slug + ": " + e.getMessage()
				);
			}
		} else if (colouredName != null && !colouredName.isBlank()) {
			ItemMeta meta = stack.getItemMeta();
			if (meta != null) {
				meta.setDisplayName(colouredName);
				stack.setItemMeta(meta);
			}
		}

		List<String> custom = data.getLore();
		if (custom != null && !custom.isEmpty()) {
			ItemMeta meta = stack.getItemMeta();
			if (meta != null) {
				List<String> lore = meta.hasLore() && meta.getLore() != null
						? new ArrayList<>(meta.getLore())
						: new ArrayList<>();
				for (String line : custom) {
					if (line == null || line.isBlank()) {
						continue;
					}
					lore.add(formatLoreLine(line));
				}
				meta.setLore(lore);
				stack.setItemMeta(meta);
			}
		}

		ItemMeta meta = stack.getItemMeta();
		if (meta != null && RPCharacters.plugin != null) {
			NamespacedKey key = new NamespacedKey(RPCharacters.plugin, PDC_KEY);
			meta.getPersistentDataContainer().set(
					key, PersistentDataType.STRING, data.getKitKey()
			);
			stack.setItemMeta(meta);
		}
		return stack;
	}

	static String formatDisplayName(KitCustomiseData data) {
		if (data == null || data.getDisplayName() == null || data.getDisplayName().isBlank()) {
			return null;
		}
		String plain = data.getDisplayName().trim();
		List<String> colours = data.getNameColours();
		if (colours == null || colours.isEmpty()) {
			return plain;
		}
		try {
			return StringFormatter.applyColourGradient(plain, new ArrayList<>(colours));
		} catch (Exception e) {
			return plain;
		}
	}

	static String formatLoreLine(String line) {
		if (line == null) {
			return "";
		}
		try {
			return StringFormatter.formatHex(line.replace('&', '\u00A7'));
		} catch (Exception e) {
			return line;
		}
	}

	static boolean matchesKitItem(ItemStack stack, String itemId, String kitKey) {
		if (stack == null || stack.getType() == Material.AIR) {
			return false;
		}
		if (RPCharacters.plugin != null) {
			ItemMeta meta = stack.getItemMeta();
			if (meta != null) {
				NamespacedKey key = new NamespacedKey(RPCharacters.plugin, PDC_KEY);
				String tagged = meta.getPersistentDataContainer().get(key, PersistentDataType.STRING);
				if (tagged != null && tagged.equalsIgnoreCase(kitKey)) {
					return true;
				}
			}
		}
		try {
			NBTItem nbt = NBTItem.get(stack);
			if (!nbt.hasType()) {
				return false;
			}
			String id = nbt.getString("MMOITEMS_ITEM_ID");
			return id != null && id.equalsIgnoreCase(itemId);
		} catch (Exception e) {
			return false;
		}
	}

	static String itemIdFromPath(String path) {
		if (path == null || path.isBlank()) {
			return "";
		}
		int dot = path.lastIndexOf('.');
		return dot >= 0 ? path.substring(dot + 1) : path;
	}
}
