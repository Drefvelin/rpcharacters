package net.tfminecraft.RPCharacters.chat;

import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.tfminecraft.RPCharacters.Loaders.ChatLoader;

public final class ChatChannelExecutor implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage("§cPlease run this command as a player.");
			return true;
		}

		String channelKey = ChatLoader.resolveChannelFromCommand(label.toLowerCase(Locale.ROOT));
		if (channelKey == null) {
			return true;
		}

		ChatChannel channel = ChatLoader.getChannel(channelKey);
		if (channel == null) {
			return true;
		}

		if (args == null || args.length == 0) {
			player.sendMessage("§cUsage: /" + label + " <message>");
			return true;
		}

		String message = Stream.of(args).collect(Collectors.joining(" ")).stripLeading();
		if (message.isEmpty()) {
			player.sendMessage("§cUsage: /" + label + " <message>");
			return true;
		}

		ChatManager.dispatch(player, channel, message, true);
		return true;
	}
}
