package net.tfminecraft.RPCharacters;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import net.tfminecraft.RPCharacters.Loaders.CalendarLoader;
import net.tfminecraft.RPCharacters.Loaders.ChatLoader;
import net.tfminecraft.RPCharacters.Loaders.ConfigLoader;
import net.tfminecraft.RPCharacters.Loaders.MaskLoader;
import net.tfminecraft.RPCharacters.Loaders.SkillPointTomeLoader;
import net.tfminecraft.RPCharacters.Loaders.PermissionGroupsLoader;
import net.tfminecraft.RPCharacters.Loaders.PersonaLoader;
import net.tfminecraft.RPCharacters.Loaders.ProfileLoader;
import net.tfminecraft.RPCharacters.Loaders.ProfileViewLoader;
import net.tfminecraft.RPCharacters.Loaders.RaceLoader;
import net.tfminecraft.RPCharacters.Loaders.RollLoader;
import net.tfminecraft.RPCharacters.Loaders.StageLoader;
import net.tfminecraft.RPCharacters.Loaders.TraitLoader;
import net.tfminecraft.RPCharacters.Managers.ClueInputManager;
import net.tfminecraft.RPCharacters.Managers.ClueItemListener;
import net.tfminecraft.RPCharacters.Managers.CommandManager;
import net.tfminecraft.RPCharacters.Managers.CreationManager;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Managers.SkillPointCommandListener;
import net.tfminecraft.RPCharacters.Managers.SkillPointTomeListener;
import net.tfminecraft.RPCharacters.Managers.SpawnedClueManager;
import net.tfminecraft.RPCharacters.chat.ChatCooldownManager;
import net.tfminecraft.RPCharacters.chat.ChatManager;
import net.tfminecraft.RPCharacters.conversation.ConversationManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Utils.CommandTabCompleter;
import net.tfminecraft.RPCharacters.identity.DisplayIdentityService;
import net.tfminecraft.RPCharacters.persona.PersonaCooldownManager;
import net.tfminecraft.RPCharacters.profile.ProfileManager;
import net.tfminecraft.RPCharacters.profile.ProfileViewCooldownManager;
import net.tfminecraft.RPCharacters.roll.RollManager;
import net.tfminecraft.RPCharacters.placeholder.RpCharactersExpansion;

public class RPCharacters extends JavaPlugin{
	public static RPCharacters plugin;
	
	private final CommandManager commandManager = new CommandManager();
	private static final PlayerManager playerManager = new PlayerManager();
	private final CreationManager creationManager = new CreationManager();
	private final ClueInputManager clueInputManager = new ClueInputManager();
	private final SpawnedClueManager spawnedClueManager = SpawnedClueManager.get();
	private final ClueItemListener clueItemListener = new ClueItemListener();
	private final SkillPointTomeListener skillPointTomeListener = new SkillPointTomeListener();
	private final SkillPointCommandListener skillPointCommandListener = new SkillPointCommandListener();
	private final ConversationManager conversationManager = new ConversationManager();
	private final ChatManager chatManager = new ChatManager();
	private final ProfileManager profileManager = new ProfileManager();
	private final RollManager rollManager = new RollManager();
	
	private final ConfigLoader configLoader = new ConfigLoader();
	private final StageLoader stageLoader = new StageLoader();
	private final RaceLoader raceLoader = new RaceLoader();
	private final TraitLoader traitLoader = new TraitLoader();
	private final ProfileLoader profileLoader = new ProfileLoader();
	private final PersonaLoader personaLoader = new PersonaLoader();
	private final PermissionGroupsLoader permissionGroupsLoader = new PermissionGroupsLoader();
	private final MaskLoader maskLoader = new MaskLoader();
	private final SkillPointTomeLoader skillPointTomeLoader = new SkillPointTomeLoader();
	private final ChatLoader chatLoader = new ChatLoader();
	private final ProfileViewLoader profileViewLoader = new ProfileViewLoader();
	private final RollLoader rollLoader = new RollLoader();
	private final CalendarLoader calendarLoader = new CalendarLoader();
	
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
		getCommand("roll").setExecutor(rollManager);
		registerPlaceholderApi();
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
		getServer().getPluginManager().registerEvents(skillPointTomeListener, this);
		getServer().getPluginManager().registerEvents(skillPointCommandListener, this);
		getServer().getPluginManager().registerEvents(commandManager, this);
		getServer().getPluginManager().registerEvents(spawnedClueManager, this);
		getServer().getPluginManager().registerEvents(conversationManager, this);
		getServer().getPluginManager().registerEvents(chatManager, this);
		getServer().getPluginManager().registerEvents(ChatCooldownManager.get(), this);
		getServer().getPluginManager().registerEvents(profileManager, this);
		getServer().getPluginManager().registerEvents(ProfileViewCooldownManager.get(), this);
		getServer().getPluginManager().registerEvents(PersonaCooldownManager.get(), this);
		getServer().getPluginManager().registerEvents(rollManager, this);
	}
	public void startManagers() {
		playerManager.start();
		spawnedClueManager.startTicks();
	}
	public void loadConfigs() {
		configLoader.load(new File(getDataFolder(), "config.yml"));
		personaLoader.load(new File(getDataFolder(), "persona.yml"));
		permissionGroupsLoader.load(new File(getDataFolder(), "permission-groups.yml"));
		maskLoader.load(new File(getDataFolder(), "masks.yml"));
		skillPointTomeLoader.load(new File(getDataFolder(), "items.yml"));
		chatLoader.load(new File(getDataFolder(), "chat.yml"));
		profileViewLoader.load(new File(getDataFolder(), "profile-view.yml"));
		rollLoader.load(new File(getDataFolder(), "rolls.yml"));
		calendarLoader.load(new File(getDataFolder(), "calendar.yml"));
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
				"profile.yml",
				"persona.yml",
				"masks.yml",
				"items.yml",
				"chat.yml",
				"profile-view.yml",
				"rolls.yml",
				"calendar.yml",
				"permission-groups.yml"
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

	public static RPCharacter getActiveCharacter(Player player) {
		if (player == null) {
			return null;
		}
		PlayerData data = PlayerManager.get(player);
		return data != null ? data.getActiveCharacter() : null;
	}

	public static String getCharacterName(Player player) {
		return DisplayIdentityService.resolveCharacterName(player);
	}

	public static String getDisplay(Player player) {
		return DisplayIdentityService.resolveDisplay(player);
	}

	public static String getDisplayNoMask(Player player) {
		return DisplayIdentityService.resolveDisplayNoMask(player);
	}

	private void registerPlaceholderApi() {
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
			new RpCharactersExpansion().register();
			getLogger().info("Registered PlaceholderAPI expansion: rpcharacters");
		} else {
			getLogger().warning("PlaceholderAPI not found — %rpcharacters_*% placeholders will not work.");
		}
	}
}
