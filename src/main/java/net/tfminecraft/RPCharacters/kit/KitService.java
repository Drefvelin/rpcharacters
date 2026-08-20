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
		RPCharacters.plugin.getLogger().info(
				"[kit-claim] start player=" + player.getName()
						+ " char=" + character.getId()
						+ " kit=" + kitId
						+ " status=" + (status != null ? status.toStorage() : "null")
		);
		// Missing status = pre-kit / never stamped. Website treats that as eligible
		// for customise; claim must match (CE /tfmc starter is retired).
		if (status == null) {
			status = KitStatus.ELIGIBLE;
			character.setKitStatus(kitId, status);
			RPCharacters.plugin.getLogger().info(
					"[kit-claim] stamped missing status as eligible for "
							+ player.getName() + " char=" + character.getId()
							+ " kit=" + kitId
			);
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
		boolean pendingSkin = claimStatus.ok
				&& net.tfminecraft.RPCharacters.api.ProvinceSystemClient.claimStatusPendingSkin(
						claimStatus.body
				);
		boolean pendingPack = claimStatus.ok
				&& net.tfminecraft.RPCharacters.api.ProvinceSystemClient.claimStatusPendingPack(
						claimStatus.body
				);
		if (claimStatus.ok) {
			String body = claimStatus.body != null ? claimStatus.body : "";
			String snippet = body.length() > 400 ? body.substring(0, 400) + "…" : body;
			RPCharacters.plugin.getLogger().info(
					"[kit-claim] claim-status ok pending_skin=" + pendingSkin
							+ " pending_pack=" + pendingPack
							+ " body=" + snippet
			);
		} else {
			RPCharacters.plugin.getLogger().warning(
					"[kit-claim] claim-status failed for " + player.getName()
							+ " kit=" + kitId + ": " + claimStatus.error
			);
		}
		if (pendingSkin) {
			RPTexts.send(player, RPTexts.WARN
					+ "A custom kit item is still waiting for approval.");
			return;
		}
		if (pendingPack) {
			RPTexts.send(player, RPTexts.WARN
					+ "A custom kit skin is still pending pack. "
					+ "It will be added within 24 hours - try claiming again after that.");
			return;
		}

		net.tfminecraft.RPCharacters.ingest.KitCustomiseIngestService
				.ingestReadyForCharacterOnMain(player, character);

		List<String> editableKeys = kit.editableKitKeys();
		int customiseCount = 0;
		for (KitCustomiseData data : character.getKitCustomisations().values()) {
			if (data == null || data.getKitKey().isBlank()) {
				continue;
			}
			if (!editableKeys.contains(data.getKitKey())) {
				continue;
			}
			customiseCount++;
			int loreSize = data.getLore() != null ? data.getLore().size() : 0;
			RPCharacters.plugin.getLogger().info(
					"[kit-claim] stamped customise kit_key=" + data.getKitKey()
							+ " display_name=" + data.getDisplayName()
							+ " lore_lines=" + loreSize
							+ " skin_slug=" + data.getSkinSlug()
							+ " path=" + data.getPath()
			);
		}
		RPCharacters.plugin.getLogger().info(
				"[kit-claim] after ingest editable customise count=" + customiseCount
						+ " editable_keys=" + editableKeys
		);

		for (KitCustomiseData data : character.getKitCustomisations().values()) {
			if (data == null || data.getKitKey().isBlank()) {
				continue;
			}
			if (!editableKeys.contains(data.getKitKey())) {
				continue;
			}
			boolean present = KitCustomiseApplyService.isSkinPresent(data);
			RPCharacters.plugin.getLogger().info(
					"[kit-claim] skin-ready kit_key=" + data.getKitKey()
							+ " skin_slug=" + data.getSkinSlug()
							+ " present=" + present
			);
		}
		if (!KitCustomiseApplyService.requiredSkinsReady(character, editableKeys)) {
			RPTexts.send(player, RPTexts.WARN
					+ "Kit is not ready yet, awaiting skins.");
			return;
		}

		List<KitItemDefinition> definitions = kit.getItems();
		if (definitions.isEmpty()) {
			RPCharacters.plugin.getLogger().warning(
					"[kit-claim] skipped for " + player.getName() + ": kit '" + kitId + "' has no items."
			);
			RPTexts.send(player, RPTexts.ERROR + "That kit is not configured. Contact staff.");
			return;
		}

		List<ItemStack> stacks = new ArrayList<>();
		for (KitItemDefinition def : definitions) {
			List<ItemStack> built = buildStacks(def);
			if (built.isEmpty()) {
				RPCharacters.plugin.getLogger().warning(
						"[kit-claim] kit '" + kitId + "' could not build path '" + def.getPath()
								+ "' for " + player.getName() + " — skipped line."
				);
				continue;
			}
			stacks.addAll(built);
		}
		if (stacks.isEmpty()) {
			RPCharacters.plugin.getLogger().warning(
					"[kit-claim] aborted for " + player.getName() + " kit=" + kitId + ": no stacks produced."
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

		for (KitCustomiseData data : character.getKitCustomisations().values()) {
			if (data == null || data.getKitKey().isBlank()) {
				continue;
			}
			if (editableKeys.contains(data.getKitKey())) {
				boolean replaced = KitCustomiseApplyService.applyToInventory(player, data);
				RPCharacters.plugin.getLogger().info(
						"[kit-claim] applyToInventory kit_key=" + data.getKitKey()
								+ " replaced=" + replaced
				);
			}
		}

		RPTexts.send(player, RPTexts.SUCCESS + "You claimed the " + kit.getDisplayName() + " kit!");
		if (dropped) {
			RPTexts.send(player, RPTexts.WARN + "Some kit items did not fit and were dropped at your feet.");
		}
	}

	/**
	 * Staff reset: restore claimability for one character + kit; clear cooldown and
	 * customisations. Caller must ensure target is online with loaded PlayerData.
	 */
	public static final class ResetResult {
		public final boolean ok;
		public final String message;
		public final boolean psWipeOk;
		public final String psWipeError;

		private ResetResult(boolean ok, String message, boolean psWipeOk, String psWipeError) {
			this.ok = ok;
			this.message = message;
			this.psWipeOk = psWipeOk;
			this.psWipeError = psWipeError;
		}

		public static ResetResult fail(String message) {
			return new ResetResult(false, message, true, null);
		}

		public static ResetResult ok(String message, boolean psWipeOk, String psWipeError) {
			return new ResetResult(true, message, psWipeOk, psWipeError);
		}
	}

	private static final class ResolvedKitTarget {
		final PlayerData pd;
		final RPCharacter character;
		final KitDefinition kit;
		final String kitId;
		final String label;

		ResolvedKitTarget(
				PlayerData pd,
				RPCharacter character,
				KitDefinition kit,
				String kitId,
				String label
		) {
			this.pd = pd;
			this.character = character;
			this.kit = kit;
			this.kitId = kitId;
			this.label = label;
		}
	}

	/**
	 * Resolve player + public character id (slug) + kit. UUID character id accepted as fallback.
	 */
	private static Object resolveKitTarget(Player target, String characterIdRaw, String kitIdRaw, String usage) {
		if (target == null || !target.isOnline()) {
			return ResetResult.fail("Player must be online.");
		}
		String characterRef = characterIdRaw != null ? characterIdRaw.trim() : "";
		String kitId = kitIdRaw != null ? kitIdRaw.trim().toLowerCase(Locale.ROOT) : "";
		if (characterRef.isEmpty() || kitId.isEmpty()) {
			return ResetResult.fail(usage);
		}
		KitDefinition kit = KitLoader.getKit(kitId);
		if (kit == null) {
			return ResetResult.fail("Unknown kit id '" + kitId + "'.");
		}
		if (!PlayerManager.exists(target)) {
			return ResetResult.fail("No character data loaded for " + target.getName() + ".");
		}
		PlayerData pd = PlayerManager.get(target);
		if (pd == null) {
			return ResetResult.fail("No character data loaded for " + target.getName() + ".");
		}
		RPCharacter character = pd.getCharacterBySlug(characterRef);
		if (character == null) {
			character = pd.getCharacterById(characterRef);
		}
		if (character == null) {
			return ResetResult.fail(
					"Character '" + characterRef + "' not found for " + target.getName() + "."
			);
		}
		String label = character.getSlug() != null && !character.getSlug().isBlank()
				? character.getSlug()
				: character.getId();
		return new ResolvedKitTarget(pd, character, kit, kitId, label);
	}

	/**
	 * Staff: make kit claimable again. Keeps in-game and ProvinceSystem customisations.
	 */
	public static ResetResult reclaimKit(Player target, String characterIdRaw, String kitIdRaw) {
		Object resolved = resolveKitTarget(
				target,
				characterIdRaw,
				kitIdRaw,
				"Usage: /rpcharacter reclaimkit <player> <character_id> <kit_id>"
		);
		if (resolved instanceof ResetResult fail) {
			return fail;
		}
		ResolvedKitTarget t = (ResolvedKitTarget) resolved;
		t.character.setKitStatus(t.kitId, KitStatus.ELIGIBLE);
		t.pd.setLastKitClaimAtMs(t.kitId, null);
		RPCharacters.getPlayerManager().savePlayer(target);
		net.tfminecraft.RPCharacters.ingest.RosterSyncService.pushRosterForPlayer(target);
		return ResetResult.ok(
				"Reclaimed kit '" + t.kitId + "' for " + target.getName()
						+ " character " + t.label + " (customisations kept).",
				true,
				null
		);
	}

	/**
	 * Staff: full wipe — reclaimable + clear in-game and ProvinceSystem customisations.
	 */
	public static ResetResult resetKit(Player target, String characterIdRaw, String kitIdRaw) {
		Object resolved = resolveKitTarget(
				target,
				characterIdRaw,
				kitIdRaw,
				"Usage: /rpcharacter resetkit <player> <character_id> <kit_id>"
		);
		if (resolved instanceof ResetResult fail) {
			return fail;
		}
		ResolvedKitTarget t = (ResolvedKitTarget) resolved;

		t.character.setKitStatus(t.kitId, KitStatus.ELIGIBLE);
		t.pd.setLastKitClaimAtMs(t.kitId, null);
		for (String key : t.kit.editableKitKeys()) {
			t.character.removeKitCustomise(key);
		}
		RPCharacters.getPlayerManager().savePlayer(target);
		net.tfminecraft.RPCharacters.ingest.RosterSyncService.pushRosterForPlayer(target);

		String playerUuid = target.getUniqueId().toString();
		net.tfminecraft.RPCharacters.api.ProvinceSystemClient.SimpleResult wipe =
				net.tfminecraft.RPCharacters.api.ProvinceSystemClient.clearLoreItemCustomisations(
						playerUuid, t.character.getId(), t.kitId
				);
		String msg = "Reset kit '" + t.kitId + "' for " + target.getName()
				+ " character " + t.label + " (customisations wiped).";
		if (!wipe.ok) {
			RPCharacters.plugin.getLogger().warning(
					"[resetkit] ProvinceSystem customise wipe failed: " + wipe.error
			);
			return ResetResult.ok(msg, false, wipe.error);
		}
		return ResetResult.ok(msg, true, null);
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
