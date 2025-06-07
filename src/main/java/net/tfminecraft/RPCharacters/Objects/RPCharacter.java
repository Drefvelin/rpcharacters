package net.tfminecraft.RPCharacters.Objects;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.MMOCoreAPI;
import net.Indyuce.mmocore.api.player.profess.PlayerClass;
import net.Indyuce.mmocore.manager.ClassManager;
import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Database.Database;
import net.tfminecraft.RPCharacters.Loaders.RaceLoader;
import net.tfminecraft.RPCharacters.Objects.Attributes.AttributeData;
import net.tfminecraft.RPCharacters.Objects.Races.Race;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.Utils.Integrator;
import net.tfminecraft.RPCharacters.enums.Status;

public class RPCharacter {
	private String id;
	private String name;
	private Player owner;

	private String mmoClass;
	
	private Boolean active;
	private Status status;
	
	private Race race;
	
	private List<String> desc = new ArrayList<>();
	
	private List<Trait> traits = new ArrayList<Trait>();
	
	private AttributeData attributeData;
	
	public RPCharacter(Player p) {
		owner = p;
		attributeData = new AttributeData();
		status = Status.ALIVE;
		active = false;
		id = UUID.randomUUID().toString();
	}
	public RPCharacter(Player p, String i, String n, Boolean a, Status s, Race r, List<Trait> t, String c) {
		owner = p;
		attributeData = new AttributeData();
		status = s;
		active = a;
		id = i;
		name = n;
		race = r;
		traits = t;
		mmoClass = c;
		update();
	}
	
	public void update() {
		if(active) {
			if(mmoClass != null) {
				PlayerClass newClass = MMOCore.plugin.classManager.get(mmoClass);
				if(newClass != null) {
					net.Indyuce.mmocore.api.player.PlayerData.get(owner).setClass(newClass);
					owner.sendMessage("§eYour class was changed to "+newClass.getName());
				}
			}
		}
		attributeData = new AttributeData();
		attributeData.mergeFrom(race.getRaceData().getAttributeData());
		desc = new ArrayList<>();
		for(Trait t : traits) {
			if(Cache.backgroundTraitTypes.contains(t.getTraitData().getKey())) {
				desc.add(" ");
				for(String s : t.getDesc()) {
					desc.add(s);
				}
			}
			attributeData.mergeFrom(t.getTraitData().getAttributeData());
		}
	}
	
	public Player getOwner() {
		return owner;
	}
	public List<String> getDescription(){
		return desc;
	}
	public Boolean isActive() {
		return active;
	}
	public boolean hasMMOClass() {
		return mmoClass != null;
	}
	public void setMMOClass(String s) {
		mmoClass = s.toUpperCase();
	}
	public String getMMOClass() {
		return mmoClass;
	}
	public void activate() {
		if(mmoClass != null) {
			PlayerClass newClass = MMOCore.plugin.classManager.get(mmoClass);
			if(newClass != null) {
				net.Indyuce.mmocore.api.player.PlayerData.get(owner).setClass(newClass);
				owner.sendMessage("§eYour class was changed to "+newClass.getName());
			}
		}
		modify("name", name, false);
		modify("race", race.getId(), false);
		Database.log(owner, "Activated the character "+name);
		active = true;
		Integrator i = new Integrator();
		i.integrate(owner, this);
	}
	public void deactivate() {
		if(mmoClass == null) mmoClass = net.Indyuce.mmocore.api.player.PlayerData.get(owner).getProfess().getId();
		modify("name", "Unknown", false);
		modify("race", "Human", false);
		Database.log(owner, "Deactivated the character "+name);
		active = false;
		Integrator i = new Integrator();
		i.remove(owner, this, true);
	}
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
		Database.log(owner, "Set the status of the character "+name+" to "+status.toString());
		this.status = status;
	}
	public void setRace(Race race) {
		this.race = race;
	}
	public void setTraits(List<Trait> traits) {
		this.traits = traits;
	}
	public void setAttributeData(AttributeData attributeData) {
		this.attributeData = attributeData;
	}
	public Race getRace() {
		return race;
	}
	public void removeTrait(Trait t) {
		for(int i = 0; i<traits.size(); i++) {
			Trait trait = traits.get(i);
			if(trait.equals(t)) {
				if(active) Database.log(owner, "-"+t.getId()+" ("+name+")");
				traits.remove(i);
				return;
			}
		}
	}
	public void addTrait(Trait t) {
		if(active) Database.log(owner, "+"+t.getId()+" ("+name+")");
		this.traits.add(t);
	}
	public List<Trait> getTraits() {
		return traits;
	}
	public AttributeData getAttributeData() {
		return attributeData;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public void modify(String type, String value) {
		modify(type, value, true);
	}
	public void modify(String type, String value, boolean affect) {
		if(type.equalsIgnoreCase("name")) {
			if(affect) name = value;
			new BukkitRunnable() {
				@Override
				public void run() {
					Integrator i = new Integrator();
					i.dispatchCommand(owner, "char set name "+value);
				}
			}.runTask(RPCharacters.plugin);
		} else if(type.equalsIgnoreCase("race")) {
			Race newRace = RaceLoader.getByString(value);
			if(affect && newRace != null) race = newRace;
			new BukkitRunnable() {
				@Override
				public void run() {
					Integrator i = new Integrator();
					i.dispatchCommand(owner, "char set race "+newRace.getName());
				}
			}.runTask(RPCharacters.plugin);
		}
	}
}
