package net.tfminecraft.RPCharacters.Utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

class ProstheticTraitRulesTest {

	private static final Function<String, String> LOOKUP = traitId -> {
		return Map.of(
				"wooden_claw_arm", "one_handed",
				"pegleg", "one_legged")
				.get(traitId.toLowerCase());
	};

	@Test
	void matchingInjuryIsSupersededByProsthetic() {
		Set<String> toRemove = ProstheticTraitRules.injuriesSupersededByProsthetics(
				List.of("wooden_claw_arm", "one_handed", "blind"),
				LOOKUP);
		assertEquals(Set.of("one_handed"), toRemove);
		List<String> kept = ProstheticTraitRules.withoutTraitIds(
				List.of("wooden_claw_arm", "one_handed", "blind"),
				toRemove);
		assertEquals(List.of("wooden_claw_arm", "blind"), kept);
	}

	@Test
	void unrelatedInjuryIsKept() {
		Set<String> toRemove = ProstheticTraitRules.injuriesSupersededByProsthetics(
				List.of("pegleg", "blind"),
				LOOKUP);
		assertEquals(Set.of("one_legged"), toRemove);
		List<String> kept = ProstheticTraitRules.withoutTraitIds(
				List.of("pegleg", "blind"),
				toRemove);
		assertEquals(List.of("pegleg", "blind"), kept);
	}

	@Test
	void noProstheticLeavesTraitsUnchanged() {
		Set<String> toRemove = ProstheticTraitRules.injuriesSupersededByProsthetics(
				List.of("one_handed", "blind"),
				LOOKUP);
		assertTrue(toRemove.isEmpty());
		List<String> kept = ProstheticTraitRules.withoutTraitIds(
				List.of("one_handed", "blind"),
				toRemove);
		assertEquals(List.of("one_handed", "blind"), kept);
	}
}
