package com.palmergames.bukkit.TownyChat.standalone;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StandaloneFormatTest {

	@Test
	void replacesPlayerNameAndMessage() {
		String out = StandaloneFormat.apply("&f{playername} &7» &f{msg}",
			Map.of("playername", "humitan"));
		assertEquals("&fhumitan &7» &f%2$s", out);
	}

	@Test
	void letsBukkitFillDisplayName() {
		String out = StandaloneFormat.apply("{modplayername}: {msg}", Map.of());
		assertEquals("%1$s: %2$s", out);
	}

	@Test
	void removesUnknownTownyTags() {
		String out = StandaloneFormat.apply("[{town}][{nation}]{playername} {msg}",
			Map.of("playername", "humitan"));
		assertEquals("[][]humitan %2$s", out);
	}

	@Test
	void removesStrayPercentSignsSoFormatCannotBreak() {
		String out = StandaloneFormat.apply("100% {playername} %d {msg}", Map.of("playername", "a"));
		assertEquals("100 a d %2$s", out);
	}

	@Test
	void keepsMessageAndDisplayPlaceholders() {
		String out = StandaloneFormat.apply("%1$s %2$s", Map.of());
		assertEquals("%1$s %2$s", out);
	}

	@Test
	void removesExplicitThirdPlaceholder() {
		String out = StandaloneFormat.apply("%1$s %3$s %2$s", Map.of());
		assertEquals("%1$s  %2$s", out);
	}

	@Test
	void listsTagsForValidation() {
		assertTrue(StandaloneFormat.tags("{playername} {msg} {town}")
			.containsAll(java.util.Set.of("playername", "msg", "town")));
	}
}
