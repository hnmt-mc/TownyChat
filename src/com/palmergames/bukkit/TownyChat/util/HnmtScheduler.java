package com.palmergames.bukkit.TownyChat.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

/**
 * Towny に依存しないスケジューラ。Paper では Bukkit のスケジューラ、
 * Folia ではリフレクションでリージョン単位のスケジューラを使う。
 * <p>
 * Towny の {@code TaskScheduler} を使わないことで、Towny を入れていない
 * サーバーでも TownyChat を起動できる。
 */
public final class HnmtScheduler {
	private final Plugin plugin;
	private final boolean folia;

	public HnmtScheduler(Plugin plugin) {
		this.plugin = plugin;
		this.folia = classExists("io.papermc.paper.threadedregions.RegionizedServer");
	}

	/**
	 * 次の tick でタスクを実行する。
	 */
	public void run(Runnable task) {
		if (folia) {
			runGlobal(task);
			return;
		}
		Bukkit.getScheduler().runTask(plugin, task);
	}

	/**
	 * 指定 tick 後にタスクを実行する。エンティティの所属スレッドで動かす。
	 */
	public void runLater(Entity entity, Runnable task, long delay) {
		if (folia) {
			runEntityLater(entity, task, delay);
			return;
		}
		Bukkit.getScheduler().runTaskLater(plugin, task, delay);
	}

	/**
	 * エンティティの所属スレッドでタスクを実行する。
	 */
	public void run(Entity entity, Runnable task) {
		if (folia) {
			runEntity(entity, task);
			return;
		}
		Bukkit.getScheduler().runTask(plugin, task);
	}

	/**
	 * エンティティの処理スレッド上にいるかを返す。
	 */
	public boolean isEntityThread(Entity entity) {
		if (!folia) {
			return Bukkit.isPrimaryThread();
		}
		try {
			return (boolean) Bukkit.class.getMethod("isOwnedByCurrentRegion", Entity.class).invoke(null, entity);
		} catch (ReflectiveOperationException e) {
			// 判定できないときは、安全側で「別スレッド」とみなす。
			return false;
		}
	}

	private void runGlobal(Runnable task) {
		try {
			Object scheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
			var consumer = new java.util.function.Consumer<Object>() {
				@Override
				public void accept(Object scheduledTask) {
					task.run();
				}
			};
			scheduler.getClass()
				.getMethod("run", Plugin.class, java.util.function.Consumer.class)
				.invoke(scheduler, plugin, consumer);
		} catch (ReflectiveOperationException e) {
			// Folia の API が変わっていたら、ログに残して Bukkit の経路へ落とす。
			plugin.getLogger().warning("Folia のグローバルスケジューラを呼べませんでした: " + e);
			Bukkit.getScheduler().runTask(plugin, task);
		}
	}

	private void runEntityLater(Entity entity, Runnable task, long delay) {
		try {
			Object scheduler = Entity.class.getMethod("getScheduler").invoke(entity);
			var consumer = new java.util.function.Consumer<Object>() {
				@Override
				public void accept(Object scheduledTask) {
					task.run();
				}
			};
			scheduler.getClass()
				.getMethod("runDelayed", Plugin.class, java.util.function.Consumer.class, Runnable.class, long.class)
				.invoke(scheduler, plugin, consumer, (Runnable) () -> {}, delay);
		} catch (ReflectiveOperationException e) {
			plugin.getLogger().warning("Folia のエンティティスケジューラを呼べませんでした: " + e);
			Bukkit.getScheduler().runTaskLater(plugin, task, delay);
		}
	}

	private void runEntity(Entity entity, Runnable task) {
		try {
			Object scheduler = Entity.class.getMethod("getScheduler").invoke(entity);
			var consumer = new java.util.function.Consumer<Object>() {
				@Override
				public void accept(Object scheduledTask) {
					task.run();
				}
			};
			scheduler.getClass()
				.getMethod("run", Plugin.class, java.util.function.Consumer.class, Runnable.class)
				.invoke(scheduler, plugin, consumer, (Runnable) () -> {});
		} catch (ReflectiveOperationException e) {
			plugin.getLogger().warning("Folia のエンティティスケジューラを呼べませんでした: " + e);
			Bukkit.getScheduler().runTask(plugin, task);
		}
	}

	private static boolean classExists(String name) {
		try {
			Class.forName(name);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
}
