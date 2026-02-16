package top.mc_plfd_host.ezobserver;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import top.mc_plfd_host.ezobserver.command.EzObserverCommand;
import top.mc_plfd_host.ezobserver.config.ConfigManager;
import top.mc_plfd_host.ezobserver.config.EnchantmentConflictManager;
import top.mc_plfd_host.ezobserver.config.MessageManager;
import top.mc_plfd_host.ezobserver.config.PotionEffectLimitManager;
import top.mc_plfd_host.ezobserver.config.WhitelistManager;
import top.mc_plfd_host.ezobserver.listener.ItemMoveListener;
import top.mc_plfd_host.ezobserver.listener.PlayerEffectListener;
import top.mc_plfd_host.ezobserver.monitor.RealTimeMonitor;
import top.mc_plfd_host.ezobserver.permission.PermissionManager;
import top.mc_plfd_host.ezobserver.report.ReportManager;
import top.mc_plfd_host.ezobserver.scanner.WorldScanner;
import top.mc_plfd_host.ezobserver.util.FoliaUtil;

public class EzObserver extends JavaPlugin {

    private static EzObserver instance;
    private ConfigManager configManager;
    private EnchantmentConflictManager enchantmentConflictManager;
    private PotionEffectLimitManager potionEffectLimitManager;
    private MessageManager messageManager;
    private WorldScanner worldScanner;
    private WhitelistManager whitelistManager;
    private PermissionManager permissionManager;
    private RealTimeMonitor realTimeMonitor;
    private ReportManager reportManager;
    private BukkitAudiences adventure;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize adventure platform
        this.adventure = BukkitAudiences.create(this);
        
        // Initialize config manager
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // Initialize enchantment conflict manager
        enchantmentConflictManager = new EnchantmentConflictManager(configManager);
        
        // Initialize potion effect limit manager
        potionEffectLimitManager = new PotionEffectLimitManager();
        
        // Initialize message manager
        messageManager = new MessageManager(this);
        messageManager.loadMessages();
        
        // Initialize whitelist manager
        whitelistManager = new WhitelistManager(this);
        
        // Initialize permission manager
        permissionManager = new PermissionManager(this);
        
        // Initialize real-time monitor
        realTimeMonitor = new RealTimeMonitor(this);
        realTimeMonitor.startMonitoring();
        
        // Initialize report manager
        reportManager = new ReportManager(this, realTimeMonitor);
        
        // Initialize scanner
        worldScanner = new WorldScanner(this);
        
        // Register event listener
        getServer().getPluginManager().registerEvents(new ItemMoveListener(this), this);
        
        // Register player effect listener (only triggers on effect updates for high performance)
        PlayerEffectListener playerEffectListener = new PlayerEffectListener(this);
        getServer().getPluginManager().registerEvents(playerEffectListener, this);
        
        // Register command
        EzObserverCommand commandExecutor = new EzObserverCommand(this);
        PluginCommand command = getCommand("ezobserver");
        if (command != null) {
            command.setExecutor(commandExecutor);
            command.setTabCompleter(commandExecutor);
        }
        
        // 显示服务器类型和 Folia 兼容性信息
        String serverType = FoliaUtil.getServerType();
        boolean isFolia = FoliaUtil.isFolia();
        
        getLogger().info("EzObserver enabled - Author: Kush_ShuL");
        getLogger().info("Server type: " + serverType + (isFolia ? " (Folia compatible mode)" : ""));
        
        if (isFolia) {
            getLogger().info("Folia detected! Using region-based scheduling for thread safety.");
        }
        
        // 显示高级功能启用状态
        getLogger().info("Advanced features loaded:");
        getLogger().info("- Real-time monitoring: " + (configManager.isRealTimeMonitoringEnabled() ? "Enabled" : "Disabled"));
        getLogger().info("- Auto-fix: " + (configManager.isAutoFixEnabled() ? "Enabled" : "Disabled"));
        getLogger().info("- Permission system: Active");
        getLogger().info("- Report system: Active");
    }

    @Override
    public void onDisable() {
        try {
            // 清理实时监控器
            if (realTimeMonitor != null) {
                try {
                    realTimeMonitor.stopMonitoring();
                } catch (Exception e) {
                    getLogger().warning("停止实时监控器时发生错误: " + e.getMessage());
                }
            }
            
            // 清理权限管理器资源
            if (permissionManager != null) {
                try {
                    permissionManager.cleanupOfflinePlayers();
                } catch (Exception e) {
                    getLogger().warning("清理权限管理器时发生错误: " + e.getMessage());
                }
            }
            
            // 清理报告管理器
            // 报告管理器无需特殊清理
            
            // 清理世界扫描器
            // 世界扫描器无需特殊清理
            
            // 清理白名单管理器
            // 白名单管理器无需特殊清理
            
            // 清理Adventure资源
            if (this.adventure != null) {
                try {
                    this.adventure.close();
                } catch (Exception e) {
                    getLogger().warning("关闭Adventure时发生错误: " + e.getMessage());
                } finally {
                    this.adventure = null;
                }
            }
            
            // 清理静态引用以防止内存泄漏
            instance = null;
            
            getLogger().info("EzObserver disabled successfully");
        } catch (Exception e) {
            getLogger().severe("插件禁用时发生严重错误: " + e.getMessage());
            getLogger().severe("异常详情: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }

    public BukkitAudiences adventure() {
        if (this.adventure == null) {
            throw new IllegalStateException("Tried to access Adventure when the plugin was disabled!");
        }
        return this.adventure;
    }

    public static EzObserver getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public WorldScanner getWorldScanner() {
        return worldScanner;
    }

    public EnchantmentConflictManager getEnchantmentConflictManager() {
        return enchantmentConflictManager;
    }

    public PotionEffectLimitManager getPotionEffectLimitManager() {
        return potionEffectLimitManager;
    }

    public WhitelistManager getWhitelistManager() {
        return whitelistManager;
    }

    public PermissionManager getPermissionManager() {
        return permissionManager;
    }

    public RealTimeMonitor getRealTimeMonitor() {
        return realTimeMonitor;
    }

    public ReportManager getReportManager() {
        return reportManager;
    }
}