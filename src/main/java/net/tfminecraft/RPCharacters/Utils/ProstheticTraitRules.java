package net.tfminecraft.RPCharacters.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;

import net.tfminecraft.RPCharacters.Loaders.ProstheticLoader;
import net.tfminecraft.RPCharacters.Loaders.TraitLoader;
import net.tfminecraft.RPCharacters.Objects.ProstheticReplacement;
import net.tfminecraft.RPCharacters.Objects.RPCharacter;
import net.tfminecraft.RPCharacters.Objects.Trait.Trait;

public final class ProstheticTraitRules {

	private ProstheticTraitRules() {
	}

	/**
	 * Removes permanent backstory injuries superseded by an owned prosthetic trait.
	 *
	 * @return {@code true} if at least one injury trait was removed
	 */
	public static boolean stripReplacedInjuries(RPCharacter character) {
		if (character == null) {
			return false;
		}
		Set<String> injuryIdsToRemove = injuriesSupersededByProsthetics(
				character.getTraits().stream().map(Trait::getId).toList(),
				ProstheticTraitRules::permanentInjuryForOwnedTrait);
		if (injuryIdsToRemove.isEmpty()) {
			return false;
		}
		List<Trait> toRemove = new ArrayList<>();
		for (Trait trait : character.getTraits()) {
			if (injuryIdsToRemove.contains(trait.getId().toLowerCase(Locale.ROOT))) {
				toRemove.add(trait);
			}
		}
		for (Trait trait : toRemove) {
			character.removeTrait(trait);
		}
		return !toRemove.isEmpty();
	}

	static Set<String> injuriesSupersededByProsthetics(
			List<String> ownedTraitIds,
			Function<String, String> permanentInjuryForTraitId) {
		Set<String> injuriesToRemove = new HashSet<>();
		for (String traitId : ownedTraitIds) {
			String injuryId = permanentInjuryForTraitId.apply(traitId);
			if (injuryId != null && !injuryId.isBlank()) {
				injuriesToRemove.add(injuryId.toLowerCase(Locale.ROOT));
			}
		}
		return injuriesToRemove;
	}

	static List<String> withoutTraitIds(List<String> ownedTraitIds, Set<String> traitIdsToRemove) {
		if (traitIdsToRemove.isEmpty()) {
			return ownedTraitIds;
		}
		List<String> kept = new ArrayList<>(ownedTraitIds.size());
		for (String traitId : ownedTraitIds) {
			if (!traitIdsToRemove.contains(traitId.toLowerCase(Locale.ROOT))) {
				kept.add(traitId);
			}
		}
		return kept;
	}

	private static String permanentInjuryForOwnedTrait(String traitId) {
		Trait trait = TraitLoader.getByString(traitId);
		if (trait == null || !trait.getTraitData().isProstheticKey()) {
			return null;
		}
		ProstheticReplacement replacement = ProstheticLoader.getReplacementForProsthetic(traitId);
		if (replacement == null) {
			return null;
		}
		return replacement.getPermanentInjuryId();
	}
}
