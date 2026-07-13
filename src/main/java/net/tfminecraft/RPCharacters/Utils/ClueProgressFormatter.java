package net.tfminecraft.RPCharacters.Utils;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;

public final class ClueProgressFormatter {

	private ClueProgressFormatter() {}

	public static String guiTitle(RPCharacter character) {
		int stored = character.getPlayerClues().size();
		int required = character.getCluesNeeded();
		return RPTexts.format(RPTexts.MUTED + "Clues (" + RPTexts.WARN + required + RPTexts.MUTED + "/"
				+ RPTexts.WARN + required + RPTexts.MUTED + " required · " + RPTexts.WARN + stored
				+ RPTexts.MUTED + " stored, max " + RPTexts.WARN + Cache.maxClues + RPTexts.MUTED + ")");
	}

	public static String progressLine(RPCharacter character) {
		int stored = character.getPlayerClues().size();
		int required = character.getCluesNeeded();
		return RPTexts.format(RPTexts.WARN + required + RPTexts.MUTED + "/" + RPTexts.WARN + required
				+ " " + RPTexts.MUTED + "required · " + RPTexts.WARN + stored + " " + RPTexts.MUTED
				+ "stored, max " + RPTexts.WARN + Cache.maxClues);
	}

	public static String lackingCluesMessage(RPCharacter character) {
		return RPTexts.format(RPTexts.ERROR + "Your character does not have enough clues ("
				+ progressLine(character) + RPTexts.ERROR + ").");
	}
}
