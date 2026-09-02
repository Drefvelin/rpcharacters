package net.tfminecraft.RPCharacters.grave;

import java.util.Map;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;

final class GraveRecover {

	private GraveRecover() {}

	/**
	 * Transfers all grave loot and XP to the player. Overflow drops at {@code overflowAt}.
	 *
	 * @return {@code true} when items or XP were transferred (insurance may consume a charge)
	 */
	static boolean recover(Player player, Grave grave, Location overflowAt) {
		if (grave.isEmpty()) {
			send(player, GraveLoader.getMessageEmpty());
			GraveManager.get().despawn(grave);
			return false;
		}
		Location dropAt = overflowAt != null ? overflowAt.clone() : player.getLocation();
		World world = dropAt.getWorld() != null ? dropAt.getWorld() : player.getWorld();
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
		return true;
	}

	static Location graveOverflowLocation(Grave grave, Player player) {
		Location dropAt = grave.getBlockLocation();
		if (dropAt == null) {
			return player.getLocation();
		}
		return dropAt.clone().add(0.5, 0.5, 0.5);
	}

	static void sendMessage(Player player, String message) {
		send(player, message);
	}

	private static void send(Player player, String message) {
		if (player == null || message == null || message.isBlank()) {
			return;
		}
		player.sendMessage(StringFormatter.formatHex(message.replace('&', '\u00A7')));
	}
}
