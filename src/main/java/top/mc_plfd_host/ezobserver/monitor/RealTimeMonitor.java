package top.mc_plfd_host.ezobserver.monitor;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import top.mc_plfd_host.ezobserver.EzObserver;
import top.mc_plfd_host.ezobserver.checker.ItemChecker;
import top.mc_plfd_host.ezobserver.config.ConfigManager;
import top.mc_plfd_host.ezobserver.util.FoliaUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RealTimeMonitor {
    
    private final EzObserver plugin;
    private final ConfigManager configManager;
    private final ItemChecker itemChecker;
    private final Map<UUID, Integer> playerViolationCount = new ConcurrentHashMap<>();
    private final Map<UUID, List<String>> playerViolationHistory = new ConcurrentHashMap<>();
    private volatile boolean monitoringEnabled = true;
    private final Object monitoringLock = new Object();
    private volatile int scanInterval = 20; // 每秒扫描一次
    
    public RealTimeMonitor(EzObserver plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.itemChecker = new ItemChecker(plugin);
    }
    
    /**
     * 开始实时监控
     */
    public void startMonitoring() {
        if (!configManager.isRealTimeMonitoringEnabled()) {
            return;
        }
        
        scanInterval = configManager.getRealTimeScanInterval();
        
        // 使用Folia兼容的调度器
        if (FoliaUtil.isFolia()) {
            plugin.getServer().getGlobalRegionScheduler().runAtFixedRate(plugin, task -> {
                synchronized (monitoringLock) {
                    if (!monitoringEnabled) {
                        task.cancel();
                        return;
                    }
                }
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player != null && player.isOnline()) {
                        // 使用EntityScheduler进行玩家相关操作
                        player.getScheduler().run(plugin, playerTask -> scanPlayerInventory(player), null);
                    }
                }
            }, 20L, scanInterval);
        } else {
            // 兼容旧版本
            plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, () -> {
                synchronized (monitoringLock) {
                    if (!monitoringEnabled) return;
                }
                
                for (Player player : Bukkit.getOnlinePlayers()) {
                    scanPlayerInventory(player);
                }
            }, 0L, scanInterval);
        }
    }
    
    /**
     * 扫描玩家背包
     */
    private void scanPlayerInventory(Player player) {
        if (player == null || !player.isOnline()) return;
        
        UUID playerId = player.getUniqueId();
        int violationCount = 0;
        List<String> currentViolations = new ArrayList<>();
        
        // 扫描背包
        for (ItemStack item : player.getInventory().getContents()) {
            if (item == null || item.getType() == Material.AIR) continue;
            
            List<String> itemViolations = itemChecker.checkItem(item);
            if (!itemViolations.isEmpty()) {
                violationCount++;
                currentViolations.addAll(itemViolations);
                
                // 记录违规
                recordViolation(playerId, itemViolations);
                
                // 自动修复
                if (configManager.isAutoFixEnabled()) {
                    handleAutoFix(player, item, itemViolations);
                }
            }
        }
        
        // 更新违规计数
        if (violationCount > 0) {
            playerViolationCount.merge(playerId, violationCount, Integer::sum);
            playerViolationHistory.computeIfAbsent(playerId, k -> new ArrayList<>())
                .addAll(currentViolations);
            
            // 限制历史记录大小
            limitHistorySize(playerId);
        }
    }
    
    /**
     * 自动修复违规物品
     */
    private void handleAutoFix(Player player, ItemStack item, List<String> violations) {
        if (configManager.isAutoDeleteEnabled()) {
            // 删除违规物品
            player.getInventory().remove(item);
            plugin.getLogger().info("自动删除玩家 " + player.getName() + " 的违规物品: " + violations);
        } else if (configManager.isAutoFixEnabled()) {
            // 修复违规物品
            // 这里可以添加具体的修复逻辑
            plugin.getLogger().info("检测到玩家 " + player.getName() + " 的违规物品: " + violations);
        }
    }
    
    /**
     * 记录违规
     */
    private void recordViolation(UUID playerId, List<String> violations) {
        // 可以在这里添加数据库记录或文件记录
        plugin.getLogger().info("玩家 " + playerId + " 违规: " + violations);
    }
    
    /**
     * 限制历史记录大小
     */
    private void limitHistorySize(UUID playerId) {
        List<String> history = playerViolationHistory.get(playerId);
        if (history != null && history.size() > 100) {
            synchronized (history) {
                if (history.size() > 100) {
                    history.subList(0, history.size() - 100).clear();
                }
            }
        }
    }
    
    /**
     * 获取玩家违规统计
     */
    public Map<String, Object> getPlayerStats(UUID playerId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalViolations", playerViolationCount.getOrDefault(playerId, 0));
        stats.put("recentViolations", playerViolationHistory.getOrDefault(playerId, new ArrayList<>()));
        return stats;
    }
    
    /**
     * 获取服务器统计
     */
    public Map<String, Object> getServerStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPlayers", playerViolationCount.size());
        stats.put("totalViolations", playerViolationCount.values().stream().mapToInt(Integer::intValue).sum());
        synchronized (monitoringLock) {
            stats.put("monitoringEnabled", monitoringEnabled);
        }
        return stats;
    }
    
    /**
     * 停止监控
     */
    public void stopMonitoring() {
        synchronized (monitoringLock) {
            monitoringEnabled = false;
        }
        
        // 清理数据
        playerViolationCount.clear();
        playerViolationHistory.clear();
        
        plugin.getLogger().info("实时监控已停止");
    }
    
    /**
     * 获取违规排行榜
     */
    public List<Map.Entry<UUID, Integer>> getViolationLeaderboard() {
        return playerViolationCount.entrySet().stream()
            .sorted(Map.Entry.<UUID, Integer>comparingByValue().reversed())
            .limit(10)
            .toList();
    }
}