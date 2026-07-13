package net.tfminecraft.RPCharacters.Loaders;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.Plugins.TLibs.Interface.LoaderInterface;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.RPCharacters;

public final class InjuryPoolLoader implements LoaderInterface {

	private static final Map<String, Integer> weights = new HashMap<>();

	@Override
	public void load(File configFile) {
		FileConfiguration config = new YamlConfiguration();
		try {
			config.load(configFile);
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}

		weights.clear();
		if (!config.isConfigurationSection("injuries")) {
			return;
		}

		ConfigurationSection section = config.getConfigurationSection("injuries");
		for (String key : section.getKeys(false)) {
			ConfigurationSection injurySection = section.getConfigurationSection(key);
			if (injurySection == null) {
				continue;
			}
			int weight = injurySection.getInt("weight", 0);
			if (weight < 1) {
				RPCharacters.plugin.getLogger().warning("Injury '" + key + "' has invalid weight — skipped.");
				continue;
			}
			if (TraitLoader.getByString(key) == null) {
				RPCharacters.plugin.getLogger().warning("Injury '" + key + "' references unknown trait — skipped.");
				continue;
			}
			weights.put(key.toLowerCase(Locale.ROOT), weight);
		}
	}

	public static Set<String> getPoolTraitIds() {
		return Collections.unmodifiableSet(weights.keySet());
	}

	public static int countRemainingInjuries(Set<String> ownedTraitIds) {
		int count = 0;
		for (String traitId : weights.keySet()) {
			if (!ownsTrait(ownedTraitIds, traitId)) {
				count++;
			}
		}
		return count;
	}

	public static Trait pickRandom(Set<String> ownedTraitIds) {
		List<String> eligible = new ArrayList<>();
		List<Integer> eligibleWeights = new ArrayList<>();
		int total = 0;

		for (Map.Entry<String, Integer> entry : weights.entrySet()) {
			if (ownsTrait(ownedTraitIds, entry.getKey())) {
				continue;
			}
			eligible.add(entry.getKey());
			eligibleWeights.add(entry.getValue());
			total += entry.getValue();
		}

		if (eligible.isEmpty() || total <= 0) {
			return null;
		}

		int roll = ThreadLocalRandom.current().nextInt(total);
		int cumulative = 0;
		for (int i = 0; i < eligible.size(); i++) {
			cumulative += eligibleWeights.get(i);
			if (roll < cumulative) {
				return TraitLoader.getByString(eligible.get(i));
			}
		}

		return TraitLoader.getByString(eligible.get(eligible.size() - 1));
	}

	private static boolean ownsTrait(Set<String> ownedTraitIds, String traitId) {
		for (String owned : ownedTraitIds) {
			if (owned.equalsIgnoreCase(traitId)) {
				return true;
			}
		}
		return false;
	}
}
