package net.tfminecraft.RPCharacters.Database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.tfminecraft.RPCharacters.Loaders.RaceLoader;
import net.tfminecraft.RPCharacters.Loaders.TraitLoader;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Races.Race;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.identity.NameColour;
import net.tfminecraft.RPCharacters.persona.PermissionGroupService;
import net.tfminecraft.RPCharacters.Utils.DurationParser;
import net.tfminecraft.RPCharacters.Utils.Integrator;
import net.tfminecraft.RPCharacters.enums.Status;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class Database {
	private JSONObject json; // org.json.simple
    JSONParser parser = new JSONParser();

	public static void log(Player p, String action) {
		try {
			// Base folder
			File baseFolder = new File("plugins/RPCharacters/logs");
			if (!baseFolder.exists()) baseFolder.mkdirs();

			// Get first letter, lowercase
			String firstLetter = p.getName().substring(0, 1).toLowerCase();

			// Subfolder (A-Z)
			File subFolder = new File(baseFolder, firstLetter);
			if (!subFolder.exists()) subFolder.mkdirs();

			// The player's log file
			File logFile = new File(subFolder, p.getName() + ".txt");

			// Write to file (append mode)
			FileWriter writer = new FileWriter(logFile, true);

			// Log format: timestamp + action
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
			String timestamp = LocalDateTime.now().format(formatter);
			String logEntry = "[" + timestamp + "] " + action + System.lineSeparator();

			writer.write(logEntry);
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public PlayerData loadPlayer(Player p) {
		File file = new File("plugins/RPCharacters/data/playerdata", p.getUniqueId().toString()+".json");
		if (file.exists()) {
        	try {
				json = (JSONObject) parser.parse(new InputStreamReader(new FileInputStream(file), "UTF-8"));
				if(json.containsKey("to remove")) {
					List<String> remove = new ArrayList<>();
					int i = 0;
					JSONArray removeArray = (JSONArray) json.get("to remove");
					while(i < removeArray.size()) {
						remove.add(removeArray.get(i).toString());
						i++;
					}
					Integrator integrator = new Integrator();
					for(String a : remove) {
						integrator.remove(p, a);
					}
				}
				Long lastCharacterSwitchAtMs = null;
				if (json.containsKey("last-character-switch-ms")) {
					lastCharacterSwitchAtMs = ((Number) json.get("last-character-switch-ms")).longValue();
				} else if (json.containsKey("cooldown")) {
					int remainingMinutes = (int) Math.round(((Number) json.get("cooldown")).doubleValue());
					lastCharacterSwitchAtMs = PermissionGroupService.migrateLegacyCooldownMinutes(remainingMinutes);
				}
				boolean eighteen = json.containsKey("eighteen") ? Boolean.parseBoolean((String) json.get("eighteen")) : false;
				List<String> completedStages = new ArrayList<>();
				int i = 0;
				JSONArray stageArray = (JSONArray) json.get("completed stages");
				while(i < stageArray.size()) {
					completedStages.add(stageArray.get(i).toString());
					i++;
				}
				int createdAtEpochSeconds = DurationParser.resolveCreatedAtEpochSeconds(
						json.containsKey("created-at"),
						json.containsKey("created-at") ? ((Number) json.get("created-at")).intValue() : 0,
						json.containsKey("account-playtime-seconds"),
						json.containsKey("account-playtime-seconds") ? ((Number) json.get("account-playtime-seconds")).intValue() : 0,
						file);
				Integer accountSkillPointsTotal = null;
				if (json.containsKey("account-skill-points-total")) {
					accountSkillPointsTotal = ((Number) json.get("account-skill-points-total")).intValue();
				}
				PlayerData pd = new PlayerData(p, completedStages, lastCharacterSwitchAtMs, eighteen, createdAtEpochSeconds,
						accountSkillPointsTotal);
				if (json.containsKey("account-attribute-points-total")) {
					pd.setAccountAttributePointsTotal(((Number) json.get("account-attribute-points-total")).intValue());
				}
				loadAccountProfessionPoints(pd, json);
				loadInvestigationPoints(pd, json);
				if (json.containsKey("permadeath-tutorial-dismissed")) {
					pd.setPermadeathTutorialDismissed(Boolean.parseBoolean((String) json.get("permadeath-tutorial-dismissed")));
				}
				loadCharacters(pd);
				return pd;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
        }
		return null;
	}

	/**
	 * Load player data by UUID without requiring an online Player.
	 * Returns a new empty PlayerData if no file exists.
	 */
	public PlayerData loadPlayerData(java.util.UUID uuid) {
		if (uuid == null) {
			return null;
		}
		org.bukkit.entity.Player online = Bukkit.getPlayer(uuid);
		if (online != null) {
			PlayerData loaded = loadPlayer(online);
			return loaded != null ? loaded : new PlayerData(online);
		}
		File file = new File("plugins/RPCharacters/data/playerdata", uuid.toString() + ".json");
		if (!file.exists()) {
			return new PlayerData(uuid);
		}
		try {
			json = (JSONObject) parser.parse(new InputStreamReader(new FileInputStream(file), "UTF-8"));
			Long lastCharacterSwitchAtMs = null;
			if (json.containsKey("last-character-switch-ms")) {
				lastCharacterSwitchAtMs = ((Number) json.get("last-character-switch-ms")).longValue();
			} else if (json.containsKey("cooldown")) {
				int remainingMinutes = (int) Math.round(((Number) json.get("cooldown")).doubleValue());
				lastCharacterSwitchAtMs = PermissionGroupService.migrateLegacyCooldownMinutes(remainingMinutes);
			}
			boolean eighteen = json.containsKey("eighteen") ? Boolean.parseBoolean((String) json.get("eighteen")) : false;
			List<String> completedStages = new ArrayList<>();
			int i = 0;
			JSONArray stageArray = (JSONArray) json.get("completed stages");
			if (stageArray != null) {
				while (i < stageArray.size()) {
					completedStages.add(stageArray.get(i).toString());
					i++;
				}
			}
			int createdAtEpochSeconds = DurationParser.resolveCreatedAtEpochSeconds(
					json.containsKey("created-at"),
					json.containsKey("created-at") ? ((Number) json.get("created-at")).intValue() : 0,
					json.containsKey("account-playtime-seconds"),
					json.containsKey("account-playtime-seconds") ? ((Number) json.get("account-playtime-seconds")).intValue() : 0,
					file);
			Integer accountSkillPointsTotal = null;
			if (json.containsKey("account-skill-points-total")) {
				accountSkillPointsTotal = ((Number) json.get("account-skill-points-total")).intValue();
			}
			PlayerData pd = new PlayerData(
					uuid, completedStages, lastCharacterSwitchAtMs, eighteen, createdAtEpochSeconds,
					accountSkillPointsTotal);
			if (json.containsKey("account-attribute-points-total")) {
				pd.setAccountAttributePointsTotal(((Number) json.get("account-attribute-points-total")).intValue());
			}
			loadAccountProfessionPoints(pd, json);
			loadInvestigationPoints(pd, json);
			if (json.containsKey("permadeath-tutorial-dismissed")) {
				pd.setPermadeathTutorialDismissed(Boolean.parseBoolean((String) json.get("permadeath-tutorial-dismissed")));
			}
			loadCharacters(pd);
			return pd;
		} catch (Exception ex) {
			ex.printStackTrace();
			return new PlayerData(uuid);
		}
	}

	public void loadCharacters(PlayerData pd) {
		File folder = new File("plugins/RPCharacters/data/characterdata", pd.getUniqueId().toString());
		if (!folder.exists() || !folder.isDirectory()) {
			return;
		}
		File[] files = folder.listFiles();
		if (files == null) {
			return;
		}
    	for (final File file : files) {
            if (!file.isDirectory()) {
            	try {
    				json = (JSONObject) parser.parse(new InputStreamReader(new FileInputStream(file), "UTF-8"));
    				String id = (String) json.get("id");
    				String name = (String) json.get("name");
    				Status status = Status.valueOf(((String) json.get("status")).toUpperCase());
    				Boolean active = Boolean.parseBoolean((String) json.get("active"));
    				Race r = RaceLoader.getByString((String) json.get("race"));
					String mmoClass = json.containsKey("class") ? (String) json.get("class") : null;
    				if(r == null) {
    					r = RaceLoader.get().get(0);
    				}
    				List<Trait> traits = new ArrayList<Trait>();
    				int i = 0;
    				JSONArray traitArray = (JSONArray) json.get("traits");
    				while(i < traitArray.size()) {
    					Trait t = TraitLoader.getByString(traitArray.get(i).toString());
    					if(t != null) {
    						traits.add(t);
    					}
    					i++;
    				}
    				List<String> clues = new ArrayList<>();
    				if (json.containsKey("clues")) {
    					JSONArray clueArray = (JSONArray) json.get("clues");
    					int j = 0;
    					while (j < clueArray.size()) {
    						clues.add(clueArray.get(j).toString());
    						j++;
    					}
    				}
    				RPCharacter c = new RPCharacter(pd.getPlayer(), id, name, active, status, r, traits, mmoClass, clues);
    				int createdAtEpochSeconds = DurationParser.resolveCreatedAtEpochSeconds(
    						json.containsKey("created-at"),
    						json.containsKey("created-at") ? ((Number) json.get("created-at")).intValue() : 0,
    						json.containsKey("playtime-seconds"),
    						json.containsKey("playtime-seconds") ? ((Number) json.get("playtime-seconds")).intValue() : 0,
    						file);
    				c.setCreatedAtEpochSeconds(createdAtEpochSeconds);
    				c.setConversationCounts(loadConversationCounts(json));
    				c.setConversationLastAtMs(loadConversationLastAt(json));
    				loadPersonaFields(c, json);
    				loadProfessionFields(c, json);
    				loadExtraAttributeAllocation(c, json);
    				if (c.getSlug() == null || c.getSlug().isBlank()) {
    					pd.assignSlug(c);
    				}
    				pd.addCharacter(c);
    			} catch (Exception ex) {
    				ex.printStackTrace();
    			}
            }
        }
	}
	@SuppressWarnings("unchecked")
	public void savePlayer(PlayerData pd) {
		try {
			File subFolder = new File("plugins/RPCharacters/data/characterdata", pd.getUniqueId().toString());
			if(!subFolder.exists()) subFolder.mkdir();
			File file = new File("plugins/RPCharacters/data/playerdata", pd.getUniqueId().toString()+".json");
			file.createNewFile();
        	PrintWriter pw = new PrintWriter(file, "UTF-8");
        	pw.print("{");
        	pw.print("}");
        	pw.flush();
        	pw.close();
            HashMap<String, Object> defaults = new HashMap<String, Object>();
        	json = (JSONObject) parser.parse(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        	defaults.put("eighteen", String.valueOf(pd.isEighteen()));
			if (pd.getLastCharacterSwitchAtMs() != null) {
				defaults.put("last-character-switch-ms", pd.getLastCharacterSwitchAtMs());
			}
        	int i = 0;
        	JSONArray stageArray = new JSONArray();
        	while(i < pd.getCompletedStages().size()) {
        		stageArray.add(pd.getCompletedStages().get(i));
        		i++;
        	}
        	defaults.put("completed stages", stageArray);
			if (pd.getCreatedAtEpochSeconds() > 0) {
				defaults.put("created-at", pd.getCreatedAtEpochSeconds());
			}
			if (!pd.needsSkillPointsMigration()) {
				defaults.put("account-skill-points-total", pd.getAccountSkillPointsTotal());
			}
			if (!pd.needsAttributePointsMigration()) {
				defaults.put("account-attribute-points-total", pd.getAccountAttributePointsTotal());
			}
			if (pd.isProfessionPointsInitialized()) {
				defaults.put("account-profession-points", toAccountProfessionPointsJson(pd.getAccountProfessionPointsMap()));
				defaults.put("profession-points-initialized", "true");
			}
			if (!pd.needsInvestigationPointsInit()) {
				defaults.put("investigation-points", pd.getInvestigationPoints());
			}
			if (pd.getLastInvestigationRegenMs() != null) {
				defaults.put("investigation-regen-ms", pd.getLastInvestigationRegenMs());
			}
			if (pd.hasDismissedPermadeathTutorial()) {
				defaults.put("permadeath-tutorial-dismissed", "true");
			}
        	for(RPCharacter c : pd.getCharacters()) {
        		saveCharacter(pd, c);
        		if(c.isActive() && pd.getPlayer() != null) {
            		Integrator integrator = new Integrator();
            		
            		i = 0;
                	JSONArray removeArray = new JSONArray();
                	List<String> remove = integrator.getRemoveList(pd.getPlayer(), c);
                	while(i < remove.size()) {
                		removeArray.add(remove.get(i));
                		i++;
                	}
                	defaults.put("to remove", removeArray);
            	}
        	}
        	save(file, defaults);
        } catch (Throwable ex) {
			ex.printStackTrace();
        }
	}
	@SuppressWarnings("unchecked")
	public void saveCharacter(PlayerData pd, RPCharacter c) {
		try {
			File dir = new File("plugins/RPCharacters/data/characterdata", pd.getUniqueId().toString());
			if (!dir.exists()) {
				dir.mkdirs();
			}
			File file = new File(dir, c.getId()+".json");
			file.createNewFile();
        	PrintWriter pw = new PrintWriter(file, "UTF-8");
        	pw.print("{");
        	pw.print("}");
        	pw.flush();
        	pw.close();
            HashMap<String, Object> defaults = new HashMap<String, Object>();
        	json = (JSONObject) parser.parse(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        	defaults.put("id", c.getId());
        	defaults.put("name", c.getName());
        	defaults.put("status", c.getStatus().toString());
        	defaults.put("race", c.getRace().getId());
        	defaults.put("active", c.isActive().toString());
			if(c.hasMMOClass()) defaults.put("class", c.getMMOClass());
        	int i = 0;
        	JSONArray traitArray = new JSONArray();
        	while(i < c.getTraits().size()) {
        		traitArray.add(c.getTraits().get(i).getId());
        		i++;
        	}
        	defaults.put("traits", traitArray);
        	i = 0;
        	JSONArray clueArray = new JSONArray();
        	while (i < c.getPlayerClues().size()) {
        		clueArray.add(c.getPlayerClues().get(i));
        		i++;
        	}
        	defaults.put("clues", clueArray);
			if (c.getCreatedAtEpochSeconds() > 0) {
				defaults.put("created-at", c.getCreatedAtEpochSeconds());
			}
			defaults.put("conversations", toConversationCountsJson(c.getConversationCounts()));
			defaults.put("conversation-last-at", toConversationLastAtJson(c.getConversationLastAtMs()));
			savePersonaFields(defaults, c);
			saveProfessionFields(defaults, c);
			saveExtraAttributeAllocation(defaults, c);
        	save(file, defaults);
        } catch (Throwable ex) {
			ex.printStackTrace();
        }
	}
	@SuppressWarnings("unchecked")
	public boolean save(File file, HashMap<String, Object> defaults) {
	  try {
		  JSONObject toSave = new JSONObject();
	  
	    for (String s : defaults.keySet()) {
	      Object o = defaults.get(s);
	      if (o instanceof String) {
	        toSave.put(s, getString(s, defaults));
	      } else if (o instanceof Double) {
	        toSave.put(s, getDouble(s, defaults));
	      } else if (o instanceof Integer) {
	        toSave.put(s, getInteger(s, defaults));
	      } else if (o instanceof JSONObject) {
	        toSave.put(s, getObject(s, defaults));
	      } else if (o instanceof JSONArray) {
	        toSave.put(s, getArray(s, defaults));
	      }
	    }
	  
	    TreeMap<String, Object> treeMap = new TreeMap<String, Object>(String.CASE_INSENSITIVE_ORDER);
	    treeMap.putAll(toSave);
	  
	   Gson g = new GsonBuilder().setPrettyPrinting().create();
	   String prettyJsonString = g.toJson(treeMap);
	  
	    FileWriter fw = new FileWriter(file);
	    fw.write(prettyJsonString);
	    fw.flush();
	    fw.close();
	  
	    return true;
	  } catch (Exception ex) {
	    ex.printStackTrace();
	    return false;
	  }
	}
	
	public String getRawData(String key, HashMap<String, Object> defaults) {
	    return json.containsKey(key) ? json.get(key).toString()
	       : (defaults.containsKey(key) ? defaults.get(key).toString() : key);
	  }
	
	  public String getString(String key, HashMap<String, Object> defaults) {
	    return ChatColor.translateAlternateColorCodes('&', getRawData(key, defaults));
	  }
	
	  public boolean getBoolean(String key, HashMap<String, Object> defaults) {
	    return Boolean.valueOf(getRawData(key, defaults));
	  }
	
	  public double getDouble(String key, HashMap<String, Object> defaults) {
	    try {
	      return Double.parseDouble(getRawData(key, defaults));
	    } catch (Exception ex) { }
	    return -1;
	  }
	
	  public double getInteger(String key, HashMap<String, Object> defaults) {
	    try {
	      return Integer.parseInt(getRawData(key, defaults));
	    } catch (Exception ex) { }
	    return -1;
	  }
	 
	  public JSONObject getObject(String key, HashMap<String, Object> defaults) {
	     return json.containsKey(key) ? (JSONObject) json.get(key)
	       : (defaults.containsKey(key) ? (JSONObject) defaults.get(key) : new JSONObject());
	  }
	 
	  public JSONArray getArray(String key, HashMap<String, Object> defaults) {
		     return json.containsKey(key) ? (JSONArray) json.get(key)
		       : (defaults.containsKey(key) ? (JSONArray) defaults.get(key) : new JSONArray());
	  }

	private Map<String, Integer> loadConversationCounts(JSONObject characterJson) {
		Map<String, Integer> counts = new HashMap<>();
		if (!characterJson.containsKey("conversations")) {
			return counts;
		}
		Object raw = characterJson.get("conversations");
		if (!(raw instanceof JSONObject)) {
			return counts;
		}
		JSONObject conversations = (JSONObject) raw;
		for (Object key : conversations.keySet()) {
			Object value = conversations.get(key);
			if (value instanceof Number) {
				counts.put(key.toString(), ((Number) value).intValue());
			}
		}
		return counts;
	}

	private Map<String, Long> loadConversationLastAt(JSONObject characterJson) {
		Map<String, Long> lastAt = new HashMap<>();
		if (!characterJson.containsKey("conversation-last-at")) {
			return lastAt;
		}
		Object raw = characterJson.get("conversation-last-at");
		if (!(raw instanceof JSONObject)) {
			return lastAt;
		}
		JSONObject conversationLastAt = (JSONObject) raw;
		for (Object key : conversationLastAt.keySet()) {
			Object value = conversationLastAt.get(key);
			if (value instanceof Number) {
				lastAt.put(key.toString(), ((Number) value).longValue());
			}
		}
		return lastAt;
	}

	private JSONObject toConversationCountsJson(Map<String, Integer> counts) {
		JSONObject conversations = new JSONObject();
		if (counts == null) {
			return conversations;
		}
		for (Map.Entry<String, Integer> entry : counts.entrySet()) {
			conversations.put(entry.getKey(), entry.getValue());
		}
		return conversations;
	}

	private JSONObject toConversationLastAtJson(Map<String, Long> lastAt) {
		JSONObject conversationLastAt = new JSONObject();
		if (lastAt == null) {
			return conversationLastAt;
		}
		for (Map.Entry<String, Long> entry : lastAt.entrySet()) {
			conversationLastAt.put(entry.getKey(), entry.getValue());
		}
		return conversationLastAt;
	}

	private void loadPersonaFields(RPCharacter character, JSONObject characterJson) {
		if (characterJson.containsKey("alias")) {
			character.setAlias((String) characterJson.get("alias"));
		}
		if (characterJson.containsKey("gender")) {
			character.setGender((String) characterJson.get("gender"));
		}
		if (characterJson.containsKey("description")) {
			character.setPersonaDescription((String) characterJson.get("description"));
		}
		if (characterJson.containsKey("name-colour")) {
			Object raw = characterJson.get("name-colour");
			if (raw instanceof JSONObject colourJson) {
				character.setNameColour(NameColour.fromJson(colourJson));
			}
		}
		if (characterJson.containsKey("name-colour-staff")) {
			character.setNameColourStaffOverride(Boolean.parseBoolean(characterJson.get("name-colour-staff").toString()));
		}
		if (characterJson.containsKey("birthday")) {
			character.setBirthday((String) characterJson.get("birthday"));
		}
		if (characterJson.containsKey("slug")) {
			character.setSlug((String) characterJson.get("slug"));
		}
		if (characterJson.containsKey("hidden")) {
			character.setHidden(Boolean.parseBoolean(characterJson.get("hidden").toString()));
		}
	}

	@SuppressWarnings("unchecked")
	private void savePersonaFields(HashMap<String, Object> defaults, RPCharacter character) {
		if (character.getAlias() != null && !character.getAlias().isBlank()) {
			defaults.put("alias", character.getAlias());
		}
		if (character.getGender() != null && !character.getGender().isBlank()) {
			defaults.put("gender", character.getGender());
		}
		if (character.getPersonaDescription() != null && !character.getPersonaDescription().isBlank()) {
			defaults.put("description", character.getPersonaDescription());
		}
		if (character.getNameColour() != null) {
			defaults.put("name-colour", character.getNameColour().toJsonObject());
		}
		if (character.isNameColourStaffOverride()) {
			defaults.put("name-colour-staff", "true");
		}
		if (character.getBirthday() != null && !character.getBirthday().isBlank()) {
			defaults.put("birthday", character.getBirthday());
		}
		if (character.getSlug() != null && !character.getSlug().isBlank()) {
			defaults.put("slug", character.getSlug());
		}
		if (character.isHidden()) {
			defaults.put("hidden", "true");
		}
	}

	private void loadInvestigationPoints(PlayerData pd, JSONObject playerJson) {
		if (playerJson.containsKey("investigation-points")) {
			pd.setInvestigationPoints(((Number) playerJson.get("investigation-points")).intValue());
		}
		if (playerJson.containsKey("investigation-regen-ms")) {
			pd.setLastInvestigationRegenMs(((Number) playerJson.get("investigation-regen-ms")).longValue());
		}
	}

	private void loadAccountProfessionPoints(PlayerData pd, JSONObject playerJson) {
		if (playerJson.containsKey("profession-points-initialized")) {
			pd.setProfessionPointsInitialized(Boolean.parseBoolean(playerJson.get("profession-points-initialized").toString()));
		}
		if (playerJson.containsKey("account-profession-points")) {
			JSONObject pointsJson = (JSONObject) playerJson.get("account-profession-points");
			for (Object key : pointsJson.keySet()) {
				pd.setAccountProfessionPoints(key.toString(), ((Number) pointsJson.get(key)).intValue());
			}
		}
	}

	@SuppressWarnings("unchecked")
	private JSONObject toAccountProfessionPointsJson(Map<String, Integer> points) {
		JSONObject object = new JSONObject();
		for (Map.Entry<String, Integer> entry : points.entrySet()) {
			object.put(entry.getKey(), entry.getValue());
		}
		return object;
	}

	private void loadProfessionFields(RPCharacter character, JSONObject characterJson) {
		if (!characterJson.containsKey("profession-upgrades")) {
			return;
		}
		JSONArray upgradeArray = (JSONArray) characterJson.get("profession-upgrades");
		List<String> upgrades = new ArrayList<>();
		for (int i = 0; i < upgradeArray.size(); i++) {
			upgrades.add(upgradeArray.get(i).toString());
		}
		character.setProfessionUpgrades(upgrades);
	}

	@SuppressWarnings("unchecked")
	private void saveProfessionFields(HashMap<String, Object> defaults, RPCharacter character) {
		if (character.getProfessionUpgrades().isEmpty()) {
			return;
		}
		JSONArray upgradeArray = new JSONArray();
		for (String upgradeId : character.getProfessionUpgrades()) {
			upgradeArray.add(upgradeId);
		}
		defaults.put("profession-upgrades", upgradeArray);
	}

	private void loadExtraAttributeAllocation(RPCharacter character, JSONObject characterJson) {
		if (!characterJson.containsKey("extra-attribute-allocation")) {
			return;
		}
		Object raw = characterJson.get("extra-attribute-allocation");
		if (!(raw instanceof JSONObject)) {
			return;
		}
		JSONObject allocationJson = (JSONObject) raw;
		Map<String, Integer> allocation = new HashMap<>();
		for (Object key : allocationJson.keySet()) {
			Object value = allocationJson.get(key);
			if (value instanceof Number) {
				int amount = ((Number) value).intValue();
				if (amount > 0) {
					allocation.put(key.toString(), amount);
				}
			}
		}
		character.setExtraAttributeAllocation(allocation);
	}

	@SuppressWarnings("unchecked")
	private void saveExtraAttributeAllocation(HashMap<String, Object> defaults, RPCharacter character) {
		if (character.getSpentExtraAttributePoints() <= 0) {
			return;
		}
		JSONObject allocationJson = new JSONObject();
		for (Map.Entry<String, Integer> entry : character.getExtraAttributeAllocation().entrySet()) {
			allocationJson.put(entry.getKey(), entry.getValue());
		}
		defaults.put("extra-attribute-allocation", allocationJson);
	}
}
