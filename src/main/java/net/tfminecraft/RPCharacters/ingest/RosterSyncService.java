package net.tfminecraft.RPCharacters.ingest;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.api.ProvinceSystemClient;
import net.tfminecraft.RPCharacters.Database.Database;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;

/**
 * Push a player's character roster mirror to ProvinceSystem.
 */
public final class RosterSyncService {

	private static final Database DB = new Database();

	private RosterSyncService() {}

	public static void pushRosterAsync(UUID playerUuid) {
		if (playerUuid == null || RPCharacters.plugin == null) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(RPCharacters.plugin, () -> pushRosterNow(playerUuid));
	}

	public static void pushRosterForPlayer(Player player) {
		if (player == null) {
			return;
		}
		pushRosterAsync(player.getUniqueId());
	}

	@SuppressWarnings("unchecked")
	public static void pushRosterNow(UUID playerUuid) {
		if (playerUuid == null) {
			return;
		}
		PlayerData pd = null;
		Player online = Bukkit.getPlayer(playerUuid);
		if (online != null && PlayerManager.exists(online)) {
			pd = PlayerManager.get(online);
		} else {
			pd = DB.loadPlayerData(playerUuid);
		}
		if (pd == null) {
			return;
		}

		JSONObject root = new JSONObject();
		root.put("player_uuid", playerUuid.toString());
		JSONArray characters = new JSONArray();
		for (RPCharacter c : pd.getCharacters()) {
			if (c == null || c.getId() == null) {
				continue;
			}
			JSONObject row = new JSONObject();
			row.put("id", c.getId());
			row.put("name", c.getName() != null ? c.getName() : "");
			row.put("status", c.getStatus() != null ? c.getStatus().toString() : "ALIVE");
			row.put("race", c.getRace() != null ? c.getRace().getId() : null);
			row.put("class", c.hasMMOClass() ? c.getMMOClass() : null);
			if (c.getCreatedAtEpochSeconds() > 0) {
				row.put("created_at", String.valueOf(c.getCreatedAtEpochSeconds()));
			}
			characters.add(row);
		}
		root.put("characters", characters);

		ProvinceSystemClient.SimpleResult result = ProvinceSystemClient.pushRoster(root.toJSONString());
		if (!result.ok && RPCharacters.plugin != null) {
			RPCharacters.plugin.getLogger().warning(
				"[character-roster] push failed for " + playerUuid + ": " + result.error
			);
		}
	}
}
