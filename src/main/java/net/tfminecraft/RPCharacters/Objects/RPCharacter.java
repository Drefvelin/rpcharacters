package net.tfminecraft.RPCharacters.Objects;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.entity.Player;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Objects.Attributes.AttributeData;
import net.tfminecraft.RPCharacters.Objects.Races.Race;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.Utils.Integrator;
import net.tfminecraft.RPCharacters.enums.Status;

public class RPCharacter {
	private String id;
	private String name;
	private Player owner;
	
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
	public RPCharacter(Player p, String i, String n, Boolean a, Status s, Race r, List<Trait> t) {
		owner = p;
		attributeData = new AttributeData();
		status = s;
		active = a;
		id = i;
		name = n;
		race = r;
		traits = t;
		update();
	}
	
	public void update() {
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
	public void activate() {
		active = true;
		Integrator i = new Integrator();
		i.integrate(owner, this);
	}
	public void deactivate() {
		active = false;
		Integrator i = new Integrator();
		i.remove(owner, this, true);
	}
	public Status getStatus() {
		return status;
	}
	public void setStatus(Status status) {
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
	public void addTrait(Trait t) {
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
		if(type.equalsIgnoreCase("name")) {
			name = value;
			Integrator i = new Integrator();
			i.dispatchCommand(owner, "char set name "+value);
		} else if(type.equalsIgnoreCase("race")) {
			name = value;
			Integrator i = new Integrator();
			i.dispatchCommand(owner, "char set race "+value);
		}
	}
}
