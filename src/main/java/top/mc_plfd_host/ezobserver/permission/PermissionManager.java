package top.mc_plfd_host.ezobserver.permission;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import top.mc_plfd_host.ezobserver.EzObserver;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PermissionManager {
    
    private final EzObserver plugin;
    private final Map<UUID, PlayerPermissions> playerPermissions = new HashMap<>();
    
    // 权限节点
    public static final String PERMISSION_BYPASS = "ezobserver.bypass";
    public static final String PERMISSION_ADMIN = "ezobserver.admin";
    public static final String PERMISSION_SCAN = "ezobserver.scan";
    public static final String PERMISSION_FIX = "ezobserver.fix";
    public static final String PERMISSION_REPORT = "ezobserver.report";
    public static final String PERMISSION_MONITOR = "ezobserver.monitor";
    public static final String PERMISSION_WHITELIST = "ezobserver.whitelist";
    
    public PermissionManager(EzObserver plugin) {
        this.plugin = plugin;
        registerPermissions();
    }
    
    /**
     * 注册所有权限
     */
    private void registerPermissions() {
        try {
            // 管理员权限
            Bukkit.getPluginManager().addPermission(new Permission(
                PERMISSION_ADMIN,
                "EzObserver 管理员权限 - 所有命令的访问权限",
                PermissionDefault.OP
            ));
            
            // 绕过检测权限
            Bukkit.getPluginManager().addPermission(new Permission(
                PERMISSION_BYPASS,
                "EzObserver 绕过检测权限 - 不会被检测系统检查",
                PermissionDefault.FALSE
            ));
            
            // 扫描权限
            Bukkit.getPluginManager().addPermission(new Permission(
                PERMISSION_SCAN,
                "EzObserver 扫描权限 - 可以扫描物品和世界",
                PermissionDefault.OP
            ));
            
            // 修复权限
            Bukkit.getPluginManager().addPermission(new Permission(
                PERMISSION_FIX,
                "EzObserver 修复权限 - 可以修复违规物品",
                PermissionDefault.OP
            ));
            
            // 报告权限
            Bukkit.getPluginManager().addPermission(new Permission(
                PERMISSION_REPORT,
                "EzObserver 报告权限 - 可以查看报告和统计",
                PermissionDefault.OP
            ));
            
            // 监控权限
            Bukkit.getPluginManager().addPermission(new Permission(
                PERMISSION_MONITOR,
                "EzObserver 监控权限 - 可以管理实时监控系统",
                PermissionDefault.OP
            ));
            
            // 白名单权限
            Bukkit.getPluginManager().addPermission(new Permission(
                PERMISSION_WHITELIST,
                "EzObserver 白名单权限 - 可以管理物品白名单",
                PermissionDefault.OP
            ));
            
        } catch (Exception e) {
            plugin.getLogger().warning("注册权限时出错: " + e.getMessage());
        }
    }
    
    /**
     * 检查玩家是否有特定权限
     */
    public boolean hasPermission(Player player, String permission) {
        if (player == null) return false;
        return player.hasPermission(permission);
    }
    
    /**
     * 检查玩家是否有管理员权限
     */
    public boolean hasAdminPermission(Player player) {
        return hasPermission(player, PERMISSION_ADMIN);
    }
    
    /**
     * 检查玩家是否可以绕过检测
     */
    public boolean canBypass(Player player) {
        return hasPermission(player, PERMISSION_BYPASS) || hasPermission(player, PERMISSION_ADMIN);
    }
    
    /**
     * 获取玩家权限信息
     */
    public PlayerPermissions getPlayerPermissions(Player player) {
        return playerPermissions.computeIfAbsent(player.getUniqueId(), k -> new PlayerPermissions(player));
    }
    
    /**
     * 更新玩家权限缓存
     */
    public void updatePlayerPermissions(Player player) {
        PlayerPermissions permissions = getPlayerPermissions(player);
        permissions.updatePermissions();
    }
    
    /**
     * 移除玩家权限缓存
     */
    public void removePlayerPermissions(UUID playerId) {
        playerPermissions.remove(playerId);
    }
    
    /**
     * 获取所有权限列表
     */
    public Map<String, String> getAllPermissions() {
        Map<String, String> permissions = new HashMap<>();
        permissions.put(PERMISSION_ADMIN, "管理员权限 - 所有命令");
        permissions.put(PERMISSION_BYPASS, "绕过检测权限");
        permissions.put(PERMISSION_SCAN, "扫描权限");
        permissions.put(PERMISSION_FIX, "修复权限");
        permissions.put(PERMISSION_REPORT, "报告权限");
        permissions.put(PERMISSION_MONITOR, "监控权限");
        permissions.put(PERMISSION_WHITELIST, "白名单管理权限");
        return permissions;
    }
    
    /**
     * 清理离线玩家权限缓存
     */
    public void cleanupOfflinePlayers() {
        playerPermissions.entrySet().removeIf(entry -> 
            Bukkit.getPlayer(entry.getKey()) == null
        );
    }
    
    /**
     * 玩家权限信息类
     */
    public static class PlayerPermissions {
        private final Player player;
        private final Map<String, Boolean> permissions = new HashMap<>();
        
        public PlayerPermissions(Player player) {
            this.player = player;
            updatePermissions();
        }
        
        public void updatePermissions() {
            permissions.clear();
            permissions.put(PERMISSION_ADMIN, player.hasPermission(PERMISSION_ADMIN));
            permissions.put(PERMISSION_BYPASS, player.hasPermission(PERMISSION_BYPASS));
            permissions.put(PERMISSION_SCAN, player.hasPermission(PERMISSION_SCAN));
            permissions.put(PERMISSION_FIX, player.hasPermission(PERMISSION_FIX));
            permissions.put(PERMISSION_REPORT, player.hasPermission(PERMISSION_REPORT));
            permissions.put(PERMISSION_MONITOR, player.hasPermission(PERMISSION_MONITOR));
            permissions.put(PERMISSION_WHITELIST, player.hasPermission(PERMISSION_WHITELIST));
        }
        
        public boolean hasPermission(String permission) {
            return permissions.getOrDefault(permission, false);
        }
        
        public Map<String, Boolean> getAllPermissions() {
            return new HashMap<>(permissions);
        }
    }
}