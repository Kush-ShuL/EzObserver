package top.mc_plfd_host.ezobserver.config;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import top.mc_plfd_host.ezobserver.EzObserver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("FieldCanBeLocal")
public class ConfigManager {

    private final EzObserver plugin;
    // config字段需要在多个方法中使用，保持为实例字段
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
    private final Map<String, Integer> enchantmentLimits;
    private final Map<String, Double> attributeLimits;
    
    // 修正模式设置
    private boolean removeOverLimitEnchantments;
    private boolean downgradeEnchantments;
    private boolean removeOverLimitAttributes;
    private boolean downgradeAttributes;
    private boolean removeUnbreakable;
    
    // 自定义禁止物品设置
    private boolean bannedItemsEnabled;
    private String bannedItemsActionMode;
    private final List<String> bannedNameKeywords;
    private final List<String> bannedLoreKeywords;
    private final Set<Material> bannedMaterials;
    private final Set<Material> bannedSpawnEggs;
    
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
    private final Set<String> bannedPotionEffects;
    private final Map<String, Integer> potionEffectLimits;
    private final Map<String, Integer> potionDurationLimits;
    
    // 白名单管理器
    private final WhitelistManager whitelistManager;
    
    // 高级功能配置
    private boolean realTimeMonitoringEnabled;
    private boolean autoFixEnabled;
    private boolean autoDeleteEnabled;
    private int realTimeScanInterval;
    private int maxViolationHistory;
    private boolean reportGenerationEnabled;
    private long reportRetentionDays;
    private boolean permissionBypassEnabled;

    // 忽略的 NBT 路径列表(已归一化为小写,用于 PersistentDataContainer 前缀匹配)
    private final List<String> ignoredNbtPaths;

    // 允许的特殊附魔组合,每条为一个附魔名(小写) -> 等级 的不可变 map
    // 物品附魔集合(名 -> 等级)需与其中某条完全相等才视为豁免
    private final List<Map<String, Integer>> allowedEnchantmentCombinations;

    // 线程安全锁对象
    private final Object configLock = new Object();

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
        this.whitelistManager = new WhitelistManager(plugin);
        this.ignoredNbtPaths = new ArrayList<>();
        this.allowedEnchantmentCombinations = new ArrayList<>();
    }

    public void loadConfig() {
        synchronized (configLock) {
            try {
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
                    enchantmentLimits.clear();
                    var enchantmentSection = config.getConfigurationSection("enchantment-limits");
                    if (enchantmentSection != null) {
                        for (String key : enchantmentSection.getKeys(false)) {
                            try {
                                int value = config.getInt("enchantment-limits." + key);
                                enchantmentLimits.put(key.toUpperCase(), value);
                            } catch (Exception e) {
                                plugin.getLogger().warning("无效的附魔限制配置: " + key + " = " + config.get("enchantment-limits." + key));
                            }
                        }
                    }
                }
                
                // 加载属性修饰符限制
                if (config.isConfigurationSection("attribute-limits")) {
                    attributeLimits.clear();
                    var attributeSection = config.getConfigurationSection("attribute-limits");
                    if (attributeSection != null) {
                        for (String key : attributeSection.getKeys(false)) {
                            try {
                                double value = config.getDouble("attribute-limits." + key);
                                attributeLimits.put(key.toUpperCase(), value);
                            } catch (Exception e) {
                                plugin.getLogger().warning("无效的属性限制配置: " + key + " = " + config.get("attribute-limits." + key));
                            }
                        }
                    }
                }
                
                // 加载修正模式设置
                removeOverLimitEnchantments = config.getBoolean("fix-settings.remove-over-limit-enchantments", true);
                downgradeEnchantments = config.getBoolean("fix-settings.downgrade-enchantments", false);
                removeOverLimitAttributes = config.getBoolean("fix-settings.remove-over-limit-attributes", true);
                downgradeAttributes = config.getBoolean("fix-settings.downgrade-attributes", false);
                removeUnbreakable = config.getBoolean("fix-settings.remove-unbreakable", true);
                
                // 加载自定义禁止物品设置
                bannedItemsEnabled = config.getBoolean("banned-items.enabled", true);
                bannedItemsActionMode = config.getString("banned-items.action-mode", "delete");
                bannedNameKeywords.clear();
                bannedNameKeywords.addAll(config.getStringList("banned-items.name-keywords"));
                bannedLoreKeywords.clear();
                bannedLoreKeywords.addAll(config.getStringList("banned-items.lore-keywords"));
                
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
                    var effectLimitsSection = config.getConfigurationSection("potion-check.effect-limits");
                    if (effectLimitsSection != null) {
                        for (String key : effectLimitsSection.getKeys(false)) {
                            try {
                                int value = config.getInt("potion-check.effect-limits." + key);
                                potionEffectLimits.put(key.toUpperCase(), value);
                            } catch (Exception e) {
                                plugin.getLogger().warning("无效的药水效果限制配置: " + key + " = " + config.get("potion-check.effect-limits." + key));
                            }
                        }
                    }
                }
                
                potionDurationLimits.clear();
                if (config.isConfigurationSection("potion-check.duration-limits")) {
                    var durationLimitsSection = config.getConfigurationSection("potion-check.duration-limits");
                    if (durationLimitsSection != null) {
                        for (String key : durationLimitsSection.getKeys(false)) {
                            try {
                                int value = config.getInt("potion-check.duration-limits." + key);
                                potionDurationLimits.put(key.toUpperCase(), value);
                            } catch (Exception e) {
                                plugin.getLogger().warning("无效的药水持续时间限制配置: " + key + " = " + config.get("potion-check.duration-limits." + key));
                            }
                        }
                    }
                }

                // 加载忽略的 NBT 路径(已归一化为小写,用于 PDC 命名空间/全键前缀匹配)
                ignoredNbtPaths.clear();
                for (String path : config.getStringList("ignored-nbt-paths")) {
                    if (path != null && !path.isEmpty()) {
                        ignoredNbtPaths.add(path.toLowerCase());
                    }
                }
                if (!ignoredNbtPaths.isEmpty()) {
                    plugin.getLogger().info("已加载 " + ignoredNbtPaths.size() + " 个忽略的 NBT 路径");
                }

                // 加载允许的附魔组合(字符串格式: "enchant:level,enchant:level")
                allowedEnchantmentCombinations.clear();
                for (String combo : config.getStringList("allowed-enchantment-combinations")) {
                    Map<String, Integer> parsed = parseEnchantmentCombination(combo);
                    if (!parsed.isEmpty()) {
                        allowedEnchantmentCombinations.add(parsed);
                    } else if (combo != null && !combo.trim().isEmpty()) {
                        plugin.getLogger().warning("无效的允许附魔组合配置: " + combo);
                    }
                }
                if (!allowedEnchantmentCombinations.isEmpty()) {
                    plugin.getLogger().info("已加载 " + allowedEnchantmentCombinations.size() + " 个允许的附魔组合");
                }

                // 加载高级功能配置
                realTimeMonitoringEnabled = config.getBoolean("advanced.real-time-monitoring.enabled", true);
                autoFixEnabled = config.getBoolean("advanced.auto-fix.enabled", false);
                autoDeleteEnabled = config.getBoolean("advanced.auto-delete.enabled", false);
                realTimeScanInterval = config.getInt("advanced.real-time-monitoring.scan-interval", 20);
                maxViolationHistory = config.getInt("advanced.max-violation-history", 100);
                reportGenerationEnabled = config.getBoolean("advanced.report-generation.enabled", true);
                reportRetentionDays = config.getLong("advanced.report-generation.retention-days", 30);
                permissionBypassEnabled = config.getBoolean("advanced.permission-bypass.enabled", true);
                
                plugin.getLogger().info("配置加载完成");
            } catch (Exception e) {
                plugin.getLogger().severe("配置加载失败: " + e.getMessage());
                plugin.getLogger().severe("异常详情: " + e.getClass().getName() + ": " + e.getMessage());
            }
        }
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        loadConfig();
        whitelistManager.reloadWhitelist();
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

    public boolean isRemoveUnbreakable() {
        return removeUnbreakable;
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
    
    public WhitelistManager getWhitelistManager() {
        return whitelistManager;
    }

    // 高级功能配置方法
    public boolean isRealTimeMonitoringEnabled() {
        return realTimeMonitoringEnabled;
    }

    public boolean isAutoFixEnabled() {
        return autoFixEnabled;
    }

    public boolean isAutoDeleteEnabled() {
        return autoDeleteEnabled;
    }

    public int getRealTimeScanInterval() {
        return realTimeScanInterval;
    }

    public int getMaxViolationHistory() {
        return maxViolationHistory;
    }

    public boolean isReportGenerationEnabled() {
        return reportGenerationEnabled;
    }

    public long getReportRetentionDays() {
        return reportRetentionDays;
    }

    public boolean isPermissionBypassEnabled() {
        return permissionBypassEnabled;
    }

    // 忽略的 NBT 路径相关方法
    public List<String> getIgnoredNbtPaths() {
        return ignoredNbtPaths;
    }

    /**
     * 判断物品是否包含被忽略 NBT 路径前缀标记的数据(基于 PersistentDataContainer)。
     * <p>
     * 匹配规则:遍历物品 PDC 中所有 NamespacedKey,若任一键的命名空间
     * (例如 "mythicmobs") 或完整键 (例如 "mythicmobs:item_id") 以配置列表中
     * 任意前缀开头(大小写不敏感),则视为来自被忽略的合法来源,返回 true。
     *
     * @param item 待检查物品
     * @return 是否匹配任一忽略路径
     */
    public boolean isIgnoredNbtItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        // noinspection ConstantConditions
        return isIgnoredNbtMeta(item.getItemMeta());
    }

    /**
     * 基于 ItemMeta 的 PDC 判断是否匹配被忽略的 NBT 路径前缀。
     * 与 {@link #isIgnoredNbtItem(ItemStack)} 等价,仅在调用方已持有 ItemMeta 时使用以省一次取值。
     */
    public boolean isIgnoredNbtMeta(ItemMeta meta) {
        if (meta == null || ignoredNbtPaths.isEmpty()) {
            return false;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        // getKeys() 永远不会返回 null;空容器直接跳过循环
        for (NamespacedKey key : pdc.getKeys()) {
            String namespace = key.getNamespace().toLowerCase();
            String fullKey = (key.getNamespace() + ":" + key.getKey()).toLowerCase();
            for (String ignored : ignoredNbtPaths) {
                if (namespace.startsWith(ignored) || fullKey.startsWith(ignored)) {
                    return true;
                }
            }
        }
        return false;
    }

    // 允许的附魔组合相关方法
    public List<Map<String, Integer>> getAllowedEnchantmentCombinations() {
        return allowedEnchantmentCombinations;
    }

    /**
     * 判断物品的附魔集合是否与某条允许的附魔组合完全相等(集合与等级均一致)。
     * 物品比配置多任何附魔或少任何附魔都视为不匹配,避免过宽放过可疑物品。
     *
     * @param item 待检查物品
     * @return 是否匹配任一允许的附魔组合
     */
    public boolean isAllowedEnchantmentCombination(ItemStack item) {
        if (item == null || allowedEnchantmentCombinations.isEmpty()) {
            return false;
        }
        Map<Enchantment, Integer> itemEnchants = item.getEnchantments();
        if (itemEnchants.isEmpty()) {
            return false;
        }
        Map<String, Integer> itemByName = new HashMap<>();
        for (Map.Entry<Enchantment, Integer> e : itemEnchants.entrySet()) {
            // Enchantment.getKey() 在原版附魔上始终为 minecraft:<name>,取 <name> 部分
            itemByName.put(e.getKey().getKey().getKey().toLowerCase(), e.getValue());
        }
        for (Map<String, Integer> allowed : allowedEnchantmentCombinations) {
            if (itemByName.equals(allowed)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断物品的附魔是否包含某条允许的附魔组合作为子集(等级须精确匹配)。
     * 用于 OP 物品总附魔等级检查的豁免:服务器允许该组合存在,则物品在总等级
     * 维度上的"可疑性"也被相应忽略,只要组合内每条 (enchant, level) 都精确出现即可。
     *
     * <p>注意:此方法与 {@link #isAllowedEnchantmentCombination(ItemStack)} 语义不同,
     * 后者要求物品附魔与配置完全相等(用于冲突检查的精确豁免)。</p>
     *
     * @param item 待检查物品
     * @return 是否包含任一允许的附魔组合(子集)
     */
    public boolean containsAllowedEnchantmentCombination(ItemStack item) {
        if (item == null || allowedEnchantmentCombinations.isEmpty()) {
            return false;
        }
        Map<Enchantment, Integer> itemEnchants = item.getEnchantments();
        if (itemEnchants.isEmpty()) {
            return false;
        }
        Map<String, Integer> itemByName = new HashMap<>();
        for (Map.Entry<Enchantment, Integer> e : itemEnchants.entrySet()) {
            itemByName.put(e.getKey().getKey().getKey().toLowerCase(), e.getValue());
        }
        for (Map<String, Integer> allowed : allowedEnchantmentCombinations) {
            boolean allMatched = true;
            for (Map.Entry<String, Integer> required : allowed.entrySet()) {
                Integer actual = itemByName.get(required.getKey());
                if (actual == null || !actual.equals(required.getValue())) {
                    allMatched = false;
                    break;
                }
            }
            if (allMatched) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析单条附魔组合字符串,例如 "sharpness:5,smite:5" ->
     * {"sharpness"=5, "smite"=5}。大小写不敏感,无法解析的项被忽略。
     */
    private Map<String, Integer> parseEnchantmentCombination(String combo) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (combo == null) {
            return result;
        }
        for (String part : combo.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int colon = trimmed.indexOf(':');
            if (colon <= 0 || colon == trimmed.length() - 1) {
                continue;
            }
            String name = trimmed.substring(0, colon).trim().toLowerCase();
            String levelStr = trimmed.substring(colon + 1).trim();
            try {
                int level = Integer.parseInt(levelStr);
                if (level > 0) {
                    result.put(name, level);
                }
            } catch (NumberFormatException ignored) {
                // 跳过非数字等级
            }
        }
        return result;
    }
}