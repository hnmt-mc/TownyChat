package com.palmergames.bukkit.TownyChat.util;

import com.palmergames.bukkit.TownyChat.listener.ChatClipboardListener;
import io.papermc.paper.chat.ChatRenderer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ChatClipboardTest {
	private static Player player() {
		return (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
				(proxy, method, args) -> switch (method.getName()) {
					case "hashCode" -> System.identityHashCode(proxy);
					case "equals" -> proxy == args[0];
					default -> throw new AssertionError("Unexpected Bukkit access: " + method.getName());
				});
	}
	private static void assertClick(Component component, String text) { assertClick(component, text, null); }
	private static void assertClick(Component component, String text, ClickEvent inherited) {
		var effective = component.clickEvent() == null ? inherited : component.clickEvent();
		assertEquals(ClickEvent.copyToClipboard(text), effective);
		component.children().forEach(child -> assertClick(child, text, effective));
		if (component instanceof TranslatableComponent translated) {
			translated.arguments().forEach(argument -> { if (argument.value() instanceof Component child) assertClick(child, text, effective); });
		}
	}
	@Test void copiesWholeChannelLineWithoutColorCodesAndKeepsHoverAndStyle() {
		var hover = HoverEvent.showText(Component.text("Town information"));
		var original = Component.text("[Global] ", NamedTextColor.AQUA)
				.append(Component.text("humisan", NamedTextColor.GREEN).hoverEvent(hover).clickEvent(ClickEvent.suggestCommand("/msg humisan ")))
				.append(Component.text(" » 日本語のチャット ✨ 100%", NamedTextColor.GRAY));
		var copied = ChatClipboard.copyable(original);
		assertClick(copied, "[Global] humisan » 日本語のチャット ✨ 100%");
		assertTrue(copied.children().stream().allMatch(child -> child.clickEvent() == null));
		assertEquals(original.color(), copied.color()); assertEquals(hover, copied.children().get(0).hoverEvent());
		assertEquals(original.children().get(0).color(), copied.children().get(0).color());
		assertEquals(PlainTextComponentSerializer.plainText().serialize(original), PlainTextComponentSerializer.plainText().serialize(copied));
		assertEquals(ClickEvent.suggestCommand("/msg humisan "), original.children().get(0).clickEvent());
	}
	@Test void vanillaChatCopiesItsDisplayedFormatAndTranslationArgumentsGetTheSameClick() {
		var original = Component.translatable("chat.type.text", Component.text("humisan"), Component.text("hello"));
		var copied = ChatClipboard.copyable(original);
		assertClick(copied, "<humisan> hello");
		assertEquals("chat.type.text", ((TranslatableComponent) copied).key());
	}
	@Test void numericTranslationArgumentsArePreserved() {
		var original = Component.translatable("test", TranslationArgument.numeric(42), Component.text("text"));
		var copied = (TranslatableComponent) ChatClipboard.copyable(original);
		assertEquals(42, copied.arguments().get(0).value());
	}
	@Test void preservesPerViewerRenderingAndDoesNotChangeConsoleOutput() {
		Player first = player(), second = player(); var calls = new AtomicInteger();
		ChatRenderer original = (source, name, message, viewer) -> {
			calls.incrementAndGet(); return Component.text(viewer == first ? "[Town] humisan: hello" : "[Nation] humisan: hello");
		};
		var wrapped = ChatClipboardListener.withClipboard(original);
		assertClick(wrapped.render(first, Component.text("humisan"), Component.text("hello"), first), "[Town] humisan: hello");
		assertClick(wrapped.render(first, Component.text("humisan"), Component.text("hello"), second), "[Nation] humisan: hello");
		assertNull(wrapped.render(first, Component.text("humisan"), Component.text("hello"), Audience.empty()).clickEvent());
		assertEquals(3, calls.get());
	}
	@Test void listenerDoesNotChangeCancelledChatsOrTheirRecipients() {
		Player source = player(); var viewers = new java.util.HashSet<Audience>(); viewers.add(source);
		ChatRenderer original = (sender, name, message, viewer) -> message;
		var event = new io.papermc.paper.event.player.AsyncChatEvent(true, source, viewers, original,
				Component.text("hello"), Component.text("hello"), net.kyori.adventure.chat.SignedMessage.system("hello", Component.text("hello")));
		event.setCancelled(true); new ChatClipboardListener().onChat(event);
		assertSame(original, event.renderer()); assertEquals(java.util.Set.of(source), event.viewers()); assertTrue(event.isCancelled());
	}
	@Test void listenerDecoratesTheExistingRendererWithoutEditingMessageOrRecipients() {
		Player source = player(); var viewers = new java.util.HashSet<Audience>(); viewers.add(source);
		Component message = Component.text("hello");
		ChatRenderer original = (sender, name, body, viewer) -> Component.text("[Town] ").append(name).append(Component.text(": ")).append(body);
		var event = new io.papermc.paper.event.player.AsyncChatEvent(true, source, viewers, original,
				message, message, net.kyori.adventure.chat.SignedMessage.system("hello", Component.text("hello")));
		new ChatClipboardListener().onChat(event);
		assertEquals(message, event.message()); assertEquals(java.util.Set.of(source), event.viewers()); assertFalse(event.isCancelled());
		assertClick(event.renderer().render(source, Component.text("humisan"), message, source), "[Town] humisan: hello");
	}

	@Test void clipboardContentCannotExecuteCommandsOrParsePlayerMarkup() {
		var original = Component.text("[g] humisan: /op humisan <click:run_command:/op humisan> &a");
		var copied = ChatClipboard.copyable(original);
		assertEquals(ClickEvent.Action.COPY_TO_CLIPBOARD, copied.clickEvent().action());
		assertEquals("[g] humisan: /op humisan <click:run_command:/op humisan> &a", copied.clickEvent().value());
	}
}
