package net.tfminecraft.RPCharacters.Loaders;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.Plugins.TLibs.Interface.LoaderInterface;
import net.tfminecraft.RPCharacters.Cache;

public class ConfigLoader implements LoaderInterface{

	@Override
	public void load(File configFile) {
		FileConfiguration config = new YamlConfiguration();
        try {
        	config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        if(config.contains("attributes")) {
        	Cache.attributes = config.getStringList("attributes");
        }
        if(config.contains("professions")) {
        	Cache.professions = config.getStringList("professions");
        }
        if(config.contains("editable-trait-types")) {
        	Cache.editableTraits = config.getStringList("editable-trait-types");
        }
        Cache.backgroundTraitTypes = config.getStringList("background-trait-types");
        Cache.continent = config.getString("continent", "Cerrith");
        Cache.requireCharacter = config.getBoolean("require-character", false);
        Cache.noCharacterFreeze = config.getBoolean("no-character-freeze", true);
        Cache.lackingCluesFreeze = config.getBoolean("lacking-clues-freeze", true);
        Cache.skillPointsAdminDebugMessages = config.getBoolean("skill-points-admin-debug-messages", false);
        Cache.startingProfessionFactor = config.getInt("base-profession-factor", -15);

        Cache.defaultCluesRequired = config.getInt("default-clues-required",
        		config.getInt("required-clues", 2));
        Cache.evilCluesRequired = config.getInt("evil-clues-required", Cache.evilCluesRequired);
        if (config.isConfigurationSection("character-description")) {
            Cache.characterDescriptionMinLength = config.getInt("character-description.length-minimum",
                    Cache.characterDescriptionMinLength);
            Cache.characterDescriptionMaxLength = config.getInt("character-description.length-maximum",
                    Cache.characterDescriptionMaxLength);
        }
        Cache.clueMinLength = config.getInt("clue-min-length", 12);
        Cache.clueMaxLength = config.getInt("clue-max-length", 120);
        Cache.maxClues = config.getInt("max-clues", 16);
        Cache.raceClueTemplate = config.getString("race-clue",
        		"This seems to be the mark of {a/an} {race}");

        Map<String, Integer> overrides = new HashMap<>();
        ConfigurationSection overrideSection = config.getConfigurationSection("trait-clue-overrides");
        if (overrideSection != null) {
        	for (String key : overrideSection.getKeys(false)) {
        		overrides.put(key.toLowerCase(), overrideSection.getInt(key));
        	}
        }
        Cache.traitClueOverrides = overrides;

        Cache.spawnedClueTimerHours = config.getInt("spawned-clue-timer", 48);
        Cache.spawnedClueVisualYOffset = config.getDouble("spawned-clue-visual-y-offset", 0.2);
        Cache.spawnedClueLineSpacing = config.getDouble("spawned-clue-line-spacing", 0.22);
        Cache.spawnedClueFirstLineOffset = config.getDouble("spawned-clue-first-line-offset", 0.18);
        Cache.spawnedClueScale = (float) config.getDouble("spawned-clue-scale", 0.6);
        Cache.spawnedClueParticleInterval = config.getInt("spawned-clue-particle-interval", 10);
        Cache.clueSpawnRadius = config.getInt("clue-spawn-radius", 3);
        Cache.spawnedClueLineLength = config.getInt("spawned-clue-line-length", 12);

        if (config.isConfigurationSection("playtime")) {
            Cache.playtimeTickSeconds = Math.max(1, config.getInt("playtime.tick-seconds", 60));
        } else {
            Cache.playtimeTickSeconds = 60;
        }
        if (config.isConfigurationSection("conversation")) {
            Cache.conversationReplyTimeoutSeconds = Math.max(1,
                    config.getInt("conversation.reply-timeout-seconds", 30));
            Cache.conversationPairCooldownHours = Math.max(0,
                    config.getInt("conversation.pair-cooldown-hours", 2));
        } else {
            Cache.conversationReplyTimeoutSeconds = 30;
            Cache.conversationPairCooldownHours = 2;
        }
        validateClueConfig();
	}

	private void validateClueConfig() {
		if (Cache.defaultCluesRequired > Cache.maxClues) {
			Bukkit.getLogger().severe("[RPCharacters] default-clues-required (" + Cache.defaultCluesRequired
					+ ") exceeds max-clues (" + Cache.maxClues + "). Requirement will be capped at max-clues.");
		}
		if (Cache.evilCluesRequired > Cache.maxClues) {
			Bukkit.getLogger().severe("[RPCharacters] evil-clues-required (" + Cache.evilCluesRequired
					+ ") exceeds max-clues (" + Cache.maxClues + "). Requirement will be capped at max-clues.");
		}
		for (Map.Entry<String, Integer> entry : Cache.traitClueOverrides.entrySet()) {
			if (entry.getValue() > Cache.maxClues) {
				Bukkit.getLogger().severe("[RPCharacters] trait-clue-overrides." + entry.getKey() + " ("
						+ entry.getValue() + ") exceeds max-clues (" + Cache.maxClues
						+ "). Requirement will be capped at max-clues.");
			}
		}
	}
}
