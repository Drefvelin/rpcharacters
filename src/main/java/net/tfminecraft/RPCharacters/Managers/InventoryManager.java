package net.tfminecraft.RPCharacters.Managers;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Creation.CharacterCreation;
import net.tfminecraft.RPCharacters.Creation.Dependency;
import net.tfminecraft.RPCharacters.Creation.Stages.SelectionStage;
import net.tfminecraft.RPCharacters.Holder.RPCHolder;
import net.tfminecraft.RPCharacters.Loaders.RaceLoader;
import net.tfminecraft.RPCharacters.Loaders.TraitLoader;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.SelectableItem;
import net.tfminecraft.RPCharacters.Objects.Attributes.AttributeData;
import net.tfminecraft.RPCharacters.Objects.Attributes.AttributeModifier;
import net.tfminecraft.RPCharacters.Objects.Experience.ExperienceModifier;
import net.tfminecraft.RPCharacters.Objects.Races.Race;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.enums.Status;

public class InventoryManager {
	public void characterView(Player p, RPCharacter c) {
		PlayerData pd = PlayerManager.get(c.getOwner());
		Inventory i = RPCharacters.plugin.getServer().createInventory(new RPCHolder(c.getOwner()), 27, "§7Character Info");
		i.setItem(10, getCharacterItem(c, false));
		i.setItem(12, getDescriptionItem(c));
		i.setItem(14, getTraitsItem(c));
		i.setItem(26, getBackButton());
		if(c.getStatus().equals(Status.ALIVE) && c.getOwner().equals(p)) {
			i.setItem(8, getKillItem());
		}
		if(c.getStatus().equals(Status.ALIVE) && !c.isActive() && !pd.hasCooldown() && c.getOwner().equals(p)) {
			i.setItem(6, getSwitchItem());
		}
		int slotn = 0;
		while(slotn < i.getSize()) {
			if(i.getItem(slotn) == null) {
				ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta fm = fill.getItemMeta();
				fm.setDisplayName("§8 ");
				fill.setItemMeta(fm);
				i.setItem(slotn, fill);
			}
			slotn++;
		}
		p.openInventory(i);
	}
	public void deadView(Player p, Player t) {
		Inventory i = RPCharacters.plugin.getServer().createInventory(new RPCHolder(t), 27, "§7Dead Characters");
		PlayerData pd = PlayerManager.get(t);
		for(int x = 0; x<i.getSize()-1; x++) {
			List<RPCharacter> chars = pd.getCharacters(Status.DEAD);
			if(x < chars.size()) {
				i.setItem(x, getCharacterItem(chars.get(x), true));
			}
		}
		i.setItem(i.getSize()-1, getBackButton());
		int slotn = 0;
		while(slotn < i.getSize()) {
			if(i.getItem(slotn) == null) {
				ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta fm = fill.getItemMeta();
				fm.setDisplayName("§8 ");
				fill.setItemMeta(fm);
				i.setItem(slotn, fill);
			}
			slotn++;
		}
		p.openInventory(i);
	}
	public void profileView(Player p, Player t) {
		Inventory i = RPCharacters.plugin.getServer().createInventory(new RPCHolder(t), 27, "§7Character Menu");
		i.setItem(0, getPlayerHead(t));
		PlayerData pd = PlayerManager.get(t);
		for(int x = 0; x<Cache.characterSlots.size(); x++) {
			List<RPCharacter> chars = pd.getCharacters(Status.ALIVE);
			if(x >= chars.size()) {
				i.setItem(Cache.characterSlots.get(x), getEmptyCharacterItem(pd));
			} else {
				i.setItem(Cache.characterSlots.get(x), getCharacterItem(chars.get(x), true));
			}
		}
		i.setItem(Cache.deadSlot, getDeadCharactersItem(pd));
		int slotn = 0;
		while(slotn < i.getSize()) {
			if(i.getItem(slotn) == null) {
				ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta fm = fill.getItemMeta();
				fm.setDisplayName("§8 ");
				fill.setItemMeta(fm);
				i.setItem(slotn, fill);
			}
			slotn++;
		}
		p.openInventory(i);
	}
	public void selectionView(Player player, SelectionStage s, CharacterCreation cc) {
		Inventory i = RPCharacters.plugin.getServer().createInventory(new RPCHolder(player, s), s.getSize(), "§7"+WordUtils.capitalize(s.getKey())+ " Selection");
		for(int x = 0; x<s.getSlots().size(); x++) {
			if(x >= s.getOptions().size()) break;
			i.setItem(s.getSlots().get(x), getSelectableItem(player, s, s.getOptions().get(x), cc));
		}
		i.setItem(s.getSize()-1, getConfirmItem());
		if(cc != null) i.setItem(s.getSize()-9, createCancelItem(cc));
		player.openInventory(i);
	}
	public void selectionUpdate(Inventory i, Player player, SelectionStage s, CharacterCreation cc) {
		for(int x = 0; x<s.getSlots().size(); x++) {
			if(x >= s.getOptions().size()) break;
			i.setItem(s.getSlots().get(x), getSelectableItem(player, s, s.getOptions().get(x), cc));
		}
	}
	public void confirmView(Player player) {
		Inventory i = RPCharacters.plugin.getServer().createInventory(new RPCHolder(player), 27, "§7Confirm Action");
		i.setItem(11, createItemStack(Material.GREEN_CONCRETE, "§aConfirm"));
		i.setItem(15, createItemStack(Material.RED_CONCRETE, "§cCancel"));
		Integer slot = 0;
		while(slot < i.getSize()) {
			if(i.getItem(slot) == null) {
				ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta fm = fill.getItemMeta();
				fm.setDisplayName("§8 ");
				fill.setItemMeta(fm);
				i.setItem(slot, fill);
			}
			slot++;
		}
		player.openInventory(i);
	}
	public String formatTime(Integer time) {
		Integer remainder = time % 60;
		Integer hoursTime = time/60;
		Integer minutes = remainder % 60;
		String hours = String.valueOf(hoursTime);
		String mins = String.valueOf(minutes);
		String formattedTime = "";
		if(hoursTime > 0) {
			formattedTime = formattedTime+hours + "h ";
		}
		if(minutes > 0) {
			formattedTime = formattedTime + mins + "m ";
		}
		if(minutes == 0 && hoursTime == 0) {
			formattedTime = "0m";
		}
		return formattedTime;
	}
	public ItemStack createItemStack(Material m, String name) {
		ItemStack i = new ItemStack(m, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName(name);
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack getBackButton() {
		ItemStack i = new ItemStack(Material.BARRIER, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§cBack");
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack createCancelItem(CharacterCreation cc) {
		ItemStack i = new ItemStack(Material.BARRIER, 1);
		ItemMeta meta = i.getItemMeta();
		List<String> lore = new ArrayList<>();
		if(cc != null) {
			meta.setDisplayName("§cCancel Creation");
		
			lore.add("§7Cancel the current character creation");
			lore.add("§cThis is not reversible!");
		} else{ 
			meta.setDisplayName("§cCancel");
		
			lore.add("§7Cancel the edit");
		}
		
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack getKillItem() {
		ItemStack i = new ItemStack(Material.IRON_AXE, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§cKill Character");
		List<String> lore = new ArrayList<String>();
		lore.add("§7This will kill the character");
		lore.add("§7and add it to the list of dead characters");
		lore.add("§7Only staff can reverse this");
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack getSwitchItem() {
		ItemStack i = new ItemStack(Material.EMERALD, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§aSwitch to Character");
		List<String> lore = new ArrayList<String>();
		lore.add("§7This will switch you to this character");
		lore.add("§7You will be put on cooldown from switching again");
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack getDescriptionItem(RPCharacter c) {
		ItemStack i = new ItemStack(Material.WRITABLE_BOOK, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§7Character Background");
		List<String> lore = new ArrayList<String>();
		lore.add("§7------------------------");
		lore.add("§eBackground:");
		lore.add(" ");
		for(String s : c.getDescription()) {
			lore.add(s);
		}
		lore.add("§7------------------------");
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack getTraitsItem(RPCharacter c) {
		ItemStack i = new ItemStack(Material.GOLDEN_APPLE, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§7Character Traits");
		List<String> lore = new ArrayList<String>();
		lore.add("§7------------------------");
		lore.add("§eTraits:");
		lore.add(" ");
		for(Trait t : c.getTraits()) {
			if(Cache.backgroundTraitTypes.contains(t.getTraitData().getKey())) continue;
			lore.add("§f- "+t.getName()+" §7("+WordUtils.capitalize(t.getTraitData().getKey()+")"));
		}
		lore.add("§7------------------------");
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack getCharacterItem(RPCharacter c, boolean click) {
		PlayerData pd = PlayerManager.get(c.getOwner());
		ItemStack i = new ItemStack(Material.ENDER_PEARL, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§eCharacter: §7"+c.getName());
		List<String> lore = new ArrayList<String>();
		lore.add(" ");
		lore.add("§7Race: "+c.getRace().getName());
		lore.add("§7Status: §f"+c.getStatus().toString());
		lore.add(" ");
		if(click) {
			lore.add("§bClick §7for details");
			if(c.isActive()) {
				meta.addEnchant(Enchantment.DURABILITY, 1, true);
				meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			}
		}
		lore.add(" ");
		if(pd.hasCooldown()) {
			lore.add("§eYou are on Cooldown: §f"+formatTime(pd.getRemainingTime()));
		}
		NamespacedKey key = new NamespacedKey(RPCharacters.plugin, "character_id");
		meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, c.getId());
		NamespacedKey oKey = new NamespacedKey(RPCharacters.plugin, "owner");
		meta.getPersistentDataContainer().set(oKey, PersistentDataType.STRING, c.getOwner().getName());
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack getDeadCharactersItem(PlayerData pd) {
		ItemStack i = new ItemStack(Material.SKELETON_SKULL, 1);
		int count = pd.getCharacters(Status.DEAD).size();
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§7Dead Characters: §f"+count);
		List<String> lore = new ArrayList<String>();
		lore.add("§7Click to view dead characters");
		meta.setLore(lore);
		NamespacedKey oKey = new NamespacedKey(RPCharacters.plugin, "owner");
		meta.getPersistentDataContainer().set(oKey, PersistentDataType.STRING, pd.getPlayer().getName());
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack getEmptyCharacterItem(PlayerData pd) {
		ItemStack i = new ItemStack(Material.YELLOW_CONCRETE, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§eEmpty Slot");
		List<String> lore = new ArrayList<String>();
		lore.add("§7Click to create a new character");
		if(pd.hasCooldown()) {
			lore.add(" ");
			lore.add("§eYou are on Cooldown: §f"+formatTime(pd.getRemainingTime()));
		}
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
	@SuppressWarnings("deprecation")
	public ItemStack getPlayerHead(Player p) {
		ItemStack i = new ItemStack(Material.PLAYER_HEAD, 1);
		SkullMeta m = (SkullMeta) i.getItemMeta();
		m.setDisplayName("§7"+p.getName());
		m.setOwningPlayer(Bukkit.getOfflinePlayer(p.getName()));
		List<String> lore = new ArrayList<String>();
		lore.add("§eCharacter profile of "+p.getName());
		m.setLore(lore);
		i.setItemMeta(m);
		return i;
	}
	
	public ItemStack getConfirmItem() {
		ItemStack i = new ItemStack(Material.LIME_DYE, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName("§aCONFIRM");
		i.setItemMeta(meta);
		return i;
	}
	
	public ItemStack getSelectableItem(Player p, SelectionStage stage, SelectableItem s, CharacterCreation cc) {
		ItemStack i = new ItemStack(Material.BARRIER, 1);
		if(s.isSelected()) {
			i.setType(Material.GREEN_CONCRETE);
		} else {
			i.setType(Material.RED_CONCRETE);
		}
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName(s.getName());
		if(s.getType().equalsIgnoreCase("race")) {
			Race r = RaceLoader.getByString(s.getId());
			List<String> lore = new ArrayList<>();
			for(String d : r.getDesc()) {
				lore.add(d);
			}
			if(r.getRaceData().getAttributeData().hasModifiers()) {
				lore.add(" ");
				addModifiers(p, lore, r.getRaceData().getAttributeData(), cc);
				lore.add(" ");
			}
			meta.setLore(lore);
		} else if(s.getType().equalsIgnoreCase("trait")) {
			Trait t = TraitLoader.getByString(s.getId());
			List<String> lore = new ArrayList<>();
			for(String d : t.getDesc()) {
				lore.add(d);
			}
			if(t.getTraitData().hasDependency()) {
				Dependency d = t.getTraitData().getDependency();
				lore.add("§7------------------------");
				if(d.getMode().equalsIgnoreCase("all")) {
					lore.add("§eRequires all of these:");
				} else if(d.getMode().equalsIgnoreCase("one-or-more")) {
					lore.add("§eRequires at least one of these:");
				}
				for(String dep : d.getDependencies()) {
					lore.add("§f- "+WordUtils.capitalize(dep));
				}
				lore.add("§7------------------------");
			}
			if(t.getTraitData().hasExclusives()) {
				lore.add("§7------------------------");
				lore.add("§eMutually Exclusive with:");
				for(String e : t.getTraitData().getExclusive()) {
					lore.add("§f- "+WordUtils.capitalize(e));
				}
				lore.add("§7------------------------");
			}
			if(t.getTraitData().getAttributeData().hasModifiers()) {
				lore.add(" ");
				addModifiers(p, lore, t.getTraitData().getAttributeData(), cc);
				lore.add(" ");
			}
			if(t.getTraitData().hasCost()) {
				lore.add("§eCost: §7"+t.getTraitData().getCost());
				lore.add(" ");
			}
			if(stage.hasPoints()) {
				lore.add("§eUnspent Points: §7"+stage.getPoints());
			}
			meta.setLore(lore);
		}
		i.setItemMeta(meta);
		return i;
	}
	
	public void addModifiers(Player p, List<String> lore, AttributeData data, CharacterCreation cc) {
		AttributeData current = null;
		if(cc != null) {
			current = cc.getTempData();
		} else {
			if(!PlayerManager.get(p).hasActiveCharacter()) return;
			current = PlayerManager.get(p).getActiveCharacter().getAttributeData();
		}
		for(AttributeModifier m : current.getModifiers()) {
			int amount = m.getAmount();
			int added = 0;
			if(data.hasModifier(m)) {
				added = data.getAmount(m);
			}
			if(added > 0) {
				lore.add("§7"+WordUtils.capitalize(m.getType())+ ": §f"+amount+" §a(+"+added+")");
			} else if(added == 0) {
				lore.add("§7"+WordUtils.capitalize(m.getType())+ ": §f"+amount);
			} else {
				lore.add("§7"+WordUtils.capitalize(m.getType())+ ": §f"+amount+" §c("+added+")");
			}
		}
		for(ExperienceModifier m : current.getExperienceModifiers()) {
			int amount = m.getModifier();
			int added = 0;
			if(data.hasXPModifier(m)) {
				added = data.getAmount(m);
			}
			if(amount == 0 && added == 0) continue;
			if(added > 0) {
				lore.add("§7"+WordUtils.capitalize(m.getAlias())+ ": §f"+amount+"§e% §a(+"+added+"§e%)");
			} else if(added < 0){
				lore.add("§7"+WordUtils.capitalize(m.getAlias())+ ": §f"+amount+"§e% §c("+added+"§e%)");
			}
		}
	}
}
