package net.tfminecraft.RPCharacters.Utils;

import java.lang.reflect.Method;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class MaskChecker {

	private static final String MASK_RESOLVER = "net.tfminecraft.thievery.mask.MaskResolver";
	private static Boolean thieveryPresent;

	private MaskChecker() {}

	public static boolean isWearingMask(Player player) {
		if (player == null || !isThieveryPresent()) {
			return false;
		}
		try {
			Class<?> cls = Class.forName(MASK_RESOLVER);
			Method method = cls.getMethod("isWearingMask", Player.class);
			Object result = method.invoke(null, player);
			return result instanceof Boolean && (Boolean) result;
		} catch (ReflectiveOperationException ex) {
			return false;
		}
	}

	private static boolean isThieveryPresent() {
		if (thieveryPresent == null) {
			thieveryPresent = Bukkit.getPluginManager().getPlugin("Thievery") != null;
		}
		return thieveryPresent;
	}
}
