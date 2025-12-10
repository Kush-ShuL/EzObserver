package top.mc_plfd_host.ezobserver;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import top.mc_plfd_host.ezobserver.command.EzObserverCommand;
import top.mc_plfd_host.ezobserver.config.ConfigManager;
import top.mc_plfd_host.ezobserver.config.MessageManager;
import top.mc_plfd_host.ezobserver.listener.ItemMoveListener;
import top.mc_plfd_host.ezobserver.scanner.WorldScanner;

public class EzObserver extends JavaPlugin {

    private static EzObserver instance;
    private ConfigManager configManager;
    private MessageManager messageManager;
    private WorldScanner worldScanner;

    @Override
    public void onEnable() {
        instance = this;
        
        // 初始化配置管理器
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // 初始化消息管理器
        messageManager = new MessageManager(this);
        messageManager.loadMessages();
        
        // 初始化扫描器
        worldScanner = new WorldScanner(this);
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new ItemMoveListener(this), this);
        
        // 注册命令
        EzObserverCommand commandExecutor = new EzObserverCommand(this);
        PluginCommand command = getCommand("ezobserver");
        if (command != null) {
            command.setExecutor(commandExecutor);
            command.setTabCompleter(commandExecutor);
        }
        
        getLogger().info("EzObserver 已启用 - 作者: Kush_ShuL");
    }

    @Override
    public void onDisable() {
        getLogger().info("EzObserver 已禁用");
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
}