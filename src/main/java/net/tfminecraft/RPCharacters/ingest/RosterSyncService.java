package net.tfminecraft.RPCharacters.ingest;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.player.profess.PlayerClass;
import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.api.ProvinceSystemClient;
import net.tfminecraft.RPCharacters.Creation.Stage;
import net.tfminecraft.RPCharacters.Creation.Stages.AttributesStage;
import net.tfminecraft.RPCharacters.Database.Database;
import net.tfminecraft.RPCharacters.Loaders.KitLoader;
import net.tfminecraft.RPCharacters.Loaders.StageLoader;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.identity.PersonaService;
import net.tfminecraft.RPCharacters.kit.KitDefinition;
import net.tfminecraft.RPCharacters.kit.KitService;
import net.tfminecraft.RPCharacters.kit.KitStatus;
import net.tfminecraft.RPCharacters.persona.CharacterSlotService;
import net.tfminecraft.RPCharacters.persona.PermissionGroupService;
import net.tfminecraft.RPCharacters.Utils.ClueFormatter;

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
			appendSheetFields(row, c);
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

	@SuppressWarnings("unchecked")
	private static void appendSheetFields(JSONObject row, RPCharacter c) {
		if (c.getRace() != null && c.getRace().getName() != null) {
			String raceName = strip(c.getRace().getName());
			if (!raceName.isBlank()) {
				row.put("race_name", raceName);
			}
		}
		if (c.hasMMOClass()) {
			String className = resolveClassName(c.getMMOClass());
			if (className != null && !className.isBlank()) {
				row.put("class_name", className);
			}
		}
		String age = PersonaService.resolveAge(c);
		if (age != null && !age.isBlank()) {
			row.put("age", age);
		}
		String birthday = c.getBirthday();
		if (birthday != null && !birthday.isBlank()) {
			row.put("birthday", birthday.trim());
		}
		String gender = c.getGender();
		if (gender != null && !gender.isBlank()) {
			row.put("gender", gender.trim());
		}
		String description = c.getPersonaDescription();
		if (description != null && !description.isBlank()) {
			row.put("description", description.trim());
		}
		JSONObject attributes = attributeRanks(c);
		if (!attributes.isEmpty()) {
			row.put("attributes", attributes);
		}
		JSONArray traits = traitRows(c);
		if (!traits.isEmpty()) {
			row.put("traits", traits);
		}
		JSONArray clues = clueRows(c);
		if (!clues.isEmpty()) {
			row.put("clues", clues);
		}
	}

	@SuppressWarnings("unchecked")
	private static JSONObject attributeRanks(RPCharacter character) {
		JSONObject out = new JSONObject();
		java.util.List<String> attrs = Cache.attributes;
		if (attrs == null || attrs.isEmpty() || character == null) {
			return out;
		}
		int maxCheck = 16;
		for (Stage stage : StageLoader.oList) {
			if (stage instanceof AttributesStage attributes) {
				maxCheck = Math.max(1, attributes.getMaxRank());
				break;
			}
		}
		for (String attr : attrs) {
			if (attr == null || attr.isBlank()) {
				continue;
			}
			int rank = 0;
			for (int n = 1; n <= maxCheck; n++) {
				if (hasTraitId(character, AttributesStage.traitId(attr, n))) {
					rank = n;
				} else {
					break;
				}
			}
			if (rank > 0) {
				out.put(attr.trim().toLowerCase(java.util.Locale.ROOT), Integer.valueOf(rank));
			}
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	private static JSONArray traitRows(RPCharacter character) {
		JSONArray out = new JSONArray();
		if (character == null || character.getTraits() == null) {
			return out;
		}
		for (Trait trait : character.getTraits()) {
			if (trait == null || trait.getId() == null || trait.getTraitData() == null) {
				continue;
			}
			String key = trait.getTraitData().getKey();
			if (key != null && key.equalsIgnoreCase("injury")) {
				continue;
			}
			// Skip pure attribute-rank traits (str1, dex2, …)
			if (isAttributeRankTrait(trait.getId())) {
				continue;
			}
			JSONObject row = new JSONObject();
			row.put("id", trait.getId());
			row.put("name", strip(trait.getName()));
			if (key != null && !key.isBlank()) {
				row.put("key", key.trim().toLowerCase(java.util.Locale.ROOT));
			}
			out.add(row);
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	private static JSONArray clueRows(RPCharacter character) {
		JSONArray out = new JSONArray();
		if (character == null) {
			return out;
		}
		for (String clue : character.getPlayerClues()) {
			if (clue == null || clue.isBlank()) {
				continue;
			}
			String plain = ClueFormatter.stripColor(clue);
			if (plain != null && !plain.isBlank()) {
				out.add(plain.trim());
			}
		}
		return out;
	}

	private static boolean isAttributeRankTrait(String traitId) {
		if (traitId == null || traitId.isBlank()) {
			return false;
		}
		String id = traitId.trim().toLowerCase(java.util.Locale.ROOT);
		java.util.List<String> attrs = Cache.attributes;
		if (attrs == null) {
			return false;
		}
		for (String attr : attrs) {
			if (attr == null || attr.isBlank()) {
				continue;
			}
			String abbrev = AttributesStage.abbrevFor(attr).toLowerCase(java.util.Locale.ROOT);
			if (id.matches(java.util.regex.Pattern.quote(abbrev) + "[0-9]+")) {
				return true;
			}
		}
		return false;
	}

	private static boolean hasTraitId(RPCharacter character, String id) {
		if (character == null || character.getTraits() == null || id == null) {
			return false;
		}
		for (Trait trait : character.getTraits()) {
			if (trait.getId().equalsIgnoreCase(id)) {
				return true;
			}
		}
		return false;
	}

	private static String resolveClassName(String classId) {
		if (classId == null || classId.isBlank()) {
			return null;
		}
		try {
			PlayerClass playerClass = MMOCore.plugin.classManager.get(classId);
			if (playerClass != null && playerClass.getName() != null) {
				return strip(playerClass.getName());
			}
		} catch (Throwable ignored) {
			// fail-soft
		}
		return classId;
	}

	private static String strip(String raw) {
		if (raw == null) {
			return "";
		}
		return ChatColor.stripColor(raw);
	}
}
