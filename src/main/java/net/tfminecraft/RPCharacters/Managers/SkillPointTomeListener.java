package net.tfminecraft.RPCharacters.Managers;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import net.tfminecraft.RPCharacters.Loaders.SkillPointTomeLoader;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.SkillPointTomeDefinition;
import net.tfminecraft.RPCharacters.Utils.RPTexts;
import net.tfminecraft.RPCharacters.mmocore.ClassService;

public class SkillPointTomeListener implements Listener {

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onPlayerInteract(PlayerInteractEvent event) {
		Action action = event.getAction();
		if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
			return;
		}
		if (event.getHand() != EquipmentSlot.HAND) {
			return;
		}

		Player player = event.getPlayer();
		ItemStack item = player.getInventory().getItemInMainHand();
		SkillPointTomeDefinition tome = SkillPointTomeLoader.resolve(item);
		if (tome == null) {
			return;
		}

		PlayerData pd = PlayerManager.get(player);
		if (pd == null || !pd.hasActiveCharacter()) {
			RPTexts.send(player, RPTexts.ERROR + "You need an active character to use skill point tomes.");
			return;
		}

		event.setCancelled(true);
		ClassService.grantSkillPoints(player, tome.getSkillPoints());

		int points = tome.getSkillPoints();
		String label = points == 1 ? "skill point" : "skill points";
		RPTexts.send(player, RPTexts.SUCCESS + "+" + points + " " + label);
		player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.8f, 1.4f);

		if (item.getAmount() <= 1) {
			player.getInventory().setItemInMainHand(null);
		} else {
			item.setAmount(item.getAmount() - 1);
		}
	}
}
