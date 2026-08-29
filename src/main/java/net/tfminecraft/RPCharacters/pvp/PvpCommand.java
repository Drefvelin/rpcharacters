package net.tfminecraft.RPCharacters.pvp;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import net.tfminecraft.RPCharacters.Loaders.PvpLoader;
import net.tfminecraft.RPCharacters.Managers.PlayerManager;
import net.tfminecraft.RPCharacters.Objects.PlayerData;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.Utils.RPTexts;

public final class PvpCommand implements CommandExecutor, TabCompleter {

	public static final String COMMAND = "pvp";

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!COMMAND.equalsIgnoreCase(command.getName())) {
			return false;
		}
		if (!(sender instanceof Player player)) {
			RPTexts.send(sender, PvpLoader.getPlayersOnly());
			return true;
		}
		if (args.length == 0) {
			sendUsage(player);
			return true;
		}
		String sub = args[0].toLowerCase(Locale.ROOT);
		switch (sub) {
			case "start" -> {
				startWarning(player);
				return true;
			}
			case "lethal" -> {
				setLethal(player, true);
				return true;
			}
			case "nonlethal" -> {
				setLethal(player, false);
				return true;
			}
			default -> {
				sendUsage(player);
				return true;
			}
		}
	}

	private void sendUsage(Player player) {
		RPTexts.send(player, PvpLoader.getUsage());
		PlayerData pd = PlayerManager.get(player);
		if (pd == null || !pd.hasActiveCharacter()) {
			return;
		}
		String mode = pd.getActiveCharacter().isPvpLethal() ? "lethal" : "nonlethal";
		RPTexts.send(player, PvpLoader.getCurrent().replace("{mode}", mode));
	}

	private void setLethal(Player player, boolean lethal) {
		PlayerData pd = PlayerManager.get(player);
		if (pd == null || !pd.hasActiveCharacter()) {
			RPTexts.send(player, PvpLoader.getNoCharacter());
			return;
		}
		RPCharacter character = pd.getActiveCharacter();
		character.setPvpLethal(lethal);
		RPCharacters.getPlayerManager().savePlayer(player);
		RPTexts.send(player, lethal ? PvpLoader.getLethal() : PvpLoader.getNonlethal());
	}

	private void startWarning(Player player) {
		Location origin = player.getLocation();
		double radius = PvpLoader.getStartRadius();
		double radiusSq = radius * radius;
		List<UUID> targets = new ArrayList<>();
		for (Player nearby : player.getWorld().getPlayers()) {
			if (nearby.getLocation().distanceSquared(origin) <= radiusSq) {
				targets.add(nearby.getUniqueId());
			}
		}
		String warning = PvpLoader.getStartWarning()
				.replace("{seconds}", String.valueOf(PvpLoader.getStartWarnSeconds()));
		broadcast(targets, warning);

		int warnSeconds = PvpLoader.getStartWarnSeconds();
		int from = Math.min(PvpLoader.getStartCountdownFrom(), warnSeconds);
		for (int count = from; count >= 1; count--) {
			int delaySeconds = warnSeconds - count;
			int n = count;
			new BukkitRunnable() {
				@Override
				public void run() {
					broadcast(targets, PvpLoader.getCountdown().replace("{count}", String.valueOf(n)));
				}
			}.runTaskLater(RPCharacters.plugin, delaySeconds * 20L);
		}
	}

	private void broadcast(List<UUID> targets, String raw) {
		for (UUID id : targets) {
			Player online = Bukkit.getPlayer(id);
			if (online != null && online.isOnline()) {
				RPTexts.send(online, raw);
			}
		}
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (args.length == 1) {
			String prefix = args[0].toLowerCase(Locale.ROOT);
			List<String> out = new ArrayList<>();
			for (String opt : new String[] { "start", "lethal", "nonlethal" }) {
				if (opt.startsWith(prefix)) {
					out.add(opt);
				}
			}
			return out;
		}
		return List.of();
	}
}
