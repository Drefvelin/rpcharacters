package net.tfminecraft.RPCharacters.kit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Applied kit customise snapshot persisted on a character.
 */
public final class KitCustomiseData {

	private final String kitKey;
	private final String displayName;
	private final List<String> lore;
	private final String skinSlug;
	private final String path;

	public KitCustomiseData(
			String kitKey,
			String displayName,
			List<String> lore,
			String skinSlug,
			String path
	) {
		this.kitKey = kitKey != null ? kitKey.trim() : "";
		this.displayName = displayName != null ? displayName : "";
		this.lore = lore != null
				? Collections.unmodifiableList(new ArrayList<>(lore))
				: List.of();
		this.skinSlug = skinSlug != null && !skinSlug.isBlank() ? skinSlug.trim() : null;
		this.path = path != null ? path.trim() : "";
	}

	public String getKitKey() {
		return kitKey;
	}

	public String getDisplayName() {
		return displayName;
	}

	public List<String> getLore() {
		return lore;
	}

	public String getSkinSlug() {
		return skinSlug;
	}

	public String getPath() {
		return path;
	}
}
