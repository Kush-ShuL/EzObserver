package top.mc_plfd_host.ezobserver;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import top.mc_plfd_host.ezobserver.command.EzObserverCommand;
import top.mc_plfd_host.ezobserver.config.ConfigManager;
import top.mc_plfd_host.ezobserver.config.EnchantmentConflictManager;
import top.mc_plfd_host.ezobserver.config.MessageManager;
import top.mc_plfd_host.ezobserver.listener.ItemMoveListener;
import top.mc_plfd_host.ezobserver.scanner.WorldScanner;

public class EzObserver extends JavaPlugin {

    private static EzObserver instance;
    private ConfigManager configManager;
    private EnchantmentConflictManager enchantmentConflictManager;
    private MessageManager messageManager;
    private WorldScanner worldScanner;

    @Override
    public void onEnable() {
        instance = this;
        
        // Initialize config manager
        configManager = new ConfigManager(this);
        configManager.loadConfig();
        
        // Initialize enchantment conflict manager
        enchantmentConflictManager = new EnchantmentConflictManager(configManager);
        
        // Initialize message manager
        messageManager = new MessageManager(this);
        messageManager.loadMessages();
        
        // Initialize scanner
        worldScanner = new WorldScanner(this);
        
        // Register event listener
        getServer().getPluginManager().registerEvents(new ItemMoveListener(this), this);
        
        // Register command
        EzObserverCommand commandExecutor = new EzObserverCommand(this);
        PluginCommand command = getCommand("ezobserver");
        if (command != null) {
            command.setExecutor(commandExecutor);
            command.setTabCompleter(commandExecutor);
        }
        
        getLogger().info("EzObserver enabled - Author: Kush_ShuL");
    }

    @Override
    public void onDisable() {
        getLogger().info("EzObserver disabled");
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
}