package net.tfminecraft.RPCharacters.injuries;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.tfminecraft.RPCharacters.Loaders.InjuryPoolLoader;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Utils.Integrator;
import net.tfminecraft.RPCharacters.Utils.TraitChangeService;
import net.tfminecraft.RPCharacters.permadeath.PermadeathService;

public final class InjuryHealingService {

	private InjuryHealingService() {
	}

	public static void start() {
		Bukkit.getLogger().info("[RPCharacters] Starting Injury Healing Service");
		long intervalTicks = InjuryPoolLoader.getHealingTickIntervalTicks();
		new BukkitRunnable() {
			@Override
			public void run() {
				tick();
			}
		}.runTaskTimer(RPCharacters.plugin, intervalTicks, intervalTicks);
	}

	static void tick() {
		long intervalMs = InjuryPoolLoader.getHealingTickIntervalMs();
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player.isDead() || PermadeathService.isAwaitingPermakillRespawn(player)) {
				continue;
			}

			PlayerData pd = PlayerManager.get(player);
			if (pd == null || !pd.hasActiveCharacter()) {
				continue;
			}

			RPCharacter character = pd.getActiveCharacter();
			if (!character.isActive()) {
				continue;
			}

			processCharacter(player, character, intervalMs);
		}
	}

	private static void processCharacter(Player player, RPCharacter character, long intervalMs) {
		List<Trait> healingTraits = new ArrayList<>();
		for (Trait trait : character.getTraits()) {
			if (trait.hasDuration()) {
				healingTraits.add(trait);
			}
		}
		if (healingTraits.isEmpty()) {
			return;
		}

		boolean needsRefresh = false;
		List<Trait> toRemove = new ArrayList<>();

		for (Trait trait : healingTraits) {
			String traitId = trait.getId();
			long remaining = character.getDurationRemainingMs(traitId);
			if (remaining < 0L) {
				remaining = trait.getDurationMs();
				character.setDurationRemainingMs(traitId, remaining);
			}

			long newRemaining = remaining - intervalMs;
			if (newRemaining <= 0L) {
				toRemove.add(trait);
			} else {
				character.setDurationRemainingMs(traitId, newRemaining);
				needsRefresh = true;
			}
		}

		if (needsRefresh && toRemove.isEmpty()) {
			refreshCharacter(player, character);
			RPCharacters.getPlayerManager().savePlayer(player);
		}

		for (Trait trait : toRemove) {
			TraitChangeService.removeTrait(player, character, trait);
			TraitChangeService.sendLostMessage(player, trait);
		}
	}

	private static void refreshCharacter(Player player, RPCharacter character) {
		if (character.isActive()) {
			Integrator integrator = new Integrator();
			integrator.remove(player, character, false);
			character.update();
			integrator.integrate(player, character);
		} else {
			character.update();
		}
	}
}
