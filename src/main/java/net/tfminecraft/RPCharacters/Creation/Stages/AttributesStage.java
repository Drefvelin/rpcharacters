package net.tfminecraft.RPCharacters.Creation.Stages;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Creation.CharacterCreation;
import net.tfminecraft.RPCharacters.Creation.Stage;
import net.tfminecraft.RPCharacters.Loaders.TraitLoader;
import net.tfminecraft.RPCharacters.Managers.InventoryManager;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.Utils.RPTexts;

/**
 * Creation point-buy sheet: spend exactly {@code points} across attributes
 * with cost of the n-th rank = n, max rank per attribute from config.
 */
public class AttributesStage extends Stage {

	private static final Map<String, String> ATTR_ABBR = Map.of(
		"strength", "str",
		"dexterity", "dex",
		"constitution", "con",
		"intelligence", "int",
		"wisdom", "wis",
		"charisma", "cha"
	);

	private final int pool;
	private final int maxRank;
	private final int size;
	private final String key;
	private final List<String> attributes;

	private final Map<String, Integer> ranks = new LinkedHashMap<>();
	private int remaining;
	private boolean active;

	public AttributesStage(Stage s, ConfigurationSection config) {
		copyBaseFields(s);
		this.key = config.getString("key", "attributes");
		this.pool = config.contains("points") ? config.getInt("points") : 12;
		this.maxRank = config.contains("max-rank") ? config.getInt("max-rank") : 2;
		this.size = config.contains("gui-size") ? config.getInt("gui-size") : 36;
		this.attributes = new ArrayList<>();
		if (config.contains("attributes")) {
			for (String a : config.getStringList("attributes")) {
				if (a != null && !a.isBlank()) {
					this.attributes.add(a.trim().toLowerCase(Locale.ROOT));
				}
			}
		}
		if (this.attributes.isEmpty()) {
			for (String a : Cache.attributes) {
				if (a != null && !a.isBlank()) {
					this.attributes.add(a.trim().toLowerCase(Locale.ROOT));
				}
			}
		}
		resetRanks();
		this.active = false;
	}

	public AttributesStage(AttributesStage another) {
		copyBaseFields(another);
		this.key = another.key;
		this.pool = another.pool;
		this.maxRank = another.maxRank;
		this.size = another.size;
		this.attributes = new ArrayList<>(another.attributes);
		resetRanks();
		this.active = false;
	}

	private void resetRanks() {
		ranks.clear();
		for (String attr : attributes) {
			ranks.put(attr, 0);
		}
		remaining = pool;
	}

	public String getKey() {
		return key;
	}

	public int getPool() {
		return pool;
	}

	public int getMaxRank() {
		return maxRank;
	}

	public int getSize() {
		return size;
	}

	public int getRemaining() {
		return remaining;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public List<String> getAttributes() {
		return attributes;
	}

	public int getRank(String attr) {
		return ranks.getOrDefault(normalize(attr), 0);
	}

	public static String abbrevFor(String attr) {
		String n = normalize(attr);
		return ATTR_ABBR.getOrDefault(n, n.length() >= 3 ? n.substring(0, 3) : n);
	}

	public static String traitId(String attr, int rank) {
		return abbrevFor(attr) + rank;
	}

	/** Cost to purchase the n-th rank (1-based). */
	public static int costForRank(int rank) {
		return Math.max(0, rank);
	}

	public int spentPoints() {
		int spent = 0;
		for (String attr : attributes) {
			int r = getRank(attr);
			for (int n = 1; n <= r; n++) {
				spent += costForRank(n);
			}
		}
		return spent;
	}

	private static String normalize(String attr) {
		return attr == null ? "" : attr.trim().toLowerCase(Locale.ROOT);
	}

	public boolean tryIncrease(String attr) {
		String n = normalize(attr);
		if (!ranks.containsKey(n)) {
			return false;
		}
		int rank = ranks.get(n);
		if (rank >= maxRank) {
			return false;
		}
		int cost = costForRank(rank + 1);
		if (remaining < cost) {
			return false;
		}
		ranks.put(n, rank + 1);
		remaining -= cost;
		return true;
	}

	public boolean tryDecrease(String attr) {
		String n = normalize(attr);
		if (!ranks.containsKey(n)) {
			return false;
		}
		int rank = ranks.get(n);
		if (rank <= 0) {
			return false;
		}
		int refund = costForRank(rank);
		ranks.put(n, rank - 1);
		remaining += refund;
		return true;
	}

	public void hydrateFromCharacter(RPCharacter character) {
		resetRanks();
		if (character == null) {
			return;
		}
		for (String attr : attributes) {
			int rank = 0;
			if (hasTraitId(character, traitId(attr, 1))) {
				rank++;
			}
			if (hasTraitId(character, traitId(attr, 2))) {
				rank++;
			}
			ranks.put(attr, Math.min(rank, maxRank));
		}
		remaining = pool - spentPoints();
		if (remaining < 0) {
			remaining = 0;
		}
	}

	private static boolean hasTraitId(RPCharacter character, String id) {
		for (Trait t : character.getTraits()) {
			if (t.getId().equalsIgnoreCase(id)) {
				return true;
			}
		}
		return false;
	}

	public void confirm(Player p, CharacterCreation cc) {
		if (remaining != 0) {
			RPTexts.send(p, RPTexts.ERROR + "Spend all " + pool + " attribute points ("
				+ remaining + " left).");
			return;
		}
		active = false;
		p.closeInventory();
		if (cc == null) {
			return;
		}
		List<Trait> toRemove = new ArrayList<>();
		for (Trait trait : cc.getCharacter().getTraits()) {
			if (trait.getTraitData().getKey() != null
				&& trait.getTraitData().getKey().equalsIgnoreCase(key)) {
				toRemove.add(trait);
			}
		}
		for (Trait trait : toRemove) {
			cc.getCharacter().removeTrait(trait);
		}
		for (String attr : attributes) {
			int rank = getRank(attr);
			for (int n = 1; n <= rank; n++) {
				Trait t = TraitLoader.getByString(traitId(attr, n));
				if (t == null) {
					RPTexts.send(p, RPTexts.ERROR + "Missing attribute trait "
						+ traitId(attr, n) + " — check attributes-traits.yml");
					continue;
				}
				cc.getCharacter().addTrait(t);
			}
		}
		RPTexts.send(p, RPTexts.SUCCESS + "Attributes set.");
		new BukkitRunnable() {
			@Override
			public void run() {
				if (cc.isEditingFromSummary()) {
					cc.returnToSummary();
				} else if (autoNext()) {
					cc.runStage();
				} else {
					cc.setCanNext(true);
				}
			}
		}.runTaskLater(RPCharacters.plugin, 2L);
	}

	public void execute(Player p, CharacterCreation cc) {
		if (cc != null && cc.isCancelled()) {
			return;
		}
		active = true;
		InventoryManager inv = new InventoryManager();
		inv.attributesView(p, this, cc);
	}
}
