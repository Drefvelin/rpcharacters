package net.tfminecraft.RPCharacters.mmocore;

import java.util.Locale;

import net.tfminecraft.RPCharacters.Cache;

public final class IgnoredAttributes {

	private IgnoredAttributes() {}

	public static boolean isIgnored(String attributeId) {
		if (attributeId == null) {
			return false;
		}
		return Cache.ignoredAttributes.contains(attributeId.trim().toLowerCase(Locale.ROOT));
	}
}
