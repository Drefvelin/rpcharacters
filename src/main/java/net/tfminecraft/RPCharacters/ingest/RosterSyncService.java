package net.tfminecraft.RPCharacters.ingest;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.api.ProvinceSystemClient;
import net.tfminecraft.RPCharacters.Database.Database;
import net.tfminecraft.RPCharacters.Loaders.KitLoader;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.kit.KitDefinition;
import net.tfminecraft.RPCharacters.kit.KitService;
import net.tfminecraft.RPCharacters.kit.KitStatus;
import net.tfminecraft.RPCharacters.persona.CharacterSlotService;
import net.tfminecraft.RPCharacters.persona.PermissionGroupService;

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
			JSONObject statuses = new JSONObject();
			for (var entry : c.getKitStatuses().entrySet()) {
				if (entry.getKey() != null && entry.getValue() != null) {
					statuses.put(entry.getKey(), entry.getValue().toStorage());
				}
			}
			if (!statuses.isEmpty()) {
				row.put("kit_statuses", statuses);
			}
			KitStatus starter = c.getKitStatus(KitLoader.DEFAULT_KIT_ID);
			if (starter != null) {
				row.put("kit_status", starter.toStorage());
			}
			characters.add(row);
		}
		root.put("characters", characters);

		JSONObject cooldowns = new JSONObject();
		for (KitDefinition kit : KitLoader.getKits().values()) {
			if (kit == null) {
				continue;
			}
			JSONObject one = new JSONObject();
			one.put(
					"seconds_remaining",
					Integer.valueOf((int) (KitService.cooldownRemainingMs(pd, kit.getId()) / 1000L))
			);
			one.put("hours", Integer.valueOf(kit.getCooldownHours()));
			cooldowns.put(kit.getId(), one);
		}
		if (!cooldowns.isEmpty()) {
			root.put("kit_cooldowns", cooldowns);
		}
		root.put(
			"kit_cooldown_seconds_remaining",
			Integer.valueOf((int) (KitService.cooldownRemainingMs(pd, KitLoader.DEFAULT_KIT_ID) / 1000L))
		);
		root.put(
			"kit_cooldown_hours",
			Integer.valueOf(KitLoader.getCooldownHours())
		);

		if (online != null) {
			root.put("max_alive_characters", CharacterSlotService.getMaxAliveCharacters(online));
			root.put("name_colour_stops", Integer.valueOf(PermissionGroupService.getNameColourStops(online)));
		}

		boolean realAgeSet = pd.getCompletedStages().contains("creation_age_set_stage")
			|| pd.getCompletedStages().contains("age_stage");
		if (realAgeSet) {
			root.put("real_age_set", Boolean.TRUE);
			root.put("eighteen", Boolean.valueOf(pd.isEighteen()));
		}

		long accountAge = 0L;
		if (pd.getCreatedAtEpochSeconds() > 0) {
			accountAge = Math.max(0L, System.currentTimeMillis() / 1000L - pd.getCreatedAtEpochSeconds());
		}
		root.put("account_age_seconds", Long.valueOf(accountAge));

		ProvinceSystemClient.SimpleResult result = ProvinceSystemClient.pushRoster(root.toJSONString());
		if (!result.ok) {
			RPCharacters.plugin.getLogger().warning(
					"[roster] push failed for " + playerUuid + ": " + result.error
			);
		}
	}
}
