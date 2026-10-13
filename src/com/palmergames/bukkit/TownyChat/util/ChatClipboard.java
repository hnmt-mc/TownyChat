package com.palmergames.bukkit.TownyChat.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;
import net.kyori.adventure.text.TranslationArgument;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public final class ChatClipboard {
	private ChatClipboard() { }

	public static Component copyable(Component rendered) {
		String text = PlainTextComponentSerializer.plainText().serialize(plainChat(rendered));
		return applyClick(rendered, ClickEvent.copyToClipboard(text));
	}

	private static Component applyClick(Component component, ClickEvent click) {
		Component result = component.clickEvent(click).children(component.children().stream()
				.map(child -> applyClick(child, null)).toList());
		if (result instanceof TranslatableComponent translated) {
			result = translated.arguments(translated.arguments().stream().map(argument ->
					argument.value() instanceof Component child
							? TranslationArgument.component(applyClick(child, null)) : argument).toList());
		}
		return result;
	}

	private static Component plainChat(Component component) {
		// Expand vanilla chat formatting so the clipboard contains text instead of a translation key.
		if (component instanceof TranslatableComponent translated
				&& translated.key().equals("chat.type.text") && translated.arguments().size() == 2
				&& translated.arguments().get(0).value() instanceof Component name
				&& translated.arguments().get(1).value() instanceof Component message) {
			Component result = Component.text("<").append(plainChat(name)).append(Component.text("> ")).append(plainChat(message));
			for (Component child : component.children()) result = result.append(plainChat(child));
			return result;
		}
		return component.children(component.children().stream().map(ChatClipboard::plainChat).toList());
	}
}
