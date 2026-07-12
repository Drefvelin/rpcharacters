package net.tfminecraft.RPCharacters.Objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.entity.Player;

import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.player.profess.PlayerClass;
import net.tfminecraft.RPCharacters.mmocore.ClassService;
import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Database.Database;
import net.tfminecraft.RPCharacters.identity.NameColour;
import net.tfminecraft.RPCharacters.Loaders.RaceLoader;
import net.tfminecraft.RPCharacters.Objects.Attributes.AttributeData;
import net.tfminecraft.RPCharacters.Objects.Races.Race;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.Utils.ClueFormatter;
import net.tfminecraft.RPCharacters.Utils.Integrator;
import net.tfminecraft.RPCharacters.enums.ClueAddResult;
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

	private List<String> playerClues = new ArrayList<>();

	private int playtimeSeconds;
	private Map<String, Integer> conversationCounts = new HashMap<>();
	private Map<String, Long> conversationLastAtMs = new HashMap<>();

	private String alias;
	private NameColour nameColour;
	private boolean nameColourStaffOverride;
	private String gender;
	private String personaDescription;
	private String birthday;
	
	private AttributeData attributeData;
	
	public RPCharacter(Player p) {
		owner = p;
		attributeData = new AttributeData();
		status = Status.ALIVE;
		active = false;
		id = UUID.randomUUID().toString();
	}
	public RPCharacter(Player p, String i, String n, Boolean a, Status s, Race r, List<Trait> t, String c) {
		this(p, i, n, a, s, r, t, c, new ArrayList<>());
	}

	public RPCharacter(Player p, String i, String n, Boolean a, Status s, Race r, List<Trait> t, String c, List<String> clues) {
		owner = p;
		attributeData = new AttributeData();
		status = s;
		active = a;
		id = i;
		name = n;
		race = r;
		traits = t;
		mmoClass = c;
		playerClues = new ArrayList<>();
		if (clues != null) {
			for (String clue : clues) {
				playerClues.add(ClueFormatter.format(clue));
			}
		}
		update();
	}
	
	public void update() {
		if(active) {
			if(mmoClass != null) {
				PlayerClass newClass = MMOCore.plugin.classManager.get(mmoClass);
				if(newClass != null) {
					ClassService.applyClass(owner, mmoClass);
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
				ClassService.applyClass(owner, mmoClass);
				owner.sendMessage("§eYour class was changed to "+newClass.getName());
			}
		}
		Database.log(owner, "Activated the character "+name);
		active = true;
		Integrator i = new Integrator();
		i.integrate(owner, this);
	}
	public void deactivate() {
		if(mmoClass == null) mmoClass = net.Indyuce.mmocore.api.player.PlayerData.get(owner).getProfess().getId();
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

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		if (alias == null || alias.isBlank()) {
			this.alias = null;
			return;
		}
		this.alias = ClueFormatter.stripColor(alias);
	}

	public void clearAlias() {
		this.alias = null;
	}

	public NameColour getNameColour() {
		return nameColour;
	}

	public void setNameColour(NameColour nameColour) {
		this.nameColour = nameColour;
	}

	public boolean isNameColourStaffOverride() {
		return nameColourStaffOverride;
	}

	public void setNameColourStaffOverride(boolean nameColourStaffOverride) {
		this.nameColourStaffOverride = nameColourStaffOverride;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getPersonaDescription() {
		return personaDescription;
	}

	public void setPersonaDescription(String personaDescription) {
		this.personaDescription = personaDescription;
	}

	public String getBirthday() {
		return birthday;
	}

	public void setBirthday(String birthday) {
		if (birthday == null || birthday.isBlank()) {
			this.birthday = null;
			return;
		}
		this.birthday = birthday.trim();
	}

	public String getEffectiveDisplayPlain() {
		String effective = alias != null && !alias.isBlank() ? alias : name;
		if (effective == null) {
			return "";
		}
		return ClueFormatter.stripColor(effective);
	}

	public void modify(String type, String value) {
		modify(type, value, true);
	}

	public void modify(String type, String value, boolean affect) {
		if (type.equalsIgnoreCase("name")) {
			if (affect) {
				setName(value);
			}
		} else if (type.equalsIgnoreCase("race")) {
			Race newRace = RaceLoader.getByString(value);
			if (affect && newRace != null) {
				race = newRace;
			}
		}
	}

	/**
	 * Required player-authored clue count for this character.
	 * Uses the highest matching trait override (not additive), capped by max-clues.
	 */
	public int getCluesNeeded() {
		int needed = Cache.defaultCluesRequired;
		if (hasAnyEvilTrait()) {
			needed = Math.max(needed, Cache.evilCluesRequired);
		}
		for (Trait trait : traits) {
			Integer override = Cache.traitClueOverrides.get(trait.getId().toLowerCase());
			if (override != null && override > needed) {
				needed = override;
			}
		}
		return Math.min(needed, Cache.maxClues);
	}

	private boolean hasAnyEvilTrait() {
		for (Trait trait : traits) {
			if (trait.getTraitData() != null && "evil".equalsIgnoreCase(trait.getTraitData().getKey())) {
				return true;
			}
		}
		return false;
	}

	/** Whether this character has enough player clues to satisfy {@link #getCluesNeeded()}. */
	public boolean hasEnoughClues() {
		return playerClues.size() >= getCluesNeeded();
	}

	public List<String> getPlayerClues() {
		return Collections.unmodifiableList(playerClues);
	}

	public boolean canAddClue() {
		return playerClues.size() < Cache.maxClues;
	}

	public ClueAddResult addPlayerClue(String raw) {
		if (!canAddClue()) return ClueAddResult.AT_MAX;

		String plain = ClueFormatter.stripColor(raw);
		if (plain.length() < Cache.clueMinLength) return ClueAddResult.TOO_SHORT;
		if (plain.length() > Cache.clueMaxLength) return ClueAddResult.TOO_LONG;

		String formatted = ClueFormatter.format(raw);
		for (String existing : playerClues) {
			if (ClueFormatter.stripColor(existing).equalsIgnoreCase(plain)) {
				return ClueAddResult.DUPLICATE;
			}
		}

		playerClues.add(formatted);
		Database.log(owner, "Added clue to " + name + ": " + plain);
		return ClueAddResult.SUCCESS;
	}

	public boolean removePlayerClue(int index) {
		if (index < 0 || index >= playerClues.size()) return false;
		String removed = ClueFormatter.stripColor(playerClues.remove(index));
		Database.log(owner, "Removed clue from " + name + ": " + removed);
		return true;
	}

	public String getClueAddErrorMessage(ClueAddResult result) {
		switch (result) {
			case AT_MAX:
				return "§cYou cannot have more than " + Cache.maxClues + " clues.";
			case TOO_SHORT:
			case TOO_LONG:
				return ClueFormatter.lengthRangeMessage();
			case DUPLICATE:
				return "§cYou already have a clue like that.";
			default:
				return null;
		}
	}

	public int getPlaytimeSeconds() {
		return playtimeSeconds;
	}

	public void setPlaytimeSeconds(int playtimeSeconds) {
		this.playtimeSeconds = Math.max(0, playtimeSeconds);
	}

	public void addPlaytimeSeconds(int seconds) {
		if (seconds <= 0) {
			return;
		}
		playtimeSeconds += seconds;
	}

	public Map<String, Integer> getConversationCounts() {
		return conversationCounts;
	}

	public void setConversationCounts(Map<String, Integer> conversationCounts) {
		this.conversationCounts = conversationCounts != null ? new HashMap<>(conversationCounts) : new HashMap<>();
	}

	public Map<String, Long> getConversationLastAtMs() {
		return conversationLastAtMs;
	}

	public void setConversationLastAtMs(Map<String, Long> conversationLastAtMs) {
		this.conversationLastAtMs = conversationLastAtMs != null ? new HashMap<>(conversationLastAtMs) : new HashMap<>();
	}

	public int getConversationCount(String otherCharacterId) {
		if (otherCharacterId == null) {
			return 0;
		}
		return conversationCounts.getOrDefault(otherCharacterId, 0);
	}

	public boolean canCountConversationWith(RPCharacter other, long nowMs) {
		if (other == null) {
			return false;
		}
		Long lastAt = conversationLastAtMs.get(other.getId());
		if (lastAt == null) {
			return true;
		}
		long cooldownMs = Cache.conversationPairCooldownHours * 60L * 60L * 1000L;
		return nowMs - lastAt >= cooldownMs;
	}

	public void recordConversationWith(RPCharacter other, long nowMs) {
		if (other == null) {
			return;
		}
		String otherId = other.getId();
		conversationCounts.put(otherId, getConversationCount(otherId) + 1);
		conversationLastAtMs.put(otherId, nowMs);
	}
}
