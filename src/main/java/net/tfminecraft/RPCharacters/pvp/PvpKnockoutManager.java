package net.tfminecraft.RPCharacters.pvp;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import net.tfminecraft.RPCharacters.Loaders.PvpLoader;
import net.tfminecraft.RPCharacters.Managers.CreationManager;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.permadeath.PermadeathService;

public final class PvpKnockoutManager implements Listener {

	private final Map<UUID, Knockout> knockouts = new ConcurrentHashMap<>();
	private BukkitTask tickTask;

	public void start() {
		if (tickTask != null) {
			tickTask.cancel();
		}
		tickTask = new BukkitRunnable() {
			@Override
			public void run() {
				tick();
			}
		}.runTaskTimer(RPCharacters.plugin, 0L, PvpLoader.getFreezePeriodTicks());
	}

	public void shutdown() {
		if (tickTask != null) {
			tickTask.cancel();
			tickTask = null;
		}
		knockouts.clear();
	}

	/**
	 * Spigot {@code PlayerDeathEvent} is not cancellable on this API. Cancel the
	 * killing blow at HIGHEST instead so death (and permadeath MONITOR) never runs.
	 */
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onLethalDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player player)) {
			return;
		}
		if (shouldSkipKnockout(player)) {
			return;
		}
		if (player.getHealth() - event.getFinalDamage() > 0) {
			return;
		}
		PlayerData pd = PlayerManager.get(player);
		if (pd == null || !pd.hasActiveCharacter()) {
			return;
		}
		RPCharacter character = pd.getActiveCharacter();
		if (character.isPvpLethal()) {
			return;
		}

		event.setCancelled(true);
		applyKnockout(player);
	}

	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		knockouts.remove(event.getPlayer().getUniqueId());
	}

	private void applyKnockout(Player player) {
		double maxHealth = 20.0;
		var maxAttr = player.getAttribute(Attribute.MAX_HEALTH);
		if (maxAttr != null) {
			maxHealth = maxAttr.getValue();
		}
		player.setHealth(Math.max(0.1, Math.min(1.0, maxHealth)));

		player.performCommand("crawl");
		int durationTicks = PvpLoader.getKnockoutSeconds() * 20;
		player.addPotionEffect(new PotionEffect(
				PotionEffectType.BLINDNESS,
				durationTicks,
				PvpLoader.getBlindnessAmplifier(),
				false,
				false,
				true));

		knockouts.put(player.getUniqueId(), new Knockout(
				player.getLocation().clone(),
				System.currentTimeMillis() + PvpLoader.getKnockoutSeconds() * 1000L));
	}

	private void tick() {
		long now = System.currentTimeMillis();
		Iterator<Map.Entry<UUID, Knockout>> it = knockouts.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Knockout> entry = it.next();
			Player player = Bukkit.getPlayer(entry.getKey());
			if (player == null || !player.isOnline()) {
				it.remove();
				continue;
			}
			if (now >= entry.getValue().untilMs || shouldSkipTick(player)) {
				it.remove();
				continue;
			}
			enforceFreeze(player, entry.getValue().location);
			if (!isCrawling(player)) {
				player.performCommand("crawl");
			}
		}
	}

	private boolean shouldSkipKnockout(Player player) {
		if (PermadeathService.isAwaitingPermakillRespawn(player)) {
			return true;
		}
		if (CreationManager.activeCreators.containsKey(player)) {
			return true;
		}
		return false;
	}

	private boolean shouldSkipTick(Player player) {
		if (player.isDead() || PermadeathService.isAwaitingPermakillRespawn(player)) {
			return true;
		}
		if (CreationManager.activeCreators.containsKey(player)) {
			return true;
		}
		if (!player.getGameMode().equals(GameMode.SURVIVAL)) {
			return true;
		}
		return false;
	}

	private void enforceFreeze(Player player, Location freezeAt) {
		Location here = player.getLocation();
		if (here.getX() == freezeAt.getX()
				&& here.getY() == freezeAt.getY()
				&& here.getZ() == freezeAt.getZ()) {
			return;
		}
		Location dest = freezeAt.clone();
		dest.setYaw(here.getYaw());
		dest.setPitch(here.getPitch());
		player.teleport(dest);
	}

	private boolean isCrawling(Player player) {
		Pose pose = player.getPose();
		if ("CRAWLING".equals(pose.name())) {
			return true;
		}
		return pose == Pose.SWIMMING && !player.isInWater();
	}

	private static final class Knockout {
		private final Location location;
		private final long untilMs;

		private Knockout(Location location, long untilMs) {
			this.location = location;
			this.untilMs = untilMs;
		}
	}
}
