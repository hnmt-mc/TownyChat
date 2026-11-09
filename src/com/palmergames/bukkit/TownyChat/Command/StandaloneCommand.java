package com.palmergames.bukkit.TownyChat.command;

import com.palmergames.bukkit.TownyChat.Chat;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

/**
 * Towny を入れていないサーバー向けのコマンド。
 * チャンネルは全体だけなので、切り替えは案内だけを返す。
 */
public class StandaloneCommand implements CommandExecutor {
	private final Chat plugin;

	public StandaloneCommand(Chat plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (command.getName().equalsIgnoreCase("channel")) {
			sender.sendMessage(ChatColor.GRAY + "このサーバーでは全体チャットだけを使えます。");
			return true;
		}

		if (args.length == 0 || args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?")) {
			sender.sendMessage(ChatColor.GOLD + "--- TownyChat ---");
			sender.sendMessage(ChatColor.YELLOW + "/townychat reload" + ChatColor.GRAY + " 設定を読み直します。");
			return true;
		}

		if (args[0].equalsIgnoreCase("reload")) {
			plugin.reload();
			sender.sendMessage(ChatColor.GREEN + "TownyChat の設定を読み直しました。");
			return true;
		}

		sender.sendMessage(ChatColor.RED + "使い方: /townychat reload");
		return true;
	}
}
