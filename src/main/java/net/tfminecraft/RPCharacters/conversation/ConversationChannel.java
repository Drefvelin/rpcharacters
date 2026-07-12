package net.tfminecraft.RPCharacters.conversation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;

public class ConversationChannel {

	private final String id;
	private final int range;
	private final List<String> commands;

	public ConversationChannel(String id, ConfigurationSection config) {
		this.id = id;
		this.range = config.getInt("range", 15);
		List<String> cmds = config.getStringList("commands");
		this.commands = cmds != null ? new ArrayList<>(cmds) : new ArrayList<>();
	}

	public String getId() {
		return id;
	}

	public int getRange() {
		return range;
	}

	public List<String> getCommands() {
		return Collections.unmodifiableList(commands);
	}
}
