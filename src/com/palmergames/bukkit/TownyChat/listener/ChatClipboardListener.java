package com.palmergames.bukkit.TownyChat.listener;

import com.palmergames.bukkit.TownyChat.util.ChatClipboard;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class ChatClipboardListener implements Listener {
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onChat(AsyncChatEvent event) {
		if (event.isCancelled()) return;
		event.renderer(withClipboard(event.renderer()));
	}

	public static ChatRenderer withClipboard(ChatRenderer original) {
		return (source, sourceDisplayName, message, viewer) -> {
			var rendered = original.render(source, sourceDisplayName, message, viewer);
			return viewer instanceof Player ? ChatClipboard.copyable(rendered) : rendered;
		};
	}
}
