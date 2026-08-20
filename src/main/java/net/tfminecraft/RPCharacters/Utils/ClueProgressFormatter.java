package net.tfminecraft.RPCharacters.Utils;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;

public final class ClueProgressFormatter {

	private ClueProgressFormatter() {}

	public static String guiTitle(RPCharacter character) {
		int stored = character.getPlayerClues().size();
		int required = character.getCluesNeeded();
		return RPTexts.formatGui(RPTexts.MUTED + "Clues (" + RPTexts.GUI_WARN + required + RPTexts.MUTED + "/"
				+ RPTexts.GUI_WARN + required + RPTexts.MUTED + " required · " + RPTexts.GUI_WARN + stored
				+ RPTexts.MUTED + " stored, max " + RPTexts.GUI_WARN + Cache.maxClues + RPTexts.MUTED + ")");
	}

	public static String progressLine(RPCharacter character) {
		int stored = character.getPlayerClues().size();
		int required = character.getCluesNeeded();
		return RPTexts.formatDisplay(RPTexts.WARN + required + RPTexts.MUTED + "/" + RPTexts.WARN + required
				+ " " + RPTexts.MUTED + "required · " + RPTexts.WARN + stored + " " + RPTexts.MUTED
				+ "stored, max " + RPTexts.WARN + Cache.maxClues);
	}

	public static String lackingCluesMessage(RPCharacter character) {
		return RPTexts.formatDisplay(RPTexts.ERROR + "Your character does not have enough clues ("
				+ progressLine(character) + RPTexts.ERROR + ").");
	}
}
