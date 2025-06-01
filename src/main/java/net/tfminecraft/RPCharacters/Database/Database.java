package net.tfminecraft.RPCharacters.Database;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.tfminecraft.RPCharacters.Loaders.RaceLoader;
import net.tfminecraft.RPCharacters.Loaders.TraitLoader;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Races.Race;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.Utils.Integrator;
import net.tfminecraft.RPCharacters.enums.Status;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class Database {
	private JSONObject json; // org.json.simple
    JSONParser parser = new JSONParser();
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
				int cooldown = (int) Math.round(((Double) json.get("cooldown")));
				List<String> completedStages = new ArrayList<>();
				int i = 0;
				JSONArray stageArray = (JSONArray) json.get("completed stages");
				while(i < stageArray.size()) {
					completedStages.add(stageArray.get(i).toString());
					i++;
				}
				PlayerData pd = new PlayerData(p, completedStages, cooldown);
				loadCharacters(pd);
				return pd;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
        }
		return null;
	}
	public void loadCharacters(PlayerData pd) {
		File folder = new File("plugins/RPCharacters/data/characterdata", pd.getPlayer().getUniqueId().toString());
    	for (final File file : folder.listFiles()) {
            if (!file.isDirectory()) {
            	try {
    				json = (JSONObject) parser.parse(new InputStreamReader(new FileInputStream(file), "UTF-8"));
    				String id = (String) json.get("id");
    				String name = (String) json.get("name");
    				Status status = Status.valueOf(((String) json.get("status")).toUpperCase());
    				Boolean active = Boolean.parseBoolean((String) json.get("active"));
    				Race r = RaceLoader.getByString((String) json.get("race"));
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
    				RPCharacter c = new RPCharacter(pd.getPlayer(), id, name, active, status, r, traits);
    				if(c.isActive()) {
    					Integrator integrator = new Integrator();
    					integrator.integrate(pd.getPlayer(), c);
    				}
    				pd.addCharacter(c);
    				if(active) {
    					pd.setActive(true);
    				}
    			} catch (Exception ex) {
    				ex.printStackTrace();
    			}
            }
        }
	}
	@SuppressWarnings("unchecked")
	public void savePlayer(PlayerData pd) {
		try {
			File subFolder = new File("plugins/RPCharacters/data/characterdata", pd.getPlayer().getUniqueId().toString());
			if(!subFolder.exists()) subFolder.mkdir();
			File file = new File("plugins/RPCharacters/data/playerdata", pd.getPlayer().getUniqueId().toString()+".json");
			file.createNewFile();
        	PrintWriter pw = new PrintWriter(file, "UTF-8");
        	pw.print("{");
        	pw.print("}");
        	pw.flush();
        	pw.close();
            HashMap<String, Object> defaults = new HashMap<String, Object>();
        	json = (JSONObject) parser.parse(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        	defaults.put("cooldown", pd.getRemainingTime());
        	int i = 0;
        	JSONArray stageArray = new JSONArray();
        	while(i < pd.getCompletedStages().size()) {
        		stageArray.add(pd.getCompletedStages().get(i));
        		i++;
        	}
        	defaults.put("completed stages", stageArray);
        	for(RPCharacter c : pd.getCharacters()) {
        		saveCharacter(pd, c);
        		if(c.isActive()) {
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
			File file = new File("plugins/RPCharacters/data/characterdata", pd.getPlayer().getUniqueId().toString()+"/"+c.getId()+".json");
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
        	int i = 0;
        	JSONArray traitArray = new JSONArray();
        	while(i < c.getTraits().size()) {
        		traitArray.add(c.getTraits().get(i).getId());
        		i++;
        	}
        	defaults.put("traits", traitArray);
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
}
