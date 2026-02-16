package top.mc_plfd_host.ezobserver.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import top.mc_plfd_host.ezobserver.EzObserver;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class WhitelistManager {
    private final EzObserver plugin;
    private final Set<String> whitelistedItems = new HashSet<>();
    private boolean enabled = true;
    private String mode = "strict";
    private final ConcurrentHashMap<String, Boolean> cache = new ConcurrentHashMap<>();
    
    public WhitelistManager(EzObserver plugin) {
        this.plugin = plugin;
        loadWhitelist();
    }
    
    private void loadWhitelist() {
        File whitelistFile = new File(plugin.getDataFolder(), "whitelist.yml");
        if (!whitelistFile.exists()) {
            createDefaultWhitelist(whitelistFile);
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(whitelistFile);
        
        enabled = config.getBoolean("enabled", true);
        mode = config.getString("mode", "strict");
        
        whitelistedItems.clear();
        whitelistedItems.addAll(config.getStringList("whitelisted-items"));
    }
    
    private void createDefaultWhitelist(File file) {
        YamlConfiguration config = new YamlConfiguration();
        
        config.set("enabled", true);
        config.set("mode", "strict");
        config.set("whitelisted-items", new ArrayList<>());
        
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().warning("无法创建白名单配置文件: " + e.getMessage());
        }
    }
    
    /**
     * 检查物品是否在白名单中
     * 使用缓存机制提升性能
     */
    public boolean isWhitelisted(ItemStack item) {
        if (!enabled || whitelistedItems.isEmpty() || item == null) {
            return false;
        }
        
        // 使用物品类型作为快速缓存键
        String cacheKey = item.getType().name();
        
        // 检查缓存
        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }
        
        String itemKey = getItemKey(item);
        boolean result = itemKey != null && whitelistedItems.contains(itemKey);
        
        // 缓存结果（最多缓存1000个物品）
        if (cache.size() < 1000) {
            cache.put(cacheKey, result);
        }
        
        return result;
    }
    
    /**
     * 添加物品到白名单
     */
    public void addToWhitelist(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        String itemKey = getItemKey(item);
        if (itemKey != null) {
            whitelistedItems.add(itemKey);
            saveWhitelist();
        }
    }
    
    /**
     * 从白名单中移除物品
     */
    public void removeFromWhitelist(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return;
        }
        String itemKey = getItemKey(item);
        if (itemKey != null) {
            whitelistedItems.remove(itemKey);
            saveWhitelist();
        }
    }
    
    /**
     * 获取白名单条目列表
     */
    public List<String> getWhitelistEntries() {
        return new ArrayList<>(whitelistedItems);
    }
    
    /**
     * 重新加载白名单
     */
    public void reloadWhitelist() {
        loadWhitelist();
    }
    
    /**
     * 生成物品的唯一标识符
     */
    private String getItemKey(ItemStack item) {
        if (item == null || item.getType() == null) {
            return null;
        }
        
        StringBuilder key = new StringBuilder();
        key.append(item.getType().name());
        
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                // 使用legacy API避免类型不兼容
                if (meta.hasDisplayName()) {
                    String displayName = meta.getDisplayName();
                    if (!displayName.isEmpty()) {
                        key.append(":").append(displayName);
                    }
                }
                
                if (meta.hasLore()) {
                    List<String> lore = meta.getLore();
                    if (lore != null && !lore.isEmpty()) {
                        key.append(":").append(String.join(";", lore));
                    }
                }
            }
        }
        
        return key.toString();
    }
    
    /**
     * 保存白名单到文件
     */
    private void saveWhitelist() {
        File whitelistFile = new File(plugin.getDataFolder(), "whitelist.yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(whitelistFile);
        
        config.set("enabled", enabled);
        config.set("mode", mode);
        config.set("whitelisted-items", new ArrayList<>(whitelistedItems));
        
        try {
            config.save(whitelistFile);
        } catch (IOException e) {
            plugin.getLogger().warning("无法保存白名单配置文件: " + e.getMessage());
        }
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public String getMode() {
        return mode;
    }
}