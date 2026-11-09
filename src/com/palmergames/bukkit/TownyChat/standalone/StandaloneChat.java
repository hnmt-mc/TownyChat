package com.palmergames.bukkit.TownyChat.standalone;

import com.palmergames.bukkit.TownyChat.Chat;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Towny を入れていないサーバー向けのチャット処理。
 * 全体チャットだけを扱い、町・国・同盟のチャンネルは使わない。
 * <p>
 * 設定は {@code plugins/TownyChat/standalone.yml} に置く。既存の
 * {@code ChatConfig.yml}（Towny のフォルダにある）は読まない。
 */
public final class StandaloneChat implements Listener {
	private static final String DEFAULT_FORMAT = "&8[&a全体&8] {permprefix}{playername}&f: {msg}";
	private static final String DEFAULT_EXCLAMATION_FORMAT = "&8[&a全体&8] {permprefix}{playername}&f: {msg}";

	private final Chat plugin;
	private final String format;
	private final String exclamationFormat;
	private final EventPriority priority;
	private final boolean showChannelOnJoin;

	private StandaloneChat(Chat plugin, String format, String exclamationFormat,
	                       EventPriority priority, boolean showChannelOnJoin) {
		this.plugin = plugin;
		this.format = format;
		this.exclamationFormat = exclamationFormat;
		this.priority = priority;
		this.showChannelOnJoin = showChannelOnJoin;
	}

	/**
	 * 設定を読み、既定ファイルが無ければ作る。
	 *
	 * @return 読み込みに成功したら true
	 */
	public static boolean load(Chat plugin) {
		File file = new File(plugin.getDataFolder(), "standalone.yml");
		YamlConfiguration config = new YamlConfiguration();
		if (file.exists()) {
			try {
				config.load(file);
			} catch (IOException | org.bukkit.configuration.InvalidConfigurationException e) {
				plugin.getLogger().severe("standalone.yml を読めませんでした: " + e.getMessage());
				return false;
			}
		} else {
			config.set("format", DEFAULT_FORMAT);
			config.set("exclamation-format", DEFAULT_EXCLAMATION_FORMAT);
			config.set("listener-priority", "LOW");
			config.set("show-channel-on-join", true);
			try {
				plugin.getDataFolder().mkdirs();
				config.save(file);
			} catch (IOException e) {
				plugin.getLogger().severe("standalone.yml を作れませんでした: " + e.getMessage());
				return false;
			}
		}

		String format = config.getString("format", DEFAULT_FORMAT);
		String exclamation = config.getString("exclamation-format", DEFAULT_EXCLAMATION_FORMAT);
		EventPriority priority = parsePriority(config.getString("listener-priority", "LOW"));
		boolean showOnJoin = config.getBoolean("show-channel-on-join", true);
		plugin.registerStandalone(new StandaloneChat(plugin, format, exclamation, priority, showOnJoin));
		return true;
	}

	private static EventPriority parsePriority(String raw) {
		if (raw == null)
			return EventPriority.LOW;
		try {
			return EventPriority.valueOf(raw.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			return EventPriority.LOW;
		}
	}

	/**
	 * リスナーを登録する。
	 */
	public void enable() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
		plugin.registerClipboardListener();
		plugin.getLogger().info("Towny が見つからないため、全体チャットだけで起動しました。");
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onChat(AsyncPlayerChatEvent event) {
		if (priority != EventPriority.LOWEST)
			return;
		handle(event);
	}

	/**
	 * 設定した優先度でチャットを処理する。Bukkit のアノテーションは固定値しか
	 * 取れないため、設定と一致するハンドラだけが実際に処理する。
	 */
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onChatLow(AsyncPlayerChatEvent event) {
		if (priority != EventPriority.LOW)
			return;
		handle(event);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onChatNormal(AsyncPlayerChatEvent event) {
		if (priority != EventPriority.NORMAL)
			return;
		handle(event);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onChatHigh(AsyncPlayerChatEvent event) {
		if (priority != EventPriority.HIGH)
			return;
		handle(event);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void onChatHighest(AsyncPlayerChatEvent event) {
		if (priority != EventPriority.HIGHEST)
			return;
		handle(event);
	}

	private void handle(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		if (plugin.isEssentialsMuted(player)) {
			event.setCancelled(true);
			return;
		}

		String message = event.getMessage();
		testColourCodes(event, player);

		boolean forceGlobal = message.startsWith("!");
		if (forceGlobal) {
			event.setMessage(message.substring(1));
			message = event.getMessage();
		}

		Map<String, String> values = buildValues(player, message);
		String template = forceGlobal ? exclamationFormat : format;
		String parsed = Chat.usingPlaceholderAPI ? PlaceholderAPI.setPlaceholders(player, template) : template;
		event.setFormat(ChatColor.translateAlternateColorCodes('&', StandaloneFormat.apply(parsed, values)));
	}

	private Map<String, String> buildValues(Player player, String message) {
		Map<String, String> values = new HashMap<>();
		values.put("playername", player.getName());
		values.put("displayname", player.getDisplayName());
		values.put("worldname", player.getWorld().getName());
		values.put("msg", message);
		if (Chat.usingPlaceholderAPI) {
			values.put("permprefix", PlaceholderAPI.setPlaceholders(player, "%luckperms_prefix%"));
			values.put("group", PlaceholderAPI.setPlaceholders(player, "%luckperms_primary_group_name%"));
		} else {
			values.put("permprefix", "");
			values.put("group", "");
		}
		return values;
	}

	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		if (showChannelOnJoin) {
			event.getPlayer().sendMessage(ChatColor.GRAY + "全体チャットに参加しています。");
		}
	}

	private void testColourCodes(AsyncPlayerChatEvent event, Player player) {
		if ((event.getMessage().contains("&L") || event.getMessage().contains("&l"))
			&& !player.hasPermission("townychat.chat.format.bold"))
			event.setMessage(event.getMessage().replaceAll("&L", "").replaceAll("&l", ""));

		if ((event.getMessage().contains("&O") || event.getMessage().contains("&o"))
			&& !player.hasPermission("townychat.chat.format.italic"))
			event.setMessage(event.getMessage().replaceAll("&O", "").replaceAll("&o", ""));

		if ((event.getMessage().contains("&K") || event.getMessage().contains("&k"))
			&& !player.hasPermission("townychat.chat.format.magic"))
			event.setMessage(event.getMessage().replaceAll("&K", "").replaceAll("&k", ""));

		if ((event.getMessage().contains("&N") || event.getMessage().contains("&n"))
			&& !player.hasPermission("townychat.chat.format.underline"))
			event.setMessage(event.getMessage().replaceAll("&N", "").replaceAll("&n", ""));

		if ((event.getMessage().contains("&M") || event.getMessage().contains("&m"))
			&& !player.hasPermission("townychat.chat.format.strike"))
			event.setMessage(event.getMessage().replaceAll("&M", "").replaceAll("&m", ""));

		if ((event.getMessage().contains("&R") || event.getMessage().contains("&r"))
			&& !player.hasPermission("townychat.chat.format.reset"))
			event.setMessage(event.getMessage().replaceAll("&R", "").replaceAll("&r", ""));

		if (player.hasPermission("townychat.chat.color"))
			event.setMessage(ChatColor.translateAlternateColorCodes('&', event.getMessage()));
	}
}
