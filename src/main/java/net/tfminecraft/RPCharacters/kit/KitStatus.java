package net.tfminecraft.RPCharacters.kit;

/**
 * Per-character starter kit state. Missing/null on disk = legacy (never grant).
 */
public enum KitStatus {
	ELIGIBLE,
	GRANTED,
	INELIGIBLE;

	public static KitStatus fromStorage(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			return KitStatus.valueOf(raw.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public String toStorage() {
		return name().toLowerCase();
	}
}
