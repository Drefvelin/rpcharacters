package net.tfminecraft.RPCharacters.ingest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.api.ProvinceSystemClient;
import net.tfminecraft.RPCharacters.Database.Database;
import net.tfminecraft.RPCharacters.kit.KitCustomiseApplyService;
import net.tfminecraft.RPCharacters.kit.KitCustomiseData;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;

/**
 * Pull ready lore-item customises from ProvinceSystem and apply onto characters.
 */
public final class KitCustomiseIngestService {

	private static final Database DB = new Database();

	private KitCustomiseIngestService() {}

	public static void pullAsync(JavaPlugin plugin) {
		if (plugin == null) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> pullNow(plugin));
	}

	/**
	 * Fetch + apply on the calling async thread (HTTP), then main-thread apply.
	 * Safe to call from {@link CharacterIngestService} after creates pull.
	 */
	public static void pullNow(JavaPlugin plugin) {
		if (plugin == null) {
			return;
		}
		ProvinceSystemClient.SimpleResult pending =
				ProvinceSystemClient.fetchPendingLoreItems();
		if (!pending.ok) {
			plugin.getLogger().warning(
					"[kit-customise] pending fetch failed: " + pending.error
			);
			return;
		}
		List<JSONObject> items = ProvinceSystemClient.parsePendingLoreItems(pending.body);
		if (items.isEmpty()) {
			return;
		}
		List<JSONObject> results = applyAllOnMain(plugin, items);
		if (!results.isEmpty()) {
			ProvinceSystemClient.SimpleResult ack =
					ProvinceSystemClient.ackLoreItems(buildAckJson(results));
			if (!ack.ok) {
				plugin.getLogger().warning("[kit-customise] ack failed: " + ack.error);
			} else {
				plugin.getLogger().info(
						"[kit-customise] processed " + results.size() + " ready item(s)"
				);
			}
		}
	}

	/**
	 * Must run on the main thread. Sync-fetches ready lore items for one character,
	 * applies them inline (no scheduler hop), and acks. Used before kit claim.
	 */
	public static void ingestReadyForCharacterOnMain(Player player, RPCharacter character) {
		if (player == null || character == null) {
			return;
		}
		String characterId = character.getId();
		String playerUuid = player.getUniqueId().toString();
		if (characterId == null || characterId.isBlank()) {
			return;
		}
		ProvinceSystemClient.SimpleResult pending =
				ProvinceSystemClient.fetchPendingLoreItems();
		if (!pending.ok) {
			RPCharacters.plugin.getLogger().warning(
					"[kit-customise] claim pull failed: " + pending.error
			);
			return;
		}
		List<JSONObject> all = ProvinceSystemClient.parsePendingLoreItems(pending.body);
		List<JSONObject> mine = new ArrayList<>();
		for (JSONObject row : all) {
			if (row == null) {
				continue;
			}
			String cid = stringOf(row.get("character_id"));
			String uuid = stringOf(row.get("player_uuid"));
			if (characterId.equalsIgnoreCase(cid) && playerUuid.equalsIgnoreCase(uuid)) {
				mine.add(row);
			}
		}
		if (mine.isEmpty()) {
			return;
		}
		List<JSONObject> results = new ArrayList<>();
		for (JSONObject row : mine) {
			results.add(applyOne(row));
		}
		ProvinceSystemClient.SimpleResult ack =
				ProvinceSystemClient.ackLoreItems(buildAckJson(results));
		if (!ack.ok) {
			RPCharacters.plugin.getLogger().warning(
					"[kit-customise] claim ack failed: " + ack.error
			);
		}
	}

	private static List<JSONObject> applyAllOnMain(JavaPlugin plugin, List<JSONObject> items) {
		if (items == null || items.isEmpty()) {
			return List.of();
		}
		AtomicReference<List<JSONObject>> ref = new AtomicReference<>(List.of());
		CountDownLatch latch = new CountDownLatch(1);
		Bukkit.getScheduler().runTask(plugin, () -> {
			try {
				List<JSONObject> results = new ArrayList<>();
				for (JSONObject row : items) {
					results.add(applyOne(row));
				}
				ref.set(results);
			} finally {
				latch.countDown();
			}
		});
		try {
			latch.await(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return ref.get();
	}

	@SuppressWarnings("unchecked")
	private static JSONObject applyOne(JSONObject row) {
		JSONObject result = new JSONObject();
		String characterId = stringOf(row.get("character_id"));
		String kitKey = stringOf(row.get("kit_key"));
		String playerUuidStr = stringOf(row.get("player_uuid"));
		result.put("character_id", characterId);
		result.put("kit_key", kitKey);
		result.put("player_uuid", playerUuidStr);
		try {
			if (characterId.isBlank() || kitKey.isBlank() || playerUuidStr.isBlank()) {
				result.put("ok", false);
				result.put("error", "missing character_id, kit_key, or player_uuid");
				return result;
			}
			UUID playerUuid = UUID.fromString(playerUuidStr);
			Player online = Bukkit.getPlayer(playerUuid);
			PlayerData pd;
			boolean inManager = false;
			if (online != null && PlayerManager.exists(online)) {
				pd = PlayerManager.get(online);
				inManager = true;
			} else if (online != null) {
				pd = DB.loadPlayer(online);
				if (pd == null) {
					pd = new PlayerData(online);
				}
			} else {
				pd = DB.loadPlayerData(playerUuid);
			}
			if (pd == null) {
				result.put("ok", false);
				result.put("error", "player data not loaded");
				return result;
			}

			RPCharacter character = pd.getCharacterById(characterId);
			if (character == null) {
				for (RPCharacter c : pd.getCharacters()) {
					if (c != null && characterId.equalsIgnoreCase(c.getId())) {
						character = c;
						break;
					}
				}
			}
			if (character == null) {
				result.put("ok", false);
				result.put("error", "character not found");
				return result;
			}

			List<String> lore = new ArrayList<>();
			Object loreObj = row.get("lore");
			if (loreObj instanceof JSONArray arr) {
				for (Object line : arr) {
					if (line != null) {
						lore.add(line.toString());
					}
				}
			}
			String displayName = stringOf(row.get("display_name"));
			String skinSlug = stringOf(row.get("skin_slug"));
			if (skinSlug.isBlank()) {
				skinSlug = null;
			}
			String path = stringOf(row.get("path"));
			KitCustomiseData data = new KitCustomiseData(
					kitKey, displayName, lore, skinSlug, path
			);
			character.putKitCustomise(data);

			if (inManager && online != null) {
				RPCharacters.getPlayerManager().savePlayer(online);
			} else {
				DB.savePlayer(pd);
			}

			if (online != null && online.isOnline()) {
				boolean active = pd.hasActiveCharacter()
						&& pd.getActiveCharacter() != null
						&& character.getId().equalsIgnoreCase(pd.getActiveCharacter().getId());
				if (active) {
					KitCustomiseApplyService.applyToInventory(online, data);
				}
			}
			result.put("ok", true);
		} catch (Exception e) {
			result.put("ok", false);
			result.put(
					"error",
					e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()
			);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private static String buildAckJson(List<JSONObject> results) {
		JSONObject root = new JSONObject();
		JSONArray arr = new JSONArray();
		arr.addAll(results);
		root.put("results", arr);
		return root.toJSONString();
	}

	private static String stringOf(Object value) {
		return value == null ? "" : value.toString().trim();
	}
}
