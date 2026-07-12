package net.tfminecraft.RPCharacters;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.tfminecraft.RPCharacters.Loaders.ConfigLoader;
import net.tfminecraft.RPCharacters.Loaders.ProfileLoader;
import net.tfminecraft.RPCharacters.Loaders.RaceLoader;
import net.tfminecraft.RPCharacters.Loaders.StageLoader;
import net.tfminecraft.RPCharacters.Loaders.TraitLoader;
import net.tfminecraft.RPCharacters.Managers.ClueInputManager;
import net.tfminecraft.RPCharacters.Managers.ClueItemListener;
import net.tfminecraft.RPCharacters.Managers.CommandManager;
import net.tfminecraft.RPCharacters.Managers.CreationManager;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Managers.SpawnedClueManager;
import net.tfminecraft.RPCharacters.conversation.ConversationManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Utils.CommandTabCompleter;

public class RPCharacters extends JavaPlugin{
	public static RPCharacters plugin;
	
	private final CommandManager commandManager = new CommandManager();
	private static final PlayerManager playerManager = new PlayerManager();
	private final CreationManager creationManager = new CreationManager();
	private final ClueInputManager clueInputManager = new ClueInputManager();
	private final SpawnedClueManager spawnedClueManager = SpawnedClueManager.get();
	private final ClueItemListener clueItemListener = new ClueItemListener();
	private final ConversationManager conversationManager = new ConversationManager();
	
	private final ConfigLoader configLoader = new ConfigLoader();
	private final StageLoader stageLoader = new StageLoader();
	private final RaceLoader raceLoader = new RaceLoader();
	private final TraitLoader traitLoader = new TraitLoader();
	private final ProfileLoader profileLoader = new ProfileLoader();
	
	@Override
	public void onEnable() {
		plugin = this;
		createFolders();
		createConfigs();
		registerListeners();
		loadConfigs();
		spawnedClueManager.loadAllFromDisk();
		loadPlayers();
		startManagers();
		getCommand(commandManager.cmd1).setExecutor(commandManager);
		getCommand("rpcharacter").setTabCompleter(new CommandTabCompleter());
	}
	@Override
	public void onDisable() {
		spawnedClueManager.shutdown();
		save();
	}
	
	public void save() {
		for(Player p : Bukkit.getOnlinePlayers()) {
			playerManager.savePlayer(p);
		}
	}
	public void loadPlayers() {
		for(Player p : Bukkit.getOnlinePlayers()) {
			playerManager.initiatePlayer(p);
		}
	}
	
	public void registerListeners() {
		getServer().getPluginManager().registerEvents(playerManager, this);
		getServer().getPluginManager().registerEvents(creationManager, this);
		getServer().getPluginManager().registerEvents(clueInputManager, this);
		getServer().getPluginManager().registerEvents(clueItemListener, this);
		getServer().getPluginManager().registerEvents(commandManager, this);
		getServer().getPluginManager().registerEvents(spawnedClueManager, this);
		getServer().getPluginManager().registerEvents(conversationManager, this);
		
	}
	public void startManagers() {
		playerManager.start();
		spawnedClueManager.startTicks();
	}
	public void loadConfigs() {
		configLoader.load(new File(getDataFolder(), "config.yml"));
		profileLoader.load(new File(getDataFolder(), "profile.yml"));
		raceLoader.load(new File(getDataFolder(), "races.yml"));
		File folder = new File(getDataFolder(), "traits");
    	for (final File file : folder.listFiles()) {
    		if(!file.isDirectory()) {
    			traitLoader.load(file);
    		}
    	}
		stageLoader.load(new File(getDataFolder(), "stages.yml"));
	}
	
	public void createFolders() {
		if (!getDataFolder().exists()) getDataFolder().mkdir();
		File subFolder = new File(getDataFolder(), "traits");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "data");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "data/playerdata");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "data/characterdata");
		if(!subFolder.exists()) subFolder.mkdir();
	}
	
	public void createConfigs() {
		String[] files = {
				"stages.yml",
				"races.yml",
				"config.yml",
				"profile.yml"
				};
		for(String s : files) {
			File newConfigFile = new File(getDataFolder(), s);
	        if (!newConfigFile.exists()) {
	        	newConfigFile.getParentFile().mkdirs();
	            saveResource(s, false);
	        }
		}
	}
	
	public void reload() {
		loadConfigs();
	}
	public void reloadMessage(Player p) {
		p.sendMessage(ChatColor.GREEN + "[RPCharacters]" + ChatColor.YELLOW + " Reloading plugin...");
		reload();
		p.sendMessage(ChatColor.GREEN + "[RPCharacters]" + ChatColor.YELLOW + " Reloading complete!");
	}

	public static PlayerManager getPlayerManager() {
		return playerManager;
	}

	public static SpawnedClueManager getSpawnedClueManager() {
		return SpawnedClueManager.get();
	}

	public static int getAccountPlaytimeSeconds(Player player) {
		if (player == null) {
			return 0;
		}
		PlayerData data = PlayerManager.get(player);
		return data != null ? data.getAccountPlaytimeSeconds() : 0;
	}

	public static int getCharacterPlaytimeSeconds(RPCharacter character) {
		return character != null ? character.getPlaytimeSeconds() : 0;
	}

	public static int getConversationCount(RPCharacter character, String otherCharacterId) {
		return character != null ? character.getConversationCount(otherCharacterId) : 0;
	}

	public static List<Map.Entry<String, Integer>> getTopConversationPartners(RPCharacter character, int limit) {
		return ConversationManager.getTopPartners(character, limit);
	}
}
