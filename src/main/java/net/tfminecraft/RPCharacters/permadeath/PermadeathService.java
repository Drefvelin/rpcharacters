package net.tfminecraft.RPCharacters.permadeath;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerRespawnEvent;

import net.tfminecraft.RPCharacters.Loaders.InjuryPoolLoader;
import net.tfminecraft.RPCharacters.Loaders.PermadeathZoneLoader;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Utils.TraitChangeService;
import net.tfminecraft.RPCharacters.clues.discovery.ClueAdminModeService;
import net.tfminecraft.RPCharacters.enums.Status;

public final class PermadeathService {

	private static final String INJURY_KEY = "injury";
	private static final ConcurrentHashMap<UUID, Boolean> pendingPermadeathRespawn = new ConcurrentHashMap<>();
	private static final Set<UUID> pendingPermakillSounds = ConcurrentHashMap.newKeySet();
	private static final Set<UUID> ignoreNextZoneDeath = ConcurrentHashMap.newKeySet();

	private PermadeathService() {
	}

	public static void handleDeath(Player player, Location deathLocation) {
		if (ignoreNextZoneDeath.remove(player.getUniqueId())) {
			return;
		}
		if (ClueAdminModeService.isEnabled(player)) {
			return;
		}
		if (!WorldGuardBridge.isAvailable()) {
			return;
		}
		if (WorldGuardBridge.getPermadeathZoneAt(deathLocation) == null) {
			return;
		}

		PlayerData pd = PlayerManager.get(player);
		if (pd == null || !pd.hasActiveCharacter()) {
			return;
		}

		RPCharacter character = pd.getActiveCharacter();
		PermadeathRisk risk = computeRisk(character);

		if (rollPermadeath(risk.getChancePercent())) {
			killCharacter(player, character, PermakillCause.PERMADEATH_ZONE);
			return;
		}

		Trait picked = InjuryPoolLoader.pickRandom(collectOwnedTraitIds(character));
		if (picked == null) {
			if (risk.getChancePercent() >= 100) {
				killCharacter(player, character, PermakillCause.PERMADEATH_ZONE);
			} else {
				RPCharacters.plugin.getLogger().warning(
						"Injury pool empty or misconfigured for " + player.getName()
								+ " — skipping permadeath consequence.");
			}
			return;
		}

		TraitChangeService.addTrait(player, character, picked);
		TraitChangeService.sendGainedMessage(player, picked);
		PermadeathTitles.showInjury(player, picked);
	}

	public static void handlePlayerRespawn(Player player, PlayerRespawnEvent event) {
		boolean permadeathRespawn = consumePendingPermadeathRespawn(player);
		if (permadeathRespawn) {
			Location spawn = WorldSpawnService.getSpawn();
			if (spawn != null) {
				event.setRespawnLocation(spawn);
			}
			PermadeathZoneListener.silentZoneSync(player, event.getRespawnLocation());
		}

		if (consumePendingPermakillSounds(player)) {
			PermadeathSounds.playPermakill(player);
		}

		if (permadeathRespawn) {
			RPCharacters.getPlayerManager().releaseFreeze(player);
			RPCharacters.getPlayerManager().reevaluateFreeze(player);
		}
	}

	public static boolean isAwaitingPermakillRespawn(Player player) {
		UUID id = player.getUniqueId();
		return pendingPermadeathRespawn.containsKey(id) || pendingPermakillSounds.contains(id);
	}

	public static PermadeathRisk computeRisk(RPCharacter character) {
		int injuryCount = countInjuryTraits(character);
		int chancePerInjury = PermadeathZoneLoader.getChancePerInjury();
		int chancePercent = injuryCount * chancePerInjury;
		int poolSize = InjuryPoolLoader.getPoolTraitIds().size();
		int remaining = InjuryPoolLoader.countRemainingInjuries(collectOwnedTraitIds(character));
		if (poolSize > 0 && remaining == 0 && injuryCount > 0) {
			chancePercent = 100;
		}
		return new PermadeathRisk(injuryCount, chancePercent, chancePerInjury);
	}

	public static boolean applyRandomInjury(Player player, RPCharacter character) {
		Trait picked = InjuryPoolLoader.pickRandom(collectOwnedTraitIds(character));
		if (picked == null) {
			return false;
		}
		TraitChangeService.addTrait(player, character, picked);
		TraitChangeService.sendGainedMessage(player, picked);
		return true;
	}

	public static boolean killCharacter(Player player, RPCharacter character) {
		return killCharacter(player, character, PermakillCause.OTHER);
	}

	public static boolean killCharacter(Player player, RPCharacter character, PermakillCause cause) {
		if (!character.getStatus().equals(Status.ALIVE)) {
			return false;
		}

		CharacterPermakillEvent event = new CharacterPermakillEvent(player, character, cause);
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			return false;
		}

		boolean fromPermadeathZone = cause == PermakillCause.PERMADEATH_ZONE;
		boolean wasActive = character.isActive();
		String killedName = character.getName();
		character.setStatus(Status.DEAD);
		String replacementName = null;
		if (wasActive) {
			character.deactivate();
			PlayerData pd = PlayerManager.get(player);
			if (pd != null && pd.getCharacters(Status.ALIVE).size() > 0) {
				RPCharacter replacement = pd.getCharacters(Status.ALIVE).get(0);
				pd.setActiveCharacter(replacement);
				replacementName = replacement.getName();
			}
		}

		RPCharacters.getPlayerManager().savePlayer(player);

		net.tfminecraft.RPCharacters.ingest.RosterSyncService.pushRosterForPlayer(player);

		if (!player.isDead() && cause != PermakillCause.PERMADEATH_ZONE) {
			RPCharacters.getPlayerManager().reevaluateFreeze(player);
		}

		if (fromPermadeathZone) {
			markPendingPermadeathRespawn(player);
			PermadeathZoneListener.clearZoneTracking(player);
			RPCharacters.getPlayerManager().releaseFreeze(player);
		}
		if (wasActive) {
			markPendingPermakillSounds(player);
			PermadeathTitles.showPermakill(player, killedName, replacementName, fromPermadeathZone);
			if (!player.isDead()) {
				scheduleEntityDeath(player);
			}
		}
		return true;
	}

	public static void clearPendingPermadeathRespawn(Player player) {
		UUID uuid = player.getUniqueId();
		pendingPermadeathRespawn.remove(uuid);
		pendingPermakillSounds.remove(uuid);
		ignoreNextZoneDeath.remove(uuid);
	}

	public static void markPendingPermakillSounds(Player player) {
		pendingPermakillSounds.add(player.getUniqueId());
	}

	public static boolean consumePendingPermakillSounds(Player player) {
		return pendingPermakillSounds.remove(player.getUniqueId());
	}

	private static void scheduleEntityDeath(Player player) {
		ignoreNextZoneDeath.add(player.getUniqueId());
		Bukkit.getScheduler().runTask(RPCharacters.plugin, () -> {
			if (!player.isOnline() || player.isDead()) {
				ignoreNextZoneDeath.remove(player.getUniqueId());
				return;
			}
			player.setHealth(0);
		});
	}

	public static void markPendingPermadeathRespawn(Player player) {
		pendingPermadeathRespawn.put(player.getUniqueId(), Boolean.TRUE);
	}

	public static boolean consumePendingPermadeathRespawn(Player player) {
		return pendingPermadeathRespawn.remove(player.getUniqueId()) != null;
	}

	private static boolean rollPermadeath(int chancePercent) {
		if (chancePercent <= 0) {
			return false;
		}
		if (chancePercent >= 100) {
			return true;
		}
		return ThreadLocalRandom.current().nextInt(100) < chancePercent;
	}

	private static int countInjuryTraits(RPCharacter character) {
		int count = 0;
		for (Trait trait : character.getTraits()) {
			if (trait.getTraitData().getKey() != null
					&& trait.getTraitData().getKey().equalsIgnoreCase(INJURY_KEY)) {
				count++;
			}
		}
		return count;
	}

	private static Set<String> collectOwnedTraitIds(RPCharacter character) {
		Set<String> ids = new HashSet<>();
		for (Trait trait : character.getTraits()) {
			ids.add(trait.getId().toLowerCase());
		}
		return ids;
	}
}
