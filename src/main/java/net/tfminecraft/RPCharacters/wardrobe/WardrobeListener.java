package net.tfminecraft.RPCharacters.wardrobe;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import me.Plugins.TLibs.Armour.ArmorEquipEvent;
import me.Plugins.TLibs.Armour.ArmorType;

import net.tfminecraft.RPCharacters.RPCharacters;

/**
 * Mask helmet on/off → re-apply wardrobe skin; clear cache on quit.
 */
public final class WardrobeListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onArmorEquip(ArmorEquipEvent event) {
		if (event.getType() != ArmorType.HELMET) {
			return;
		}
		Player player = event.getPlayer();
		if (player == null || !player.isOnline()) {
			return;
		}
		// Helmet inventory updates after the event; apply next tick.
		Bukkit.getScheduler().runTask(RPCharacters.plugin, () -> {
			if (player.isOnline()) {
				WardrobeService.applyFor(player);
			}
		});
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		WardrobeCache.clear(event.getPlayer());
	}
}
