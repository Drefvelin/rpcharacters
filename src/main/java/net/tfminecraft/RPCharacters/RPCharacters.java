package net.tfminecraft.RPCharacters;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import net.tfminecraft.RPCharacters.Loaders.CalendarLoader;
import net.tfminecraft.RPCharacters.Loaders.ChatLoader;
import net.tfminecraft.RPCharacters.Loaders.ConfigLoader;
import net.tfminecraft.RPCharacters.Loaders.MaskLoader;
import net.tfminecraft.RPCharacters.Loaders.AttributePointTomeLoader;
import net.tfminecraft.RPCharacters.Loaders.RemedyLoader;
import net.tfminecraft.RPCharacters.Loaders.SkillPointTomeLoader;
import net.tfminecraft.RPCharacters.Loaders.PermissionGroupsLoader;
import net.tfminecraft.RPCharacters.Loaders.WebCreatorLoader;
import net.tfminecraft.RPCharacters.Loaders.PersonaLoader;
import net.tfminecraft.RPCharacters.Loaders.ProfessionLoader;
import net.tfminecraft.RPCharacters.Loaders.ProfessionsGlobalLoader;
import net.tfminecraft.RPCharacters.Loaders.ProfileLoader;
import net.tfminecraft.RPCharacters.Loaders.ProfileViewLoader;
import net.tfminecraft.RPCharacters.Loaders.RaceLoader;
import net.tfminecraft.RPCharacters.Loaders.RollLoader;
import net.tfminecraft.RPCharacters.Loaders.SpeechBubbleLoader;
import net.tfminecraft.RPCharacters.Loaders.SmartMessageLoader;
import net.tfminecraft.RPCharacters.Loaders.StageLoader;
import net.tfminecraft.RPCharacters.Loaders.TraitLoader;
import net.tfminecraft.RPCharacters.Managers.ClueInputManager;
import net.tfminecraft.RPCharacters.Loaders.ClueDiscoveryLoader;
import net.tfminecraft.RPCharacters.Loaders.MagnifyingGlassLoader;
import net.tfminecraft.RPCharacters.Loaders.InjuryPoolLoader;
import net.tfminecraft.RPCharacters.Loaders.InjuryProgressionLoader;
import net.tfminecraft.RPCharacters.Loaders.FuelTemplateLoader;
import net.tfminecraft.RPCharacters.Loaders.ProstheticLoader;
import net.tfminecraft.RPCharacters.Loaders.KitLoader;
import net.tfminecraft.RPCharacters.Loaders.PermadeathZoneLoader;
import net.tfminecraft.RPCharacters.Loaders.PvpLoader;
import net.tfminecraft.RPCharacters.Managers.CommandManager;
import net.tfminecraft.RPCharacters.Managers.CreationManager;
import net.tfminecraft.RPCharacters.Managers.ClueDisturbanceListener;
import net.tfminecraft.RPCharacters.Managers.MagnifyingGlassListener;
import net.tfminecraft.RPCharacters.Managers.PlaceClueManager;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Managers.AttributePointCommandListener;
import net.tfminecraft.RPCharacters.Managers.AttributePointSpendListener;
import net.tfminecraft.RPCharacters.Managers.AttributePointTomeListener;
import net.tfminecraft.RPCharacters.Managers.RemedyListener;
import net.tfminecraft.RPCharacters.permadeath.PermadeathZoneListener;
import net.tfminecraft.RPCharacters.permadeath.WorldGuardBridge;
import net.tfminecraft.RPCharacters.prosthetics.ProstheticInstallListener;
import net.tfminecraft.RPCharacters.prosthetics.ProstheticRefuelListener;
import net.tfminecraft.RPCharacters.injuries.RpInjureListener;
import net.tfminecraft.RPCharacters.Managers.SkillPointCommandListener;
import net.tfminecraft.RPCharacters.Managers.SkillPointTomeListener;
import net.tfminecraft.RPCharacters.Managers.SpawnedClueManager;
import net.tfminecraft.RPCharacters.chat.ChatChannelCommandHandler;
import net.tfminecraft.RPCharacters.chat.ChatChannelPreferenceManager;
import net.tfminecraft.RPCharacters.chat.ChatCooldownManager;
import net.tfminecraft.RPCharacters.chat.ChatManager;
import net.tfminecraft.RPCharacters.conversation.ConversationManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Utils.CommandTabCompleter;
import net.tfminecraft.RPCharacters.Utils.RPTexts;
import net.tfminecraft.RPCharacters.api.CharacterSkull;
import net.tfminecraft.RPCharacters.identity.DisplayIdentityService;
import net.tfminecraft.RPCharacters.mail.CharacterMailTarget;
import net.tfminecraft.RPCharacters.mail.MailRecipientDirectory;
import net.tfminecraft.RPCharacters.persona.PersonaCooldownManager;
import net.tfminecraft.RPCharacters.profile.ProfileManager;
import net.tfminecraft.RPCharacters.profile.ProfileViewCooldownManager;
import net.tfminecraft.RPCharacters.mmocore.AttributePointService;
import net.tfminecraft.RPCharacters.professions.ProfessionCommandHandler;
import net.tfminecraft.RPCharacters.professions.ProfessionEffectService;
import net.tfminecraft.RPCharacters.professions.ProfessionListener;
import net.tfminecraft.RPCharacters.grave.GraveDeathListener;
import net.tfminecraft.RPCharacters.grave.GraveInsuranceListener;
import net.tfminecraft.RPCharacters.grave.GraveInteractListener;
import net.tfminecraft.RPCharacters.grave.GraveLoader;
import net.tfminecraft.RPCharacters.grave.GraveManager;
import net.tfminecraft.RPCharacters.grave.GraveVisualManager;
import net.tfminecraft.RPCharacters.grave.LastSolidTracker;
import net.tfminecraft.RPCharacters.pvp.PvpCommand;
import net.tfminecraft.RPCharacters.pvp.PvpKnockoutManager;
import net.tfminecraft.RPCharacters.roll.RollManager;
import net.tfminecraft.RPCharacters.placeholder.RpCharactersExpansion;
import net.tfminecraft.RPCharacters.speechbubble.SpeechBubbleListener;
import net.tfminecraft.RPCharacters.speechbubble.SpeechBubbleManager;
import net.tfminecraft.RPCharacters.speechbubble.fake.FakeBubbleManager;
import net.tfminecraft.RPCharacters.speechbubble.fake.ProtocolLibBridge;
import net.tfminecraft.RPCharacters.wardrobe.WardrobeListener;
import net.tfminecraft.RPCharacters.wardrobe.WardrobeService;

public class RPCharacters extends JavaPlugin{
	public static RPCharacters plugin;
	
	private final CommandManager commandManager = new CommandManager();
	private static final PlayerManager playerManager = new PlayerManager();
	private final CreationManager creationManager = new CreationManager();
	private final ClueInputManager clueInputManager = new ClueInputManager();
	private final PlaceClueManager placeClueManager = new PlaceClueManager();
	private final MagnifyingGlassListener magnifyingGlassListener = new MagnifyingGlassListener();
	private final ClueDisturbanceListener clueDisturbanceListener = new ClueDisturbanceListener();
	private final SpawnedClueManager spawnedClueManager = SpawnedClueManager.get();
	private final net.tfminecraft.RPCharacters.playtime.PlaytimeListener playtimeListener =
			new net.tfminecraft.RPCharacters.playtime.PlaytimeListener();
	private final SkillPointTomeListener skillPointTomeListener = new SkillPointTomeListener();
	private final SkillPointCommandListener skillPointCommandListener = new SkillPointCommandListener();
	private final AttributePointTomeListener attributePointTomeListener = new AttributePointTomeListener();
	private final RemedyListener remedyListener = new RemedyListener();
	private final ProstheticRefuelListener prostheticRefuelListener = new ProstheticRefuelListener();
	private final ProstheticInstallListener prostheticInstallListener = new ProstheticInstallListener();
	private final PermadeathZoneListener permadeathZoneListener = new PermadeathZoneListener();
	private final AttributePointCommandListener attributePointCommandListener = new AttributePointCommandListener();
	private final AttributePointSpendListener attributePointSpendListener = new AttributePointSpendListener();
	private final ConversationManager conversationManager = new ConversationManager();
	private final ChatManager chatManager = new ChatManager();
	private final ProfileManager profileManager = new ProfileManager();
	private final RollManager rollManager = new RollManager();
	private final ProfessionListener professionListener = new ProfessionListener();
	private ProfessionEffectService professionEffectService;
	private final ProfessionCommandHandler professionCommandHandler = new ProfessionCommandHandler();
	private final SpeechBubbleListener speechBubbleListener = new SpeechBubbleListener();
	private final ChatChannelCommandHandler chatChannelCommandHandler = new ChatChannelCommandHandler();
	private final WardrobeListener wardrobeListener = new WardrobeListener();
	private final RpInjureListener rpInjureListener = new RpInjureListener();

	private ConfigLoader configLoader;
	private StageLoader stageLoader;
	private RaceLoader raceLoader;
	private TraitLoader traitLoader;
	private ProfileLoader profileLoader;
	private PersonaLoader personaLoader;
	private PermissionGroupsLoader permissionGroupsLoader;
	private WebCreatorLoader webCreatorLoader;
	private MaskLoader maskLoader;
	private SkillPointTomeLoader skillPointTomeLoader;
	private AttributePointTomeLoader attributePointTomeLoader;
	private RemedyLoader remedyLoader;
	private ChatLoader chatLoader;
	private ProfileViewLoader profileViewLoader;
	private RollLoader rollLoader;
	private CalendarLoader calendarLoader;
	private ProfessionsGlobalLoader professionsGlobalLoader;
	private SpeechBubbleLoader speechBubbleLoader;
	private SmartMessageLoader smartMessageLoader;
	private ClueDiscoveryLoader clueDiscoveryLoader;
	private MagnifyingGlassLoader magnifyingGlassLoader;
	private PermadeathZoneLoader permadeathZoneLoader;
	private InjuryPoolLoader injuryPoolLoader;
	private FuelTemplateLoader fuelTemplateLoader;
	private InjuryProgressionLoader injuryProgressionLoader;
	private ProstheticLoader prostheticLoader;
	private KitLoader kitLoader;
	private PvpLoader pvpLoader;
	private GraveLoader graveLoader;
	private final PvpCommand pvpCommand = new PvpCommand();
	private final PvpKnockoutManager pvpKnockoutManager = new PvpKnockoutManager();

	private void initDependencyComponents() {
		if (configLoader != null) {
			return;
		}
		professionEffectService = new ProfessionEffectService();
		configLoader = new ConfigLoader();
		stageLoader = new StageLoader();
		raceLoader = new RaceLoader();
		traitLoader = new TraitLoader();
		profileLoader = new ProfileLoader();
		personaLoader = new PersonaLoader();
		permissionGroupsLoader = new PermissionGroupsLoader();
		webCreatorLoader = new WebCreatorLoader();
		maskLoader = new MaskLoader();
		skillPointTomeLoader = new SkillPointTomeLoader();
		attributePointTomeLoader = new AttributePointTomeLoader();
		remedyLoader = new RemedyLoader();
		chatLoader = new ChatLoader();
		profileViewLoader = new ProfileViewLoader();
		rollLoader = new RollLoader();
		calendarLoader = new CalendarLoader();
		professionsGlobalLoader = new ProfessionsGlobalLoader();
		speechBubbleLoader = new SpeechBubbleLoader();
		smartMessageLoader = new SmartMessageLoader();
		clueDiscoveryLoader = new ClueDiscoveryLoader();
		magnifyingGlassLoader = new MagnifyingGlassLoader();
		permadeathZoneLoader = new PermadeathZoneLoader();
		injuryPoolLoader = new InjuryPoolLoader();
		fuelTemplateLoader = new FuelTemplateLoader();
		injuryProgressionLoader = new InjuryProgressionLoader();
		prostheticLoader = new ProstheticLoader();
		kitLoader = new KitLoader();
		pvpLoader = new PvpLoader();
		graveLoader = new GraveLoader();
	}
	
	@Override
	public void onEnable() {
		plugin = this;
		initDependencyComponents();
		createFolders();
		createConfigs();
		registerListeners();
		loadConfigs();
		spawnedClueManager.loadAllFromDisk();
		net.tfminecraft.RPCharacters.playtime.PlaytimeService.loadAllFromDisk();
		GraveManager.get().loadAll();
		MailRecipientDirectory.scanFromDisk();
		loadPlayers();
		startManagers();
		getCommand(commandManager.cmd1).setExecutor(commandManager);
		getCommand("rpcharacter").setTabCompleter(new CommandTabCompleter());
		getCommand("roll").setExecutor(rollManager);
		getCommand(ProfessionCommandHandler.COMMAND).setExecutor(professionCommandHandler);
		getCommand(ProfessionCommandHandler.COMMAND).setTabCompleter(professionCommandHandler);
		getCommand("channel").setExecutor(chatChannelCommandHandler);
		getCommand("channel").setTabCompleter(chatChannelCommandHandler);
		getCommand("channeltoggle").setExecutor(chatChannelCommandHandler);
		getCommand("channeltoggle").setTabCompleter(chatChannelCommandHandler);
		getCommand(PvpCommand.COMMAND).setExecutor(pvpCommand);
		getCommand(PvpCommand.COMMAND).setTabCompleter(pvpCommand);
		registerPlaceholderApi();
	}
	@Override
	public void onDisable() {
		net.tfminecraft.RPCharacters.ingest.CharacterIngestService.stopPeriodicPull();
		WardrobeService.stopSoftRefresh();
		ProtocolLibBridge.shutdown();
		GraveVisualManager.get().shutdown();
		SpeechBubbleManager.get().shutdown();
		net.tfminecraft.RPCharacters.clues.discovery.ClueDiscoveryVisualManager.get().shutdown();
		spawnedClueManager.shutdown();
		net.tfminecraft.RPCharacters.playtime.PlaytimeService.shutdown();
		pvpKnockoutManager.shutdown();
		LastSolidTracker.get().shutdown();
		GraveManager.get().saveAll();
		save();
	}
	
	public void save() {
		for(Player p : Bukkit.getOnlinePlayers()) {
			PlayerManager.stampActiveCharacterLocation(p);
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
		getServer().getPluginManager().registerEvents(placeClueManager, this);
		getServer().getPluginManager().registerEvents(magnifyingGlassListener, this);
		getServer().getPluginManager().registerEvents(clueDisturbanceListener, this);
		getServer().getPluginManager().registerEvents(skillPointTomeListener, this);
		getServer().getPluginManager().registerEvents(skillPointCommandListener, this);
		getServer().getPluginManager().registerEvents(attributePointTomeListener, this);
		getServer().getPluginManager().registerEvents(remedyListener, this);
		getServer().getPluginManager().registerEvents(prostheticRefuelListener, this);
		getServer().getPluginManager().registerEvents(prostheticInstallListener, this);
		getServer().getPluginManager().registerEvents(permadeathZoneListener, this);
		getServer().getPluginManager().registerEvents(attributePointCommandListener, this);
		getServer().getPluginManager().registerEvents(attributePointSpendListener, this);
		getServer().getPluginManager().registerEvents(commandManager, this);
		getServer().getPluginManager().registerEvents(spawnedClueManager, this);
		getServer().getPluginManager().registerEvents(playtimeListener, this);
		getServer().getPluginManager().registerEvents(conversationManager, this);
		getServer().getPluginManager().registerEvents(chatManager, this);
		getServer().getPluginManager().registerEvents(ChatCooldownManager.get(), this);
		getServer().getPluginManager().registerEvents(ChatChannelPreferenceManager.get(), this);
		getServer().getPluginManager().registerEvents(profileManager, this);
		getServer().getPluginManager().registerEvents(ProfileViewCooldownManager.get(), this);
		getServer().getPluginManager().registerEvents(PersonaCooldownManager.get(), this);
		getServer().getPluginManager().registerEvents(professionListener, this);
		getServer().getPluginManager().registerEvents(professionEffectService, this);
		getServer().getPluginManager().registerEvents(speechBubbleListener, this);
		getServer().getPluginManager().registerEvents(wardrobeListener, this);
		getServer().getPluginManager().registerEvents(rpInjureListener, this);
		getServer().getPluginManager().registerEvents(pvpKnockoutManager, this);
		getServer().getPluginManager().registerEvents(pvpCommand, this);
		getServer().getPluginManager().registerEvents(new GraveDeathListener(), this);
		getServer().getPluginManager().registerEvents(new GraveInteractListener(), this);
		getServer().getPluginManager().registerEvents(new GraveInsuranceListener(), this);
	}
	public void startManagers() {
		playerManager.start();
		spawnedClueManager.startTicks();
		net.tfminecraft.RPCharacters.playtime.PlaytimeService.startTicks();
		SpeechBubbleManager.get().startTicks();
		ProtocolLibBridge.init(this);
		GraveVisualManager.get().startTicks();
		FakeBubbleManager.get().startTicks();
		net.tfminecraft.RPCharacters.ingest.CharacterIngestService.startPeriodicPull(this);
		WardrobeService.startSoftRefresh(this);
		pvpKnockoutManager.start();
		LastSolidTracker.get().start();
	}
	public void loadConfigs() {
		configLoader.load(new File(getDataFolder(), "config.yml"));
		personaLoader.load(new File(getDataFolder(), "persona.yml"));
		permissionGroupsLoader.load(new File(getDataFolder(), "permission-groups.yml"));
		webCreatorLoader.load(new File(getDataFolder(), "web-creator.yml"));
		maskLoader.load(new File(getDataFolder(), "masks.yml"));
		skillPointTomeLoader.load(new File(getDataFolder(), "items.yml"));
		attributePointTomeLoader.load(new File(getDataFolder(), "items.yml"));
		magnifyingGlassLoader.load(new File(getDataFolder(), "items.yml"));
		permadeathZoneLoader.load(new File(getDataFolder(), "zones.yml"));
		clueDiscoveryLoader.load(new File(getDataFolder(), "clue-discovery.yml"));
		chatLoader.load(new File(getDataFolder(), "chat.yml"));
		speechBubbleLoader.load(new File(getDataFolder(), "speechbubbles.yml"));
		smartMessageLoader.load(new File(getDataFolder(), "smart-messages.yml"));
		profileViewLoader.load(new File(getDataFolder(), "profile-view.yml"));
		rollLoader.load(new File(getDataFolder(), "rolls.yml"));
		calendarLoader.load(new File(getDataFolder(), "calendar.yml"));
		File professionsGlobal = new File(getDataFolder(), "professions.yml");
		if (professionsGlobal.isFile()) {
			professionsGlobalLoader.load(professionsGlobal);
		}
		ProfessionLoader.reload(new File(getDataFolder(), "professions"));
		profileLoader.load(new File(getDataFolder(), "profile.yml"));
		raceLoader.load(new File(getDataFolder(), "races.yml"));
		File folder = new File(getDataFolder(), "traits");
		TraitLoader.oList.clear();
		File[] traitFiles = folder.listFiles();
		if (traitFiles != null) {
			for (final File file : traitFiles) {
				if (!file.isDirectory()) {
					traitLoader.load(file);
				}
			}
		}
		fuelTemplateLoader.load(new File(getDataFolder(), "fuel-templates.yml"));
		injuryProgressionLoader.load(new File(getDataFolder(), "injury-progression.yml"));
		prostheticLoader.load(new File(getDataFolder(), "prosthetics.yml"));
		injuryPoolLoader.load(new File(getDataFolder(), "injuries.yml"));
		remedyLoader.load(new File(getDataFolder(), "items.yml"));
		StageLoader.oList.clear();
		stageLoader.load(new File(getDataFolder(), "stages.yml"));
		kitLoader.loadPreferred(getDataFolder());
		pvpLoader.load(new File(getDataFolder(), "pvp.yml"));
		graveLoader.load(new File(getDataFolder(), "graves.yml"));
		WorldGuardBridge.init();
		net.tfminecraft.RPCharacters.catalog.CreationCatalogSyncService.pushAsync(this);
		net.tfminecraft.RPCharacters.ingest.CharacterIngestService.tryPullAsync(this);
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
		subFolder = new File(getDataFolder(), "professions");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "assets");
		if(!subFolder.exists()) subFolder.mkdir();
		subFolder = new File(getDataFolder(), "graves");
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
				"clue-discovery.yml",
				"chat.yml",
				"speechbubbles.yml",
				"smart-messages.yml",
				"profile-view.yml",
				"rolls.yml",
				"calendar.yml",
				"permission-groups.yml",
				"web-creator.yml",
				"zones.yml",
				"injuries.yml",
				"injury-progression.yml",
				"fuel-templates.yml",
				"prosthetics.yml",
				"kits.yml",
				"pvp.yml",
				"graves.yml"
				};
		for(String s : files) {
			File newConfigFile = new File(getDataFolder(), s);
	        if (!newConfigFile.exists()) {
	        	newConfigFile.getParentFile().mkdirs();
	            saveResource(s, false);
	        }
		}
		File knifeSkin = new File(getDataFolder(), "assets/knife_skin.png");
		if (!knifeSkin.exists()) {
			knifeSkin.getParentFile().mkdirs();
			saveResource("assets/knife_skin.png", false);
		}
		File journalSkin = new File(getDataFolder(), "assets/journal_skin.png");
		if (!journalSkin.exists()) {
			journalSkin.getParentFile().mkdirs();
			saveResource("assets/journal_skin.png", false);
		}
		File journalSkinSigned = new File(getDataFolder(), "assets/journal_skin_signed.png");
		if (!journalSkinSigned.exists()) {
			journalSkinSigned.getParentFile().mkdirs();
			saveResource("assets/journal_skin_signed.png", false);
		}
		File maskedSkin = new File(getDataFolder(), "assets/masked.png");
		if (!maskedSkin.exists()) {
			maskedSkin.getParentFile().mkdirs();
			saveResource("assets/masked.png", false);
		}
		String[] traitFiles = {
				"ambition-traits.yml",
				"attributes-traits.yml",
				"cataclysm-traits.yml",
				"celestial-traits.yml",
				"combat-traits.yml",
				"evil-traits.yml",
				"expedition-traits.yml",
				"gift-traits.yml",
				"homeland-traits.yml",
				"injury-traits.yml",
				"motivation-traits.yml",
				"personality-traits.yml",
				"physical-traits.yml",
				"prosthetic-traits.yml",
				"reclamation-traits.yml",
				"virtue-traits.yml"
		};
		for (String traitFile : traitFiles) {
			File traitConfig = new File(getDataFolder(), "traits/" + traitFile);
			if (!traitConfig.exists()) {
				traitConfig.getParentFile().mkdirs();
				saveResource("traits/" + traitFile, false);
			}
		}
	}
	
	public void reload() {
		loadConfigs();
		LastSolidTracker.get().start();
		ProfessionCommandHandler.reapplyActiveCharacterPerms();
		// Catalog + pending pull already run inside loadConfigs(); also refresh website sheets.
		net.tfminecraft.RPCharacters.ingest.RosterSyncService.pushAllOnlineAsync();
	}

	public void reloadConfigs(CommandSender sender) {
		String name = sender != null ? sender.getName() : "unknown";
		getLogger().info("Config reload requested by " + name);
		if (sender != null) {
			RPTexts.sendPrefixed(sender, RPTexts.WARN + "Reloading configs...");
		}
		try {
			reload();
			getLogger().info("Config reload complete (catalog + online roster sync kicked).");
			if (sender != null) {
				RPTexts.sendPrefixed(sender, RPTexts.WARN + "Reloading complete!");
			}
		} catch (Exception e) {
			getLogger().severe("Config reload failed: " + e.getMessage());
			e.printStackTrace();
			if (sender != null) {
				RPTexts.sendPrefixed(sender, RPTexts.ERROR + "Reload failed. Check console.");
			}
		}
	}

	public static PlayerManager getPlayerManager() {
		return playerManager;
	}

	/** External Discord gate (TFMCWeb); see {@link PlayerManager#setDiscordGate(Player, boolean)}. */
	public static void setDiscordGate(Player player, boolean required) {
		playerManager.setDiscordGate(player, required);
	}

	/** External Discord gate by UUID (online reevaluate if present). */
	public static void setDiscordGate(java.util.UUID id, boolean required) {
		playerManager.setDiscordGate(id, required);
	}

	public static boolean isDiscordGate(java.util.UUID id) {
		return playerManager.isDiscordGate(id);
	}

	public static SpawnedClueManager getSpawnedClueManager() {
		return SpawnedClueManager.get();
	}

	public static PlaceClueManager getPlaceClueManager() {
		return plugin.placeClueManager;
	}

	public static int getAccountAgeSeconds(Player player) {
		if (player == null) {
			return 0;
		}
		PlayerData data = PlayerManager.get(player);
		return data != null ? data.getAgeSeconds() : 0;
	}

	public static int getCharacterAgeSeconds(RPCharacter character) {
		return character != null ? character.getAgeSeconds() : 0;
	}

	/** @deprecated use {@link #getAccountAgeSeconds(Player)} */
	@Deprecated
	public static int getAccountPlaytimeSeconds(Player player) {
		return getAccountAgeSeconds(player);
	}

	/** @deprecated use {@link #getCharacterAgeSeconds(RPCharacter)} */
	@Deprecated
	public static int getCharacterPlaytimeSeconds(RPCharacter character) {
		return getCharacterAgeSeconds(character);
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

	/** Player head for the viewer's active character, or their account head. */
	public static ItemStack getSkull(Player player) {
		return CharacterSkull.ofActive(player);
	}

	public static List<CharacterMailTarget> listMailTargets() {
		return MailRecipientDirectory.listMailTargets();
	}

	/**
	 * Fetch missing base wardrobe textures from ProvinceSystem, then run {@code onComplete}
	 * on the main thread (e.g. before opening the bird mail character picker).
	 */
	public static void refreshMailTargetTexturesAsync(Runnable onComplete) {
		MailRecipientDirectory.refreshMissingTexturesAsync(onComplete);
	}

	public static Location getMailTargetLocation(UUID ownerUuid, String characterId) {
		return MailRecipientDirectory.getMailTargetLocation(ownerUuid, characterId);
	}

	public static String getCharacterName(Player player) {
		return DisplayIdentityService.resolveCharacterName(player);
	}

	public static String getDisplay(Player player) {
		return DisplayIdentityService.resolveDisplay(player);
	}

	public static String getDisplayTab(Player player) {
		return DisplayIdentityService.resolveDisplayTab(player);
	}

	public static String getDisplaySafe(Player player) {
		return DisplayIdentityService.resolveDisplaySafe(player);
	}

	public static void grantAttributePoints(Player player, int amount) {
		AttributePointService.grantAttributePoints(player, amount);
	}

	public static int getAccountAttributePointsTotal(Player player) {
		if (player == null) {
			return 0;
		}
		PlayerData data = PlayerManager.get(player);
		return data != null ? data.getAccountAttributePointsTotal() : 0;
	}

	public static int getFreeAttributePoints(Player player) {
		if (player == null) {
			return 0;
		}
		PlayerData data = PlayerManager.get(player);
		if (data == null) {
			return 0;
		}
		RPCharacter active = data.getActiveCharacter();
		if (active == null) {
			return data.getAccountAttributePointsTotal();
		}
		return Math.max(0, data.getAccountAttributePointsTotal() - active.getSpentExtraAttributePoints());
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
