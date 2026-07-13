package net.tfminecraft.RPCharacters.Managers;

import java.util.ArrayList;
import java.util.List;

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

import me.plugins.tlibs.shaded.lang3.text.WordUtils;
import net.Indyuce.mmocore.MMOCore;
import net.Indyuce.mmocore.api.player.profess.PlayerClass;
import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Permissions;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Creation.CharacterCreation;
import net.tfminecraft.RPCharacters.Creation.Dependency;
import net.tfminecraft.RPCharacters.Creation.Stage;
import net.tfminecraft.RPCharacters.Creation.StageEditLock;
import net.tfminecraft.RPCharacters.Creation.Stages.SelectionStage;
import net.tfminecraft.RPCharacters.Creation.Stages.SummaryStage;
import net.tfminecraft.RPCharacters.Holder.RPCHolder;
import net.tfminecraft.RPCharacters.Loaders.RaceLoader;
import net.tfminecraft.RPCharacters.Loaders.StageLoader;
import net.tfminecraft.RPCharacters.Loaders.TraitLoader;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.SelectableItem;
import net.tfminecraft.RPCharacters.Objects.Attributes.AttributeData;
import net.tfminecraft.RPCharacters.Objects.Attributes.AttributeModifier;
import net.tfminecraft.RPCharacters.Objects.Experience.ExperienceModifier;
import net.tfminecraft.RPCharacters.Objects.Races.Race;
import net.tfminecraft.RPCharacters.Objects.Trait.PotionData;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.Utils.AgeFormatter;
import net.tfminecraft.RPCharacters.Utils.ClueFormatter;
import net.tfminecraft.RPCharacters.Utils.ClueProgressFormatter;
import net.tfminecraft.RPCharacters.Utils.RPTexts;
import net.tfminecraft.RPCharacters.identity.DisplayIdentityService;
import net.tfminecraft.RPCharacters.identity.PersonaService;
import net.tfminecraft.RPCharacters.mmocore.MmoCoreClassGuiHelper;
import net.tfminecraft.RPCharacters.persona.CharacterSlotService;
import net.tfminecraft.RPCharacters.persona.PermissionGroupService;
import net.tfminecraft.RPCharacters.enums.CreationGuiContext;
import net.tfminecraft.RPCharacters.enums.Status;
import net.tfminecraft.RPCharacters.permadeath.PermadeathService;

public class InventoryManager {
	private static final String CHARACTER_ID_KEY = "character_id";
	private static final String CLUE_INDEX_KEY = "clue_index";
	private static final String OPEN_CLUES_GUI_KEY = "open_clues_gui";
	private static final String SUMMARY_ACTION_KEY = "summary_action";

	private static String t(String raw) {
		return RPTexts.format(raw);
	}

	public void characterView(Player p, RPCharacter c) {
		PlayerData pd = PlayerManager.get(c.getOwner());
		Inventory i = RPCharacters.plugin.getServer().createInventory(new RPCHolder(c.getOwner()), 27, t(RPTexts.MUTED + "Character Info"));
		i.setItem(10, getCharacterItem(c, false));
		i.setItem(12, getDescriptionItem(c));
		i.setItem(14, getTraitsItem(c));
		if (c.getStatus().equals(Status.ALIVE) && c.getOwner().equals(p)) {
			i.setItem(16, getCluesPreviewItem(c));
		}
		i.setItem(26, getBackButton());
		if(c.getStatus().equals(Status.ALIVE) && c.getOwner().equals(p)) {
			i.setItem(8, getKillItem());
		}
		if (c.getStatus().equals(Status.DEAD) && Permissions.isAdmin(p)) {
			boolean canRevive = CharacterSlotService.hasFreeSlot(c.getOwner(), pd);
			int alive = pd.getCharacters(Status.ALIVE).size();
			int max = CharacterSlotService.getMaxAliveCharacters(c.getOwner());
			i.setItem(4, getReviveItem(canRevive, alive, max));
		}
		if(c.getStatus().equals(Status.ALIVE) && !c.isActive() && (!PermissionGroupService.hasCharacterSwitchCooldown(p, pd) || Permissions.isAdmin(p)) && c.getOwner().equals(p)) {
			i.setItem(6, getSwitchItem());
		}
		int slotn = 0;
		while(slotn < i.getSize()) {
			if(i.getItem(slotn) == null) {
				ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta fm = fill.getItemMeta();
				fm.setDisplayName(t(RPTexts.MUTED + " "));
				fill.setItemMeta(fm);
				i.setItem(slotn, fill);
			}
			slotn++;
		}
		p.openInventory(i);
	}

	public void cluesView(Player viewer, RPCharacter c) {
		cluesView(viewer, c, CreationGuiContext.NONE, null);
	}

	@SuppressWarnings("deprecation")
	public void cluesView(Player viewer, RPCharacter c, CreationGuiContext context, CharacterCreation creation) {
		int clueCount = c.getPlayerClues().size();
		int rows = Math.max(3, Math.min(6, ((clueCount + 2) + 8) / 9));
		String title = ClueProgressFormatter.guiTitle(c);
		RPCHolder holder = context == CreationGuiContext.CREATION_SUMMARY
				|| context == CreationGuiContext.EDIT_SUMMARY
				? new RPCHolder(c.getOwner(), creation, context)
				: new RPCHolder(c.getOwner());
		Inventory i = RPCharacters.plugin.getServer().createInventory(holder, rows * 9, title);

		NamespacedKey characterKey = new NamespacedKey(RPCharacters.plugin, CHARACTER_ID_KEY);
		NamespacedKey clueIndexKey = new NamespacedKey(RPCharacters.plugin, CLUE_INDEX_KEY);

		int slot = 0;
		List<String> clues = c.getPlayerClues();
		for (int index = 0; index < clues.size(); index++) {
			if (slot >= i.getSize() - 9) break;
			ItemStack paper = new ItemStack(Material.PAPER, 1);
			ItemMeta meta = paper.getItemMeta();
			meta.setDisplayName(t(RPTexts.MUTED + "Clue #" + (index + 1)));
			List<String> lore = new ArrayList<>();
			lore.add(clues.get(index));
			lore.add(RPTexts.spacer());
			lore.add(t(RPTexts.ERROR + "Click to remove"));
			meta.setLore(lore);
			meta.getPersistentDataContainer().set(characterKey, PersistentDataType.STRING, c.getId());
			meta.getPersistentDataContainer().set(clueIndexKey, PersistentDataType.INTEGER, index);
			paper.setItemMeta(meta);
			i.setItem(slot, paper);
			slot++;
		}

		if (c.canAddClue() && c.getOwner().equals(viewer)) {
			ItemStack add = new ItemStack(Material.LIME_DYE, 1);
			ItemMeta addMeta = add.getItemMeta();
			addMeta.setDisplayName(t(RPTexts.SUCCESS + "Add Clue"));
			List<String> addLore = new ArrayList<>();
			addLore.add(t(RPTexts.MUTED + "Click to type a new clue in chat"));
			if (c.hasEnoughClues()) {
				addLore.add(t(RPTexts.MUTED + "Minimum met. Extra clues optional up to " + RPTexts.WARN + Cache.maxClues));
			}
			addMeta.setLore(addLore);
			addMeta.getPersistentDataContainer().set(characterKey, PersistentDataType.STRING, c.getId());
			add.setItemMeta(addMeta);
			i.setItem(8, add);
		} else if (!c.canAddClue() && c.getOwner().equals(viewer)) {
			ItemStack max = new ItemStack(Material.GRAY_DYE, 1);
			ItemMeta maxMeta = max.getItemMeta();
			maxMeta.setDisplayName(t(RPTexts.MUTED + "Clue limit reached"));
			List<String> maxLore = new ArrayList<>();
			maxLore.add(t(RPTexts.MUTED + "You cannot have more than " + RPTexts.WARN + Cache.maxClues + " " + RPTexts.MUTED + "clues."));
			maxMeta.setLore(maxLore);
			max.setItemMeta(maxMeta);
			i.setItem(8, max);
		}

		i.setItem(i.getSize() - 1, getBackButton(c.getId()));

		int slotn = 0;
		while (slotn < i.getSize()) {
			if (i.getItem(slotn) == null) {
				ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta fm = fill.getItemMeta();
				fm.setDisplayName(t(RPTexts.MUTED + " "));
				fill.setItemMeta(fm);
				i.setItem(slotn, fill);
			}
			slotn++;
		}
		viewer.openInventory(i);
	}

	@SuppressWarnings("deprecation")
	public void creationSummaryView(Player player, CharacterCreation creation, SummaryStage summary) {
		boolean editing = creation.isEditing();
		String title = editing ? t(RPTexts.MUTED + "Edit Character") : t(RPTexts.MUTED + "Creation Summary");
		Inventory inventory = RPCharacters.plugin.getServer().createInventory(
				new RPCHolder(player, summary, creation, CreationGuiContext.NONE),
				54,
				title);
		RPCharacter character = creation.getCharacter();
		NamespacedKey actionKey = new NamespacedKey(RPCharacters.plugin, SUMMARY_ACTION_KEY);

		int[] entrySlots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25,
				28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43};
		int slotIndex = 0;

		for (var entry : summary.getEntries().entrySet()) {
			if (slotIndex >= entrySlots.length) {
				break;
			}
			String entryKey = entry.getKey();
			String stageId = entry.getValue();
			ItemStack item = buildSummaryEntryItem(player, character, entryKey, stageId, editing);
			if (item == null) {
				continue;
			}
			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				boolean locked = isSummaryEntryLocked(player, character, stageId);
				if (!locked) {
					String action = "clues".equalsIgnoreCase(stageId) ? "clues" : "edit:" + stageId;
					meta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, action);
				}
				item.setItemMeta(meta);
			}
			inventory.setItem(entrySlots[slotIndex++], item);
		}

		boolean canConfirm = editing || canConfirmCreation(creation);
		ItemStack confirm = new ItemStack(canConfirm ? Material.LIME_CONCRETE : Material.RED_CONCRETE, 1);
		ItemMeta confirmMeta = confirm.getItemMeta();
		if (confirmMeta != null) {
			if (editing) {
				confirmMeta.setDisplayName(t(RPTexts.SUCCESS + "Done"));
				confirmMeta.setLore(List.of(t(RPTexts.MUTED + "Close the editor.")));
			} else {
				confirmMeta.setDisplayName(t(canConfirm ? RPTexts.SUCCESS + "Confirm Character" : RPTexts.ERROR + "Cannot Confirm Yet"));
				List<String> confirmLore = new ArrayList<>();
				if (!canConfirm) {
					confirmLore.add(t(RPTexts.MUTED + "Complete all required choices and clues first."));
				} else {
					confirmLore.add(t(RPTexts.MUTED + "Click to create your character."));
				}
				confirmMeta.setLore(confirmLore);
			}
			confirmMeta.getPersistentDataContainer().set(actionKey, PersistentDataType.STRING, "confirm");
			confirm.setItemMeta(confirmMeta);
		}
		inventory.setItem(53, confirm);

		fillEmptySlots(inventory);
		player.openInventory(inventory);
	}

	private boolean isSummaryEntryLocked(Player player, RPCharacter character, String stageId) {
		if ("clues".equalsIgnoreCase(stageId)) {
			return false;
		}
		Stage stage = StageLoader.getById(stageId);
		return stage != null && !StageEditLock.canEdit(player, stage, character);
	}

	private boolean canConfirmCreation(CharacterCreation creation) {
		RPCharacter character = creation.getCharacter();
		if (character == null) {
			return false;
		}
		PlayerData pd = PlayerManager.get(creation.getCharacter().getOwner());
		if (pd == null || !CharacterSlotService.hasFreeSlot(character.getOwner(), pd)) {
			return false;
		}
		if (!character.hasEnoughClues()) {
			return false;
		}
		if (character.getName() == null || character.getName().isBlank()) {
			return false;
		}
		if (!character.hasMMOClass()) {
			return false;
		}
		if (character.getRace() == null) {
			return false;
		}
		if (character.getBirthday() == null || character.getBirthday().isBlank()) {
			return false;
		}
		String description = character.getPersonaDescription();
		return description != null && !description.isBlank();
	}

	@SuppressWarnings("deprecation")
	private ItemStack buildSummaryEntryItem(Player player, RPCharacter character, String entryKey, String stageId,
			boolean editing) {
		String label = WordUtils.capitalize(entryKey.replace('_', ' '));
		List<String> lore = new ArrayList<>();
		boolean locked = editing && isSummaryEntryLocked(player, character, stageId);
		if (locked) {
			lore.add(t(RPTexts.ERROR + "Locked"));
		} else {
			lore.add(t(RPTexts.MUTED + "Click to change"));
		}
		if (editing && !"clues".equalsIgnoreCase(stageId)) {
			Stage stage = StageLoader.getById(stageId);
			String lockLore = StageEditLock.lockLore(stage, character);
			if (lockLore != null && !locked) {
				lore.add(t(lockLore));
			}
		}
		ItemStack item;

		switch (entryKey.toLowerCase()) {
			case "name" -> {
				item = new ItemStack(Material.NAME_TAG, 1);
				lore.add(0, character.getName() != null ? character.getName() : t(RPTexts.MUTED + "Not set"));
			}
			case "class" -> {
				item = new ItemStack(Material.NETHERITE_SWORD, 1);
				if (character.hasMMOClass()) {
					PlayerClass playerClass = MMOCore.plugin.classManager.get(character.getMMOClass());
					if (playerClass != null && playerClass.getIcon() != null
							&& playerClass.getIcon().getType() != Material.AIR) {
						item = playerClass.getIcon().clone();
					}
					lore.add(0, playerClass != null ? playerClass.getName() : t(RPTexts.MUTED + character.getMMOClass()));
				} else {
					lore.add(0, t(RPTexts.MUTED + "Not selected"));
				}
			}
			case "race" -> {
				if (character.getRace() != null) {
					item = new ItemStack(Material.PLAYER_HEAD, 1);
					lore.add(0, character.getRace().getName());
				} else {
					item = new ItemStack(Material.PLAYER_HEAD, 1);
					lore.add(0, t(RPTexts.MUTED + "Not selected"));
				}
			}
			case "age" -> {
				item = new ItemStack(Material.CLOCK, 1);
				lore.clear();
				lore.add(t(RPTexts.MUTED + "Age: " + RPTexts.WARN + PersonaService.resolveAge(character)));
				lore.add(t(RPTexts.MUTED + "Birthday: " + RPTexts.WARN + PersonaService.resolveBirthday(character)));
				lore.add(t(RPTexts.MUTED + "Click to change"));
			}
			case "description" -> {
				item = new ItemStack(Material.WRITABLE_BOOK, 1);
				String description = character.getPersonaDescription();
				if (description == null || description.isBlank()) {
					lore.add(0, t(RPTexts.MUTED + "Not set"));
				} else {
					lore.addAll(0, wrapSummaryText(description, 40));
				}
			}
			case "clues" -> {
				item = new ItemStack(Material.BOOK, 1);
				lore.add(0, t(character.getPlayerClues().size() + "/" + character.getCluesNeeded()));
				lore.add(1, t(RPTexts.COMMAND + "/rpcharacter clues"));
				lore.remove(t(RPTexts.MUTED + "Click to change"));
			}
			default -> {
				if ("clues".equalsIgnoreCase(stageId)) {
					return null;
				}
				List<String> traitNames = new ArrayList<>();
				if (character.getTraits() != null) {
					for (Trait trait : character.getTraits()) {
						if (trait.getTraitData().getKey().equalsIgnoreCase(entryKey)) {
							traitNames.add(trait.getName());
						}
					}
				}
				item = new ItemStack(Material.PAPER, 1);
				if (traitNames.isEmpty()) {
					lore.add(0, t(RPTexts.MUTED + "Not selected"));
				} else {
					lore.add(0, RPTexts.joinFormatted(traitNames, ", "));
				}
			}
		}

		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(t(RPTexts.WARN + label));
			meta.setLore(lore);
			item.setItemMeta(meta);
		}
		return item;
	}

	private List<String> wrapSummaryText(String text, int width) {
		List<String> lines = new ArrayList<>();
		String[] words = text.split("\\s+");
		StringBuilder current = new StringBuilder();
		for (String word : words) {
			if (current.length() + word.length() + 1 > width) {
				lines.add(t(RPTexts.MUTED + current));
				current = new StringBuilder(word);
			} else if (current.length() == 0) {
				current.append(word);
			} else {
				current.append(' ').append(word);
			}
		}
		if (current.length() > 0) {
			lines.add(t(RPTexts.MUTED + current));
		}
		return lines;
	}

	private void fillEmptySlots(Inventory inventory) {
		for (int slot = 0; slot < inventory.getSize(); slot++) {
			if (inventory.getItem(slot) == null) {
				ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta meta = fill.getItemMeta();
				meta.setDisplayName(t(RPTexts.MUTED + " "));
				fill.setItemMeta(meta);
				inventory.setItem(slot, fill);
			}
		}
	}

	@SuppressWarnings("deprecation")
	public ItemStack getCluesPreviewItem(RPCharacter c) {
		ItemStack i = new ItemStack(Material.BOOK, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName(t(RPTexts.MUTED + "Character Clues"));
		List<String> lore = new ArrayList<>();
		lore.add(t(RPTexts.MUTED + "------------------------"));
		lore.add(t(RPTexts.WARN + "Clues (" + c.getPlayerClues().size() + "/" + c.getCluesNeeded() + "):"));
		lore.add(RPTexts.spacer());
		if (c.getPlayerClues().isEmpty()) {
			lore.add(t(RPTexts.MUTED + "No clues yet."));
		} else {
			for (String clue : c.getPlayerClues()) {
				lore.add(clue);
			}
		}
		lore.add(RPTexts.spacer());
		lore.add(t(RPTexts.MUTED + "------------------------"));
		lore.add(t(RPTexts.COMMAND + "/rpcharacter clues"));
		meta.setLore(lore);
		NamespacedKey characterKey = new NamespacedKey(RPCharacters.plugin, CHARACTER_ID_KEY);
		NamespacedKey openKey = new NamespacedKey(RPCharacters.plugin, OPEN_CLUES_GUI_KEY);
		meta.getPersistentDataContainer().set(characterKey, PersistentDataType.STRING, c.getId());
		meta.getPersistentDataContainer().set(openKey, PersistentDataType.BYTE, (byte) 1);
		i.setItemMeta(meta);
		return i;
	}

	public void traitsView(Player p, RPCharacter c) {
		int visibleTraitCount = 0;
		for(Trait t : c.getTraits()) {
			if(Cache.backgroundTraitTypes.contains(t.getTraitData().getKey())) continue;
			visibleTraitCount++;
		}
		int rows = Math.max(3, Math.min(6, ((visibleTraitCount + 1) + 8) / 9));
		Inventory i = RPCharacters.plugin.getServer().createInventory(new RPCHolder(c.getOwner()), rows * 9, t(RPTexts.MUTED + "Trait List"));

		int slot = 0;
		for(Trait t : c.getTraits()) {
			if(slot >= i.getSize() - 1) break;
			if(Cache.backgroundTraitTypes.contains(t.getTraitData().getKey())) continue;
			i.setItem(slot, getTraitInfoItem(t, c.getId()));
			slot++;
		}
		i.setItem(i.getSize() - 1, getBackButton(c.getId()));

		int slotn = 0;
		while(slotn < i.getSize()) {
			if(i.getItem(slotn) == null) {
				ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta fm = fill.getItemMeta();
				fm.setDisplayName(t(RPTexts.MUTED + " "));
				fill.setItemMeta(fm);
				i.setItem(slotn, fill);
			}
			slotn++;
		}
		p.openInventory(i);
	}
	public void deadView(Player p, Player t) {
		Inventory i = RPCharacters.plugin.getServer().createInventory(new RPCHolder(t), 27, t(RPTexts.MUTED + "Dead Characters"));
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
				fm.setDisplayName(t(RPTexts.MUTED + " "));
				fill.setItemMeta(fm);
				i.setItem(slotn, fill);
			}
			slotn++;
		}
		p.openInventory(i);
	}
	public void profileView(Player p, Player t) {
		Inventory i = RPCharacters.plugin.getServer().createInventory(new RPCHolder(t), 27, t(RPTexts.MUTED + "Character Menu"));
		i.setItem(0, getPlayerHead(t));
		PlayerData pd = PlayerManager.get(t);
		List<RPCharacter> aliveChars = pd.getCharacters(Status.ALIVE);
		int maxAllowed = CharacterSlotService.getMaxAliveCharacters(t);
		boolean overLimit = aliveChars.size() > maxAllowed;
		for (int slotIndex = 0; slotIndex < CharacterSlotService.getDisplaySlotCount(); slotIndex++) {
			int inventorySlot = Cache.characterSlots.get(slotIndex);
			if (slotIndex < aliveChars.size()) {
				i.setItem(inventorySlot, getCharacterItem(aliveChars.get(slotIndex), true, overLimit));
			} else if (slotIndex < maxAllowed) {
				i.setItem(inventorySlot, getEmptyCharacterItem(pd));
			} else if (CharacterSlotService.shouldShowLockedSlot(slotIndex, maxAllowed)) {
				i.setItem(inventorySlot, getLockedCharacterItem(t, slotIndex));
			}
		}
		i.setItem(Cache.deadSlot, getDeadCharactersItem(pd));
		int slotn = 0;
		while(slotn < i.getSize()) {
			if(i.getItem(slotn) == null) {
				ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta fm = fill.getItemMeta();
				fm.setDisplayName(t(RPTexts.MUTED + " "));
				fill.setItemMeta(fm);
				i.setItem(slotn, fill);
			}
			slotn++;
		}
		p.openInventory(i);
	}
	public void selectionView(Player player, SelectionStage s, CharacterCreation cc) {
		String label = s.getKey();
		if (label == null || label.isBlank()) {
			label = s.getTarget() != null ? s.getTarget() : "Selection";
		}
		@SuppressWarnings("deprecation")
		Inventory i = RPCharacters.plugin.getServer().createInventory(new RPCHolder(player, s), s.getSize(), t(RPTexts.MUTED + WordUtils.capitalize(label) + " Selection"));
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
		Inventory i = RPCharacters.plugin.getServer().createInventory(new RPCHolder(player), 27, t(RPTexts.MUTED + "Confirm Action"));
		i.setItem(11, createItemStack(Material.GREEN_CONCRETE, t(RPTexts.SUCCESS + "Confirm")));
		i.setItem(15, createItemStack(Material.RED_CONCRETE, t(RPTexts.ERROR + "Cancel")));
		Integer slot = 0;
		while(slot < i.getSize()) {
			if(i.getItem(slot) == null) {
				ItemStack fill = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
				ItemMeta fm = fill.getItemMeta();
				fm.setDisplayName(t(RPTexts.MUTED + " "));
				fill.setItemMeta(fm);
				i.setItem(slot, fill);
			}
			slot++;
		}
		player.openInventory(i);
	}
	public String formatTime(Integer time) {
		Integer days = time / 1440;
		Integer remainderAfterDays = time % 1440;
		Integer hours = remainderAfterDays / 60;
		Integer minutes = remainderAfterDays % 60;

		String formattedTime = "";

		if (days > 0) {
			formattedTime += days + "d ";
		}
		if (hours > 0) {
			formattedTime += hours + "h ";
		}
		if (minutes > 0) {
			formattedTime += minutes + "m ";
		}
		if (days == 0 && hours == 0 && minutes == 0) {
			formattedTime = "0m";
		}

		return formattedTime.trim();
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
		meta.setDisplayName(t(RPTexts.ERROR + "Back"));
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack getBackButton(String characterId) {
		ItemStack i = getBackButton();
		ItemMeta meta = i.getItemMeta();
		NamespacedKey key = new NamespacedKey(RPCharacters.plugin, CHARACTER_ID_KEY);
		meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, characterId);
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack createCancelItem(CharacterCreation cc) {
		ItemStack i = new ItemStack(Material.BARRIER, 1);
		ItemMeta meta = i.getItemMeta();
		List<String> lore = new ArrayList<>();
		if(cc != null) {
			meta.setDisplayName(t(RPTexts.ERROR + "Cancel Creation"));
		
			lore.add(t(RPTexts.MUTED + "Cancel the current character creation"));
			lore.add(t(RPTexts.ERROR + "This is not reversible!"));
		} else{ 
			meta.setDisplayName(t(RPTexts.ERROR + "Cancel"));
		
			lore.add(t(RPTexts.MUTED + "Cancel the edit"));
		}
		
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack getKillItem() {
		ItemStack i = new ItemStack(Material.IRON_AXE, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName(t(RPTexts.ERROR + "Kill Character"));
		List<String> lore = new ArrayList<String>();
		lore.add(t(RPTexts.MUTED + "This will kill the character"));
		lore.add(t(RPTexts.MUTED + "and add it to the list of dead characters"));
		lore.add(t(RPTexts.MUTED + "Only staff can reverse this"));
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack getSwitchItem() {
		ItemStack i = new ItemStack(Material.EMERALD, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName(t(RPTexts.SUCCESS + "Switch to Character"));
		List<String> lore = new ArrayList<String>();
		lore.add(t(RPTexts.MUTED + "This will switch you to this character"));
		lore.add(t(RPTexts.MUTED + "You will be put on cooldown from switching again"));
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack getDescriptionItem(RPCharacter c) {
		ItemStack i = new ItemStack(Material.WRITABLE_BOOK, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName(t(RPTexts.MUTED + "Character Background"));
		List<String> lore = new ArrayList<String>();
		lore.add(t(RPTexts.MUTED + "------------------------"));
		lore.add(t(RPTexts.WARN + "Background:"));
		lore.add(RPTexts.spacer());
		for(String s : c.getDescription()) {
			lore.add(t(s.startsWith("§") || s.contains("#") ? s : RPTexts.MUTED + s));
		}
		lore.add(t(RPTexts.MUTED + "------------------------"));
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
	@SuppressWarnings("deprecation")
	public ItemStack getTraitsItem(RPCharacter c) {
		ItemStack i = new ItemStack(Material.GOLDEN_APPLE, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName(t(RPTexts.MUTED + "Character Traits"));
		List<String> lore = new ArrayList<String>();
		lore.add(t(RPTexts.MUTED + "------------------------"));
		lore.add(t(RPTexts.WARN + "Traits:"));
		lore.add(RPTexts.spacer());
		for(Trait trait : c.getTraits()) {
			if(Cache.backgroundTraitTypes.contains(trait.getTraitData().getKey())) continue;
			lore.add(trait.getName() + RPTexts.mutedParenthetical(WordUtils.capitalize(trait.getTraitData().getKey())));
		}
		if (c.getStatus().equals(Status.ALIVE)) {
			lore.addAll(PermadeathService.computeRisk(c).toLoreLines());
		}
		lore.add(t(RPTexts.MUTED + "------------------------"));
		lore.add(t(RPTexts.WARN + "Profession EXP:"));
		for(ExperienceModifier m : c.getAttributeData().getExperienceModifiers()) {
			int amount = m.getModifier();
			if(amount > 0) {
				lore.add(t(RPTexts.MUTED + WordUtils.capitalize(m.getAlias()) + ": " + RPTexts.SUCCESS + amount + "%"));
			} else if(amount == 0) {
				lore.add(t(RPTexts.MUTED + WordUtils.capitalize(m.getAlias()) + ": " + RPTexts.WARN + amount + "%"));
			} else if(amount < 0){
				lore.add(t(RPTexts.MUTED + WordUtils.capitalize(m.getAlias()) + ": " + RPTexts.ERROR + amount + "%"));
			}
		}
		lore.add(t(RPTexts.MUTED + "------------------------"));
		lore.add(t(RPTexts.MUTED + "Click for trait details"));
		meta.setLore(lore);
		NamespacedKey key = new NamespacedKey(RPCharacters.plugin, CHARACTER_ID_KEY);
		meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, c.getId());
		i.setItemMeta(meta);
		return i;
	}
	@SuppressWarnings("deprecation")
	public ItemStack getTraitInfoItem(Trait t, String characterId) {
		ItemStack i = new ItemStack(Material.GREEN_CONCRETE, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName(t.getName());
		List<String> lore = new ArrayList<String>();
		for(String d : t.getDesc()) {
			lore.add(d);
		}
		lore.add(RPTexts.spacer());
		lore.add(t(RPTexts.WARN + "Effects:"));

		boolean hasEffects = false;
		AttributeData data = t.getTraitData().getAttributeData();
		for(AttributeModifier modifier : data.getModifiers()) {
			hasEffects = true;
			int amount = modifier.getAmount();
			if(amount > 0) {
				lore.add(t(RPTexts.MUTED + WordUtils.capitalize(modifier.getType()) + ": " + RPTexts.SUCCESS + "+" + amount));
			} else if(amount < 0) {
				lore.add(t(RPTexts.MUTED + WordUtils.capitalize(modifier.getType()) + ": " + RPTexts.ERROR + amount));
			} else {
				lore.add(t(RPTexts.MUTED + WordUtils.capitalize(modifier.getType()) + ": " + RPTexts.WARN + "0"));
			}
		}
		for(ExperienceModifier modifier : data.getExperienceModifiers()) {
			hasEffects = true;
			int amount = modifier.getModifier();
			if(amount > 0) {
				lore.add(t(RPTexts.MUTED + WordUtils.capitalize(modifier.getAlias()) + ": " + RPTexts.SUCCESS + "+" + amount + "%"));
			} else if(amount < 0) {
				lore.add(t(RPTexts.MUTED + WordUtils.capitalize(modifier.getAlias()) + ": " + RPTexts.ERROR + amount + "%"));
			} else {
				lore.add(t(RPTexts.MUTED + WordUtils.capitalize(modifier.getAlias()) + ": " + RPTexts.WARN + "0%"));
			}
		}
		for(PotionData potion : t.getTraitData().getPotionEffects()) {
			hasEffects = true;
			lore.add(t(RPTexts.MUTED + WordUtils.capitalize(potion.getId().replace("_", " ")) + ": "
					+ (potion.getAmplifier() + 1) + " " + RPTexts.MUTED + "(3s)"));
		}
		if(!hasEffects) {
			lore.add(t(RPTexts.MUTED + "No direct effects"));
		}

		NamespacedKey key = new NamespacedKey(RPCharacters.plugin, CHARACTER_ID_KEY);
		meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, characterId);
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack getCharacterItem(RPCharacter c, boolean click) {
		return getCharacterItem(c, click, false);
	}

	public ItemStack getCharacterItem(RPCharacter c, boolean click, boolean overLimit) {
		PlayerData pd = PlayerManager.get(c.getOwner());
		ItemStack i = new ItemStack(Material.ENDER_PEARL, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName(t(RPTexts.WARN + "Character: " + RPTexts.MUTED + c.getName()));
		List<String> lore = new ArrayList<String>();
		lore.add(RPTexts.spacer());
		if (!click) {
			String display = DisplayIdentityService.resolveDisplayTab(c);
			if (display != null && !display.isBlank()) {
				lore.add(t(RPTexts.MUTED + "Display: " + display));
			}
			if (c.getAlias() != null && !c.getAlias().isBlank()) {
				lore.add(t(RPTexts.MUTED + "Name: " + RPTexts.WARN + c.getName()));
			}
		}
		lore.add(RPTexts.labeled("Race: ", c.getRace().getName()));
		if (!click) {
			lore.add(t(RPTexts.MUTED + "Gender: " + PersonaService.resolveGender(c)));
			lore.add(t(RPTexts.MUTED + "Age: " + PersonaService.resolveAge(c)));
			lore.add(t(RPTexts.MUTED + "Birthday: " + PersonaService.resolveBirthday(c)));
		}
		lore.add(t(RPTexts.MUTED + "Status: " + c.getStatus().toString()));
		if (!click) {
			lore.add(t(RPTexts.MUTED + "Created: " + AgeFormatter.formatAge(c.getAgeSeconds())));
		}
		if (!click && c.getOwner() != null && c.getOwner().hasPermission(Cache.personaCharacterHiddenPermission)) {
			if (c.getSlug() != null && !c.getSlug().isBlank()) {
				lore.add(t(RPTexts.MUTED + "Id: " + RPTexts.MUTED + c.getSlug()));
			}
			if (c.isHidden()) {
				lore.add(t(RPTexts.MUTED + "Hidden from TAB/list"));
			}
		}
		if (!click) {
			String description = PersonaService.resolveDescription(c);
			if (description != null && !description.isBlank()) {
				lore.add(RPTexts.spacer());
				lore.add(t(RPTexts.WARN + "Description:"));
				for (String line : ClueFormatter.wrapLore(RPTexts.MUTED + description)) {
					lore.add(t(line));
				}
			}
		}
		lore.add(RPTexts.spacer());
		if(click) {
			lore.add(t(RPTexts.MUTED + "Click for details"));
			if (overLimit) {
				lore.add(t(RPTexts.ERROR + "Over slot limit."));
			}
			if(c.isActive()) {
				meta.addEnchant(Enchantment.UNBREAKING, 1, true);
				meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
			}
		}
		lore.add(RPTexts.spacer());
		if(PermissionGroupService.hasCharacterSwitchCooldown(c.getOwner(), pd)) {
			lore.add(t(RPTexts.WARN + "You are on Cooldown: " + formatTime(PermissionGroupService.getRemainingCooldownMinutes(c.getOwner(), pd))));
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
		meta.setDisplayName(t(RPTexts.MUTED + "Dead Characters: " + count));
		List<String> lore = new ArrayList<String>();
		lore.add(t(RPTexts.MUTED + "Click to view dead characters"));
		meta.setLore(lore);
		NamespacedKey oKey = new NamespacedKey(RPCharacters.plugin, "owner");
		meta.getPersistentDataContainer().set(oKey, PersistentDataType.STRING, pd.getPlayer().getName());
		i.setItemMeta(meta);
		return i;
	}
	public ItemStack getEmptyCharacterItem(PlayerData pd) {
		ItemStack i = new ItemStack(Material.YELLOW_CONCRETE, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName(t(RPTexts.WARN + "Empty Slot"));
		List<String> lore = new ArrayList<String>();
		lore.add(t(RPTexts.MUTED + "Click to create a new character"));
		if(PermissionGroupService.hasCharacterSwitchCooldown(pd.getPlayer(), pd)) {
			lore.add(RPTexts.spacer());
			lore.add(t(RPTexts.WARN + "You are on Cooldown: " + formatTime(PermissionGroupService.getRemainingCooldownMinutes(pd.getPlayer(), pd))));
		}
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}

	public ItemStack getLockedCharacterItem(Player player, int slotIndex) {
		ItemStack i = new ItemStack(Material.BARRIER, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName(t(RPTexts.ERROR + "LOCKED"));
		List<String> lore = new ArrayList<>();
		lore.add(CharacterSlotService.getUnlockRequirementLore(slotIndex));
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}

	public ItemStack getReviveItem(boolean canRevive, int aliveCount, int maxSlots) {
		ItemStack i = new ItemStack(canRevive ? Material.TOTEM_OF_UNDYING : Material.GRAY_DYE, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName(t(canRevive ? RPTexts.SUCCESS + "Revive Character" : RPTexts.MUTED + "Cannot Revive"));
		List<String> lore = new ArrayList<>();
		if (canRevive) {
			lore.add(t(RPTexts.MUTED + "Restore this character to alive status"));
			lore.add(t(RPTexts.MUTED + "Owner slots: " + RPTexts.WARN + aliveCount + RPTexts.MUTED + "/" + RPTexts.WARN + maxSlots));
		} else {
			lore.add(t(RPTexts.MUTED + "Owner has no free character slots"));
			lore.add(t(RPTexts.MUTED + "Slots: " + RPTexts.WARN + aliveCount + RPTexts.MUTED + "/" + RPTexts.WARN + maxSlots));
		}
		meta.setLore(lore);
		i.setItemMeta(meta);
		return i;
	}
	@SuppressWarnings("deprecation")
	public ItemStack getPlayerHead(Player p) {
		ItemStack i = new ItemStack(Material.PLAYER_HEAD, 1);
		SkullMeta m = (SkullMeta) i.getItemMeta();
		m.setDisplayName(t(RPTexts.MUTED + p.getName()));
		m.setOwningPlayer(Bukkit.getOfflinePlayer(p.getName()));
		List<String> lore = new ArrayList<String>();
		lore.add(t(RPTexts.WARN + "Character profile of " + p.getName()));
		PlayerData pd = PlayerManager.get(p);
		if (pd != null) {
			lore.add(t(RPTexts.MUTED + "Member for: " + AgeFormatter.formatAge(pd.getAgeSeconds())));
		}
		if (pd != null && pd.isEighteen()) {
			lore.add(t(RPTexts.MUTED + "Real Age: " + RPTexts.WARN + "18+"));
		} else {
			lore.add(t(RPTexts.MUTED + "Real Age: " + RPTexts.ERROR + "below 18"));
		}
		m.setLore(lore);
		i.setItemMeta(m);
		return i;
	}
	
	public ItemStack getConfirmItem() {
		ItemStack i = new ItemStack(Material.LIME_DYE, 1);
		ItemMeta meta = i.getItemMeta();
		meta.setDisplayName(t(RPTexts.SUCCESS + "CONFIRM"));
		i.setItemMeta(meta);
		return i;
	}
	
	@SuppressWarnings("deprecation")
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
				lore.add(RPTexts.spacer());
				addModifiers(p, lore, r.getRaceData().getAttributeData(), cc);
				lore.add(RPTexts.spacer());
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
				lore.add(t(RPTexts.MUTED + "------------------------"));
				if(d.getMode().equalsIgnoreCase("all")) {
					lore.add(t(RPTexts.WARN + "Requires all of these:"));
				} else if(d.getMode().equalsIgnoreCase("one-or-more")) {
					lore.add(t(RPTexts.WARN + "Requires at least one of these:"));
				}
				for(String dep : d.getDependencies()) {
					lore.add(RPTexts.lore(WordUtils.capitalize(dep)));
				}
				lore.add(t(RPTexts.MUTED + "------------------------"));
			}
			if(t.getTraitData().hasExclusives()) {
				lore.add(t(RPTexts.MUTED + "------------------------"));
				lore.add(t(RPTexts.WARN + "Mutually Exclusive with:"));
				for(String e : t.getTraitData().getExclusive()) {
					lore.add(RPTexts.lore(WordUtils.capitalize(e)));
				}
				lore.add(t(RPTexts.MUTED + "------------------------"));
			}
			if(t.getTraitData().getAttributeData().hasModifiers()) {
				lore.add(RPTexts.spacer());
				addModifiers(p, lore, t.getTraitData().getAttributeData(), cc);
				lore.add(RPTexts.spacer());
			}
			if(t.getTraitData().hasCost()) {
				lore.add(t(RPTexts.WARN + "Cost: " + RPTexts.MUTED + t.getTraitData().getCost()));
				lore.add(RPTexts.spacer());
			}
			if(stage != null && stage.hasPoints()) {
				lore.add(t(RPTexts.WARN + "Unspent Points: " + RPTexts.MUTED + stage.getPoints()));
			}
			meta.setLore(lore);
		} else if(s.getType().equalsIgnoreCase("class")) {
			PlayerClass playerClass = MMOCore.plugin.classManager.get(s.getId());
			if (playerClass != null && playerClass.getIcon() != null && playerClass.getIcon().getType() != Material.AIR) {
				i = playerClass.getIcon().clone();
				if (s.isSelected()) {
					ItemMeta iconMeta = i.getItemMeta();
					if (iconMeta != null) {
						iconMeta.setDisplayName(t(RPTexts.SUCCESS + s.getName()));
						i.setItemMeta(iconMeta);
					}
				}
			}
			ItemMeta classMeta = i.getItemMeta();
			if (classMeta != null) {
				classMeta.setDisplayName(t((s.isSelected() ? RPTexts.SUCCESS : "") + s.getName()));
				List<String> lore = new ArrayList<>();
				if (playerClass != null) {
					lore.addAll(MmoCoreClassGuiHelper.buildClassLore(playerClass));
				}
				if (s.isSelected()) {
					lore.add(RPTexts.spacer());
					lore.add(t(RPTexts.SUCCESS + "Selected"));
				}
				classMeta.setLore(lore);
				i.setItemMeta(classMeta);
			}
			return i;
		}
		i.setItemMeta(meta);
		return i;
	}
	
	@SuppressWarnings("deprecation")
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
				lore.add(t(RPTexts.MUTED + WordUtils.capitalize(m.getType()) + ": " + amount + " " + RPTexts.SUCCESS + "(+" + added + ")"));
			} else if(added == 0) {
				lore.add(t(RPTexts.MUTED + WordUtils.capitalize(m.getType()) + ": " + amount));
			} else {
				lore.add(t(RPTexts.MUTED + WordUtils.capitalize(m.getType()) + ": " + amount + " " + RPTexts.ERROR + "(" + added + ")"));
			}
		}
		lore.add(RPTexts.spacer());
		for(ExperienceModifier m : current.getExperienceModifiers()) {
			int amount = m.getModifier();
			int added = 0;
			if(data.hasXPModifier(m)) {
				added = data.getAmount(m);
			}
			if(added > 0) {
				lore.add(t(RPTexts.MUTED + WordUtils.capitalize(m.getAlias()) + ": " + amount + RPTexts.WARN + "% "
						+ RPTexts.SUCCESS + "(+" + added + RPTexts.WARN + "%" + RPTexts.SUCCESS + ")"));
			} else if(added == 0) {
				lore.add(t(RPTexts.MUTED + WordUtils.capitalize(m.getAlias()) + ": " + amount + RPTexts.WARN + "%"));
			} else if(added < 0){
				lore.add(t(RPTexts.MUTED + WordUtils.capitalize(m.getAlias()) + ": " + amount + RPTexts.WARN + "% "
						+ RPTexts.ERROR + "(" + added + RPTexts.WARN + "%" + RPTexts.ERROR + ")"));
			}
		}
	}
}
