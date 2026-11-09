package com.palmergames.bukkit.TownyChat.util;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.TownyChat.Chat;

public class EssentialsIntegration {

	public static boolean ignoredByEssentials(Player sender, Player player) {
		com.earth2me.essentials.Essentials essentials =
			(com.earth2me.essentials.Essentials) Chat.getTownyChat().getEssentials();
		com.earth2me.essentials.User targetUser = essentials.getUser(player);
		com.earth2me.essentials.User senderUser = essentials.getUser(sender);
		return targetUser.isIgnoredPlayer(senderUser);
	}

	public static boolean isMuted(Player player) {
		com.earth2me.essentials.Essentials essentials =
			(com.earth2me.essentials.Essentials) Chat.getTownyChat().getEssentials();
		return essentials.getUser(player).isMuted();
	}
}
