package net.tfminecraft.RPCharacters.profile;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.identity.MaskService;

public final class ProfileManager implements Listener {

	public static void showProfile(Player viewer, Player target, boolean fromCommand) {
		if (viewer == null || target == null) {
			return;
		}

		PlayerData targetData = PlayerManager.get(target);
		RPCharacter targetCharacter = targetData != null ? targetData.getActiveCharacter() : null;
		boolean masked = MaskService.isMasked(target);

		CharacterProfileViewEvent event = new CharacterProfileViewEvent(
				viewer, target, targetCharacter, masked, fromCommand);
		Bukkit.getPluginManager().callEvent(event);
	}

	@EventHandler
	public void onPlayerInteract(PlayerInteractEntityEvent event) {
		if (event.getHand() == EquipmentSlot.OFF_HAND) {
			return;
		}
		if (!(event.getRightClicked() instanceof Player target)) {
			return;
		}

		Player viewer = event.getPlayer();
		if (Cache.profileRequireSneak && !viewer.isSneaking()) {
			return;
		}
		if (Cache.profileRequireEmptyHand && !isHandEmpty(viewer.getInventory().getItem(event.getHand()))) {
			return;
		}

		event.setCancelled(true);
		showProfile(viewer, target, false);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onProfileViewDeny(CharacterProfileViewEvent event) {
		Player viewer = event.getViewer();
		if (viewer == null) {
			event.setCancelled(true);
			return;
		}

		if (!viewer.hasPermission(Cache.profilePermission)) {
			viewer.sendMessage("§cYou do not have permission to view character profiles.");
			event.setCancelled(true);
			return;
		}

		if (event.getTargetCharacter() == null) {
			viewer.sendMessage("§cThat player has no active character.");
			event.setCancelled(true);
			return;
		}

		if (event.isMasked()) {
			viewer.sendMessage("§cThat player's identity is concealed.");
			event.setCancelled(true);
			return;
		}

		if (!event.isFromCommand()) {
			if (Cache.profileRequireSneak && !viewer.isSneaking()) {
				event.setCancelled(true);
				return;
			}
			if (Cache.profileRequireEmptyHand) {
				ItemStack mainHand = viewer.getInventory().getItemInMainHand();
				if (!isHandEmpty(mainHand)) {
					event.setCancelled(true);
					return;
				}
			}
		}

		if (ProfileViewCooldownManager.get().isOnCooldown(viewer, Cache.profileViewCooldownSeconds)) {
			int remaining = ProfileViewCooldownManager.get().getRemainingSeconds(viewer);
			viewer.sendMessage("§cPlease wait §e" + remaining + "§c more second(s) before viewing another profile.");
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onProfileViewDisplay(CharacterProfileViewEvent event) {
		Player viewer = event.getViewer();
		Player target = event.getTarget();
		if (viewer == null || target == null) {
			return;
		}

		List<String> lines = ProfileFormatter.format(target, event.getTargetCharacter());
		for (String line : lines) {
			viewer.sendMessage(line);
		}

		ProfileViewCooldownManager.get().applyCooldown(viewer, Cache.profileViewCooldownSeconds);
	}

	private static boolean isHandEmpty(ItemStack item) {
		return item == null || item.getType().isAir();
	}
}
