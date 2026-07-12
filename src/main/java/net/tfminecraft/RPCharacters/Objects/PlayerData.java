package net.tfminecraft.RPCharacters.Objects;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import net.tfminecraft.RPCharacters.Creation.Stage;
import net.tfminecraft.RPCharacters.enums.Status;

public class PlayerData {
	private Player p;
	private Long lastCharacterSwitchAtMs;
	private boolean eighteen;
	
	private List<RPCharacter> characters = new ArrayList<>();
	private List<String> completedStages = new ArrayList<>();
	private int accountPlaytimeSeconds;
	private Integer accountSkillPointsTotal;
	
	public PlayerData(Player p) {
		this.p = p;
		this.lastCharacterSwitchAtMs = null;
		this.eighteen = false;
		this.accountPlaytimeSeconds = 0;
		this.accountSkillPointsTotal = null;
	}
	public PlayerData(Player p, List<String> cs, Long lastCharacterSwitchAtMs, boolean b, int accountPlaytimeSeconds) {
		this(p, cs, lastCharacterSwitchAtMs, b, accountPlaytimeSeconds, null);
	}

	public PlayerData(Player p, List<String> cs, Long lastCharacterSwitchAtMs, boolean b, int accountPlaytimeSeconds,
			Integer accountSkillPointsTotal) {
		this.p = p;
		this.completedStages = cs;
		this.lastCharacterSwitchAtMs = lastCharacterSwitchAtMs;
		eighteen = b;
		this.accountPlaytimeSeconds = Math.max(0, accountPlaytimeSeconds);
		this.accountSkillPointsTotal = accountSkillPointsTotal;
	}

	public int getAccountPlaytimeSeconds() {
		return accountPlaytimeSeconds;
	}

	public void setAccountPlaytimeSeconds(int accountPlaytimeSeconds) {
		this.accountPlaytimeSeconds = Math.max(0, accountPlaytimeSeconds);
	}

	public void addAccountPlaytimeSeconds(int seconds) {
		if (seconds <= 0) {
			return;
		}
		accountPlaytimeSeconds += seconds;
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
	public void addCharacter(RPCharacter c) {
		this.characters.add(c);
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
