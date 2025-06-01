package net.tfminecraft.RPCharacters.Creation.Stages;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Creation.CharacterCreation;
import net.tfminecraft.RPCharacters.Creation.Stage;
import net.tfminecraft.RPCharacters.Loaders.RaceLoader;
import net.tfminecraft.RPCharacters.Loaders.TraitLoader;
import net.tfminecraft.RPCharacters.Managers.InventoryManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.SelectableItem;
import net.tfminecraft.RPCharacters.Objects.Races.Race;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;

public class SelectionStage extends Stage{
	private String target;
	private int minSelect;
	private int maxSelect;
	private int selections;
	private int points;
	private boolean hasPoints;
	private int size;
	private boolean active;
	private String key;
	
	private List<SelectableItem> options = new ArrayList<>();
	private List<SelectableItem> selected = new ArrayList<>();
	private List<Integer> slots = new ArrayList<>();
	
	public SelectionStage(Stage s, ConfigurationSection config) {
		setId(s.getId());
		setRepeat(s.shouldRepeat());
		setAutoNext(s.autoNext());
		setCancelled(s.isCancelled());
		if(s.hasDependency()) setDependency(s.getDependency());
		this.key = config.getString("key");
		this.active = false;
		this.target = config.getString("target");
		this.maxSelect = config.getInt("max-select");
		this.minSelect = config.getInt("min-select");
		this.slots = config.getIntegerList("slots");
		this.selections = 0;
		if(config.contains("points")) {
			this.points = config.getInt("points");
			this.hasPoints = true;
		} else {
			this.points = 0;
		}
		if(config.contains("gui-size")) {
			this.size = config.getInt("gui-size");
		} else {
			this.size = 27;
		}
		if(this.target.equalsIgnoreCase("race")) {
			for(Race r : RaceLoader.get()) {
				if(r.getRaceData().getKey().equalsIgnoreCase(key)) {
					options.add(new SelectableItem(r));
				}
			}
		} else if(this.target.equalsIgnoreCase("trait")) {
			for(Trait t : TraitLoader.get()) {
				if(t.getTraitData().getKey().equalsIgnoreCase(key)) {
					options.add(new SelectableItem(t));
				}
			}
		}
	}
	public SelectionStage(SelectionStage another) {
		setId(another.getId());
		setRepeat(another.shouldRepeat());
		setAutoNext(another.autoNext());
		if(another.hasDependency()) setDependency(another.getDependency());
		setCancelled(another.isCancelled());
		this.active = false;
		this.target = another.getTarget();
		this.options = another.getNewOptions();
		this.minSelect = another.getMinSelections();
		this.maxSelect = another.getMaxSelections();
		this.slots = another.getSlots();
		this.selections = 0;
		this.points = another.getPoints();
		this.hasPoints = another.hasPoints();
		this.size = another.getSize();
		this.key = another.getKey();
	}
	
	public List<SelectableItem> getSelection(){
		return selected;
	}
	public void select(SelectableItem i) {
		spendPoints(i.getCost());
		increase();
		selected.add(i);
	}
	public void unSelect(SelectableItem i) {
		addPoints(i.getCost());
		decrease();
		selected.remove(i);
	}
	public String getKey() {
		return key;
	}
	public int getSize() {
		return size;
	}
	public boolean hasPoints() {
		return hasPoints;
	}
	public int getPoints() {
		return points;
	}
	public void spendPoints(int p) {
		this.points = this.points-p;
	}
	public void addPoints(int p) {
		this.points = this.points+p;
	}
	public void increase() {
		selections++;
	}
	public void decrease() {
		selections--;
	}
	public int getSelections() {
		return selections;
	}
	public boolean isActive() {
		return active;
	}
	public List<Integer> getSlots() {
		return slots;
	}
	public int getMinSelections() {
		return minSelect;
	}
	public int getMaxSelections() {
		return maxSelect;
	}
	public String getTarget() {
		return target;
	}
	public List<SelectableItem> getNewOptions() {
		List<SelectableItem> list = new ArrayList<>();
		for(SelectableItem i : options) {
			list.add(new SelectableItem(i));
		}
		return list;
	}
	public List<SelectableItem> getOptions() {
		return options;
	}
	
	public void confirm(Player p, CharacterCreation cc) {
		if(selections < minSelect) {
			p.sendMessage("§cYou need to select at least "+minSelect+ " options!");
			return;
		}
		active = false;
		p.closeInventory();
		if(cc != null) {
			for(SelectableItem item : options) {
				if(item.isSelected()) {
					if(item.getType().equalsIgnoreCase("race")) {
						Race r = RaceLoader.getByString(item.getId());
						cc.getCharacter().setRace(r);
						p.sendMessage("§aRace set to "+r.getName());
					} else if(item.getType().equalsIgnoreCase("trait")) {
						Trait t = TraitLoader.getByString(item.getId());
						cc.getCharacter().addTrait(t);
						p.sendMessage("§aAdded trait "+t.getName());
					}
				}
			}
			new BukkitRunnable()
			{
				public void run()
				{
					if(autoNext()) {
						cc.runStage();
					} else {
						cc.setCanNext(true);
					}
				}
			}.runTaskLater(RPCharacters.plugin, 2L);
		}
	}

	@Override
	public void update(PlayerData pd) {
		if(!pd.hasActiveCharacter()) return;
		for(Trait t : pd.getActiveCharacter().getTraits()) {
			for(SelectableItem item : options) {
				if(item.getId().equalsIgnoreCase(t.getId())) {
					item.setSelected(true);
					select(item);
				}
			}
		}
	}
	
	public void execute(Player p, CharacterCreation cc) {
		active = true;
		InventoryManager inv = new InventoryManager();
		inv.selectionView(p, this, cc);
	}
}
