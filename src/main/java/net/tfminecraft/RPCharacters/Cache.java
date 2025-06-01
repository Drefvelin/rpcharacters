package net.tfminecraft.RPCharacters;

import java.util.ArrayList;
import java.util.List;

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
}
