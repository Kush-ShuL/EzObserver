package top.mc_plfd_host.ezobserver.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import top.mc_plfd_host.ezobserver.EzObserver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class MessageManager {

    private final EzObserver plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private Map<String, String> messages;

    public MessageManager(EzObserver plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // 加载默认消息
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream));
            messagesConfig.setDefaults(defaultConfig);
        }

        // 缓存所有消息
        messages.clear();
        for (String key : messagesConfig.getKeys(true)) {
            if (messagesConfig.isString(key)) {
                messages.put(key, messagesConfig.getString(key));
            }
        }
    }

    public void reloadMessages() {
        loadMessages();
    }

    public String getMessage(String key) {
        String message = messages.getOrDefault(key, "Missing message: " + key);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public String getMessage(String key, Map<String, String> placeholders) {
        String message = getMessage(key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return message;
    }

    public String getPrefix() {
        return getMessage("prefix") + " ";
    }

    // 便捷方法
    public String getNoPermission() {
        return getPrefix() + getMessage("no-permission");
    }

    public String getConfigReloaded() {
        return getPrefix() + getMessage("config-reloaded");
    }

    public String getPlayerOnly() {
        return getPrefix() + getMessage("player-only");
    }

    public String getBroadcastMessage(String mode) {
        switch (mode.toLowerCase()) {
            case "delete":
                return getMessage("broadcast-delete");
            case "store":
                return getMessage("broadcast-store");
            case "fix":
                return getMessage("broadcast-fix");
            default:
                return getMessage("broadcast-delete");
        }
    }

    public String getScanAlreadyRunning() {
        return getPrefix() + getMessage("scan-already-running");
    }

    public String getScanStarted() {
        return getPrefix() + getMessage("scan-started");
    }

    public String getScanError(String error) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("error", error);
        return getPrefix() + getMessage("scan-error", placeholders);
    }

    public String getStatusOn() {
        return getMessage("status-on");
    }

    public String getStatusOff() {
        return getMessage("status-off");
    }
}