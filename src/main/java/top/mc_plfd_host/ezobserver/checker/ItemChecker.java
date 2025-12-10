package top.mc_plfd_host.ezobserver.checker;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import top.mc_plfd_host.ezobserver.EzObserver;
import top.mc_plfd_host.ezobserver.config.ConfigManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ItemChecker {

    private final EzObserver plugin;
    private final ConfigManager configManager;

    public ItemChecker(EzObserver plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
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
        
        // 检查OP物品
        violations.addAll(checkOpItem(item));
        
        // 检查药水
        violations.addAll(checkPotion(item));
        
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
        
        // 检查药水效果等级
        int amplifierLimit;
        if (configManager.hasPotionEffectLimit(effectName)) {
            amplifierLimit = configManager.getPotionEffectLimit(effectName);
        } else {
            amplifierLimit = configManager.getMaxPotionAmplifier();
        }
        
        if (amplifier > amplifierLimit) {
            violations.add(String.format("药水效果 %s 等级 %d 超过限制 %d",
                effectName, amplifier + 1, amplifierLimit + 1));
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
}