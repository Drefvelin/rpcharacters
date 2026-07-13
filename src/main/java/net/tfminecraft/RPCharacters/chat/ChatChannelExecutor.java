package net.tfminecraft.RPCharacters.chat;

import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.tfminecraft.RPCharacters.Loaders.ChatLoader;
import net.tfminecraft.RPCharacters.Utils.RPTexts;

public final class ChatChannelExecutor implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			RPTexts.send(sender, RPTexts.ERROR + "Players only.");
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
			RPTexts.send(player, RPTexts.ERROR + "Usage: /" + label + " <message>");
			return true;
		}

		String message = Stream.of(args).collect(Collectors.joining(" ")).stripLeading();
		if (message.isEmpty()) {
			RPTexts.send(player, RPTexts.ERROR + "Usage: /" + label + " <message>");
			return true;
		}

		ChatManager.dispatch(player, channel, message, true);
		return true;
	}
}
