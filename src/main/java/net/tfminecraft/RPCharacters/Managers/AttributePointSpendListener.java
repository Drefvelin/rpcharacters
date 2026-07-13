package net.tfminecraft.RPCharacters.Managers;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import net.Indyuce.mmocore.api.event.PlayerAttributeUseEvent;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.mmocore.AttributePointService;

public class AttributePointSpendListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onAttributeUse(PlayerAttributeUseEvent event) {
		Player player = event.getPlayer();
		PlayerData pd = PlayerManager.get(player);
		if (pd == null || !pd.hasActiveCharacter()) {
			return;
		}
		RPCharacter character = pd.getActiveCharacter();
		AttributePointService.captureAllocationFromMmo(player, character);
		AttributePointService.applyFreeAttributePoints(player, character);
	}
}
