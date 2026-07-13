package net.tfminecraft.RPCharacters.clues.discovery;

import java.util.UUID;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.tfminecraft.RPCharacters.Loaders.ClueDiscoveryLoader;
import net.tfminecraft.RPCharacters.Objects.SpawnedClue;
import net.tfminecraft.RPCharacters.Utils.ClueFormatter;
import net.tfminecraft.RPCharacters.chat.smart.MuffleEngine;
import net.tfminecraft.RPCharacters.chat.smart.SmartMessageSettings;

public final class ClueReadabilityResolver {

	private ClueReadabilityResolver() {}

	public static String resolve(SpawnedClue clue, UUID viewerCharacterId) {
		return resolve(clue, viewerCharacterId, false);
	}

	public static String resolve(SpawnedClue clue, UUID viewerCharacterId, boolean adminMode) {
		if (clue == null) return "";
		if (adminMode) {
			return resolveForAdmin(clue);
		}
		ClueDiscoverySettings settings = ClueDiscoveryLoader.getSettings();
		double readability = clue.getPotency();

		if (readability < settings.getPotencyMinForReadable()) {
			return format(settings.getReadabilityTooFaintPlaceholder());
		}

		String plain = ClueFormatter.stripColor(clue.getClueText());
		if (readability >= settings.getReadabilityFullClarity()) {
			return ClueFormatter.format(plain);
		}

		SmartMessageSettings muffleSettings = buildMuffleSettings(settings);
		UUID utteranceId = clue.getId();
		UUID viewerId = viewerCharacterId != null ? viewerCharacterId : utteranceId;
		String muffled = MuffleEngine.apply(plain, readability, utteranceId, viewerId, muffleSettings);
		if (muffled == null || muffled.isBlank()) {
			return format(settings.getReadabilityTooFaintPlaceholder());
		}
		return muffled.startsWith("§") ? muffled : ClueFormatter.format(muffled);
	}

	/** Full clue text for admin mode, with actual potency shown for behind-the-scenes context. */
	public static String resolveForAdmin(SpawnedClue clue) {
		if (clue == null) return "";
		String plain = ClueFormatter.stripColor(clue.getClueText());
		int potencyPct = (int) Math.round(clue.getPotency() * 100);
		return ClueFormatter.format("§8[§eadmin §7" + potencyPct + "%§8] §r" + plain);
	}

	private static SmartMessageSettings buildMuffleSettings(ClueDiscoverySettings settings) {
		SmartMessageSettings muffle = new SmartMessageSettings();
		muffle.setMinAudible(settings.getReadabilityMinAudible());
		muffle.setFullClarity(settings.getReadabilityFullClarity());
		muffle.setLowIntelligibilityThreshold(settings.getPotencyMinForReadable());
		muffle.setLowIntelligibilityPlaceholder(settings.getReadabilityTooFaintPlaceholder());
		muffle.setGenericMuffle(true);
		return muffle;
	}

	private static String format(String raw) {
		if (raw == null || raw.isBlank()) return "";
		return StringFormatter.formatHex(raw.replace('&', '\u00A7'));
	}
}
