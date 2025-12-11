package top.mc_plfd_host.ezobserver.checker;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import top.mc_plfd_host.ezobserver.EzObserver;
import top.mc_plfd_host.ezobserver.config.ConfigManager;
import top.mc_plfd_host.ezobserver.config.EnchantmentConflictManager;
import top.mc_plfd_host.ezobserver.config.PotionEffectLimitManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ItemChecker {

    private final EzObserver plugin;
    private final ConfigManager configManager;
    private final EnchantmentConflictManager conflictManager;
    private final PotionEffectLimitManager potionEffectLimitManager;

    public ItemChecker(EzObserver plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.conflictManager = plugin.getEnchantmentConflictManager();
        this.potionEffectLimitManager = plugin.getPotionEffectLimitManager();
    }

    public List<String> checkItem(ItemStack item) {
        List<String> violations = new ArrayList<>();
        
        if (item == null) {
            return violations;
        }
        
        // 检查禁止的物品类型
        violations.addAll(checkBannedMaterial(item));
        
        // 检查禁止的物品名称和Lore
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            violations.addAll(checkBannedNameAndLore(meta));
            
            // 检查属性修饰符
            violations.addAll(checkAttributeModifiers(meta));
        }
        
        // 检查附魔
        violations.addAll(checkEnchantments(item));
        
        // 检查冲突附魔
        violations.addAll(checkConflictingEnchantments(item));
        
        // 检查OP物品
        violations.addAll(checkOpItem(item));
        
        // 检查药水
        violations.addAll(checkPotion(item));
        
        // 检查刷怪蛋NBT
        violations.addAll(checkSpawnEgg(item));
        
        return violations;
    }

    private List<String> checkEnchantments(ItemStack item) {
        List<String> violations = new ArrayList<>();
        
        Map<Enchantment, Integer> enchantments = item.getEnchantments();
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchant = entry.getKey();
            int level = entry.getValue();
            String enchantName = enchant.getKey().getKey();
            
            int limit;
            // 检查是否有配置的限制
            if (configManager.hasEnchantmentLimit(enchantName)) {
                limit = configManager.getEnchantmentLimit(enchantName);
            } else {
                // 未配置的附魔，使用原版最大等级
                if (configManager.isUseVanillaMaxForUnconfigured()) {
                    limit = (int) Math.ceil(enchant.getMaxLevel() * configManager.getUnconfiguredEnchantmentMultiplier());
                } else {
                    limit = configManager.getMaxEnchantmentLevel();
                }
            }
            
            if (level > limit) {
                violations.add(String.format("附魔 %s 等级 %d 超过限制 %d",
                    enchantName, level, limit));
            }
        }
        
        return violations;
    }

    /**
     * 检查冲突附魔
     */
    private List<String> checkConflictingEnchantments(ItemStack item) {
        List<String> violations = new ArrayList<>();
        
        if (conflictManager == null || !conflictManager.isConflictDetectionEnabled()) {
            return violations;
        }
        
        List<Set<String>> conflictGroups = conflictManager.findConflictingEnchantments(item);
        
        for (Set<String> conflictGroup : conflictGroups) {
            violations.add(String.format("冲突附魔组: %s (这些附魔互相冲突，将被全部移除)",
                String.join(", ", conflictGroup)));
        }
        
        return violations;
    }

    private List<String> checkAttributeModifiers(ItemMeta meta) {
        List<String> violations = new ArrayList<>();
        
        if (!meta.hasAttributeModifiers()) {
            return violations;
        }
        
        for (Attribute attribute : Attribute.values()) {
            Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(attribute);
            if (modifiers != null && !modifiers.isEmpty()) {
                for (AttributeModifier modifier : modifiers) {
                    double amount = modifier.getAmount();
                    double limit = configManager.getAttributeLimit(attribute.name());
                    
                    if (Math.abs(amount) > limit) {
                        violations.add(String.format("属性修饰符 %s 值 %.2f 超过限制 %.2f",
                            attribute.name(), amount, limit));
                    }
                }
            }
        }
        
        return violations;
    }

    public boolean isViolating(ItemStack item) {
        return !checkItem(item).isEmpty();
    }

    private List<String> checkBannedMaterial(ItemStack item) {
        List<String> violations = new ArrayList<>();
        
        if (!configManager.isBannedItemsEnabled()) {
            return violations;
        }
        
        if (configManager.isBannedMaterial(item.getType())) {
            violations.add(String.format("禁止的物品类型: %s", item.getType().name()));
        }
        
        return violations;
    }

    private List<String> checkBannedNameAndLore(ItemMeta meta) {
        List<String> violations = new ArrayList<>();
        
        if (!configManager.isBannedItemsEnabled()) {
            return violations;
        }
        
        // 检查物品名称
        if (meta.hasDisplayName()) {
            String displayName = meta.getDisplayName();
            for (String keyword : configManager.getBannedNameKeywords()) {
                if (displayName.contains(keyword)) {
                    violations.add(String.format("物品名称包含禁止关键词: %s", keyword));
                    break;
                }
            }
        }
        
        // 检查Lore
        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            if (lore != null) {
                for (String loreLine : lore) {
                    for (String keyword : configManager.getBannedLoreKeywords()) {
                        if (loreLine.contains(keyword)) {
                            violations.add(String.format("物品Lore包含禁止关键词: %s", keyword));
                            break;
                        }
                    }
                }
            }
        }
        
        return violations;
    }

    private List<String> checkOpItem(ItemStack item) {
        List<String> violations = new ArrayList<>();
        
        if (!configManager.isOpItemsEnabled()) {
            return violations;
        }
        
        // 检查附魔总等级
        Map<Enchantment, Integer> enchantments = item.getEnchantments();
        int totalLevel = 0;
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            totalLevel += entry.getValue();
            
            // 检查单个附魔是否超过原版最大等级的倍数
            Enchantment enchant = entry.getKey();
            int level = entry.getValue();
            int maxLevel = enchant.getMaxLevel();
            double maxAllowed = maxLevel * configManager.getMaxEnchantmentMultiplier();
            
            if (level > maxAllowed) {
                violations.add(String.format("OP附魔: %s 等级 %d 超过原版最大等级 %d 的 %.1f 倍",
                    enchant.getKey().getKey(), level, maxLevel, configManager.getMaxEnchantmentMultiplier()));
            }
        }
        
        if (totalLevel > configManager.getMaxTotalEnchantmentLevel()) {
            violations.add(String.format("OP物品: 附魔总等级 %d 超过限制 %d",
                totalLevel, configManager.getMaxTotalEnchantmentLevel()));
        }
        
        // 检查属性修饰符数量和数值
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            if (meta.hasAttributeModifiers()) {
                int attributeCount = 0;
                
                for (Attribute attribute : Attribute.values()) {
                    Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(attribute);
                    if (modifiers != null && !modifiers.isEmpty()) {
                        attributeCount += modifiers.size();
                    }
                }
                
                if (attributeCount > configManager.getMaxAttributeCount()) {
                    violations.add(String.format("OP物品: 属性修饰符数量 %d 超过限制 %d",
                        attributeCount, configManager.getMaxAttributeCount()));
                }
            }
        }
        
        return violations;
    }

    private List<String> checkPotion(ItemStack item) {
        List<String> violations = new ArrayList<>();
        
        if (!configManager.isPotionCheckEnabled()) {
            return violations;
        }
        
        // 检查是否是药水类物品
        Material type = item.getType();
        if (type != Material.POTION && type != Material.SPLASH_POTION &&
            type != Material.LINGERING_POTION && type != Material.TIPPED_ARROW) {
            return violations;
        }
        
        if (!item.hasItemMeta() || !(item.getItemMeta() instanceof PotionMeta)) {
            return violations;
        }
        
        PotionMeta potionMeta = (PotionMeta) item.getItemMeta();
        
        // 检查自定义药水效果
        if (potionMeta.hasCustomEffects()) {
            for (PotionEffect effect : potionMeta.getCustomEffects()) {
                violations.addAll(checkPotionEffect(effect));
            }
        }
        
        // 检查基础药水效果
        if (potionMeta.getBasePotionType() != null) {
            for (PotionEffect effect : potionMeta.getBasePotionType().getPotionEffects()) {
                violations.addAll(checkPotionEffect(effect));
            }
        }
        
        return violations;
    }

    private List<String> checkPotionEffect(PotionEffect effect) {
        List<String> violations = new ArrayList<>();
        
        PotionEffectType effectType = effect.getType();
        String effectName = effectType.getName();
        int amplifier = effect.getAmplifier();
        int duration = effect.getDuration() / 20; // 转换为秒
        
        // 检查是否是禁止的药水效果
        if (configManager.isBannedPotionEffect(effectName)) {
            violations.add(String.format("禁止的药水效果: %s", effectName));
            return violations;
        }
        
        // 使用PotionEffectLimitManager检查药水效果等级
        // 如果效果等级超过正常最大等级+2，则视为违规
        if (potionEffectLimitManager != null && potionEffectLimitManager.isOverLimit(effectName, amplifier)) {
            int maxLevel = potionEffectLimitManager.getMaxLevel(effectName);
            int limitLevel = potionEffectLimitManager.getLimitLevel(effectName);
            boolean isPotionObtainable = potionEffectLimitManager.isPotionObtainable(effectName);
            
            if (isPotionObtainable) {
                violations.add(String.format("药水效果 %s 等级 %d 超过限制（正常最大等级 %d + 容许值 2 = %d）",
                    effectName, amplifier + 1, maxLevel + 1, limitLevel + 1));
            } else {
                // 非药水可获得的效果，最大10级
                violations.add(String.format("非药水效果 %s 等级 %d 超过限制 %d",
                    effectName, amplifier + 1, limitLevel + 1));
            }
        } else {
            // 备用检查：使用配置的限制
            int amplifierLimit;
            if (configManager.hasPotionEffectLimit(effectName)) {
                amplifierLimit = configManager.getPotionEffectLimit(effectName);
            } else {
                amplifierLimit = configManager.getMaxPotionAmplifier();
            }
            
            if (amplifier > amplifierLimit) {
                violations.add(String.format("药水效果 %s 等级 %d 超过配置限制 %d",
                    effectName, amplifier + 1, amplifierLimit + 1));
            }
        }
        
        // 检查药水效果持续时间
        int durationLimit;
        if (configManager.hasPotionDurationLimit(effectName)) {
            durationLimit = configManager.getPotionDurationLimit(effectName);
        } else {
            durationLimit = configManager.getMaxPotionDuration();
        }
        
        if (duration > durationLimit) {
            violations.add(String.format("药水效果 %s 持续时间 %d秒 超过限制 %d秒",
                effectName, duration, durationLimit));
        }
        
        return violations;
    }

    /**
     * 检查刷怪蛋是否被修改了NBT
     * 刷怪蛋可能被修改为生成不同的实体（如TNT矿车）
     */
    private List<String> checkSpawnEgg(ItemStack item) {
        List<String> violations = new ArrayList<>();
        
        // 检查是否是刷怪蛋
        if (!isSpawnEgg(item.getType())) {
            return violations;
        }
        
        if (!item.hasItemMeta()) {
            return violations;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // 检查是否有自定义NBT数据（通过PersistentDataContainer或其他方式）
        // 在较新版本的Bukkit中，SpawnEggMeta可以获取生成的实体类型
        if (meta instanceof SpawnEggMeta) {
            SpawnEggMeta spawnEggMeta = (SpawnEggMeta) meta;
            
            // 获取刷怪蛋应该生成的实体类型（根据物品类型）
            String expectedEntityType = getExpectedEntityType(item.getType());
            
            // 尝试通过反射检查是否有自定义生成数据（兼容不同版本）
            try {
                // 尝试调用 getSpawnedType() 方法（某些版本可能有）
                java.lang.reflect.Method getSpawnedTypeMethod = spawnEggMeta.getClass().getMethod("getSpawnedType");
                Object actualEntityType = getSpawnedTypeMethod.invoke(spawnEggMeta);
                
                if (actualEntityType != null) {
                    String actualTypeName = actualEntityType.toString();
                    
                    // 如果实际生成的实体类型与预期不符，则可能是被修改的
                    if (expectedEntityType != null && !expectedEntityType.equals(actualTypeName)) {
                        violations.add(String.format("刷怪蛋NBT被篡改: 物品类型为 %s，但实际会生成 %s",
                            item.getType().name(), actualTypeName));
                    }
                }
            } catch (Exception e) {
                // 方法不存在或调用失败，使用备用检测方法
            }
        }
        
        // 检查是否有额外的NBT数据（如EntityTag）
        // 这些数据可能被用于生成非预期的实体或携带恶意数据
        if (hasCustomEntityTag(item)) {
            violations.add(String.format("刷怪蛋包含自定义EntityTag NBT数据，可能被篡改"));
        }
        
        return violations;
    }
    
    /**
     * 检查物品是否是刷怪蛋
     */
    private boolean isSpawnEgg(Material material) {
        return material.name().endsWith("_SPAWN_EGG");
    }
    
    /**
     * 根据刷怪蛋类型获取预期的实体类型
     */
    private String getExpectedEntityType(Material spawnEggType) {
        String name = spawnEggType.name();
        if (name.endsWith("_SPAWN_EGG")) {
            // 例如: CHICKEN_SPAWN_EGG -> CHICKEN
            return name.substring(0, name.length() - "_SPAWN_EGG".length());
        }
        return null;
    }
    
    /**
     * 检查刷怪蛋是否有自定义EntityTag
     * 这是通过检查ItemMeta的持久数据容器来实现的
     */
    private boolean hasCustomEntityTag(ItemStack item) {
        if (!item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // 检查持久数据容器是否有自定义数据
        // 在Bukkit API中，我们可以通过检查PersistentDataContainer来检测
        if (meta.getPersistentDataContainer() != null && !meta.getPersistentDataContainer().isEmpty()) {
            return true;
        }
        
        return false;
    }
}