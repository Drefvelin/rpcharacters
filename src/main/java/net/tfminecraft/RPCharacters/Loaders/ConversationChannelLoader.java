package net.tfminecraft.RPCharacters.Loaders;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import net.tfminecraft.RPCharacters.conversation.ConversationChannel;

public final class ConversationChannelLoader {

	private static final Map<String, ConversationChannel> channels = new HashMap<>();
	private static final Map<String, String> commandToChannel = new HashMap<>();

	private ConversationChannelLoader() {}

	public static void load(FileConfiguration config) {
		channels.clear();
		commandToChannel.clear();
		if (!config.isConfigurationSection("conversation.channels")) {
			return;
		}
		ConfigurationSection section = config.getConfigurationSection("conversation.channels");
		for (String key : section.getKeys(false)) {
			ConfigurationSection channelSection = section.getConfigurationSection(key);
			if (channelSection == null) {
				continue;
			}
			ConversationChannel channel = new ConversationChannel(key, channelSection);
			channels.put(key.toLowerCase(Locale.ROOT), channel);
			for (String command : channel.getCommands()) {
				commandToChannel.put(command.toLowerCase(Locale.ROOT), key.toLowerCase(Locale.ROOT));
			}
		}
	}

	public static ConversationChannel getChannel(String channelId) {
		if (channelId == null) {
			return null;
		}
		return channels.get(channelId.toLowerCase(Locale.ROOT));
	}

	public static String resolveChannelFromCommand(String commandLabel) {
		if (commandLabel == null) {
			return null;
		}
		return commandToChannel.get(commandLabel.toLowerCase(Locale.ROOT));
	}

	/** Plain chat (no command) is treated as the rp channel. */
	public static ConversationChannel getDefaultChannel() {
		return getChannel("rp");
	}
}
