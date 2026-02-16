package top.mc_plfd_host.ezobserver.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import top.mc_plfd_host.ezobserver.EzObserver;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessageManager {

    private final EzObserver plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;
    private Map<String, String> messages;
    private final MiniMessage miniMessage;

    public MessageManager(EzObserver plugin) {
        this.plugin = plugin;
        this.messages = new HashMap<>();
        this.miniMessage = MiniMessage.miniMessage();
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

    private String convertLegacyToMiniMessage(String legacy) {
        if (legacy == null) return "";
        return legacy
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&k", "<obfuscated>")
                .replace("&l", "<b>")
                .replace("&m", "<strikethrough>")
                .replace("&n", "<u>")
                .replace("&o", "<i>")
                .replace("&r", "<reset>");
    }

    public Component getMessage(String key) {
        String message = messages.getOrDefault(key, "Missing message: " + key);
        return miniMessage.deserialize(convertLegacyToMiniMessage(message));
    }

    public Component getMessage(String key, Map<String, String> placeholders) {
        String message = messages.getOrDefault(key, "Missing message: " + key);
        List<TagResolver> resolvers = new ArrayList<>();
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            resolvers.add(Placeholder.parsed(entry.getKey(), convertLegacyToMiniMessage(entry.getValue())));
        }
        return miniMessage.deserialize(convertLegacyToMiniMessage(message), TagResolver.resolver(resolvers));
    }

    public Component getPrefix() {
        return getMessage("prefix").append(Component.text(" "));
    }

    public Component getNoPermission() {
        return getPrefix().append(getMessage("no-permission"));
    }

    public Component getConfigReloaded() {
        return getPrefix().append(getMessage("config-reloaded"));
    }

    public Component getPlayerOnly() {
        return getPrefix().append(getMessage("player-only"));
    }

    public Component getBroadcastMessage(String mode) {
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

    public Component getScanAlreadyRunning() {
        return getPrefix().append(getMessage("scan-already-running"));
    }

    public Component getScanStarted() {
        return getPrefix().append(getMessage("scan-started"));
    }

    public Component getScanWarningFolia() {
        return getMessage("scan-warning-folia");
    }

    public Component getScanSkippedContainers() {
        return getMessage("scan-skipped-containers");
    }

    public Component getScanError(String error) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("error", error);
        return getPrefix().append(getMessage("scan-error", placeholders));
    }

    public String getStatusOn() {
        return messages.getOrDefault("status-on", "&aOn");
    }

    public String getStatusOff() {
        return messages.getOrDefault("status-off", "&cOff");
    }
}