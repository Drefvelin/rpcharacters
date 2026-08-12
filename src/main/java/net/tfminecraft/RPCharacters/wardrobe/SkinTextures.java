package net.tfminecraft.RPCharacters.wardrobe;

/**
 * Mojang texture property pair.
 */
public final class SkinTextures {

	private final String value;
	private final String signature;

	public SkinTextures(String value, String signature) {
		this.value = value;
		this.signature = signature;
	}

	public String getValue() {
		return value;
	}

	public String getSignature() {
		return signature;
	}

	public boolean isValid() {
		return value != null
			&& !value.isEmpty()
			&& signature != null
			&& !signature.isEmpty();
	}
}
