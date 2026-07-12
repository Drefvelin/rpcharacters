package net.tfminecraft.RPCharacters.Objects;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class PermissionGroupDefinition {

	public static final String KEY_NAME_COLOUR_STOPS = "name-colour-stops";
	public static final String KEY_CHARACTER_SWITCH_COOLDOWN_DAYS = "character-switch-cooldown-days";

	private final String id;
	private final String permission;
	private final Map<String, Integer> perks;

	public PermissionGroupDefinition(String id, String permission, Map<String, Integer> perks) {
		this.id = id;
		this.permission = permission;
		this.perks = perks != null ? Collections.unmodifiableMap(new HashMap<>(perks)) : Map.of();
	}

	public String getId() {
		return id;
	}

	public String getPermission() {
		return permission;
	}

	public Map<String, Integer> getPerks() {
		return perks;
	}

	public int getPerk(String key, int fallback) {
		return perks.getOrDefault(key, fallback);
	}
}
