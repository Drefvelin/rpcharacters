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
	public static int switchCooldown;
	public static List<Integer> characterSlots;
	public static int deadSlot;

	public static boolean requireCharacter;
	public static boolean noCharacterFreeze;
	public static boolean lackingCluesFreeze;

	public static int startingProfessionFactor;

	public static int defaultCluesRequired;
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
}
