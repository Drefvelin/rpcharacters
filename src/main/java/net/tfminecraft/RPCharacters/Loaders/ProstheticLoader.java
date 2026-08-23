package net.tfminecraft.RPCharacters.Loaders;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import me.Plugins.TLibs.Interface.LoaderInterface;
import me.Plugins.TLibs.TLibs;
import net.tfminecraft.RPCharacters.Objects.FuelTemplate;
import net.tfminecraft.RPCharacters.Objects.ProstheticReplacement;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.RPCharacters;

public final class ProstheticLoader implements LoaderInterface {

	private static final Map<String, ProstheticReplacement> byInjuryId = new HashMap<>();
	private static final Map<String, ProstheticReplacement> byProstheticId = new HashMap<>();
	private static final Map<String, List<ProstheticReplacement>> byInstallItem = new HashMap<>();
	private static final List<ProstheticReplacement> loadOrder = new ArrayList<>();

	@Override
	public void load(File configFile) {
		FileConfiguration config = new YamlConfiguration();
		try {
			config.load(configFile);
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}

		byInjuryId.clear();
		byProstheticId.clear();
		byInstallItem.clear();
		loadOrder.clear();

		if (!config.isConfigurationSection("replacements")) {
			return;
		}

		ConfigurationSection replacements = config.getConfigurationSection("replacements");
		for (String injuryId : replacements.getKeys(false)) {
			ConfigurationSection section = replacements.getConfigurationSection(injuryId);
			if (section == null) {
				continue;
			}

			Trait injuryTrait = TraitLoader.getByString(injuryId);
			if (injuryTrait == null) {
				RPCharacters.plugin.getLogger().warning(
						"Prosthetic replacement '" + injuryId + "' references unknown injury trait and was skipped.");
				continue;
			}
			if (!injuryTrait.getTraitData().isInjuryKey()) {
				RPCharacters.plugin.getLogger().warning(
						"Prosthetic replacement '" + injuryId + "' is not key injury and was skipped.");
				continue;
			}
			if (injuryTrait.getTraitData().hasDuration()) {
				RPCharacters.plugin.getLogger().warning(
						"Prosthetic replacement '" + injuryId + "' must be a permanent injury and was skipped.");
				continue;
			}

			String installItem = section.getString("install-item");
			if (installItem == null || installItem.isBlank()) {
				RPCharacters.plugin.getLogger().warning(
						"Prosthetic replacement '" + injuryId + "' has no install-item and was skipped.");
				continue;
			}

			List<String> tiers = section.getStringList("tiers");
			if (tiers == null || tiers.isEmpty()) {
				RPCharacters.plugin.getLogger().warning(
						"Prosthetic replacement '" + injuryId + "' has no tiers and was skipped.");
				continue;
			}

			List<String> normalizedTiers = new ArrayList<>();
			boolean valid = true;
			for (String tierId : tiers) {
				if (tierId == null || tierId.isBlank()) {
					continue;
				}
				Trait prostheticTrait = TraitLoader.getByString(tierId);
				if (prostheticTrait == null) {
					RPCharacters.plugin.getLogger().warning(
							"Prosthetic tier '" + tierId + "' for '" + injuryId + "' is unknown and was skipped.");
					valid = false;
					break;
				}
				if (!prostheticTrait.getTraitData().isProstheticKey()) {
					RPCharacters.plugin.getLogger().warning(
							"Prosthetic tier '" + tierId + "' for '" + injuryId
									+ "' is not key prosthetic and was skipped.");
					valid = false;
					break;
				}
				if (prostheticTrait.hasFuelTemplate()) {
					FuelTemplate template = FuelTemplateLoader.getByString(prostheticTrait.getFuelTemplateId());
					if (template == null) {
						RPCharacters.plugin.getLogger().warning(
								"Prosthetic tier '" + tierId + "' references unknown fuel template '"
										+ prostheticTrait.getFuelTemplateId() + "' and was skipped.");
						valid = false;
						break;
					}
					if (prostheticTrait.getFuelCapacity() <= 0) {
						RPCharacters.plugin.getLogger().warning(
								"Prosthetic tier '" + tierId + "' has invalid fuel-capacity and was skipped.");
						valid = false;
						break;
					}
					if (!prostheticTrait.hasPoweredVariant() || prostheticTrait.getDepoweredVariant() == null) {
						RPCharacters.plugin.getLogger().warning(
								"Prosthetic tier '" + tierId + "' is fueled but missing powered/depowered blocks.");
						valid = false;
						break;
					}
				}
				normalizedTiers.add(tierId.toLowerCase(Locale.ROOT));
			}

			if (!valid || normalizedTiers.isEmpty()) {
				continue;
			}

			String injuryKey = injuryId.toLowerCase(Locale.ROOT);
			ProstheticReplacement replacement = new ProstheticReplacement(
					injuryKey,
					installItem,
					normalizedTiers);
			byInjuryId.put(injuryKey, replacement);
			loadOrder.add(replacement);
			for (String tierId : normalizedTiers) {
				byProstheticId.put(tierId, replacement);
			}
			String itemKey = installItem.toLowerCase(Locale.ROOT);
			byInstallItem.computeIfAbsent(itemKey, ignored -> new ArrayList<>()).add(replacement);
		}
	}

	public static ProstheticReplacement getReplacement(String permanentInjuryId) {
		if (permanentInjuryId == null || permanentInjuryId.isBlank()) {
			return null;
		}
		return byInjuryId.get(permanentInjuryId.toLowerCase(Locale.ROOT));
	}

	public static ProstheticReplacement getReplacementForProsthetic(String prostheticTraitId) {
		if (prostheticTraitId == null || prostheticTraitId.isBlank()) {
			return null;
		}
		return byProstheticId.get(prostheticTraitId.toLowerCase(Locale.ROOT));
	}

	public static int getTierIndex(String prostheticTraitId) {
		ProstheticReplacement replacement = getReplacementForProsthetic(prostheticTraitId);
		if (replacement == null) {
			return -1;
		}
		return replacement.getTierIndex(prostheticTraitId);
	}

	public static String getNextTierId(String currentProstheticId) {
		ProstheticReplacement replacement = getReplacementForProsthetic(currentProstheticId);
		if (replacement == null) {
			return null;
		}
		return replacement.getNextTierId(currentProstheticId);
	}

	public static List<ProstheticReplacement> getByInstallItem(String itemPath) {
		if (itemPath == null || itemPath.isBlank()) {
			return List.of();
		}
		List<ProstheticReplacement> matches = byInstallItem.get(itemPath.toLowerCase(Locale.ROOT));
		if (matches == null) {
			return List.of();
		}
		return Collections.unmodifiableList(matches);
	}

	public static List<ProstheticReplacement> resolveForItem(ItemStack item) {
		if (item == null || item.getType().isAir()) {
			return List.of();
		}
		List<ProstheticReplacement> matches = new ArrayList<>();
		for (ProstheticReplacement replacement : loadOrder) {
			if (TLibs.getItemAPI().getChecker().checkItemWithPath(item, replacement.getInstallItem())) {
				matches.add(replacement);
			}
		}
		return Collections.unmodifiableList(matches);
	}
}
