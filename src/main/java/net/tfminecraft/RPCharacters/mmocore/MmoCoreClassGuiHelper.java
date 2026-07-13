package net.tfminecraft.RPCharacters.mmocore;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.player.profess.PlayerClass;
import net.tfminecraft.RPCharacters.Objects.SelectableItem;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Utils.RPTexts;

public final class MmoCoreClassGuiHelper {

	private static Map<String, Integer> cachedSlots;

	private MmoCoreClassGuiHelper() {}

	public static class ClassGuiData {
		private final List<SelectableItem> options;
		private final List<Integer> slots;

		public ClassGuiData(List<SelectableItem> options, List<Integer> slots) {
			this.options = options;
			this.slots = slots;
		}

		public List<SelectableItem> getOptions() {
			return options;
		}

		public List<Integer> getSlots() {
			return slots;
		}
	}

	public static ClassGuiData buildClassOptions(int guiSize) {
		List<PlayerClass> classes = new ArrayList<>();
		for (PlayerClass playerClass : MMOCore.plugin.classManager.getAll()) {
			if (isClassDisplayed(playerClass)) {
				classes.add(playerClass);
			}
		}
		classes.sort(Comparator.comparingInt(PlayerClass::getDisplayOrder));

		Map<String, Integer> configuredSlots = loadClassSlots();
		int[] fallbackSlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
				28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
		int maxUsable = Math.max(0, guiSize - 10);
		int fallbackIndex = 0;

		List<SelectableItem> options = new ArrayList<>();
		List<Integer> slots = new ArrayList<>();

		for (PlayerClass playerClass : classes) {
			options.add(new SelectableItem(playerClass));
			String classId = playerClass.getId().toLowerCase();
			Integer slot = configuredSlots.get(classId);
			if (slot == null) {
				while (fallbackIndex < fallbackSlots.length) {
					int candidate = fallbackSlots[fallbackIndex++];
					if (candidate < maxUsable && !slots.contains(candidate)) {
						slot = candidate;
						RPCharacters.plugin.getLogger().warning(
								"Class " + classId + " has no slot in class-select.yml, using slot " + candidate);
						break;
					}
				}
			}
			if (slot != null) {
				slots.add(slot);
			}
		}

		return new ClassGuiData(options, slots);
	}

	public static boolean isClassDisplayed(PlayerClass playerClass) {
		File classFile = new File(MMOCore.plugin.getDataFolder(),
				"classes/" + playerClass.getId().toLowerCase() + ".yml");
		if (!classFile.isFile()) {
			return true;
		}
		YamlConfiguration config = YamlConfiguration.loadConfiguration(classFile);
		if (config.contains("options.display")) {
			return config.getBoolean("options.display");
		}
		return true;
	}

	public static List<String> buildClassLore(PlayerClass playerClass) {
		List<String> lore = new ArrayList<>();
		if (playerClass.getDescription() != null) {
			for (String line : playerClass.getDescription()) {
				lore.add(formatLine(line));
			}
		}
		List<String> attributeLore = playerClass.getAttributeDescription();
		if (attributeLore != null && !attributeLore.isEmpty()) {
			lore.add(RPTexts.spacer());
			for (String line : attributeLore) {
				lore.add(formatLine(line));
			}
		}
		return lore;
	}

	private static String formatLine(String line) {
		if (line == null) {
			return "";
		}
		return StringFormatter.formatHex(line.replace('&', '\u00A7'));
	}

	private static Map<String, Integer> loadClassSlots() {
		if (cachedSlots != null) {
			return cachedSlots;
		}
		Map<String, Integer> slots = new HashMap<>();
		File file = new File(MMOCore.plugin.getDataFolder(), "gui/class-select.yml");
		if (!file.isFile()) {
			cachedSlots = slots;
			return slots;
		}
		YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
		for (String key : config.getKeys(false)) {
			ConfigurationSection section = config.getConfigurationSection(key);
			if (section == null) {
				continue;
			}
			String function = section.getString("function", "");
			if (!function.startsWith("class-")) {
				continue;
			}
			String classId = function.substring("class-".length()).toLowerCase();
			List<Integer> slotList = section.getIntegerList("slots");
			if (!slotList.isEmpty()) {
				slots.put(classId, slotList.get(0));
			}
		}
		cachedSlots = slots;
		return slots;
	}

	public static void invalidateSlotCache() {
		cachedSlots = null;
	}
}
