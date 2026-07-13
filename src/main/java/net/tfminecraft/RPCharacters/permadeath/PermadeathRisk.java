package net.tfminecraft.RPCharacters.permadeath;

import java.util.ArrayList;
import java.util.List;

import net.tfminecraft.RPCharacters.Utils.RPTexts;

public final class PermadeathRisk {

	private final int injuryCount;
	private final int chancePercent;
	private final int chancePerInjury;

	public PermadeathRisk(int injuryCount, int chancePercent, int chancePerInjury) {
		this.injuryCount = injuryCount;
		this.chancePercent = chancePercent;
		this.chancePerInjury = chancePerInjury;
	}

	public int getInjuryCount() {
		return injuryCount;
	}

	public int getChancePercent() {
		return chancePercent;
	}

	public int getChancePerInjury() {
		return chancePerInjury;
	}

	public List<String> toLoreLines() {
		List<String> lore = new ArrayList<>();
		lore.add(RPTexts.format(RPTexts.MUTED + "------------------------"));
		lore.add(RPTexts.format(RPTexts.WARN + "Permadeath Risk:"));
		lore.add(RPTexts.format(RPTexts.MUTED + "On death in a permadeath zone:"));

		if (injuryCount > 0) {
			lore.add(RPTexts.format(RPTexts.MUTED + "Injuries: " + RPTexts.ERROR + "+" + chancePercent + "%"));
			lore.add(RPTexts.format(RPTexts.MUTED + "Total chance: " + RPTexts.ERROR + chancePercent + "%"));
		} else {
			lore.add(RPTexts.format(RPTexts.MUTED + "Total chance: " + RPTexts.SUCCESS + "0%"));
		}

		return lore;
	}
}
