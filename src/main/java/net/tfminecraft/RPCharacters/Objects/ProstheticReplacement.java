package net.tfminecraft.RPCharacters.Objects;

import java.util.Collections;
import java.util.List;

public final class ProstheticReplacement {

	private final String permanentInjuryId;
	private final String installItem;
	private final List<String> tierTraitIds;

	public ProstheticReplacement(String permanentInjuryId, String installItem, List<String> tierTraitIds) {
		this.permanentInjuryId = permanentInjuryId;
		this.installItem = installItem;
		this.tierTraitIds = Collections.unmodifiableList(tierTraitIds);
	}

	public String getPermanentInjuryId() {
		return permanentInjuryId;
	}

	public String getInstallItem() {
		return installItem;
	}

	public List<String> getTierTraitIds() {
		return tierTraitIds;
	}

	public int getTierIndex(String prostheticTraitId) {
		if (prostheticTraitId == null) {
			return -1;
		}
		for (int i = 0; i < tierTraitIds.size(); i++) {
			if (tierTraitIds.get(i).equalsIgnoreCase(prostheticTraitId)) {
				return i;
			}
		}
		return -1;
	}

	public String getNextTierId(String currentProstheticId) {
		int index = getTierIndex(currentProstheticId);
		if (index < 0 || index + 1 >= tierTraitIds.size()) {
			return null;
		}
		return tierTraitIds.get(index + 1);
	}

	public String getTierId(int index) {
		if (index < 0 || index >= tierTraitIds.size()) {
			return null;
		}
		return tierTraitIds.get(index);
	}
}
