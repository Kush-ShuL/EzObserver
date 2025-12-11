package top.mc_plfd_host.ezobserver.fixer;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import top.mc_plfd_host.ezobserver.EzObserver;
import top.mc_plfd_host.ezobserver.config.ConfigManager;
import top.mc_plfd_host.ezobserver.config.EnchantmentConflictManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ItemFixer {

    private final EzObserver plugin;
    private final ConfigManager configManager;
    private final EnchantmentConflictManager conflictManager;

    public ItemFixer(EzObserver plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.conflictManager = plugin.getEnchantmentConflictManager();
    }

    public ItemStack fixItem(ItemStack item) {
        if (item == null) {
            return item;
        }

        ItemStack fixedItem = item.clone();

        // 修正冲突附魔（如果两个附魔冲突，则两个都移除）
        fixConflictingEnchantments(fixedItem);

        // 修正附魔 (包括单个附魔超限和OP附魔)
        fixEnchantments(fixedItem);
        
        // 修正OP物品附魔总等级
        fixOpEnchantments(fixedItem);

        // 修正刷怪蛋NBT
        fixSpawnEggNbt(fixedItem);

        // 修正属性修饰符
        if (fixedItem.hasItemMeta()) {
            ItemMeta meta = fixedItem.getItemMeta();
            if (meta != null) {
                boolean metaChanged = false;
                
                // 修正超限属性修饰符
                if (meta.hasAttributeModifiers()) {
                    fixAttributeModifiers(meta);
                    metaChanged = true;
                }
                
                // 修正OP物品属性数量
                if (meta.hasAttributeModifiers()) {
                    fixOpAttributes(meta);
                    metaChanged = true;
                }
                
                if (metaChanged) {
                    fixedItem.setItemMeta(meta);
                }
            }
        }

        plugin.getLogger().info("物品修正完成: " + fixedItem.getType().name());
        return fixedItem;
    }

    private void fixEnchantments(ItemStack item) {
        Map<Enchantment, Integer> enchantments = new HashMap<>(item.getEnchantments());
        
        if (enchantments.isEmpty()) {
            return;
        }

        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchant = entry.getKey();
            int level = entry.getValue();
            String enchantName = enchant.getKey().getKey();
            
            // 获取限制等级
            int limit;
            if (configManager.hasEnchantmentLimit(enchantName)) {
                limit = configManager.getEnchantmentLimit(enchantName);
            } else if (configManager.isUseVanillaMaxForUnconfigured()) {
                limit = (int) Math.ceil(enchant.getMaxLevel() * configManager.getUnconfiguredEnchantmentMultiplier());
            } else {
                limit = configManager.getMaxEnchantmentLevel();
            }

            // 同时检查OP物品的附魔倍数限制
            if (configManager.isOpItemsEnabled()) {
                int maxLevel = enchant.getMaxLevel();
                int opLimit = (int) Math.ceil(maxLevel * configManager.getMaxEnchantmentMultiplier());
                limit = Math.min(limit, opLimit);
            }

            if (level > limit) {
                // 先移除原附魔
                item.removeEnchantment(enchant);
                
                if (configManager.isDowngradeEnchantments()) {
                    // 降级到限制等级
                    if (limit > 0) {
                        item.addUnsafeEnchantment(enchant, limit);
                        plugin.getLogger().info("已将附魔 " + enchantName + " 从等级 " + level + " 降级到 " + limit);
                    } else {
                        plugin.getLogger().info("已移除附魔 " + enchantName + " (限制等级为0)");
                    }
                } else if (configManager.isRemoveOverLimitEnchantments()) {
                    // 移除超限附魔
                    plugin.getLogger().info("已移除超限附魔: " + enchantName + " (等级 " + level + " > 限制 " + limit + ")");
                }
            }
        }
    }

    /**
     * 修正冲突附魔
     * 如果两个附魔冲突，则两个附魔都会被移除
     */
    private void fixConflictingEnchantments(ItemStack item) {
        if (conflictManager == null || !conflictManager.isConflictDetectionEnabled()) {
            return;
        }
        
        List<String> removedEnchantments = conflictManager.removeConflictingEnchantments(item);
        
        if (!removedEnchantments.isEmpty()) {
            plugin.getLogger().info("已移除冲突附魔: " + String.join(", ", removedEnchantments));
        }
    }

    private void fixOpEnchantments(ItemStack item) {
        if (!configManager.isOpItemsEnabled()) {
            return;
        }

        Map<Enchantment, Integer> enchantments = new HashMap<>(item.getEnchantments());
        
        if (enchantments.isEmpty()) {
            return;
        }

        // 计算总附魔等级
        int totalLevel = 0;
        for (int level : enchantments.values()) {
            totalLevel += level;
        }

        int maxTotalLevel = configManager.getMaxTotalEnchantmentLevel();
        
        // 如果总等级超限，需要移除一些附魔
        if (totalLevel > maxTotalLevel) {
            plugin.getLogger().info("检测到OP物品: 附魔总等级 " + totalLevel + " 超过限制 " + maxTotalLevel);
            
            // 按等级从高到低排序，优先移除高等级附魔
            List<Map.Entry<Enchantment, Integer>> sortedEnchants = new ArrayList<>(enchantments.entrySet());
            sortedEnchants.sort((a, b) -> b.getValue() - a.getValue());
            
            for (Map.Entry<Enchantment, Integer> entry : sortedEnchants) {
                if (totalLevel <= maxTotalLevel) {
                    break;
                }
                
                Enchantment enchant = entry.getKey();
                int level = entry.getValue();
                
                item.removeEnchantment(enchant);
                totalLevel -= level;
                plugin.getLogger().info("移除OP附魔: " + enchant.getKey().getKey() + " (等级 " + level + ") 以降低总等级");
            }
        }
    }

    private void fixAttributeModifiers(ItemMeta meta) {
        if (!meta.hasAttributeModifiers()) {
            return;
        }

        for (Attribute attribute : Attribute.values()) {
            Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(attribute);
            if (modifiers != null && !modifiers.isEmpty()) {
                List<AttributeModifier> toRemove = new ArrayList<>();
                List<AttributeModifier> toAdd = new ArrayList<>();

                for (AttributeModifier modifier : modifiers) {
                    double amount = modifier.getAmount();
                    double limit = configManager.getAttributeLimit(attribute.name());

                    if (Math.abs(amount) > limit) {
                        if (configManager.isDowngradeAttributes()) {
                            // 降级到限制值
                            toRemove.add(modifier);
                            double newAmount = amount > 0 ? limit : -limit;
                            
                            // 使用新的构造方法（兼容新版本API）
                            EquipmentSlot slot = modifier.getSlot();
                            AttributeModifier newModifier;
                            if (slot != null) {
                                newModifier = new AttributeModifier(
                                    UUID.randomUUID(),
                                    modifier.getName(),
                                    newAmount,
                                    modifier.getOperation(),
                                    slot
                                );
                            } else {
                                newModifier = new AttributeModifier(
                                    UUID.randomUUID(),
                                    modifier.getName(),
                                    newAmount,
                                    modifier.getOperation()
                                );
                            }
                            toAdd.add(newModifier);
                            plugin.getLogger().info("已将属性 " + attribute.name() + " 从 " + amount + " 降级到 " + newAmount);
                        } else if (configManager.isRemoveOverLimitAttributes()) {
                            // 移除超限属性修饰符
                            toRemove.add(modifier);
                            plugin.getLogger().info("已移除超限属性修饰符: " + attribute.name() + " (值 " + amount + ")");
                        }
                    }
                }

                for (AttributeModifier modifier : toRemove) {
                    meta.removeAttributeModifier(attribute, modifier);
                }
                for (AttributeModifier modifier : toAdd) {
                    meta.addAttributeModifier(attribute, modifier);
                }
            }
        }
    }

    private void fixOpAttributes(ItemMeta meta) {
        if (!configManager.isOpItemsEnabled()) {
            return;
        }

        if (!meta.hasAttributeModifiers()) {
            return;
        }

        // 计算总属性修饰符数量
        int totalCount = 0;
        List<AttributeModifier> allModifiers = new ArrayList<>();
        
        for (Attribute attribute : Attribute.values()) {
            Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(attribute);
            if (modifiers != null && !modifiers.isEmpty()) {
                totalCount += modifiers.size();
                allModifiers.addAll(modifiers);
            }
        }

        int maxCount = configManager.getMaxAttributeCount();
        
        // 如果属性数量超限，移除多余的属性
        if (totalCount > maxCount) {
            plugin.getLogger().info("检测到OP物品: 属性修饰符数量 " + totalCount + " 超过限制 " + maxCount);
            
            int toRemoveCount = totalCount - maxCount;
            int removed = 0;
            
            for (Attribute attribute : Attribute.values()) {
                if (removed >= toRemoveCount) {
                    break;
                }
                
                Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(attribute);
                if (modifiers != null && !modifiers.isEmpty()) {
                    for (AttributeModifier modifier : new ArrayList<>(modifiers)) {
                        if (removed >= toRemoveCount) {
                            break;
                        }
                        
                        meta.removeAttributeModifier(attribute, modifier);
                        removed++;
                        plugin.getLogger().info("移除多余属性修饰符: " + attribute.name());
                    }
                }
            }
        }
    }

    /**
     * 修正刷怪蛋的NBT数据
     * 移除可能被篡改的EntityTag等数据，防止生成非预期的实体
     */
    private void fixSpawnEggNbt(ItemStack item) {
        // 检查是否是刷怪蛋
        if (!isSpawnEgg(item.getType())) {
            return;
        }
        
        if (!item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        boolean needsFix = false;
        
        // 检查并清理SpawnEggMeta中的自定义数据
        if (meta instanceof SpawnEggMeta) {
            SpawnEggMeta spawnEggMeta = (SpawnEggMeta) meta;
            
            // 获取预期的实体类型
            String expectedEntityType = getExpectedEntityType(item.getType());
            
            // 尝试通过反射检查和清理自定义生成数据
            try {
                // 尝试调用 getSpawnedType() 方法
                java.lang.reflect.Method getSpawnedTypeMethod = spawnEggMeta.getClass().getMethod("getSpawnedType");
                Object actualEntityType = getSpawnedTypeMethod.invoke(spawnEggMeta);
                
                if (actualEntityType != null) {
                    String actualTypeName = actualEntityType.toString();
                    
                    // 如果实际生成的实体类型与预期不符，需要清理
                    if (expectedEntityType != null && !expectedEntityType.equals(actualTypeName)) {
                        needsFix = true;
                        plugin.getLogger().info("检测到刷怪蛋NBT被篡改: " + item.getType().name() + " -> " + actualTypeName);
                    }
                }
            } catch (Exception e) {
                // 方法不存在或调用失败
            }
        }
        
        // 检查持久数据容器是否有自定义数据
        if (meta.getPersistentDataContainer() != null && !meta.getPersistentDataContainer().isEmpty()) {
            needsFix = true;
            plugin.getLogger().info("检测到刷怪蛋包含自定义NBT数据: " + item.getType().name());
        }
        
        // 如果需要修复，创建一个新的干净的刷怪蛋
        if (needsFix) {
            // 清理方法：创建一个新的相同类型的刷怪蛋，只保留基本属性
            ItemStack cleanEgg = new ItemStack(item.getType(), item.getAmount());
            
            // 保留显示名称和Lore（如果有的话），但移除其他NBT
            ItemMeta cleanMeta = cleanEgg.getItemMeta();
            if (cleanMeta != null) {
                if (meta.hasDisplayName()) {
                    cleanMeta.setDisplayName(meta.getDisplayName());
                }
                if (meta.hasLore()) {
                    cleanMeta.setLore(meta.getLore());
                }
                cleanEgg.setItemMeta(cleanMeta);
            }
            
            // 将原物品替换为干净的版本
            item.setType(cleanEgg.getType());
            item.setAmount(cleanEgg.getAmount());
            item.setItemMeta(cleanEgg.getItemMeta());
            
            plugin.getLogger().info("已清理刷怪蛋NBT: " + item.getType().name());
        }
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
}