package com.palmergames.bukkit.TownyChat;

import com.palmergames.bukkit.TownyChat.Command.ChannelCommand;
import com.palmergames.bukkit.TownyChat.Command.TownyChatCommand;
import com.palmergames.bukkit.TownyChat.Command.commandobjects.ChannelJoinAliasCommand;
import com.palmergames.bukkit.TownyChat.channels.Channel;
import com.palmergames.bukkit.TownyChat.channels.ChannelsHolder;
import com.palmergames.bukkit.TownyChat.command.StandaloneCommand;
import com.palmergames.bukkit.TownyChat.config.ChatSettings;
import com.palmergames.bukkit.TownyChat.config.ChannelsSettings;
import com.palmergames.bukkit.TownyChat.listener.EssentialsDiscordHookListener;
import com.palmergames.bukkit.TownyChat.listener.TownyChatPlayerListener;
import com.palmergames.bukkit.TownyChat.listener.ChatClipboardListener;
import com.palmergames.bukkit.TownyChat.standalone.StandaloneChat;
import com.palmergames.bukkit.TownyChat.tasks.onLoadedTask;
import com.palmergames.bukkit.TownyChat.util.EssentialsIntegration;
import com.palmergames.bukkit.TownyChat.util.HnmtScheduler;
import com.palmergames.bukkit.TownyChat.util.PAPIPlaceholderExpansion;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Reference Chat Plugin for Towny
 * <p>
 * Towny が無いサーバーでも起動できる。その場合は全体チャットだけを扱う
 * （{@link StandaloneChat}）。
 *
 * @link <a href="https://townyadvanced.github.io">TownyAdvanced</a>
 * @link <a href="https://github.com/TownyAdvanced/TownyChat">Source Code (GitHub)</a>
 * @link <a href="https://creativecommons.org/licenses/by-nc-nd/3.0/">License (CC BY-NC-ND 3.0)</a>
 * @author ElgarL
 * @author LlmDl
 */

public class Chat extends JavaPlugin {

	private TownyChatPlayerListener TownyPlayerListener;
	private ChannelsHolder channels;

	protected PluginManager pm;
	private static Chat chat = null;
	private final HnmtScheduler scheduler;
	private Plugin townyPlugin = null;
	private Object dynMap = null;
	private Object essentials = null;
	private Object papiexpansion;
	private StandaloneChat standalone = null;

	private static final String requiredTownyVersion = "0.103.0.0";
	public static boolean usingPlaceholderAPI = false;
	public static boolean usingEssentialsDiscord = false;
	boolean chatConfigError = false;
	boolean channelsConfigError = false;
	private static ConcurrentMap<UUID, Channel> playerChannelMap;

	public Chat() {
		chat = this;
		this.scheduler = new HnmtScheduler(this);
	}

	@Override
	public void onEnable() {
		pm = getServer().getPluginManager();
		playerChannelMap = new ConcurrentHashMap<>();

		checkPlugins();
		if (townyPlugin != null && !townyVersionCheck()) {
			disableWithMessage("Towny version does not meet required minimum version: " + requiredTownyVersion);
			return;
		}
		if (hasTowny()) {
			getLogger().info("Towny version " + townyPlugin.getPluginMeta().getVersion() + " found.");
		}

		loadConfigs();
		if (channelsConfigError || chatConfigError) {
			disableWithMessage("The config could not be loaded.");
			return;
		}

		/*
		 * This executes the task with a 1 tick delay avoiding the bukkit depends bug.
		 * TODO: What bug is this referencing? This goes back to the first version of TownyChat.
		 */
		getScheduler().run(() -> startChat());
	}

	/**
	 * 1 tick 後に、Towny の有無に合わせて機能を有効にする。
	 */
	private void startChat() {
		if (hasTowny()) {
			new onLoadedTask(this).run();
			getCommand("townychat").setExecutor(new TownyChatCommand(this));
			getCommand("channel").setExecutor(new ChannelCommand(this));
			registerObjectCommands();
			return;
		}

		StandaloneCommand command = new StandaloneCommand(this);
		getCommand("townychat").setExecutor(command);
		getCommand("channel").setExecutor(command);
		if (standalone != null) {
			standalone.enable();
		}
	}

	private void disableWithMessage(String message) {
		getLogger().severe(message);
		getLogger().severe("Disabling TownyChat...");
		pm.disablePlugin(this);
	}

	private boolean townyVersionCheck() {
		try {
			Class<?> towny = Class.forName("com.palmergames.bukkit.towny.Towny");
			Method method = towny.getMethod("isTownyVersionSupported", String.class);
			return (boolean) method.invoke(null, requiredTownyVersion);
		} catch (ReflectiveOperationException | LinkageError e) {
			return false;
		}
	}

	private void loadConfigs() {
		File root = hasTowny() ? townyPlugin.getDataFolder() : getDataFolder();
		new File(root, "settings").mkdirs();
		if (hasTowny()) {
			chatConfigError = !ChatSettings.loadCommentedChatConfig();
			channelsConfigError = !ChannelsSettings.loadCommentedChannelsConfig();
			channels = new ChannelsHolder(this);
		} else {
			chatConfigError = !StandaloneChat.load(this);
			channels = new ChannelsHolder(this);
		}
	}

	public static Chat getTownyChat() {
		return chat;
	}

	@Override
	public void onDisable() {
		if (pm != null) {
			HandlerList.unregisterAll(this);
		}
		unregisterPermissions();

		dynMap = null;
		townyPlugin = null;
		pm = null;

		channels = null;
		playerChannelMap = null;
		standalone = null;
	}

	/**
	 * Perform a reload of this plugin.
	 */
	public void reload() {
		onDisable();
		onEnable();
	}

	/**
	 * Attempt to hook any plugins we want to access.
	 */
	private void checkPlugins() {
		Plugin test;

		test = pm.getPlugin("Towny");
		if (test != null)
			townyPlugin = test;

		test = pm.getPlugin("dynmap");
		if (test != null && test.isEnabled()) {
			dynMap = test;
		}

		test = pm.getPlugin("PlaceholderAPI");
		if (test != null) {
			if (papiexpansion != null)
				((PAPIPlaceholderExpansion) papiexpansion).unregister();
			usingPlaceholderAPI = true;
			papiexpansion = new PAPIPlaceholderExpansion(this);
			((PAPIPlaceholderExpansion) papiexpansion).register();
		}

		test = pm.getPlugin("EssentialsDiscord");
		if (test != null) {
			usingEssentialsDiscord = true;
		}

		test = pm.getPlugin("Essentials");
		if (test != null)
			essentials = test;
	}

	public void registerEvents() {

		if (TownyPlayerListener == null) {
			TownyPlayerListener = new TownyChatPlayerListener(this);

			if (TownyPlayerListener != null) {
				pm.registerEvents(TownyPlayerListener, this);
				registerClipboardListener();
			}
		}

		if (usingEssentialsDiscord) {
			pm.registerEvents(new EssentialsDiscordHookListener(this), this);
		}
	}

	public void registerClipboardListener() {
		try {
			Class.forName("io.papermc.paper.event.player.AsyncChatEvent");
		} catch (ClassNotFoundException error) {
			getLogger().info("Chat clipboard support is unavailable; Paper chat API is required");
			return;
		}
		pm.registerEvents(new ChatClipboardListener(), this);
	}

	public void registerPermissions() {
		// Register all Permissions.
		for (String perm : getChannelsHandler().getAllPermissions()) {
			try {
				pm.addPermission(new Permission(perm, new HashMap<String, Boolean>()));
			} catch (IllegalArgumentException e) {
				//permission already registered.
			}
		}
	}

	public void unregisterPermissions() {
		if (channels == null || pm == null)
			return;
		for (String perm : getChannelsHandler().getAllPermissions()) {
			pm.removePermission(new Permission(perm, new HashMap<String, Boolean>()));
		}
	}

	public String getRootPath() {
		return (hasTowny() ? townyPlugin : this).getDataFolder().getPath();
	}

	public String getTownySettingsPath() {
		return getRootPath() + File.separator + "settings";
	}

	public String getChatConfigPath() {
		return getTownySettingsPath() + File.separator + "ChatConfig.yml";
	}

	public String getChannelsConfigPath() {
		return getTownySettingsPath() + File.separator + "Channels.yml";
	}

	/**
	 * @return the channels
	 */
	public ChannelsHolder getChannelsHandler() {
		return channels;
	}

	/**
	 * Towny が入っていれば、そのプラグインを返す。
	 */
	public Plugin getTowny() {
		return townyPlugin;
	}

	/**
	 * このサーバーに Towny が入っているかを返す。
	 */
	public boolean hasTowny() {
		return townyPlugin != null;
	}

	public Object getDynmap() {
		return dynMap;
	}

	public boolean isUsingEssentials() {
		return essentials != null;
	}

	public Object getEssentials() {
		return essentials;
	}

	public boolean isEssentialsMuted(Player player) {
		if (!isUsingEssentials())
			return false;
		return EssentialsIntegration.isMuted(player);
	}

	public boolean isIgnoredByEssentials(Player sender, Player player) {
		if (!isUsingEssentials())
			return false;
		return EssentialsIntegration.ignoredByEssentials(sender, player);
	}

	private void registerObjectCommands() {
		List<Command> commands = new ArrayList<Command>();
		for (Channel channel : channels.getAllChannels().values()) { // All channels
			for (String cmd : channel.getCommands()) { // All Commands of All Channels
				commands.add(new ChannelJoinAliasCommand(cmd, channel, this));
			}
		}
		try {
			final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

			bukkitCommandMap.setAccessible(true);
			CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

			commandMap.registerAll("TownyChat", commands);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public TownyChatPlayerListener getTownyPlayerListener() {
		return TownyPlayerListener;
	}

	public Channel getPlayerChannel(Player player) {
		return playerChannelMap.get(player.getUniqueId());
	}

	public void setPlayerChannel(Player player, Channel channel) {
		playerChannelMap.put(player.getUniqueId(), channel);
	}

	public HnmtScheduler getScheduler() {
		return scheduler;
	}

	/**
	 * スタンドアロン用のチャット処理を覚えておく。
	 */
	public void registerStandalone(StandaloneChat standalone) {
		this.standalone = standalone;
	}
}
