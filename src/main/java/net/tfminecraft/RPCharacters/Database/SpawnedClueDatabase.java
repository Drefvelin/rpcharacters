package net.tfminecraft.RPCharacters.Database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.tfminecraft.RPCharacters.Objects.SpawnedClue;
import net.tfminecraft.RPCharacters.RPCharacters;

public class SpawnedClueDatabase {

	private static final JSONParser PARSER = new JSONParser();

	private static File getFile() {
		return new File(RPCharacters.plugin.getDataFolder(), "data/spawned-clues.json");
	}

	@SuppressWarnings("unchecked")
	public static List<SpawnedClue> loadAll() {
		List<SpawnedClue> clues = new ArrayList<>();
		File file = getFile();
		if (!file.exists()) return clues;

		try {
			JSONArray array = (JSONArray) PARSER.parse(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			long now = System.currentTimeMillis();
			for (Object entry : array) {
				JSONObject obj = (JSONObject) entry;
				long expiresAt = ((Number) obj.get("expiresAt")).longValue();
				if (expiresAt <= now) continue;

				UUID id = UUID.fromString((String) obj.get("id"));
				String world = (String) obj.get("world");
				double x = ((Number) obj.get("x")).doubleValue();
				double y = ((Number) obj.get("y")).doubleValue();
				double z = ((Number) obj.get("z")).doubleValue();
				String clueText = (String) obj.get("clueText");
				UUID ownerUuid = UUID.fromString((String) obj.get("ownerUuid"));

				Integer targetBlockX = null;
				Integer targetBlockY = null;
				Integer targetBlockZ = null;
				if (obj.containsKey("targetBlockX") && obj.containsKey("targetBlockY") && obj.containsKey("targetBlockZ")) {
					targetBlockX = ((Number) obj.get("targetBlockX")).intValue();
					targetBlockY = ((Number) obj.get("targetBlockY")).intValue();
					targetBlockZ = ((Number) obj.get("targetBlockZ")).intValue();
				}

				List<UUID> displayEntityIds = new ArrayList<>();
				if (obj.containsKey("displayEntityIds")) {
					JSONArray displayIds = (JSONArray) obj.get("displayEntityIds");
					for (Object displayId : displayIds) {
						try {
							displayEntityIds.add(UUID.fromString((String) displayId));
						} catch (IllegalArgumentException ignored) {
						}
					}
				}

				clues.add(new SpawnedClue(id, world, x, y, z, clueText, expiresAt, ownerUuid,
						targetBlockX, targetBlockY, targetBlockZ, displayEntityIds));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return clues;
	}

	@SuppressWarnings("unchecked")
	public static void saveAll(Collection<SpawnedClue> clues) {
		try {
			File file = getFile();
			file.getParentFile().mkdirs();

			JSONArray array = new JSONArray();
			for (SpawnedClue clue : clues) {
				if (clue.isExpired()) continue;
				JSONObject obj = new JSONObject();
				obj.put("id", clue.getId().toString());
				obj.put("world", clue.getWorldName());
				obj.put("x", clue.getX());
				obj.put("y", clue.getY());
				obj.put("z", clue.getZ());
				obj.put("clueText", clue.getClueText());
				obj.put("expiresAt", clue.getExpiresAtMs());
				obj.put("ownerUuid", clue.getOwnerUuid().toString());
				if (clue.hasTargetBlock()) {
					obj.put("targetBlockX", clue.getTargetBlockX());
					obj.put("targetBlockY", clue.getTargetBlockY());
					obj.put("targetBlockZ", clue.getTargetBlockZ());
				}
				if (!clue.getDisplayEntityIds().isEmpty()) {
					JSONArray displayIds = new JSONArray();
					for (UUID displayId : clue.getDisplayEntityIds()) {
						displayIds.add(displayId.toString());
					}
					obj.put("displayEntityIds", displayIds);
				}
				array.add(obj);
			}

			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			try (FileWriter writer = new FileWriter(file, false)) {
				writer.write(gson.toJson(array));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
