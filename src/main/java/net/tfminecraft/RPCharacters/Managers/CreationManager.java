package net.tfminecraft.RPCharacters.Managers;

import java.util.HashMap;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Creation.CharacterCreation;
import net.tfminecraft.RPCharacters.Creation.Stage;
import net.tfminecraft.RPCharacters.Creation.Stages.QuestionStage;
import net.tfminecraft.RPCharacters.Creation.Stages.SelectionStage;
import net.tfminecraft.RPCharacters.Creation.Stages.SetterStage;
import net.tfminecraft.RPCharacters.Holder.RPCHolder;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.SelectableItem;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;

public class CreationManager implements Listener{
	public static HashMap<Player, CharacterCreation> activeCreators = new HashMap<>();
	
	public static void initiateCreation(Player p) {
		PlayerData pd = PlayerManager.get(p);
		if(pd.hasCooldown()) {
			p.sendMessage("§cYou are on cooldown from switching characters");
			p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
			return;
		}
		CharacterCreation cc = new CharacterCreation(p);
		cc.setCanNext(false);
		activeCreators.put(p, cc);
	}
	
	public static void next(Player p) {
		if(activeCreators.containsKey(p)) {
			activeCreators.get(p).runStage();
		}
	}
	
	@EventHandler
	public void chatEvent(AsyncPlayerChatEvent e) {
		Player p = e.getPlayer();
		if(!activeCreators.containsKey(p)) return;
		e.setCancelled(true);
		CharacterCreation cc = activeCreators.get(p);
		if(cc.getCurrentStage() instanceof QuestionStage) {
			cc.answerQuestion(e.getMessage());
		} else if(cc.getCurrentStage() instanceof SetterStage) {
			SetterStage s = (SetterStage) cc.getCurrentStage();
			s.finish(e.getMessage(), p, cc);
		}
	}

	public void click(Player p, Stage stage, CharacterCreation cc, InventoryClickEvent e) {
		RPCharacter c = null;
		if(cc != null) {
			c = cc.getCharacter();
		} else {
			c = PlayerManager.get(p).getActiveCharacter();
		}
		if(c == null) return;
		Inventory inventory = e.getClickedInventory();
		if(inventory == null) return;
		if(!(inventory.getHolder() instanceof RPCHolder)) return;
		RPCHolder h = (RPCHolder) inventory.getHolder();
		SelectionStage s = (SelectionStage) stage;
		for(int i = 0; i<s.getSlots().size(); i++) {
			if(s.getSlots().get(i) == e.getSlot()) {
				SelectableItem item = s.getOptions().get(i);
				if(!item.isSelected()) {
					for(SelectableItem stored : s.getSelection()) {
						if(stored.isExclusive(item)) {
							p.sendMessage("§cYou have one or more incompatible traits");
							p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
							return;
						}
					}
					for(Trait t : c.getTraits()) {
						if(item.isExclusive(t.getId())) {
							p.sendMessage("§cYou have one or more incompatible traits");
							p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
							return;
						}
					}
					if(s.getMaxSelections() <= s.getSelections()) {
						p.sendMessage("§cCannot make any more selections");
						p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
						return;
					}
					if(item.getCost() > s.getPoints()) {
						p.sendMessage("§cCannot afford this trait");
						p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
						return;
					}
					if(item.hasDependency()) {
						if(!item.getDependency().check(c)) {
							p.sendMessage("§cLacking requirements");
							p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
							return;
						}
					}
					s.select(item);
				} else {
					for(Trait t : c.getTraits()) {
						if(t.getTraitData().hasDependency() && t.getTraitData().getDependency().getDependencies().contains(item.getId()) && !t.getTraitData().getDependency().checkExclude(c, item.getId())) {
							p.sendMessage(t.getTraitData().getDependency().toString());
							p.sendMessage("§cYour trait "+t.getName()+" §cis dependent on this trait, remove that first!");
							p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
							return;
						}
					}
					s.unSelect(item);
				}
				p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				if(cc != null) item.click(cc);
				else item.click(c);
				InventoryManager inv = new InventoryManager();
				inv.selectionUpdate(e.getView().getTopInventory(), p, s, cc);
			}
		}
		if(e.getSlot() == s.getSize()-9) {
			h.override();
			p.closeInventory();
			if(cc != null) cc.cancel();
			return;
		}
		if(e.getSlot() == s.getSize()-1) {
			h.override();
			s.confirm(p, cc);
		}
	}

	public void nonCreationClick(Player p, InventoryClickEvent e) {
		Inventory inv = e.getClickedInventory();
		if(!e.getClickedInventory().equals(e.getView().getTopInventory())) return;
		if(!(inv.getHolder() instanceof RPCHolder)) return;
		RPCHolder h = (RPCHolder) inv.getHolder();
		e.setCancelled(true);
		click(p, h.getStage(), null, e);
	}
	
	@EventHandler
	public void selectionClick(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked();
		if(e.getClickedInventory() == null) return;
		if(!e.getClickedInventory().equals(e.getView().getTopInventory())) return;
		if(!activeCreators.containsKey(p)) {
			nonCreationClick(p, e);
			return;
		}
		e.setCancelled(true);
		CharacterCreation cc = activeCreators.get(p);
		if(cc.getCurrentStage() instanceof SelectionStage) {
			click(p, cc.getCurrentStage(), cc, e);
		}
	}
	
	@EventHandler
	public void stopClose(InventoryCloseEvent e) {
		Player p = (Player) e.getPlayer();
		if(!activeCreators.containsKey(p)) {
			if(!(e.getInventory().getHolder() instanceof RPCHolder)) return;
			RPCHolder h = (RPCHolder) e.getInventory().getHolder();
			if(h.isOverridden()) return;
			if(h.getStage() == null) return;
			Stage stage = h.getStage();
			if(stage instanceof SelectionStage) {
			new BukkitRunnable()
			{
				public void run()
				{
					InventoryManager inv = new InventoryManager();
					inv.selectionView(p, (SelectionStage) stage, null);
				}
			}.runTaskLater(RPCharacters.plugin, 3L);
			}
			return;
		}
		CharacterCreation cc = activeCreators.get(p);
		if(cc.getCurrentStage() instanceof SelectionStage) {
			SelectionStage s = (SelectionStage) cc.getCurrentStage();
			if(!s.isActive()) return;
			new BukkitRunnable()
			{
				public void run()
				{
					InventoryManager inv = new InventoryManager();
					inv.selectionView(p, s, cc);
				}
			}.runTaskLater(RPCharacters.plugin, 3L);
		}
	}
}
