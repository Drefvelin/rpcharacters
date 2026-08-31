package net.tfminecraft.RPCharacters.grave;

import java.util.Iterator;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

public final class GraveInteractListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent event) {
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		Block block = event.getClickedBlock();
		Grave grave = GraveManager.get().getAt(block);
		if (grave == null) {
			return;
		}
		event.setCancelled(true);
		event.setUseInteractedBlock(Event.Result.DENY);
		if (event.getHand() != EquipmentSlot.HAND) {
			return;
		}
		Player player = event.getPlayer();
		if (GraveLootRules.canRecover(player, grave)) {
			recover(player, grave);
			return;
		}
		if (!GraveLootRules.canSteal(player, grave)) {
			send(player, GraveLoader.getMessageLocked());
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onInventoryOpen(InventoryOpenEvent event) {
		if (isGraveInventory(event.getInventory())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBreak(BlockBreakEvent event) {
		if (GraveManager.get().isGrave(event.getBlock())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onEntityExplode(EntityExplodeEvent event) {
		removeGravesFrom(event.blockList());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onBlockExplode(BlockExplodeEvent event) {
		removeGravesFrom(event.blockList());
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPistonExtend(BlockPistonExtendEvent event) {
		if (containsGrave(event.getBlocks()) || GraveManager.get().isGrave(event.getBlock().getRelative(event.getDirection()))) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onPistonRetract(BlockPistonRetractEvent event) {
		if (containsGrave(event.getBlocks())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onHopperMove(InventoryMoveItemEvent event) {
		if (isGraveInventory(event.getSource()) || isGraveInventory(event.getDestination())) {
			event.setCancelled(true);
		}
	}

	private static void recover(Player player, Grave grave) {
		if (grave.isEmpty()) {
			send(player, GraveLoader.getMessageEmpty());
			GraveManager.get().despawn(grave);
			return;
		}
		Location dropAt = grave.getBlockLocation();
		World world = dropAt != null ? dropAt.getWorld() : player.getWorld();
		if (dropAt == null) {
			dropAt = player.getLocation();
		} else {
			dropAt = dropAt.clone().add(0.5, 0.5, 0.5);
		}
		boolean dropped = false;
		for (int slot = 0; slot < Grave.TOTAL_LOGICAL_SLOTS; slot++) {
			ItemStack item = grave.getItem(slot);
			if (Grave.isBlank(item)) {
				continue;
			}
			Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.clone());
			for (ItemStack left : leftover.values()) {
				if (!Grave.isBlank(left) && world != null) {
					world.dropItemNaturally(dropAt, left);
					dropped = true;
				}
			}
			grave.setItem(slot, null);
		}
		for (ItemStack extra : grave.getExtras()) {
			if (Grave.isBlank(extra)) {
				continue;
			}
			Map<Integer, ItemStack> leftover = player.getInventory().addItem(extra.clone());
			for (ItemStack left : leftover.values()) {
				if (!Grave.isBlank(left) && world != null) {
					world.dropItemNaturally(dropAt, left);
					dropped = true;
				}
			}
		}
		grave.clearExtras();
		int experience = grave.getExperience();
		if (experience > 0) {
			player.giveExp(experience);
			grave.setExperience(0);
		}
		send(player, GraveLoader.getMessageRecovered());
		if (dropped) {
			send(player, GraveLoader.getMessageInventoryFull());
		}
		GraveManager.get().despawn(grave);
	}

	private static void send(Player player, String message) {
		if (player == null || message == null || message.isBlank()) {
			return;
		}
		player.sendMessage(StringFormatter.formatHex(message.replace('&', '\u00A7')));
	}

	private static void removeGravesFrom(java.util.List<Block> blocks) {
		Iterator<Block> iterator = blocks.iterator();
		while (iterator.hasNext()) {
			if (GraveManager.get().isGrave(iterator.next())) {
				iterator.remove();
			}
		}
	}

	private static boolean containsGrave(Iterable<Block> blocks) {
		for (Block block : blocks) {
			if (GraveManager.get().isGrave(block)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isGraveInventory(Inventory inventory) {
		Block block = inventoryBlock(inventory);
		return block != null && GraveManager.get().isGrave(block);
	}

	private static Block inventoryBlock(Inventory inventory) {
		if (inventory == null) {
			return null;
		}
		InventoryHolder holder = inventory.getHolder();
		if (holder instanceof BlockState state) {
			return state.getBlock();
		}
		Location location = inventory.getLocation();
		return location != null ? location.getBlock() : null;
	}
}
