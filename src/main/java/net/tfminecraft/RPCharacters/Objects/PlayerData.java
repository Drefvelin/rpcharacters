package net.tfminecraft.RPCharacters.Objects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.time.Instant;

import org.bukkit.entity.Player;

import net.tfminecraft.RPCharacters.Creation.Stage;
import net.tfminecraft.RPCharacters.identity.CharacterSlug;
import net.tfminecraft.RPCharacters.enums.Status;

public class PlayerData {
	private Player p;
	private Long lastCharacterSwitchAtMs;
	private boolean eighteen;
	
	private List<RPCharacter> characters = new ArrayList<>();
	private List<String> completedStages = new ArrayList<>();
	private int createdAtEpochSeconds;
	private Integer accountSkillPointsTotal;
	private Integer accountAttributePointsTotal;
	private final Map<String, Integer> accountProfessionPoints = new HashMap<>();
	private boolean professionPointsInitialized;
	private String tempAlias;
	private int investigationPoints = -1;
	private Long lastInvestigationRegenMs;
	private boolean permadeathTutorialDismissed;
	
	public PlayerData(Player p) {
		this.p = p;
		this.lastCharacterSwitchAtMs = null;
		this.eighteen = false;
		this.createdAtEpochSeconds = (int) Instant.now().getEpochSecond();
		this.accountSkillPointsTotal = null;
	}
	public PlayerData(Player p, List<String> cs, Long lastCharacterSwitchAtMs, boolean b, int createdAtEpochSeconds) {
		this(p, cs, lastCharacterSwitchAtMs, b, createdAtEpochSeconds, null);
	}

	public PlayerData(Player p, List<String> cs, Long lastCharacterSwitchAtMs, boolean b, int createdAtEpochSeconds,
			Integer accountSkillPointsTotal) {
		this.p = p;
		this.completedStages = cs;
		this.lastCharacterSwitchAtMs = lastCharacterSwitchAtMs;
		eighteen = b;
		this.createdAtEpochSeconds = Math.max(0, createdAtEpochSeconds);
		this.accountSkillPointsTotal = accountSkillPointsTotal;
	}

	public int getCreatedAtEpochSeconds() {
		return createdAtEpochSeconds;
	}

	public void setCreatedAtEpochSeconds(int createdAtEpochSeconds) {
		this.createdAtEpochSeconds = Math.max(0, createdAtEpochSeconds);
	}

	public int getAgeSeconds() {
		if (createdAtEpochSeconds <= 0) {
			return 0;
		}
		long age = Instant.now().getEpochSecond() - createdAtEpochSeconds;
		return (int) Math.max(0L, age);
	}

	public boolean needsSkillPointsMigration() {
		return accountSkillPointsTotal == null;
	}

	public int getAccountSkillPointsTotal() {
		return accountSkillPointsTotal != null ? accountSkillPointsTotal : 0;
	}

	public void setAccountSkillPointsTotal(int accountSkillPointsTotal) {
		this.accountSkillPointsTotal = Math.max(0, accountSkillPointsTotal);
	}

	public void addAccountSkillPoints(int amount) {
		if (amount <= 0) {
			return;
		}
		setAccountSkillPointsTotal(getAccountSkillPointsTotal() + amount);
	}

	public boolean needsAttributePointsMigration() {
		return accountAttributePointsTotal == null;
	}

	public int getAccountAttributePointsTotal() {
		return accountAttributePointsTotal != null ? accountAttributePointsTotal : 0;
	}

	public void setAccountAttributePointsTotal(int accountAttributePointsTotal) {
		this.accountAttributePointsTotal = Math.max(0, accountAttributePointsTotal);
	}

	public void addAccountAttributePoints(int amount) {
		if (amount <= 0) {
			return;
		}
		setAccountAttributePointsTotal(getAccountAttributePointsTotal() + amount);
	}

	public boolean isProfessionPointsInitialized() {
		return professionPointsInitialized;
	}

	public void setProfessionPointsInitialized(boolean professionPointsInitialized) {
		this.professionPointsInitialized = professionPointsInitialized;
	}

	public int getAccountProfessionPoints(String professionId) {
		if (professionId == null) {
			return 0;
		}
		return Math.max(0, accountProfessionPoints.getOrDefault(professionId.toLowerCase(), 0));
	}

	public void setAccountProfessionPoints(String professionId, int amount) {
		if (professionId == null) {
			return;
		}
		accountProfessionPoints.put(professionId.toLowerCase(), Math.max(0, amount));
	}

	public void addAccountProfessionPoints(String professionId, int amount) {
		if (professionId == null || amount <= 0) {
			return;
		}
		setAccountProfessionPoints(professionId, getAccountProfessionPoints(professionId) + amount);
	}

	public Map<String, Integer> getAccountProfessionPointsMap() {
		return accountProfessionPoints;
	}

	public void clearAccountProfessionPoints() {
		accountProfessionPoints.clear();
	}

	public boolean isEighteen() {
		return eighteen;
	}

	public void setEighteen(Boolean b) {
		eighteen = b;
	}

	public Long getLastCharacterSwitchAtMs() {
		return lastCharacterSwitchAtMs;
	}

	public void setLastCharacterSwitchAtMs(Long lastCharacterSwitchAtMs) {
		this.lastCharacterSwitchAtMs = lastCharacterSwitchAtMs;
	}

	public void recordCharacterSwitch() {
		lastCharacterSwitchAtMs = System.currentTimeMillis();
	}

	public void clearCharacterSwitchCooldown() {
		lastCharacterSwitchAtMs = null;
	}

	public Player getPlayer() {
		return p;
	}
	public boolean hasActiveCharacter() {
		for(RPCharacter ch : characters) {
			if(ch.isActive()) return true;
		}
		return false;
	}

	public RPCharacter getActiveCharacter() {
		for(RPCharacter ch : characters) {
			if(ch.isActive()) return ch;
		}
		return null;
	}
	
	public boolean hasCharacters() {
		if(characters.size() > 0) return true;
		return false;
	}
	
	public boolean hasCompletedStage(Stage s) {
		if(completedStages.contains(s.getId())) return true;
		return false;
	}
	public RPCharacter getCharacterById(String s) {
		for(RPCharacter c : characters) {
			if(c.getId().equalsIgnoreCase(s)) return c;
		}
		return null;
	}

	public RPCharacter getCharacterBySlug(String slug) {
		if (slug == null || slug.isBlank()) {
			return null;
		}
		for (RPCharacter c : characters) {
			if (c.getSlug() != null && c.getSlug().equalsIgnoreCase(slug)) {
				return c;
			}
		}
		return null;
	}

	public void assignSlug(RPCharacter character) {
		if (character == null || character.getName() == null || character.getName().isBlank()) {
			return;
		}
		// Slug is immutable once set — only assigned at first character add or legacy migration.
		if (character.getSlug() != null && !character.getSlug().isBlank()) {
			return;
		}
		String base = CharacterSlug.fromDisplayName(character.getName());
		String candidate = base;
		int suffix = 2;
		while (isSlugTaken(candidate, character)) {
			candidate = base + "_" + suffix;
			suffix++;
		}
		character.setSlug(candidate);
	}

	private boolean isSlugTaken(String slug, RPCharacter exclude) {
		for (RPCharacter c : characters) {
			if (c == exclude) {
				continue;
			}
			if (c.getSlug() != null && c.getSlug().equalsIgnoreCase(slug)) {
				return true;
			}
		}
		return false;
	}

	public RPCharacter findFacadeCharacter() {
		for (RPCharacter c : characters) {
			if (c.getStatus() == Status.ALIVE && !c.isHidden()) {
				return c;
			}
		}
		return null;
	}

	public String getTempAlias() {
		return tempAlias;
	}

	public void setTempAlias(String tempAlias) {
		if (tempAlias == null || tempAlias.isBlank()) {
			this.tempAlias = null;
			return;
		}
		this.tempAlias = tempAlias;
	}

	public void clearTempAlias() {
		this.tempAlias = null;
	}

	public int getInvestigationPoints() {
		return investigationPoints;
	}

	public void setInvestigationPoints(int investigationPoints) {
		this.investigationPoints = Math.max(0, investigationPoints);
	}

	public boolean needsInvestigationPointsInit() {
		return investigationPoints < 0;
	}

	public Long getLastInvestigationRegenMs() {
		return lastInvestigationRegenMs;
	}

	public void setLastInvestigationRegenMs(Long lastInvestigationRegenMs) {
		this.lastInvestigationRegenMs = lastInvestigationRegenMs;
	}

	public boolean hasDismissedPermadeathTutorial() {
		return permadeathTutorialDismissed;
	}

	public void setPermadeathTutorialDismissed(boolean permadeathTutorialDismissed) {
		this.permadeathTutorialDismissed = permadeathTutorialDismissed;
	}

	public void addCharacter(RPCharacter c) {
		this.characters.add(c);
		assignSlug(c);
	}
	public List<RPCharacter> getCharacters() {
		return characters;
	}
	public List<RPCharacter> getCharacters(Status s){
		List<RPCharacter> list = new ArrayList<RPCharacter>();
		for(RPCharacter c : characters) {
			if(c.getStatus().equals(s)) list.add(c);
		}
		return list;
	}

	public List<String> getCompletedStages() {
		return completedStages;
	}
	
	public void addCompletedStage(Stage s) {
		if(!hasCompletedStage(s)) completedStages.add(s.getId());
	}
	public void setActiveCharacter(RPCharacter c) {
		for(RPCharacter ch : characters) {
			if(ch.isActive()) {
				ch.deactivate();
				recordCharacterSwitch();
			}
		}
		c.activate();
	}
	
}
