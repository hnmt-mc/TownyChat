package com.palmergames.bukkit.TownyChat.Command;

import com.palmergames.bukkit.TownyChat.Chat;
import com.palmergames.bukkit.towny.TownyAPI;

import java.util.LinkedHashMap;
import java.util.Map;

/** コマンドヘルプの説明文を Towny の翻訳に登録する。 */
final class HelpTranslations {
	private HelpTranslations() {}

	/** 言語ごとの説明文。 */
	static Map<String, Map<String, String>> texts() {
		Map<String, String> en = new LinkedHashMap<>();
		en.put("tc_help_channel_join", "Join a channel");
		en.put("tc_help_channel_leave", "Leave a channel");
		en.put("tc_help_channel_list", "List the channels you can use");
		en.put("tc_help_channel_mute", "Mute a player in a channel");
		en.put("tc_help_channel_unmute", "Unmute a player in a channel");
		en.put("tc_help_channel_mutelist", "Show the muted players of a channel");
		en.put("tc_help_channel_soundtoggle", "Toggle the channel's message sound");
		en.put("tc_help_townychat_reload", "Reload the config");

		Map<String, String> ja = new LinkedHashMap<>();
		ja.put("tc_help_channel_join", "チャンネルに参加する");
		ja.put("tc_help_channel_leave", "チャンネルから退出する");
		ja.put("tc_help_channel_list", "使えるチャンネルの一覧");
		ja.put("tc_help_channel_mute", "チャンネルでプレイヤーをミュートする");
		ja.put("tc_help_channel_unmute", "チャンネルでのミュートを解除する");
		ja.put("tc_help_channel_mutelist", "チャンネルのミュート中のプレイヤー");
		ja.put("tc_help_channel_soundtoggle", "チャンネルの通知音を切り替える");
		ja.put("tc_help_townychat_reload", "設定を読み込み直す");

		Map<String, Map<String, String>> all = new LinkedHashMap<>();
		all.put("en_US", en);
		all.put("ja_JP", ja);
		return all;
	}

	static void register(Chat plugin) {
		TownyAPI.getInstance().addTranslations(plugin, texts());
	}
}
