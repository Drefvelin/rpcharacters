package net.tfminecraft.RPCharacters.chat.smart;

import net.tfminecraft.RPCharacters.Loaders.SmartMessageLoader;
import net.tfminecraft.RPCharacters.RPCharacters;

public final class SmartMessageDebug {

	private static final String PREFIX = "[SmartMessages] ";

	private SmartMessageDebug() {}

	public static boolean isEnabled() {
		return SmartMessageLoader.getSettings().isDebugMessages();
	}

	public static void log(String stage, String message) {
		if (!isEnabled()) {
			return;
		}
		if (RPCharacters.plugin != null) {
			RPCharacters.plugin.getLogger().info(PREFIX + stage + ": " + message);
		}
	}

	public static void logSkip(String stage, String reason) {
		log(stage, "SKIP — " + reason);
	}
}
