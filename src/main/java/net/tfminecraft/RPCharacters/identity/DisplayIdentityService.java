package net.tfminecraft.RPCharacters.identity;

import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Utils.ClueFormatter;

public final class DisplayIdentityService {

	private DisplayIdentityService() {}

	public static String resolveCharacterName(Player player) {
		RPCharacter character = getActiveCharacter(player);
		return character != null && character.getName() != null ? character.getName() : "";
	}

	public static String resolveCharacterName(RPCharacter character) {
		return character != null && character.getName() != null ? character.getName() : "";
	}

	public static String resolveEffectivePlain(Player player) {
		RPCharacter character = getActiveCharacter(player);
		return resolveEffectivePlain(character);
	}

	public static String resolveEffectivePlain(RPCharacter character) {
		if (character == null) {
			return "";
		}
		return character.getEffectiveDisplayPlain();
	}

	public static String resolveDisplayNoMask(Player player) {
		RPCharacter character = getActiveCharacter(player);
		return resolveDisplayNoMask(character);
	}

	public static String resolveDisplayNoMask(RPCharacter character) {
		if (character == null) {
			return Cache.personaNoCharacterFallback;
		}
		String plain = character.getEffectiveDisplayPlain();
		if (plain.isEmpty()) {
			return "";
		}
		NameColour colour = character.getNameColour();
		List<String> hexCodes = colour != null ? colour.getHexCodes() : Collections.emptyList();
		return StringFormatter.applyColourGradient(plain, hexCodes);
	}

	public static String resolveDisplay(Player player) {
		if (MaskService.isMasked(player)) {
			return MaskService.getMaskedLabel();
		}
		return resolveDisplayNoMask(player);
	}

	public static String resolveDisplay(RPCharacter character) {
		if (character != null && character.getOwner() != null && MaskService.isMasked(character.getOwner())) {
			return MaskService.getMaskedLabel();
		}
		return resolveDisplayNoMask(character);
	}

	public static boolean isMasked(Player player) {
		return MaskService.isMasked(player);
	}

	private static RPCharacter getActiveCharacter(Player player) {
		if (player == null) {
			return null;
		}
		PlayerData data = PlayerManager.get(player);
		return data != null ? data.getActiveCharacter() : null;
	}
}
