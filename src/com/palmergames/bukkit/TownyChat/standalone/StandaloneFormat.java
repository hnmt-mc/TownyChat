package com.palmergames.bukkit.TownyChat.standalone;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Towny を入れていないサーバー向けのチャット書式。
 * TownyChat の書式タグのうち、町・国に関係しないものだけを置換する。
 * <p>
 * Bukkit の {@code AsyncPlayerChatEvent#setFormat} に渡せる形へ変換するため、
 * {@code {modplayername}} は {@code %1$s}、{@code {msg}} は {@code %2$s} にする。
 */
public final class StandaloneFormat {
	private static final Pattern TAG = Pattern.compile("\\{([a-zA-Z0-9_]+)\\}");

	private StandaloneFormat() {
	}

	/**
	 * 書式を適用する。
	 *
	 * @param template 書式
	 * @param values   {@code playername} などの置換値
	 * @return 置換後の書式。未知のタグは空になる
	 */
	public static String apply(String template, Map<String, String> values) {
		if (template == null)
			return "";
		Matcher matcher = TAG.matcher(template);
		StringBuilder out = new StringBuilder();
		while (matcher.find()) {
			String key = matcher.group(1);
			String replacement = switch (key) {
				case "modplayername" -> "%1$s";
				case "msg" -> "%2$s";
				default -> values.getOrDefault(key, "");
			};
			matcher.appendReplacement(out, Matcher.quoteReplacement(replacement));
		}
		matcher.appendTail(out);
		// TownyChat と同じく、利用者が入れた % で書式が壊れないようにする。
		return out.toString().replace("%3$s", "").replaceAll("%(?![12]\\$s)", "");
	}

	/**
	 * 書式に含まれるタグをすべて取り出す。設定の検査に使う。
	 */
	public static java.util.Set<String> tags(String template) {
		java.util.Set<String> tags = new java.util.LinkedHashSet<>();
		if (template == null)
			return tags;
		Matcher matcher = TAG.matcher(template);
		while (matcher.find())
			tags.add(matcher.group(1));
		return tags;
	}
}
