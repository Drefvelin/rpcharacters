package net.tfminecraft.RPCharacters.roll;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.Utils.RPTexts;

public final class RollManager implements Listener, CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		return true;
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onCommand(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();
		if (player.getGameMode() != GameMode.SURVIVAL) {
			return;
		}

		String raw = event.getMessage().stripLeading();
		if (!raw.startsWith("/")) {
			return;
		}

		String withoutSlash = raw.substring(1);
		int space = withoutSlash.indexOf(' ');
		String label = (space < 0 ? withoutSlash : withoutSlash.substring(0, space)).toLowerCase(Locale.ROOT);
		String remainder = space < 0 ? "" : withoutSlash.substring(space + 1).stripLeading();

		if (label.equals("roll") && remainder.isEmpty()) {
			event.setCancelled(true);
			handleDefaultRoll(player);
			return;
		}

		if (!label.equals("tfmc") || remainder.isEmpty()) {
			return;
		}

		String[] parts = remainder.split("\\s+");
		if (parts.length == 0 || !parts[0].equalsIgnoreCase("roll")) {
			return;
		}

		event.setCancelled(true);
		handleTfmcRoll(player, parts);
	}

	public void handleDefaultRoll(Player player) {
		if (!checkPermission(player)) {
			return;
		}
		int min;
		int max;
		if (player.hasPermission(Cache.rollAltPermission)) {
			min = Cache.rollAltMin;
			max = Cache.rollAltMax;
		} else {
			min = Cache.rollDefaultMin;
			max = Cache.rollDefaultMax;
		}
		executeRoll(player, min, max, 0);
	}

	private void handleTfmcRoll(Player player, String[] parts) {
		if (!checkPermission(player)) {
			return;
		}
		if (parts.length < 2) {
			RPTexts.send(player, RPTexts.WARN + "Usage: /tfmc roll <max>|<attribute> [<+/-modifier>]");
			return;
		}

		String firstArg = parts[1];
		if (isAttribute(firstArg)) {
			executeRoll(player, Cache.rollD20Min, Cache.rollD20Max, AttributeRollResolver.resolveModifier(player, firstArg));
			return;
		}

		Integer max = parsePositiveInt(firstArg);
		if (max == null) {
			RPTexts.send(player, RPTexts.ERROR + "Invalid roll maximum: " + RPTexts.WARN + firstArg);
			return;
		}

		int modifier = 0;
		if (parts.length >= 3) {
			Integer parsedModifier = parseSignedInt(parts[2]);
			if (parsedModifier == null) {
				RPTexts.send(player, RPTexts.ERROR + "Invalid modifier: " + RPTexts.WARN + parts[2]);
				return;
			}
			modifier = parsedModifier;
		}

		executeRoll(player, 1, max, modifier);
	}

	private boolean checkPermission(Player player) {
		if (player.hasPermission(Cache.rollPermission)) {
			return true;
		}
		RPTexts.send(player, RPTexts.ERROR + "You do not have permission to roll dice.");
		return false;
	}

	public static void executeRoll(Player player, int min, int max, int modifier) {
		if (player == null) {
			return;
		}
		int low = Math.min(min, max);
		int high = Math.max(min, max);
		if (high < 1) {
			RPTexts.send(player, RPTexts.ERROR + "Invalid roll range.");
			return;
		}
		if (low < 1) {
			low = 1;
		}

		int roll = ThreadLocalRandom.current().nextInt(low, high + 1);
		String message = RollFormatter.format(player, roll, high, modifier);
		if (message.isEmpty()) {
			return;
		}

		broadcast(player, message);
	}

	private static void broadcast(Player origin, String message) {
		int range = Cache.rollBroadcastRange;
		if (range <= 0) {
			origin.getServer().broadcastMessage(message);
			return;
		}

		double rangeSq = (double) range * range;
		for (Player target : origin.getWorld().getPlayers()) {
			if (target.getLocation().distanceSquared(origin.getLocation()) <= rangeSq) {
				RPTexts.send(target, message);
			}
		}
	}

	private static boolean isAttribute(String value) {
		if (value == null) {
			return false;
		}
		String normalized = value.toLowerCase(Locale.ROOT);
		for (String attribute : Cache.attributes) {
			if (attribute.equalsIgnoreCase(normalized)) {
				return true;
			}
		}
		return false;
	}

	private static Integer parsePositiveInt(String value) {
		try {
			int parsed = Integer.parseInt(value);
			return parsed >= 1 ? parsed : null;
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	private static Integer parseSignedInt(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String trimmed = value.trim();
		try {
			return Integer.parseInt(trimmed);
		} catch (NumberFormatException ex) {
			if (trimmed.startsWith("+")) {
				try {
					return Integer.parseInt(trimmed.substring(1));
				} catch (NumberFormatException ignored) {
					return null;
				}
			}
			return null;
		}
	}
}
