package top.mc_plfd_host.ezobserver.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;

/**
 * Folia 兼容性工具类
 * 提供检测 Folia 环境和使用区域调度器的方法
 * 
 * @author Kush_ShuL
 */
public class FoliaUtil {
    
    private static Boolean isFolia = null;
    private static Method getSchedulerMethod = null;
    private static Method runMethod = null;
    
    /**
     * 检测当前服务器是否是 Folia
     * Folia 是 Paper 的一个分支，使用区域化多线程
     */
    public static boolean isFolia() {
        if (isFolia == null) {
            try {
                // Folia 特有的类
                Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
                isFolia = true;
            } catch (ClassNotFoundException e) {
                isFolia = false;
            }
        }
        return isFolia;
    }
    
    /**
     * 在实体的区域调度器上执行任务
     * 如果不是 Folia，则使用 Bukkit 调度器
     * 
     * @param plugin 插件实例
     * @param entity 实体
     * @param task 要执行的任务
     */
    public static void runEntityTask(Plugin plugin, Entity entity, Runnable task) {
        if (isFolia()) {
            try {
                // 使用反射调用 Folia 的 entity.getScheduler().run()
                if (getSchedulerMethod == null) {
                    getSchedulerMethod = entity.getClass().getMethod("getScheduler");
                }
                Object scheduler = getSchedulerMethod.invoke(entity);
                
                if (runMethod == null) {
                    // Folia 的 EntityScheduler.run(Plugin, Consumer<ScheduledTask>, Runnable)
                    runMethod = scheduler.getClass().getMethod("run", Plugin.class, 
                        java.util.function.Consumer.class, Runnable.class);
                }
                
                // 执行任务
                runMethod.invoke(scheduler, plugin, (java.util.function.Consumer<Object>) (t) -> task.run(), null);
            } catch (Exception e) {
                // 如果反射失败，回退到同步执行
                plugin.getLogger().warning("Folia 调度器调用失败，回退到同步执行: " + e.getMessage());
                task.run();
            }
        } else {
            // 非 Folia 环境，使用 Bukkit 调度器
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
    
    /**
     * 在全局区域调度器上执行任务
     * 如果不是 Folia，则使用 Bukkit 调度器
     * 
     * @param plugin 插件实例
     * @param task 要执行的任务
     */
    public static void runGlobalTask(Plugin plugin, Runnable task) {
        if (isFolia()) {
            try {
                // 使用反射调用 Folia 的 Bukkit.getGlobalRegionScheduler().run()
                Method getGlobalSchedulerMethod = Bukkit.class.getMethod("getGlobalRegionScheduler");
                Object scheduler = getGlobalSchedulerMethod.invoke(null);
                
                Method runMethod = scheduler.getClass().getMethod("run", Plugin.class, 
                    java.util.function.Consumer.class);
                
                runMethod.invoke(scheduler, plugin, (java.util.function.Consumer<Object>) (t) -> task.run());
            } catch (Exception e) {
                // 如果反射失败，直接执行
                plugin.getLogger().warning("Folia 全局调度器调用失败: " + e.getMessage());
                task.run();
            }
        } else {
            // 非 Folia 环境，使用 Bukkit 调度器
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
    
    /**
     * 在异步线程上执行任务
     * Folia 和非 Folia 环境都支持
     * 
     * @param plugin 插件实例
     * @param task 要执行的任务
     */
    public static void runAsync(Plugin plugin, Runnable task) {
        if (isFolia()) {
            try {
                // Folia 使用 Bukkit.getAsyncScheduler()
                Method getAsyncSchedulerMethod = Bukkit.class.getMethod("getAsyncScheduler");
                Object scheduler = getAsyncSchedulerMethod.invoke(null);
                
                Method runNowMethod = scheduler.getClass().getMethod("runNow", Plugin.class, 
                    java.util.function.Consumer.class);
                
                runNowMethod.invoke(scheduler, plugin, (java.util.function.Consumer<Object>) (t) -> task.run());
            } catch (Exception e) {
                // 如果反射失败，使用标准异步调度
                Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
            }
        } else {
            // 非 Folia 环境
            Bukkit.getScheduler().runTaskAsynchronously(plugin, task);
        }
    }
    
    /**
     * 获取服务器类型描述
     */
    public static String getServerType() {
        if (isFolia()) {
            return "Folia";
        }
        
        // 检测 Paper
        try {
            Class.forName("com.destroystokyo.paper.PaperConfig");
            return "Paper";
        } catch (ClassNotFoundException e) {
            // 不是 Paper
        }
        
        // 检测 Spigot
        try {
            Class.forName("org.spigotmc.SpigotConfig");
            return "Spigot";
        } catch (ClassNotFoundException e) {
            // 不是 Spigot
        }
        
        return "Bukkit";
    }
}