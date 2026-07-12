package net.tfminecraft.RPCharacters.chat;

import org.bukkit.entity.Player;

import me.Plugins.TLibs.Objects.API.SubAPI.StringFormatter;
import net.tfminecraft.RPCharacters.identity.DisplayIdentityService;

public final class ChatFormatter {

	private ChatFormatter() {}

	public static String format(ChatChannel channel, Player sender, String displayName, String message) {
		if (channel == null || sender == null) {
			return "";
		}
		String template = channel.getFormat();
		if (template == null || template.isEmpty()) {
			return "";
		}
		String withTokens = template
				.replace("{display}", displayName != null ? displayName : "")
				.replace("{display_no_mask}", DisplayIdentityService.resolveDisplayNoMask(sender))
				.replace("{player}", sender.getName())
				.replace("{message}", message != null ? message : "");
		return StringFormatter.formatHex(withTokens.replace('&', '\u00A7'));
	}
}
