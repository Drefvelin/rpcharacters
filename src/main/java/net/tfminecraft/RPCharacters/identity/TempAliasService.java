package net.tfminecraft.RPCharacters.identity;

import org.bukkit.entity.Player;

import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Utils.ClueFormatter;
import net.tfminecraft.RPCharacters.Utils.RPTexts;
import net.tfminecraft.RPCharacters.persona.AliasValidator;

public final class TempAliasService {

	private TempAliasService() {}

	public static String getPlain(Player player) {
		PlayerData data = PlayerManager.get(player);
		if (data == null) {
			return "";
		}
		String alias = data.getTempAlias();
		return alias != null ? alias : "";
	}

	public static String set(Player player, String input) {
		String error = AliasValidator.validate(input);
		if (error != null) {
			return error;
		}
		PlayerData data = PlayerManager.get(player);
		if (data == null) {
			return RPTexts.formatDisplay(RPTexts.ERROR + "Player data not loaded.");
		}
		data.setTempAlias(ClueFormatter.stripColor(input).trim());
		return null;
	}

	public static void clear(Player player) {
		PlayerData data = PlayerManager.get(player);
		if (data != null) {
			data.clearTempAlias();
		}
	}
}
