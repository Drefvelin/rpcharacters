package net.tfminecraft.RPCharacters.Managers;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.RPCharacters.Utils.ClueGiver;

public class ClueItemListener implements Listener {

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onInventoryClick(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player player)) return;

		ItemStack clicked = event.getCurrentItem();
		if (ClueGiver.isClueItem(clicked)) {
			consumeClueClick(player, event.getClickedInventory(), event.getSlot(), clicked);
			event.setCancelled(true);
			return;
		}

		ItemStack cursor = event.getCursor();
		if (ClueGiver.isClueItem(cursor)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onInventoryDrag(InventoryDragEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) return;
		ItemStack oldCursor = event.getOldCursor();
		if (ClueGiver.isClueItem(oldCursor)) {
			event.setCancelled(true);
		}
	}

	private void consumeClueClick(Player player, Inventory inventory, int slot, ItemStack item) {
		if (inventory == null || item == null) return;

		String clueText = ClueGiver.getClueText(item);
		if (clueText == null) return;

		player.sendMessage(clueText);
		player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 1.2f);

		if (item.getAmount() <= 1) {
			inventory.setItem(slot, null);
		} else {
			item.setAmount(item.getAmount() - 1);
			inventory.setItem(slot, item);
		}
	}
}
