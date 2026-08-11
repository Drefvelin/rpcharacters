package net.tfminecraft.RPCharacters.kit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import me.Plugins.TLibs.TLibs;
import net.tfminecraft.RPCharacters.Loaders.KitLoader;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Utils.RPTexts;

public final class KitService {

	private KitService() {
	}

	public static long cooldownRemainingMs(PlayerData pd, String kitId) {
		KitDefinition kit = KitLoader.getKit(kitId);
		if (pd == null || kit == null) {
			return 0L;
		}
		Long last = pd.getLastKitClaimAtMs(kit.getId());
		if (last == null || last <= 0L) {
			return 0L;
		}
		long elapsed = System.currentTimeMillis() - last;
		long remaining = kit.getCooldownMs() - elapsed;
		return Math.max(0L, remaining);
	}

	public static boolean isCooldownActive(PlayerData pd, String kitId) {
		return cooldownRemainingMs(pd, kitId) > 0L;
	}

	public static int cooldownRemainingHoursCeil(PlayerData pd, String kitId) {
		long remaining = cooldownRemainingMs(pd, kitId);
		if (remaining <= 0L) {
			return 0;
		}
		return (int) Math.max(1L, (remaining + 3_599_999L) / 3_600_000L);
	}

	/** Legacy helpers for starter kit. */
	public static long cooldownRemainingMs(PlayerData pd) {
		return cooldownRemainingMs(pd, KitLoader.DEFAULT_KIT_ID);
	}

	public static boolean isCooldownActive(PlayerData pd) {
		return isCooldownActive(pd, KitLoader.DEFAULT_KIT_ID);
	}

	public static int cooldownRemainingHoursCeil(PlayerData pd) {
		return cooldownRemainingHoursCeil(pd, KitLoader.DEFAULT_KIT_ID);
	}

	/**
	 * Stamp eligibility for every configured kit. Claim via
	 * {@code /rpcharacter kit <id>} only (no auto-grant).
	 */
	public static void onCharacterCreated(Player player, PlayerData pd, RPCharacter character) {
		if (pd == null || character == null) {
			return;
		}
		for (String kitId : KitLoader.kitIds()) {
			character.setKitStatus(kitId, KitStatus.ELIGIBLE);
		}
	}

	public static void tryClaim(Player player, String kitIdRaw) {
		if (player == null || !player.isOnline()) {
			return;
		}
		String kitId = kitIdRaw != null ? kitIdRaw.trim().toLowerCase(Locale.ROOT) : "";
		KitDefinition kit = KitLoader.getKit(kitId);
		if (kit == null || kitId.isEmpty()) {
			RPTexts.send(player, RPTexts.ERROR + "Unknown kit. Usage: /rpcharacter kit <id>");
			return;
		}
		if (!PlayerManager.exists(player)) {
			RPTexts.send(player, RPTexts.ERROR + "No character data loaded.");
			return;
		}
		PlayerData pd = PlayerManager.get(player);
		if (pd == null || !pd.hasActiveCharacter()) {
			RPTexts.send(player, RPTexts.ERROR + "You need an active character to claim a kit.");
			return;
		}
		RPCharacter character = pd.getActiveCharacter();
		if (character == null) {
			RPTexts.send(player, RPTexts.ERROR + "You need an active character to claim a kit.");
			return;
		}

		KitStatus status = character.getKitStatus(kitId);
		if (status == null) {
			RPTexts.send(player, RPTexts.ERROR + "This character cannot claim that kit.");
			return;
		}
		if (kit.isOncePerCharacter()) {
			if (status == KitStatus.GRANTED) {
				RPTexts.send(player, RPTexts.ERROR + "This character has already claimed that kit.");
				return;
			}
			if (status != KitStatus.ELIGIBLE && status != KitStatus.INELIGIBLE) {
				RPTexts.send(player, RPTexts.ERROR + "This character cannot claim that kit.");
				return;
			}
		}

		if (isCooldownActive(pd, kitId)) {
			int hours = cooldownRemainingHoursCeil(pd, kitId);
			RPTexts.send(player, RPTexts.WARN
					+ "You must wait " + hours + " hours before claiming that kit again.");
			return;
		}

		String playerUuid = player.getUniqueId().toString();
		String characterId = character.getId();
		net.tfminecraft.RPCharacters.api.ProvinceSystemClient.SimpleResult claimStatus =
				net.tfminecraft.RPCharacters.api.ProvinceSystemClient.fetchLoreItemClaimStatus(
						playerUuid, characterId, kitId
				);
		if (claimStatus.ok
				&& net.tfminecraft.RPCharacters.api.ProvinceSystemClient.claimStatusPendingSkin(
						claimStatus.body
				)) {
			RPTexts.send(player, RPTexts.WARN
					+ "A custom kit item is still waiting for approval.");
			return;
		}
		if (!claimStatus.ok) {
			RPCharacters.plugin.getLogger().warning(
					"Kit claim status check failed for " + player.getName()
							+ " kit=" + kitId + ": " + claimStatus.error
			);
		}

		net.tfminecraft.RPCharacters.ingest.KitCustomiseIngestService
				.ingestReadyForCharacterOnMain(player, character);

		List<KitItemDefinition> definitions = kit.getItems();
		if (definitions.isEmpty()) {
			RPCharacters.plugin.getLogger().warning(
					"Kit claim skipped for " + player.getName() + ": kit '" + kitId + "' has no items."
			);
			RPTexts.send(player, RPTexts.ERROR + "That kit is not configured. Contact staff.");
			return;
		}

		List<ItemStack> stacks = new ArrayList<>();
		for (KitItemDefinition def : definitions) {
			List<ItemStack> built = buildStacks(def);
			if (built.isEmpty()) {
				RPCharacters.plugin.getLogger().warning(
						"Kit '" + kitId + "' could not build path '" + def.getPath()
								+ "' for " + player.getName() + " — skipped line."
				);
				continue;
			}
			stacks.addAll(built);
		}
		if (stacks.isEmpty()) {
			RPCharacters.plugin.getLogger().warning(
					"Kit claim aborted for " + player.getName() + " kit=" + kitId + ": no stacks produced."
			);
			RPTexts.send(player, RPTexts.ERROR + "That kit could not be built. Contact staff.");
			return;
		}

		boolean dropped = false;
		Location dropAt = player.getLocation();
		for (ItemStack stack : stacks) {
			Map<Integer, ItemStack> leftover = player.getInventory().addItem(stack);
			for (ItemStack left : leftover.values()) {
				if (left != null && !left.getType().isAir() && left.getAmount() > 0) {
					player.getWorld().dropItemNaturally(dropAt, left);
					dropped = true;
				}
			}
		}

		if (kit.isOncePerCharacter()) {
			character.setKitStatus(kitId, KitStatus.GRANTED);
		} else {
			character.setKitStatus(kitId, KitStatus.ELIGIBLE);
		}
		pd.setLastKitClaimAtMs(kitId, System.currentTimeMillis());
		RPCharacters.getPlayerManager().savePlayer(player);
		net.tfminecraft.RPCharacters.ingest.RosterSyncService.pushRosterForPlayer(player);

		List<String> editableKeys = kit.editableKitKeys();
		for (KitCustomiseData data : character.getKitCustomisations().values()) {
			if (data == null || data.getKitKey().isBlank()) {
				continue;
			}
			if (editableKeys.contains(data.getKitKey())) {
				KitCustomiseApplyService.applyToInventory(player, data);
			}
		}

		RPTexts.send(player, RPTexts.SUCCESS + "You claimed the " + kit.getDisplayName() + " kit!");
		if (dropped) {
			RPTexts.send(player, RPTexts.WARN + "Some kit items did not fit and were dropped at your feet.");
		}
	}

	private static List<ItemStack> buildStacks(KitItemDefinition def) {
		List<ItemStack> out = new ArrayList<>();
		if (def == null || def.getPath() == null || def.getPath().isBlank()) {
			return out;
		}
		ItemStack template;
		try {
			template = TLibs.getItemAPI().getCreator().getItemFromPath(def.getPath());
		} catch (Exception e) {
			RPCharacters.plugin.getLogger().warning(
					"Kit path '" + def.getPath() + "' threw: " + e.getMessage()
			);
			return out;
		}
		if (template == null || template.getType().isAir()) {
			return out;
		}
		int remaining = def.getAmount();
		int max = Math.max(1, template.getMaxStackSize());
		while (remaining > 0) {
			int take = Math.min(max, remaining);
			ItemStack stack = template.clone();
			stack.setAmount(take);
			out.add(stack);
			remaining -= take;
		}
		return out;
	}
}
