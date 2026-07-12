package net.tfminecraft.RPCharacters.Loaders;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.Plugins.TLibs.Interface.LoaderInterface;
import net.tfminecraft.RPCharacters.Cache;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.chat.ChatChannel;
import net.tfminecraft.RPCharacters.chat.ChatCommandRegistry;

public final class ChatLoader implements LoaderInterface {

	private static final Map<String, ChatChannel> channels = new HashMap<>();
	private static final Map<String, String> commandToChannel = new HashMap<>();

	@Override
	public void load(File configFile) {
		FileConfiguration config = new YamlConfiguration();
		try {
			config.load(configFile);
		} catch (IOException | InvalidConfigurationException e) {
			e.printStackTrace();
		}

		channels.clear();
		commandToChannel.clear();

		Cache.chatBypassCooldownPermission = config.getString("bypass-cooldown-perm",
				Cache.chatBypassCooldownPermission);
		Cache.chatDefaultChannel = config.getString("default", Cache.chatDefaultChannel);
		Cache.chatNoCharacterMessage = config.getString("no-character-message", Cache.chatNoCharacterMessage);

		if (!config.isConfigurationSection("channels")) {
			syncCommands();
			return;
		}

		ConfigurationSection section = config.getConfigurationSection("channels");
		for (String key : section.getKeys(false)) {
			ConfigurationSection channelSection = section.getConfigurationSection(key);
			if (channelSection == null) {
				continue;
			}
			ChatChannel channel = new ChatChannel(key, channelSection);
			String channelId = key.toLowerCase(Locale.ROOT);
			channels.put(channelId, channel);
			for (String command : channel.getCommands()) {
				if (command != null && !command.isBlank()) {
					commandToChannel.put(command.toLowerCase(Locale.ROOT), channelId);
				}
			}
		}

		syncCommands();
	}

	private static void syncCommands() {
		if (RPCharacters.plugin != null) {
			ChatCommandRegistry.sync(RPCharacters.plugin);
		}
	}

	public static ChatChannel getChannel(String channelId) {
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

	public static ChatChannel getDefaultChannel() {
		return getChannel(Cache.chatDefaultChannel);
	}

	public static List<String> getChannelCommands() {
		List<String> commands = new ArrayList<>();
		for (ChatChannel channel : channels.values()) {
			for (String command : channel.getCommands()) {
				if (command == null || command.isBlank()) {
					continue;
				}
				String normalized = command.toLowerCase(Locale.ROOT);
				if (!commands.contains(normalized)) {
					commands.add(normalized);
				}
			}
		}
		String defaultChannel = Cache.chatDefaultChannel != null
				? Cache.chatDefaultChannel.toLowerCase(Locale.ROOT)
				: "rp";
		commands.sort((a, b) -> {
			if (a.equals(defaultChannel)) {
				return -1;
			}
			if (b.equals(defaultChannel)) {
				return 1;
			}
			return a.compareTo(b);
		});
		return List.copyOf(commands);
	}
}
