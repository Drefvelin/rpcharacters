package net.tfminecraft.RPCharacters;

import java.io.File;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.tfminecraft.RPCharacters.Loaders.ConfigLoader;
import net.tfminecraft.RPCharacters.Loaders.ProfileLoader;
import net.tfminecraft.RPCharacters.Loaders.RaceLoader;
import net.tfminecraft.RPCharacters.Loaders.StageLoader;
import net.tfminecraft.RPCharacters.Loaders.TraitLoader;
import net.tfminecraft.RPCharacters.Managers.CommandManager;
import net.tfminecraft.RPCharacters.Managers.CreationManager;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Utils.CommandTabCompleter;

public class RPCharacters extends JavaPlugin{
	public static RPCharacters plugin;
	
	private final CommandManager commandManager = new CommandManager();
	private static final PlayerManager playerManager = new PlayerManager();
	private final CreationManager creationManager = new CreationManager();
	
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
		loadPlayers();
		startManagers();
		getCommand(commandManager.cmd1).setExecutor(commandManager);
		getCommand("rpcharacter").setTabCompleter(new CommandTabCompleter());
	}
	@Override
	public void onDisable() {
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
		getServer().getPluginManager().registerEvents(commandManager, this);
		
	}
	public void startManagers() {
		playerManager.start();
		
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
}
