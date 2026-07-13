package net.tfminecraft.RPCharacters.Utils;

import org.bukkit.entity.Player;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;
import net.tfminecraft.RPCharacters.RPCharacters;
import net.tfminecraft.RPCharacters.mmocore.AttributePointService;

public final class TraitChangeService {

	private TraitChangeService() {
	}

	public static void addTrait(Player player, RPCharacter character, Trait trait) {
		if (character.isActive()) {
			Integrator integrator = new Integrator();
			integrator.remove(player, character, false);
			character.addTrait(trait);
			character.update();
			integrator.integrate(player, character);
			AttributePointService.refreshAfterCreationLayerChange(player, character);
		} else {
			character.addTrait(trait);
			character.update();
		}

		RPCharacters.getPlayerManager().savePlayer(player);
		RPCharacters.getPlayerManager().reevaluateFreeze(player);
	}

	public static void removeTrait(Player player, RPCharacter character, Trait trait) {
		if (character.isActive()) {
			Integrator integrator = new Integrator();
			integrator.remove(player, character, false);
			character.removeTrait(trait);
			character.update();
			integrator.integrate(player, character);
			AttributePointService.refreshAfterCreationLayerChange(player, character);
		} else {
			character.removeTrait(trait);
			character.update();
		}

		RPCharacters.getPlayerManager().savePlayer(player);
		RPCharacters.getPlayerManager().reevaluateFreeze(player);
	}

	public static void sendGainedMessage(Player player, Trait trait) {
		RPTexts.send(player, resolveGainedMessage(trait));
	}

	public static String resolveGainedMessage(Trait trait) {
		String message = trait.getGainedMessage();
		if (message != null && !message.isBlank()) {
			return message;
		}
		return RPTexts.WARN + "You gained the trait " + trait.getName() + RPTexts.WARN + ".";
	}

	public static void sendLostMessage(Player player, Trait trait) {
		RPTexts.send(player, resolveLostMessage(trait));
	}

	public static String resolveLostMessage(Trait trait) {
		String message = trait.getLostMessage();
		if (message != null && !message.isBlank()) {
			return message;
		}
		return RPTexts.ERROR + "You lost the trait " + trait.getName() + RPTexts.ERROR + ".";
	}

	public static void sendRemedyCuredMessage(Player player, Trait trait) {
		RPTexts.send(player, resolveLostMessage(trait));
	}
}
