package net.tfminecraft.RPCharacters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Cache {
	public static List<String> attributes = new ArrayList<>();
	public static List<String> professions = new ArrayList<>();
	
	public static List<String> backgroundTraitTypes = new ArrayList<>();

	public static List<String> editableTraits = new ArrayList<>();
	
	//Profile stuff
	
	public static int maxAlive;
	public static List<Integer> characterSlots;
	public static int deadSlot;

	public static Map<String, Integer> permissionGroupDefaults = new HashMap<>();
	public static List<net.tfminecraft.RPCharacters.Objects.PermissionGroupDefinition> permissionGroups = new ArrayList<>();

	public static boolean requireCharacter;
	public static boolean noCharacterFreeze;
	public static boolean lackingCluesFreeze;

	public static int startingProfessionFactor;

	public static int defaultCluesRequired;
	public static int evilCluesRequired = 4;
	public static int characterDescriptionMinLength = 32;
	public static int characterDescriptionMaxLength = 256;
	public static int clueMinLength;
	public static int clueMaxLength;
	public static int maxClues;
	public static Map<String, Integer> traitClueOverrides = new HashMap<>();
	public static String raceClueTemplate;

	public static int spawnedClueTimerHours;
	public static double spawnedClueVisualYOffset;
	public static double spawnedClueLineSpacing;
	public static double spawnedClueFirstLineOffset;
	public static float spawnedClueScale;
	public static int spawnedClueParticleInterval;
	public static int clueSpawnRadius;
	public static int spawnedClueLineLength;
	public static int playtimeTickSeconds = 60;
	public static int conversationReplyTimeoutSeconds = 30;
	public static int conversationPairCooldownHours = 2;

	public static boolean skillPointsAdminDebugMessages = false;

	public static String continent = "Cerrith";

	public static String personaSetPermission = "rpchar.persona.set";
	public static String personaNamecolourPermission = "rpchar.namecolour";
	public static String personaDescriptionColorsPermission = "rpchar.persona.colors";
	public static String personaOverridePermission = "rpchar.persona.override";
	public static String personaBypassCooldownPermission = "rpchar.persona.bypasscooldown";
	public static String personaNoCharacterFallback = "§f§oUnknown";

	public static int personaDisplayNameMinLength = 3;
	public static int personaDisplayNameMaxLength = 24;
	public static int personaAliasMinLength = 3;
	public static int personaAliasMaxLength = 24;
	public static String personaAliasAllowedChars = "abcdefghijklmnopqrstuvwxyz.-'' áéíóú";
	public static int personaAliasCooldownSeconds = 10;

	public static List<String> personaGenders = new ArrayList<>();
	public static String personaGenderDefault = "Unset";
	public static int personaGenderCooldownSeconds = 10;

	public static int personaDescriptionMinLength = 3;
	public static int personaDescriptionCooldownSeconds = 10;
	public static String personaDescriptionDefaultTemplate = "A{n} {race} in {continent}.";

	public static String maskedLabel = "Masked";

	public static String chatDefaultChannel = "rp";
	public static String chatBypassCooldownPermission = "rpchar.chat.bypasscooldown";
	public static String chatNoCharacterMessage = "&cYou must have an active character to use this channel. Use &e/rpcharacter &cto create one, or &e/ooc &cfor out-of-character chat.";

	public static String profilePermission = "rpchar.profile";
	public static boolean profileRequireSneak = true;
	public static boolean profileRequireEmptyHand = true;
	public static int profileViewCooldownSeconds = 0;
	public static List<String> profileFormatLines = new ArrayList<>();

	public static String rollPermission = "rpchar.roll";
	public static String rollAltPermission = "rpchar.roll.alt";
	public static int rollDefaultMin = 1;
	public static int rollDefaultMax = 100;
	public static int rollAltMin = 1;
	public static int rollAltMax = 200;
	public static int rollD20Min = 1;
	public static int rollD20Max = 20;
	public static String rollBroadcastText = "&e{player} &7rolled a &6{roll}{modifier} &7out of {max}.";
	public static int rollBroadcastRange = 20;

	public static int calendarBaseFantasyYear = 372;
	public static String calendarEra = "AE";
	public static int calendarBaseIrlYear = 2025;
	public static double calendarDaysPerYear = 365.25;
	public static int calendarAgeDecimalPlaces = 1;
	public static int calendarAgeMinimum = 18;
	public static String calendarAgeUnsetLabel = "Unset";
}
