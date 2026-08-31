package net.tfminecraft.RPCharacters.injuries;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import io.lumine.mythic.lib.api.item.NBTItem;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem;
import net.Indyuce.mmoitems.stat.data.BooleanData;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Managers.CreationManager;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.Utils.RPTexts;
import net.tfminecraft.RPCharacters.permadeath.PermadeathService;

public final class OffhandBlockService {

	private static final long TICK_INTERVAL = 20L;
	private static final int PICKUP_DELAY_TICKS = 40;

	private OffhandBlockService() {
	}

	public static void start() {
		Bukkit.getLogger().info("[RPCharacters] Starting Offhand Block Service");
		new BukkitRunnable() {
			@Override
			public void run() {
				tick();
			}
		}.runTaskTimer(RPCharacters.plugin, 0L, TICK_INTERVAL);
	}

	static void tick() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player.isDead() || PermadeathService.isAwaitingPermakillRespawn(player)) {
				continue;
			}
			if (CreationManager.activeCreators.containsKey(player)
					|| !player.getGameMode().equals(GameMode.SURVIVAL)) {
				continue;
			}

			PlayerData pd = PlayerManager.get(player);
			if (pd == null || !pd.hasActiveCharacter()) {
				continue;
			}

			RPCharacter character = pd.getActiveCharacter();
			if (character == null || !character.isActive()) {
				continue;
			}
			if (!blocksOffhand(character)) {
				continue;
			}

			boolean dropped = false;
			if (dropHand(player, player.getInventory().getItemInOffHand(), true)) {
				dropped = true;
			}
			ItemStack main = player.getInventory().getItemInMainHand();
			if (isMmoTwoHanded(main) && dropHand(player, main, false)) {
				dropped = true;
			}
			if (dropped) {
				RPTexts.send(player, RPTexts.ERROR + "You drop your item.");
			}
		}
	}

	private static boolean blocksOffhand(RPCharacter character) {
		for (Trait trait : character.getTraits()) {
			if (trait != null && trait.getTraitData() != null && trait.getTraitData().blocksOffhand()) {
				return true;
			}
		}
		return false;
	}

	private static boolean dropHand(Player player, ItemStack stack, boolean offhand) {
		if (stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) {
			return false;
		}
		Item dropped = player.getWorld().dropItemNaturally(player.getLocation(), stack.clone());
		dropped.setPickupDelay(PICKUP_DELAY_TICKS);
		ItemStack empty = new ItemStack(Material.AIR);
		if (offhand) {
			player.getInventory().setItemInOffHand(empty);
		} else {
			player.getInventory().setItemInMainHand(empty);
		}
		return true;
	}

	private static boolean isMmoTwoHanded(ItemStack stack) {
		if (stack == null || stack.getType() == Material.AIR) {
			return false;
		}
		try {
			NBTItem nbt = NBTItem.get(stack);
			if (!nbt.hasType()) {
				return false;
			}
			LiveMMOItem mmo = new LiveMMOItem(nbt);
			BooleanData data = (BooleanData) mmo.getData(ItemStats.TWO_HANDED);
			return data != null && data.isEnabled();
		} catch (Exception ignored) {
			return false;
		}
	}
}
