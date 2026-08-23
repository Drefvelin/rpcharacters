package net.tfminecraft.RPCharacters.Managers;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Creation.CharacterCreation;
import net.tfminecraft.RPCharacters.Creation.Stage;
import net.tfminecraft.RPCharacters.Creation.SummaryEditSupport;
import net.tfminecraft.RPCharacters.Creation.StageEditLock;
import net.tfminecraft.RPCharacters.Creation.Stages.AttributesStage;
import net.tfminecraft.RPCharacters.Creation.Stages.ClueStage;
import net.tfminecraft.RPCharacters.Creation.Stages.QuestionStage;
import net.tfminecraft.RPCharacters.Creation.Stages.SelectionStage;
import net.tfminecraft.RPCharacters.Creation.Stages.SetterStage;
import net.tfminecraft.RPCharacters.Creation.Stages.SummaryStage;
import net.tfminecraft.RPCharacters.Holder.RPCHolder;
import net.tfminecraft.RPCharacters.Loaders.StageLoader;
import net.tfminecraft.RPCharacters.Loaders.TraitLoader;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.SelectableItem;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.Utils.PlaytimeGate;
import net.tfminecraft.RPCharacters.Utils.RPTexts;
import net.tfminecraft.RPCharacters.enums.CreationGuiContext;
import net.tfminecraft.RPCharacters.persona.CharacterSlotService;
import net.tfminecraft.RPCharacters.persona.PermissionGroupService;
import net.tfminecraft.RPCharacters.enums.Status;

public class CreationManager implements Listener{
	public static HashMap<Player, CharacterCreation> activeCreators = new HashMap<>();
	private static final String SUMMARY_ACTION_KEY = "summary_action";
	
	public static void initiateCreation(Player p) {
		PlayerData pd = PlayerManager.get(p);
		if (!CharacterSlotService.hasFreeSlot(p, pd)) {
			RPTexts.send(p, RPTexts.ERROR + "You don't have a free character slot!");
			p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
			return;
		}
		// Prefer landing any waiting web creates before opening in-game creator.
		net.tfminecraft.RPCharacters.ingest.CharacterIngestService.tryPullForPlayerAsync(
			net.tfminecraft.RPCharacters.RPCharacters.plugin, p.getUniqueId()
		);
		if(PermissionGroupService.hasCharacterSwitchCooldown(p, pd) && pd.getCharacters(Status.ALIVE).size() > 0 && !p.hasPermission("rpcharacters.no_cooldown")) {
			RPTexts.send(p, RPTexts.ERROR + "You are on cooldown from switching characters");
			p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
			return;
		}
		CharacterCreation cc = new CharacterCreation(p);
		cc.setCanNext(false);
		activeCreators.put(p, cc);
	}

	public static void initiateStagePreview(Player p, String stageId) {
		if (p == null) {
			return;
		}
		if (activeCreators.containsKey(p)) {
			RPTexts.send(p, RPTexts.ERROR + "You already have an active character session.");
			p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
			return;
		}
		CharacterCreation.forStagePreview(p, stageId);
	}

	public static void initiateEdit(Player p) {
		if (activeCreators.containsKey(p)) {
			CharacterCreation existing = activeCreators.get(p);
			if (existing != null && existing.isPreview()) {
				RPTexts.send(p, RPTexts.ERROR + "You are busy previewing a stage.");
			} else {
				RPTexts.send(p, RPTexts.ERROR + "You already have an active character session.");
			}
			p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
			return;
		}
		PlayerData pd = PlayerManager.get(p);
		if (pd == null || !pd.hasActiveCharacter()) {
			RPTexts.send(p, RPTexts.ERROR + "You have no active character to edit.");
			p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
			return;
		}
		CharacterCreation cc = CharacterCreation.forEdit(p, pd.getActiveCharacter());
		activeCreators.put(p, cc);
		cc.openSummary();
	}

	public static void initiateEditEntry(Player p, String entryKey) {
		if (entryKey == null || entryKey.isBlank()) {
			initiateEdit(p);
			return;
		}
		String stageId = SummaryEditSupport.resolveStageId(entryKey);
		if ("clues".equalsIgnoreCase(stageId)) {
			if (!activeCreators.containsKey(p)) {
				initiateEdit(p);
			}
			CharacterCreation cc = activeCreators.get(p);
			if (cc == null) {
				return;
			}
			InventoryManager inv = new InventoryManager();
			CreationGuiContext context = cc.isEditing() ? CreationGuiContext.EDIT_SUMMARY : CreationGuiContext.CREATION_SUMMARY;
			inv.cluesView(p, cc.getCharacter(), context, cc);
			return;
		}
		if (stageId == null) {
			RPTexts.send(p, RPTexts.ERROR + "Unknown edit option.");
			return;
		}
		Stage stage = StageLoader.getById(stageId);
		PlayerData pd = PlayerManager.get(p);
		if (pd == null || !pd.hasActiveCharacter()) {
			RPTexts.send(p, RPTexts.ERROR + "You have no active character to edit.");
			return;
		}
		if (stage != null && !StageEditLock.canEdit(p, stage, pd.getActiveCharacter())) {
			RPTexts.send(p, RPTexts.ERROR + "That choice is locked and can no longer be edited.");
			return;
		}
		if (!activeCreators.containsKey(p)) {
			CharacterCreation cc = CharacterCreation.forEdit(p, pd.getActiveCharacter());
			activeCreators.put(p, cc);
			cc.jumpToStageForEdit(stageId);
			return;
		}
		CharacterCreation existing = activeCreators.get(p);
		if (existing.isPreview()) {
			RPTexts.send(p, RPTexts.ERROR + "You are busy previewing a stage.");
			return;
		}
		if (!existing.isEditing()) {
			RPTexts.send(p, RPTexts.ERROR + "You are busy creating a character.");
			return;
		}
		existing.jumpToStageForEdit(stageId);
	}

	public static RPCharacter resolveCharacter(Player player, String characterId) {
		CharacterCreation cc = activeCreators.get(player);
		if (cc != null && cc.getCharacter().getId().equals(characterId)) {
			return cc.getCharacter();
		}
		PlayerData pd = PlayerManager.get(player);
		return pd != null ? pd.getCharacterById(characterId) : null;
	}

	public static boolean isDraftCharacter(Player player, String characterId) {
		CharacterCreation cc = activeCreators.get(player);
		return cc != null && !cc.isEditing() && !cc.isPreview()
				&& cc.getCharacter().getId().equals(characterId);
	}
	
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void chatEvent(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		if (!activeCreators.containsKey(player)) {
			return;
		}
		event.setCancelled(true);
		CharacterCreation cc = activeCreators.get(player);
		Stage activeStage = cc.getActiveStage();
		if (activeStage instanceof QuestionStage) {
			cc.answerQuestion(event.getMessage());
		} else if (activeStage instanceof SetterStage setter) {
			setter.finish(event.getMessage(), player, cc);
		} else if (activeStage instanceof ClueStage clue) {
			clue.finish(event.getMessage(), player, cc);
		}
	}

	public static void next(Player p) {
		if(activeCreators.containsKey(p)) {
			CharacterCreation cc = activeCreators.get(p);
			if (cc.isEditingFromSummary()) {
				cc.returnToSummary();
				return;
			}
			activeCreators.get(p).runStage();
		}
	}

	public void click(Player p, Stage stage, CharacterCreation cc, InventoryClickEvent e) {
		if(stage == null) return;
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
							RPTexts.send(p, RPTexts.ERROR + "You have one or more incompatible traits");
							p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
							return;
						}
					}
					for(Trait t : c.getTraits()) {
						if(item.isExclusive(t.getId()) || t.getTraitData().isExclusive(item.getId())) {
							RPTexts.send(p, RPTexts.ERROR + "You have one or more incompatible traits");
							p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
							return;
						}
					}
					if(s.getMaxSelections() <= s.getSelections()) {
						if (item.getType().equalsIgnoreCase("class") && s.getMaxSelections() == 1) {
							for (SelectableItem chosen : new ArrayList<>(s.getSelection())) {
								chosen.setSelected(false);
								s.unSelect(chosen);
							}
						} else {
							RPTexts.send(p, RPTexts.ERROR + "Cannot make any more selections");
							p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
							return;
						}
					}
					if(item.getCost() > s.getPoints()) {
						RPTexts.send(p, RPTexts.ERROR + "Cannot afford this trait");
						p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
						return;
					}
					if(item.hasDependency()) {
						if(!item.getDependency().check(c)) {
							RPTexts.send(p, RPTexts.ERROR + "Lacking requirements");
							p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
							return;
						}
					}
					if (item.getType().equalsIgnoreCase("trait")) {
						Trait trait = TraitLoader.getByString(item.getId());
						if (trait != null && !PlaytimeGate.canSelectTrait(p, trait)) {
							RPTexts.send(p, PlaytimeGate.denialMessage(p, trait));
							p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
							return;
						}
					}
					s.select(item);
				} else {
					for(Trait t : c.getTraits()) {
						if(t.getTraitData().hasDependency() && t.getTraitData().getDependency().getDependencies().contains(item.getId()) && !t.getTraitData().getDependency().checkExclude(c, item.getId())) {
							RPTexts.send(p, t.getTraitData().getDependency().toString());
							RPTexts.send(p, RPTexts.ERROR + "Your trait " + t.getName() + RPTexts.ERROR + " is dependent on this trait, remove that first!");
							p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
							return;
						}
					}
					s.unSelect(item);
				}
				p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
				if(cc != null) item.click(cc);
				else {
					item.click(c);
					c.update();
					RPCharacters.getPlayerManager().savePlayer(p);
					RPCharacters.getPlayerManager().reevaluateFreeze(p);
				}
				InventoryManager inv = new InventoryManager();
				inv.selectionUpdate(e.getView().getTopInventory(), p, s, cc);
			}
		}
		if(e.getSlot() == s.getSize()-9) {
			if(cc != null) {
				if (cc.isEditingFromSummary()) {
					h.override();
					p.closeInventory();
					cc.returnToSummary();
					return;
				}
				if (cc.isEditing()) {
					h.override();
					p.closeInventory();
					cc.returnToSummary();
					return;
				}
				cc.cancel();
			}
			h.override();
			p.closeInventory();
			return;
		}
		if(e.getSlot() == s.getSize()-1) {
			h.override();
			s.confirm(p, cc);
		}
	}

	private void handleSummaryClick(Player p, InventoryClickEvent e) {
		if (!activeCreators.containsKey(p)) {
			return;
		}
		CharacterCreation cc = activeCreators.get(p);
		ItemStack clicked = e.getCurrentItem();
		if (clicked == null || clicked.getItemMeta() == null) {
			return;
		}
		NamespacedKey actionKey = new NamespacedKey(RPCharacters.plugin, SUMMARY_ACTION_KEY);
		String action = clicked.getItemMeta().getPersistentDataContainer().get(actionKey, PersistentDataType.STRING);
		if (action == null) {
			return;
		}
		if (e.getInventory().getHolder() instanceof RPCHolder holder) {
			holder.override();
		}
		p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
		if ("confirm".equals(action)) {
			if (cc.isPreview()) {
				cc.endPreview();
			} else if (cc.isEditing()) {
				cc.closeEditSession();
			} else {
				cc.finish();
			}
			return;
		}
		if ("cancel".equals(action)) {
			p.closeInventory();
			cc.cancel();
			return;
		}
		if ("clues".equals(action)) {
			if (cc.isPreview()) {
				RPTexts.send(p, RPTexts.ERROR + "Preview is one stage only. Confirm or cancel to exit.");
				return;
			}
			InventoryManager inv = new InventoryManager();
			CreationGuiContext context = cc.isEditing() ? CreationGuiContext.EDIT_SUMMARY : CreationGuiContext.CREATION_SUMMARY;
			inv.cluesView(p, cc.getCharacter(), context, cc);
			return;
		}
		if (action.startsWith("edit:")) {
			if (cc.isPreview()) {
				RPTexts.send(p, RPTexts.ERROR + "Preview is one stage only. Confirm or cancel to exit.");
				return;
			}
			String stageId = action.substring("edit:".length());
			Stage stage = StageLoader.getById(stageId);
			if (stage != null && !StageEditLock.canEdit(p, stage, cc.getCharacter())) {
				RPTexts.send(p, RPTexts.ERROR + "That choice is locked and can no longer be edited.");
				return;
			}
			cc.jumpToStageForEdit(stageId);
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
		if (e.getView().getTitle().equals("§7Creation Summary") || e.getView().getTitle().equals("§7Edit Character")) {
			e.setCancelled(true);
			handleSummaryClick(p, e);
			return;
		}
		if(!activeCreators.containsKey(p)) {
			nonCreationClick(p, e);
			return;
		}
		e.setCancelled(true);
		CharacterCreation cc = activeCreators.get(p);
		Stage activeStage = cc.getActiveStage();
		if (activeStage instanceof SelectionStage) {
			click(p, activeStage, cc, e);
		} else if (activeStage instanceof AttributesStage) {
			attributesClick(p, (AttributesStage) activeStage, cc, e);
		}
	}

	private void attributesClick(Player p, AttributesStage s, CharacterCreation cc, InventoryClickEvent e) {
		Inventory inventory = e.getClickedInventory();
		if (inventory == null) {
			return;
		}
		if (!(inventory.getHolder() instanceof RPCHolder)) {
			return;
		}
		RPCHolder h = (RPCHolder) inventory.getHolder();
		int slot = e.getSlot();
		if (slot == s.getSize() - 9) {
			if (cc != null) {
				if (cc.isEditingFromSummary() || cc.isEditing()) {
					h.override();
					p.closeInventory();
					cc.returnToSummary();
					return;
				}
				cc.cancel();
			}
			h.override();
			p.closeInventory();
			return;
		}
		if (slot == s.getSize() - 1) {
			h.override();
			s.confirm(p, cc);
			return;
		}
		ItemStack clicked = e.getCurrentItem();
		if (clicked == null || clicked.getItemMeta() == null) {
			return;
		}
		NamespacedKey attrKey = new NamespacedKey(RPCharacters.plugin, "attr_id");
		NamespacedKey actionKey = new NamespacedKey(RPCharacters.plugin, "attr_action");
		String attr = clicked.getItemMeta().getPersistentDataContainer()
			.get(attrKey, PersistentDataType.STRING);
		String action = clicked.getItemMeta().getPersistentDataContainer()
			.get(actionKey, PersistentDataType.STRING);
		if (attr == null || action == null) {
			return;
		}
		boolean changed = false;
		if ("plus".equals(action)) {
			changed = s.tryIncrease(attr);
			if (!changed) {
				RPTexts.send(p, RPTexts.ERROR + "Cannot increase " + attr + ".");
				p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
				return;
			}
		} else if ("minus".equals(action)) {
			changed = s.tryDecrease(attr);
			if (!changed) {
				RPTexts.send(p, RPTexts.ERROR + "Cannot decrease " + attr + ".");
				p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
				return;
			}
		} else {
			return;
		}
		p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 1f, 1f);
		InventoryManager inv = new InventoryManager();
		inv.attributesUpdate(e.getView().getTopInventory(), p, s, cc);
	}
	
	@EventHandler
	public void stopClose(InventoryCloseEvent e) {
		Player p = (Player) e.getPlayer();
		if(!(e.getInventory().getHolder() instanceof RPCHolder)) return;
		RPCHolder h = (RPCHolder) e.getInventory().getHolder();
		if(!activeCreators.containsKey(p)) {
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
			} else if (stage instanceof AttributesStage) {
				new BukkitRunnable() {
					public void run() {
						InventoryManager inv = new InventoryManager();
						inv.attributesView(p, (AttributesStage) stage, null);
					}
				}.runTaskLater(RPCharacters.plugin, 3L);
			}
			return;
		}
		CharacterCreation cc = activeCreators.get(p);
		if (cc.isPreview()) {
			if (h.isOverridden()) {
				return;
			}
			cc.endPreview();
			return;
		}
		if (h.getStage() instanceof SummaryStage) {
			if (h.isOverridden()) {
				return;
			}
			new BukkitRunnable() {
				public void run() {
					if (activeCreators.containsKey(p)) {
						activeCreators.get(p).openSummary();
					}
				}
			}.runTaskLater(RPCharacters.plugin, 3L);
			return;
		}
		Stage activeStage = cc.getActiveStage();
		if(activeStage instanceof SelectionStage) {
			SelectionStage s = (SelectionStage) activeStage;
			if(!s.isActive()) return;
			if(h.isOverridden()) return;
			new BukkitRunnable()
			{
				public void run()
				{
					InventoryManager inv = new InventoryManager();
					inv.selectionView(p, s, cc);
				}
			}.runTaskLater(RPCharacters.plugin, 3L);
		} else if (activeStage instanceof AttributesStage) {
			AttributesStage s = (AttributesStage) activeStage;
			if (!s.isActive()) return;
			if (h.isOverridden()) return;
			new BukkitRunnable() {
				public void run() {
					InventoryManager inv = new InventoryManager();
					inv.attributesView(p, s, cc);
				}
			}.runTaskLater(RPCharacters.plugin, 3L);
		}
	}
}
