package net.tfminecraft.RPCharacters.Managers;

import java.util.HashMap;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitRunnable;

import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Creation.CharacterCreation;
import net.tfminecraft.RPCharacters.Creation.Stages.QuestionStage;
import net.tfminecraft.RPCharacters.Creation.Stages.SelectionStage;
import net.tfminecraft.RPCharacters.Creation.Stages.SetterStage;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
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
	
	@EventHandler
	public void selectionClick(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked();
		if(!activeCreators.containsKey(p)) return;
		e.setCancelled(true);
		CharacterCreation cc = activeCreators.get(p);
		if(cc.getCurrentStage() instanceof SelectionStage) {
			SelectionStage s = (SelectionStage) cc.getCurrentStage();
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
						for(Trait t : cc.getCharacter().getTraits()) {
							if(item.isExclusive(t.getId())) {
								p.sendMessage("§cYou have one or more incompatible traits");
								p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
								return;
							}
						}
						if(s.getMaxSelections() == s.getSelections()) {
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
							if(!item.getDependency().check(cc.getCharacter())) {
								p.sendMessage("§cLacking requirements");
								p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
								return;
							}
						}
						s.increase();
						s.spendPoints(item.getCost());
						s.select(item);
					} else {
						s.decrease();
						s.addPoints(item.getCost());
						s.unSelect(item);
					}
					p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
					item.click(cc);
					InventoryManager inv = new InventoryManager();
					inv.selectionUpdate(e.getView().getTopInventory(), p, s, cc);
				}
			}
			if(e.getSlot() == s.getSize()-1) {
				s.confirm(p, cc);
			}
		}
	}
	
	@EventHandler
	public void stopClose(InventoryCloseEvent e) {
		Player p = (Player) e.getPlayer();
		if(!activeCreators.containsKey(p)) return;
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
