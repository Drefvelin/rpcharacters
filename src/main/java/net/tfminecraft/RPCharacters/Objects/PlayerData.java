package net.tfminecraft.RPCharacters.Objects;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

import net.tfminecraft.RPCharacters.Creation.Stage;
import net.tfminecraft.RPCharacters.enums.Status;

public class PlayerData {
	private Player p;
	
	private boolean hasActive;
	private int cooldown;
	
	private List<RPCharacter> characters = new ArrayList<>();
	private List<String> completedStages = new ArrayList<>();
	
	public PlayerData(Player p) {
		this.p = p;
		this.hasActive = false;
		this.cooldown = 0;
	}
	public PlayerData(Player p, List<String> cs, int c) {
		this.p = p;
		this.completedStages = cs;
		this.hasActive = false;
		cooldown = c;
	}
	
	public boolean hasCooldown() {
		if(cooldown > 0) return true;
		return false;
	}
	public int getRemainingTime() {
		return cooldown;
	}
	public void setCooldown(int i) {
		cooldown = i;
	}
	public void resetCooldown() {
		cooldown = 30;
	}
	public void tick() {
		cooldown--;
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
				resetCooldown();
			}
		}
		c.activate();
	}
	
}
