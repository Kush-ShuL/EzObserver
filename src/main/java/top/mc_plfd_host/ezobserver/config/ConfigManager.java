package top.mc_plfd_host.ezobserver.config;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import top.mc_plfd_host.ezobserver.EzObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigManager {

    private final EzObserver plugin;
    private FileConfiguration config;
    
    // 配置选项
    private boolean enabled;
    private boolean strictMode;
    private boolean logViolations;
    private boolean broadcastViolations;
    private String broadcastMessageDelete;
    private String broadcastMessageStore;
    private String broadcastMessageFix;
    private boolean confiscateItems;
    private String confiscateMode;
    private String confiscateStoragePath;
    private int maxEnchantmentLevel;
    private boolean useVanillaMaxForUnconfigured;
    private double unconfiguredEnchantmentMultiplier;
    private int maxAttributeModifierAmount;
    private Map<String, Integer> enchantmentLimits;
    private Map<String, Double> attributeLimits;
    
    // 修正模式设置
    private boolean removeOverLimitEnchantments;
    private boolean downgradeEnchantments;
    private boolean removeOverLimitAttributes;
    private boolean downgradeAttributes;
    
    // 自定义禁止物品设置
    private boolean bannedItemsEnabled;
    private String bannedItemsActionMode;
    private List<String> bannedNameKeywords;
    private List<String> bannedLoreKeywords;
    private Set<Material> bannedMaterials;
    private Set<Material> bannedSpawnEggs;
    
    // OP物品检测设置
    private boolean opItemsEnabled;
    private int maxTotalEnchantmentLevel;
    private double maxEnchantmentMultiplier;
    private int maxAttributeCount;
    private double maxAttributeMultiplier;
    
    // 药水检测设置
    private boolean potionCheckEnabled;
    private int maxPotionDuration;
    private int maxPotionAmplifier;
    private Set<String> bannedPotionEffects;
    private Map<String, Integer> potionEffectLimits;
    private Map<String, Integer> potionDurationLimits;

    public ConfigManager(EzObserver plugin) {
        this.plugin = plugin;
        this.enchantmentLimits = new HashMap<>();
        this.attributeLimits = new HashMap<>();
        this.bannedNameKeywords = new ArrayList<>();
        this.bannedLoreKeywords = new ArrayList<>();
        this.bannedMaterials = new HashSet<>();
        this.bannedSpawnEggs = new HashSet<>();
        this.bannedPotionEffects = new HashSet<>();
        this.potionEffectLimits = new HashMap<>();
        this.potionDurationLimits = new HashMap<>();
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
        
        enabled = config.getBoolean("enabled", true);
        strictMode = config.getBoolean("strict-mode", false);
        logViolations = config.getBoolean("log-violations", true);
        broadcastViolations = config.getBoolean("broadcast-violations", true);
        broadcastMessageDelete = config.getString("broadcast-message-delete", "&c[EzObserver] &e玩家 &f{player} &e持有违规物品 &f{item}&e: &c{reason} &7物品已删除");
        broadcastMessageStore = config.getString("broadcast-message-store", "&c[EzObserver] &e玩家 &f{player} &e持有违规物品 &f{item}&e: &c{reason} &7物品已没收");
        broadcastMessageFix = config.getString("broadcast-message-fix", "&c[EzObserver] &e玩家 &f{player} &e持有违规物品 &f{item}&e: &c{reason} &a正在尝试修正违规属性……");
        confiscateItems = config.getBoolean("confiscate-items", true);
        confiscateMode = config.getString("confiscate-mode", "delete");
        confiscateStoragePath = config.getString("confiscate-storage-path", "plugins/EzObserver/confiscated/");
        maxEnchantmentLevel = config.getInt("max-enchantment-level", 10);
        useVanillaMaxForUnconfigured = config.getBoolean("use-vanilla-max-for-unconfigured", true);
        unconfiguredEnchantmentMultiplier = config.getDouble("unconfigured-enchantment-multiplier", 1.0);
        maxAttributeModifierAmount = config.getInt("max-attribute-modifier-amount", 100);
        
        // 加载附魔限制
        if (config.isConfigurationSection("enchantment-limits")) {
            for (String key : config.getConfigurationSection("enchantment-limits").getKeys(false)) {
                enchantmentLimits.put(key.toUpperCase(), config.getInt("enchantment-limits." + key));
            }
        }
        
        // 加载属性修饰符限制
        if (config.isConfigurationSection("attribute-limits")) {
            for (String key : config.getConfigurationSection("attribute-limits").getKeys(false)) {
                attributeLimits.put(key.toUpperCase(), config.getDouble("attribute-limits." + key));
            }
        }
        
        // 加载修正模式设置
        removeOverLimitEnchantments = config.getBoolean("fix-settings.remove-over-limit-enchantments", true);
        downgradeEnchantments = config.getBoolean("fix-settings.downgrade-enchantments", false);
        removeOverLimitAttributes = config.getBoolean("fix-settings.remove-over-limit-attributes", true);
        downgradeAttributes = config.getBoolean("fix-settings.downgrade-attributes", false);
        
        // 加载自定义禁止物品设置
        bannedItemsEnabled = config.getBoolean("banned-items.enabled", true);
        bannedItemsActionMode = config.getString("banned-items.action-mode", "delete");
        bannedNameKeywords = config.getStringList("banned-items.name-keywords");
        bannedLoreKeywords = config.getStringList("banned-items.lore-keywords");
        
        bannedMaterials.clear();
        for (String materialName : config.getStringList("banned-items.banned-materials")) {
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                bannedMaterials.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的物品类型: " + materialName);
            }
        }
        
        // 加载禁止的刷怪蛋类型
        bannedSpawnEggs.clear();
        for (String materialName : config.getStringList("banned-items.banned-spawn-eggs")) {
            try {
                Material material = Material.valueOf(materialName.toUpperCase());
                bannedSpawnEggs.add(material);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的刷怪蛋类型: " + materialName);
            }
        }
        
        // 加载OP物品检测设置
        opItemsEnabled = config.getBoolean("op-items.enabled", true);
        maxTotalEnchantmentLevel = config.getInt("op-items.max-total-enchantment-level", 50);
        maxEnchantmentMultiplier = config.getDouble("op-items.max-enchantment-multiplier", 2.0);
        maxAttributeCount = config.getInt("op-items.max-attribute-count", 10);
        maxAttributeMultiplier = config.getDouble("op-items.max-attribute-multiplier", 5.0);
        
        // 加载药水检测设置
        potionCheckEnabled = config.getBoolean("potion-check.enabled", true);
        maxPotionDuration = config.getInt("potion-check.max-duration", 600);
        maxPotionAmplifier = config.getInt("potion-check.max-amplifier", 2);
        
        bannedPotionEffects.clear();
        for (String effect : config.getStringList("potion-check.banned-effects")) {
            bannedPotionEffects.add(effect.toUpperCase());
        }
        
        potionEffectLimits.clear();
        if (config.isConfigurationSection("potion-check.effect-limits")) {
            for (String key : config.getConfigurationSection("potion-check.effect-limits").getKeys(false)) {
                potionEffectLimits.put(key.toUpperCase(), config.getInt("potion-check.effect-limits." + key));
            }
        }
        
        potionDurationLimits.clear();
        if (config.isConfigurationSection("potion-check.duration-limits")) {
            for (String key : config.getConfigurationSection("potion-check.duration-limits").getKeys(false)) {
                potionDurationLimits.put(key.toUpperCase(), config.getInt("potion-check.duration-limits." + key));
            }
        }
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfig();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isStrictMode() {
        return strictMode;
    }

    public boolean isLogViolations() {
        return logViolations;
    }

    public boolean isBroadcastViolations() {
        return broadcastViolations;
    }

    public String getBroadcastMessage() {
        if (isDeleteMode()) {
            return broadcastMessageDelete;
        } else if (isStoreMode()) {
            return broadcastMessageStore;
        } else if (isFixMode()) {
            return broadcastMessageFix;
        }
        return broadcastMessageDelete;
    }

    public String getBroadcastMessageDelete() {
        return broadcastMessageDelete;
    }

    public String getBroadcastMessageStore() {
        return broadcastMessageStore;
    }

    public String getBroadcastMessageFix() {
        return broadcastMessageFix;
    }

    public boolean isConfiscateItems() {
        return confiscateItems;
    }

    public String getConfiscateMode() {
        return confiscateMode;
    }

    public String getConfiscateStoragePath() {
        return confiscateStoragePath;
    }

    public boolean isDeleteMode() {
        return "delete".equalsIgnoreCase(confiscateMode);
    }

    public boolean isStoreMode() {
        return "store".equalsIgnoreCase(confiscateMode);
    }

    public boolean isFixMode() {
        return "fix".equalsIgnoreCase(confiscateMode);
    }

    public boolean isRemoveOverLimitEnchantments() {
        return removeOverLimitEnchantments;
    }

    public boolean isDowngradeEnchantments() {
        return downgradeEnchantments;
    }

    public boolean isRemoveOverLimitAttributes() {
        return removeOverLimitAttributes;
    }

    public boolean isDowngradeAttributes() {
        return downgradeAttributes;
    }

    public int getMaxEnchantmentLevel() {
        return maxEnchantmentLevel;
    }

    public int getMaxAttributeModifierAmount() {
        return maxAttributeModifierAmount;
    }

    public int getEnchantmentLimit(String enchantment) {
        return enchantmentLimits.getOrDefault(enchantment.toUpperCase(), maxEnchantmentLevel);
    }

    public boolean hasEnchantmentLimit(String enchantment) {
        return enchantmentLimits.containsKey(enchantment.toUpperCase());
    }

    public boolean isUseVanillaMaxForUnconfigured() {
        return useVanillaMaxForUnconfigured;
    }

    public double getUnconfiguredEnchantmentMultiplier() {
        return unconfiguredEnchantmentMultiplier;
    }

    public double getAttributeLimit(String attribute) {
        return attributeLimits.getOrDefault(attribute.toUpperCase(), (double) maxAttributeModifierAmount);
    }

    // 自定义禁止物品相关方法
    public boolean isBannedItemsEnabled() {
        return bannedItemsEnabled;
    }

    public List<String> getBannedNameKeywords() {
        return bannedNameKeywords;
    }

    public List<String> getBannedLoreKeywords() {
        return bannedLoreKeywords;
    }

    public Set<Material> getBannedMaterials() {
        return bannedMaterials;
    }

    public boolean isBannedMaterial(Material material) {
        return bannedMaterials.contains(material);
    }

    public String getBannedItemsActionMode() {
        return bannedItemsActionMode;
    }

    public boolean isBannedItemsDeleteMode() {
        return "delete".equalsIgnoreCase(bannedItemsActionMode);
    }

    public boolean isBannedItemsNotifyMode() {
        return "notify".equalsIgnoreCase(bannedItemsActionMode);
    }

    public Set<Material> getBannedSpawnEggs() {
        return bannedSpawnEggs;
    }

    public boolean isBannedSpawnEgg(Material material) {
        return bannedSpawnEggs.contains(material);
    }

    // OP物品检测相关方法
    public boolean isOpItemsEnabled() {
        return opItemsEnabled;
    }

    public int getMaxTotalEnchantmentLevel() {
        return maxTotalEnchantmentLevel;
    }

    public double getMaxEnchantmentMultiplier() {
        return maxEnchantmentMultiplier;
    }

    public int getMaxAttributeCount() {
        return maxAttributeCount;
    }

    public double getMaxAttributeMultiplier() {
        return maxAttributeMultiplier;
    }

    // 药水检测相关方法
    public boolean isPotionCheckEnabled() {
        return potionCheckEnabled;
    }

    public int getMaxPotionDuration() {
        return maxPotionDuration;
    }

    public int getMaxPotionAmplifier() {
        return maxPotionAmplifier;
    }

    public boolean isBannedPotionEffect(String effectType) {
        return bannedPotionEffects.contains(effectType.toUpperCase());
    }

    public int getPotionEffectLimit(String effectType) {
        return potionEffectLimits.getOrDefault(effectType.toUpperCase(), maxPotionAmplifier);
    }

    public int getPotionDurationLimit(String effectType) {
        return potionDurationLimits.getOrDefault(effectType.toUpperCase(), maxPotionDuration);
    }

    public boolean hasPotionEffectLimit(String effectType) {
        return potionEffectLimits.containsKey(effectType.toUpperCase());
    }

    public boolean hasPotionDurationLimit(String effectType) {
        return potionDurationLimits.containsKey(effectType.toUpperCase());
    }
}