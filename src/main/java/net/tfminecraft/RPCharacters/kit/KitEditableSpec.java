package net.tfminecraft.RPCharacters.kit;

/**
 * Phase 3 reserved fields on an editable kit line. Unused by grant.
 * Custom skins are player submissions (Discord review → ps_items), not staff categories.
 */
public final class KitEditableSpec {

	private final String skinPng;
	private final String baseSet;

	public KitEditableSpec(String skinPng, String baseSet) {
		this.skinPng = skinPng;
		this.baseSet = baseSet;
	}

	public String getSkinPng() {
		return skinPng;
	}

	public String getBaseSet() {
		return baseSet;
	}
}
