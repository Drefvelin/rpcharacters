package net.tfminecraft.RPCharacters.wardrobe;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

/**
 * Per-session wardrobe + original account textures.
 */
public final class WardrobeCache {

	private static final Map<UUID, WardrobeSnapshot> SNAPSHOTS = new ConcurrentHashMap<>();
	private static final Map<UUID, SkinTextures> ACCOUNT_SKINS = new ConcurrentHashMap<>();

	private WardrobeCache() {}

	public static void put(UUID playerId, WardrobeSnapshot snapshot) {
		if (playerId == null || snapshot == null) {
			return;
		}
		SNAPSHOTS.put(playerId, snapshot);
	}

	public static WardrobeSnapshot get(UUID playerId) {
		if (playerId == null) {
			return null;
		}
		return SNAPSHOTS.get(playerId);
	}

	public static WardrobeSnapshot get(Player player) {
		return player == null ? null : get(player.getUniqueId());
	}

	public static void clear(UUID playerId) {
		if (playerId == null) {
			return;
		}
		SNAPSHOTS.remove(playerId);
		ACCOUNT_SKINS.remove(playerId);
	}

	public static void clear(Player player) {
		if (player != null) {
			clear(player.getUniqueId());
		}
	}

	/**
	 * Snapshot account skin once before any wardrobe apply in this session.
	 */
	public static void captureAccountSkinIfNeeded(Player player) {
		if (player == null) {
			return;
		}
		UUID id = player.getUniqueId();
		if (ACCOUNT_SKINS.containsKey(id)) {
			return;
		}
		SkinTextures textures = SkinApplyHelper.readTextures(player);
		if (textures != null && textures.isValid()) {
			ACCOUNT_SKINS.put(id, textures);
		}
	}

	public static SkinTextures getAccountSkin(UUID playerId) {
		if (playerId == null) {
			return null;
		}
		return ACCOUNT_SKINS.get(playerId);
	}

	public static SkinTextures getAccountSkin(Player player) {
		return player == null ? null : getAccountSkin(player.getUniqueId());
	}
}
