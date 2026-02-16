package top.mc_plfd_host.ezobserver.fixer;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.BlockState;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BundleMeta;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SpawnEggMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import top.mc_plfd_host.ezobserver.EzObserver;
import top.mc_plfd_host.ezobserver.checker.ItemChecker;
import top.mc_plfd_host.ezobserver.config.ConfigManager;
import top.mc_plfd_host.ezobserver.config.EnchantmentConflictManager;
import top.mc_plfd_host.ezobserver.config.PotionEffectLimitManager;

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
    private final PotionEffectLimitManager potionEffectLimitManager;
    private final ItemChecker itemChecker;

    public ItemFixer(EzObserver plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.conflictManager = plugin.getEnchantmentConflictManager();
        this.potionEffectLimitManager = plugin.getPotionEffectLimitManager();
        this.itemChecker = new ItemChecker(plugin);
    }

    public ItemStack fixItem(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) {
            return item;
        }

        ItemStack fixedItem = item.clone();

        // 修正不可破坏属性（通用）
        fixUnbreakable(fixedItem);

        // 修正非法附魔（附魔不能应用到不允许的物品上）
        fixIllegalEnchantments(fixedItem);

        // 修正冲突附魔（如果两个附魔冲突，则两个都移除）
        fixConflictingEnchantments(fixedItem);

        // 修正附魔 (包括单个附魔超限和OP附魔)
        fixEnchantments(fixedItem);
        
        // 修正OP物品附魔总等级
        fixOpEnchantments(fixedItem);

        // 修正刷怪蛋NBT
        fixSpawnEggNbt(fixedItem);

        // 修正药水效果
        fixPotionEffects(fixedItem);

        // 修正烟花火箭飞行时间
        fixFireworkRocket(fixedItem);

        // 修正无头活塞
        fixExtendedPiston(fixedItem);

        // 修正容器内容（潜影盒、箱子等）
        fixContainer(fixedItem);

        // 修正收纳袋（Bundle）内容（1.21.4+）
        fixBundle(fixedItem);

        // 修正空数据物品（可能返回新物品）
        fixedItem = fixEmptyDataItem(fixedItem);

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
     * 修正不可破坏属性
     * 移除物品的不可破坏属性
     */
    private void fixUnbreakable(ItemStack item) {
        // 检查配置是否启用不可破坏属性移除
        if (!configManager.isRemoveUnbreakable()) {
            return;
        }
        
        if (!item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        
        if (meta.isUnbreakable()) {
            meta.setUnbreakable(false);
            item.setItemMeta(meta);
            plugin.getLogger().info("已移除不可破坏属性: " + item.getType().name());
        }
    }

    /**
     * 修正非法附魔
     * 移除不能应用到此物品上的附魔
     */
    private void fixIllegalEnchantments(ItemStack item) {
        Map<Enchantment, Integer> enchantments = new HashMap<>(item.getEnchantments());
        
        if (enchantments.isEmpty()) {
            return;
        }
        
        // 创建一个干净的物品来测试附魔是否可以应用
        ItemStack cleanItem = new ItemStack(item.getType());
        
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchant = entry.getKey();
            String enchantName = enchant.getKey().getKey();
            
            // 检查附魔是否可以应用到此物品
            if (!enchant.canEnchantItem(cleanItem)) {
                item.removeEnchantment(enchant);
                plugin.getLogger().info(String.format("已移除非法附魔: %s 不能应用到 %s 上",
                    enchantName, item.getType().name()));
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
        
        for (Attribute attribute : Attribute.values()) {
            Collection<AttributeModifier> modifiers = meta.getAttributeModifiers(attribute);
            if (modifiers != null && !modifiers.isEmpty()) {
                totalCount += modifiers.size();
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
     *
     * 典型的作弊刷怪蛋示例：
     * - entity_data.id: "tnt_minecart" (应该是 allay)
     * - explosion_power: 32 (正常TNT是4)
     * - fuse: 0 (立即爆炸)
     * - unbreakable: {} (无法破坏)
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
        List<String> reasons = new ArrayList<>();
        
        // 1. 检查是否有无法破坏属性
        if (meta.isUnbreakable()) {
            needsFix = true;
            reasons.add("无法破坏属性");
        }
        
        // 2. 检查是否有附魔（刷怪蛋不应该有附魔）
        if (!item.getEnchantments().isEmpty()) {
            needsFix = true;
            reasons.add("非法附魔");
        }
        
        // 3. 检查是否有属性修饰符
        if (meta.hasAttributeModifiers()) {
            needsFix = true;
            reasons.add("属性修饰符");
        }
        
        // 4. 检查并清理SpawnEggMeta中的自定义数据 - 使用Java 16+模式匹配
        if (meta instanceof SpawnEggMeta spawnEggMeta) {
            
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
                        reasons.add("实体类型被篡改为 " + actualTypeName);
                    }
                }
            } catch (Exception e) {
                // 方法不存在或调用失败
            }
            
            // 尝试通过 getSpawnedEntity() 方法检查（Paper 1.20.4+）
            try {
                java.lang.reflect.Method getSpawnedEntityMethod = spawnEggMeta.getClass().getMethod("getSpawnedEntity");
                Object spawnedEntity = getSpawnedEntityMethod.invoke(spawnEggMeta);
                
                if (spawnedEntity != null) {
                    needsFix = true;
                    reasons.add("自定义实体数据");
                }
            } catch (Exception e) {
                // 方法不存在或调用失败
            }
        }
        
        // 5. 检查持久数据容器是否有自定义数据
        // getPersistentDataContainer()永远不会返回null，所以不需要null检查
        if (!meta.getPersistentDataContainer().isEmpty()) {
            needsFix = true;
            reasons.add("自定义NBT数据");
        }
        
        // 如果需要修复，创建一个新的干净的刷怪蛋
        if (needsFix) {
            plugin.getLogger().warning("检测到作弊刷怪蛋 " + item.getType().name() + ": " + String.join(", ", reasons));
            
            // 清理方法：创建一个新的相同类型的刷怪蛋，只保留基本属性
            ItemStack cleanEgg = new ItemStack(item.getType(), item.getAmount());
            
            // 将原物品替换为干净的版本
            item.setType(cleanEgg.getType());
            item.setAmount(cleanEgg.getAmount());
            item.setItemMeta(cleanEgg.getItemMeta());
            
            plugin.getLogger().info("已清理作弊刷怪蛋: " + item.getType().name());
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

    /**
     * 修正药水效果
     * 移除超过正常最大等级+2的药水效果
     * 对于超高等级（如126级）的效果，直接移除所有自定义效果
     */
    private void fixPotionEffects(ItemStack item) {
        // 检查是否是药水类物品
        Material type = item.getType();
        if (type != Material.POTION && type != Material.SPLASH_POTION &&
            type != Material.LINGERING_POTION && type != Material.TIPPED_ARROW) {
            return;
        }
        
        // 使用Java 16+模式匹配
        if (!(item.getItemMeta() instanceof PotionMeta potionMeta)) {
            return;
        }
        boolean metaChanged = false;
        boolean hasExtremeEffect = false;
        
        // 检查并移除超限的自定义药水效果
        if (potionMeta.hasCustomEffects()) {
            List<PotionEffectType> effectTypesToRemove = new ArrayList<>();
            
            for (PotionEffect effect : potionMeta.getCustomEffects()) {
                String effectName = effect.getType().getName();
                int amplifier = effect.getAmplifier();
                int duration = effect.getDuration();
                
                // 检查是否是极端效果（超高等级或超长时间）
                // amplifier >= 117 (约等于 126-9) 被视为极端作弊
                if (amplifier >= PotionEffectLimitManager.EXTREME_AMPLIFIER_THRESHOLD || 
                    duration >= PotionEffectLimitManager.EXTREME_DURATION_THRESHOLD) {
                    hasExtremeEffect = true;
                    effectTypesToRemove.add(effect.getType());
                    plugin.getLogger().warning(String.format("检测到极端药水效果 %s 等级 %d 持续时间 %d，将被移除",
                        effectName, amplifier + 1, duration));
                }
                // 检查是否超过正常限制
                else if (potionEffectLimitManager != null && potionEffectLimitManager.isOverLimit(effectName, amplifier)) {
                    effectTypesToRemove.add(effect.getType());
                    int limitLevel = potionEffectLimitManager.getLimitLevel(effectName);
                    plugin.getLogger().info(String.format("药水效果 %s 等级 %d 超过限制 %d，将被移除",
                        effectName, amplifier + 1, limitLevel + 1));
                }
            }
            
            // 移除超限的效果
            for (PotionEffectType effectType : effectTypesToRemove) {
                potionMeta.removeCustomEffect(effectType);
                metaChanged = true;
            }
        }
        
        // 如果检测到极端效果，清除所有自定义效果并重置药水
        if (hasExtremeEffect) {
            potionMeta.clearCustomEffects();
            metaChanged = true;
            plugin.getLogger().warning("检测到作弊药水，已清除所有自定义效果: " + item.getType().name());
        }
        
        // 如果有修改，更新物品元数据
        if (metaChanged) {
            item.setItemMeta(potionMeta);
            plugin.getLogger().info("已修正药水效果: " + item.getType().name());
        }
    }

    /**
     * 修正烟花火箭的飞行时间
     * 将超过 3 的飞行时间重置为 3
     */
    private void fixFireworkRocket(ItemStack item) {
        // 检查是否是烟花火箭
        if (item.getType() != Material.FIREWORK_ROCKET) {
            return;
        }
        
        // 使用Java 16+模式匹配
        if (!(item.getItemMeta() instanceof FireworkMeta fireworkMeta)) {
            return;
        }
        int power = fireworkMeta.getPower();
        
        // 正常飞行时间为 1-3（power 0-2 对应飞行时间 1-3）
        // 超过 3 的需要修正
        if (power > 3) {
            fireworkMeta.setPower(3); // 重置为最大合法值
            item.setItemMeta(fireworkMeta);
            plugin.getLogger().info(String.format("已将烟花火箭飞行时间从 %d 修正为 3", power));
        }
    }

    /**
     * 修正无头活塞
     * 将处于伸出状态的活塞替换为正常活塞
     */
    private void fixExtendedPiston(ItemStack item) {
        // 检查是否是活塞
        Material type = item.getType();
        if (type != Material.PISTON && type != Material.STICKY_PISTON) {
            return;
        }
        
        if (!item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        boolean needsFix = false;
        
        // 检查是否有无法破坏属性
        if (meta.isUnbreakable()) {
            meta.setUnbreakable(false);
            needsFix = true;
            plugin.getLogger().info("已移除活塞的无法破坏属性: " + type.name());
        }
        
        // 创建一个新的干净的活塞，移除所有可能的方块状态数据
        // 因为物品形式的活塞不应该有 extended=true 的状态
        ItemStack cleanPiston = new ItemStack(type, item.getAmount());
        ItemMeta cleanMeta = cleanPiston.getItemMeta();
        
        if (cleanMeta != null) {
            // 保留显示名称和Lore
            if (meta.hasDisplayName()) {
                cleanMeta.setDisplayName(meta.getDisplayName());
            }
            if (meta.hasLore()) {
                cleanMeta.setLore(meta.getLore());
            }
            cleanPiston.setItemMeta(cleanMeta);
        }
        
        // 替换原物品
        item.setType(cleanPiston.getType());
        item.setAmount(cleanPiston.getAmount());
        item.setItemMeta(cleanPiston.getItemMeta());
        
        if (needsFix) {
            plugin.getLogger().info("已清理无头活塞: " + type.name());
        }
    }

    /**
     * 修正容器（如潜影盒、箱子等）中的内容
     */
    private void fixContainer(ItemStack item) {
        if (!isContainer(item.getType())) {
            return;
        }
        
        // 使用Java 16+模式匹配
        if (!(item.getItemMeta() instanceof BlockStateMeta blockStateMeta) || !blockStateMeta.hasBlockState()) {
            return;
        }
        
        // 使用Java 16+模式匹配
        if (!(blockStateMeta.getBlockState() instanceof InventoryHolder holder)) {
            return;
        }
        
        Inventory inventory = holder.getInventory();
        
        boolean changed = false;
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack contentItem = inventory.getItem(i);
            if (contentItem == null || contentItem.getType() == Material.AIR) continue;
            
            // 检查是否是违禁物品
            if (isBannedItem(contentItem)) {
                inventory.setItem(i, null);
                changed = true;
                plugin.getLogger().warning(String.format("从容器 %s 中移除违禁物品: %s", 
                    item.getType().name(), contentItem.getType().name()));
                continue;
            }
            
            // 递归修复
            ItemStack fixedContentItem = fixItem(contentItem);
            
            // 检查修复后是否仍然违规
            if (isStillViolating(fixedContentItem)) {
                inventory.setItem(i, null);
                changed = true;
                plugin.getLogger().warning(String.format("从容器 %s 中移除无法修复的违规物品: %s", 
                    item.getType().name(), contentItem.getType().name()));
                continue;
            }
            
            if (!contentItem.equals(fixedContentItem)) {
                inventory.setItem(i, fixedContentItem);
                changed = true;
            }
        }
        
        if (changed) {
            blockStateMeta.setBlockState(blockState);
            item.setItemMeta(blockStateMeta);
            plugin.getLogger().info("已修正容器内容: " + item.getType().name());
        }
    }
    
    /**
     * 检查材质是否是容器
     */
    private boolean isContainer(Material material) {
        String name = material.name();
        return name.endsWith("SHULKER_BOX") || 
               name.equals("CHEST") || 
               name.equals("TRAPPED_CHEST") || 
               name.equals("BARREL") || 
               name.equals("DISPENSER") || 
               name.equals("DROPPER") || 
               name.equals("HOPPER") ||
               name.equals("FURNACE") ||
               name.equals("BLAST_FURNACE") ||
               name.equals("SMOKER") ||
               name.equals("BREWING_STAND") ||
               name.equals("LECTERN") ||
               name.equals("CHISELED_BOOKSHELF") ||
               name.equals("JUKEBOX") ||
               name.equals("CRAFTER");
    }

    /**
     * 修正收纳袋（Bundle）内容
     * 递归修复收纳袋中的所有物品，移除违规物品
     *
     * 典型的作弊收纳袋示例：
     * - 包含违禁物品（如光源方块）
     * - 包含作弊药水（amplifier: 124, duration: 2147483647）
     * - 包含篡改的物品展示框（entity_data.Invisible: 1b）
     * - 包含特殊盔甲架（ShowArms, Small等属性）
     */
    private void fixBundle(ItemStack item) {
        // 检查是否是收纳袋
        if (item.getType() != Material.BUNDLE) {
            return;
        }
        
        if (!item.hasItemMeta()) {
            return;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // 检查是否是 BundleMeta（1.21.4+）- 使用Java 16+模式匹配
        if (!(meta instanceof BundleMeta bundleMeta)) {
            return;
        }
        
        // 获取收纳袋中的物品
        // getItems()永远不会返回null，所以不需要null检查
        List<ItemStack> contents = bundleMeta.getItems();
        
        if (contents.isEmpty()) {
            return;
        }
        
        List<ItemStack> cleanedContents = new ArrayList<>();
        int removedCount = 0;
        int fixedCount = 0;
        
        // 递归检查和修复每个物品
        for (ItemStack contentItem : contents) {
            if (contentItem == null) continue;
            
            // 检查是否是违禁物品
            if (isBannedItem(contentItem)) {
                removedCount++;
                plugin.getLogger().warning("从收纳袋中移除违禁物品: " + contentItem.getType().name());
                continue; // 跳过违禁物品
            }
            
            // 检查是否有自定义实体数据（物品展示框、盔甲架等）
            if (hasCustomEntityData(contentItem)) {
                removedCount++;
                plugin.getLogger().warning("从收纳袋中移除包含自定义实体数据的物品: " + contentItem.getType().name());
                continue; // 跳过包含自定义实体数据的物品
            }
            
            // 递归修复物品
            ItemStack fixedContentItem = fixItem(contentItem);
            
            // 检查修复后的物品是否仍然违规
            // 如果是，则移除
            if (isStillViolating(fixedContentItem)) {
                removedCount++;
                plugin.getLogger().warning("从收纳袋中移除无法修复的违规物品: " + contentItem.getType().name());
                continue;
            }
            
            // 如果物品被修改了，计数
            if (!contentItem.equals(fixedContentItem)) {
                fixedCount++;
            }
            
            cleanedContents.add(fixedContentItem);
        }
        
        // 如果有物品被移除或修复，更新收纳袋内容
        if (removedCount > 0 || fixedCount > 0) {
            bundleMeta.setItems(cleanedContents);
            item.setItemMeta(bundleMeta);
            
            plugin.getLogger().info(String.format("已修正收纳袋: 移除 %d 个违规物品，修复 %d 个物品，保留 %d 个物品",
                removedCount, fixedCount, cleanedContents.size()));
        }
    }
    
    /**
     * 检查物品是否是违禁物品
     */
    private boolean isBannedItem(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        Material type = item.getType();
        
        // 检查禁止的物品类型
        if (configManager.isBannedMaterial(type)) {
            return true;
        }
        
        // 检查禁止的刷怪蛋类型
        if (configManager.isBannedSpawnEgg(type)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查物品是否有自定义实体数据
     * 用于检测物品展示框、盔甲架等被篡改的物品
     */
    private boolean hasCustomEntityData(ItemStack item) {
        if (!item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        Material type = item.getType();
        
        // 检查物品展示框和盔甲架
        if (type == Material.ITEM_FRAME || type == Material.GLOW_ITEM_FRAME ||
            type == Material.ARMOR_STAND) {
            
            // 检查持久数据容器
            // getPersistentDataContainer()永远不会返回null，所以不需要null检查
            if (!meta.getPersistentDataContainer().isEmpty()) {
                return true;
            }
            
            // 检查是否有自定义显示名称（通常被篡改的物品会有特殊名称）
            if (meta.hasDisplayName()) {
                String name = meta.getDisplayName();
                // 检查是否包含可疑关键词
                if (name.contains("隐形") || name.contains("Invisible") ||
                    name.contains("arms") || name.contains("small") ||
                    name.contains("Arms") || name.contains("Small")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 检查修复后的物品是否仍然违规
     * 用于判断物品是否可以保留在收纳袋中
     */
    private boolean isStillViolating(ItemStack item) {
        if (item == null) {
            return false;
        }
        
        // 使用 ItemChecker 检查是否仍有违规
        return !itemChecker.checkItem(item).isEmpty();
    }

    /**
     * 修正空数据物品
     * 空成书、空附魔书、空地图等缺少必要数据的物品会被替换为对应的基础物品
     *
     * @param item 要修正的物品
     * @return 修正后的物品（可能是新物品）
     */
    private ItemStack fixEmptyDataItem(ItemStack item) {
        Material type = item.getType();
        
        ItemMeta meta = item.getItemMeta();
        boolean needsFix = false;
        String reason = "";
        Material replacementType = null;
        
        // 检查空成书 - 替换为书与笔 - 使用Java 16+模式匹配
        if (type == Material.WRITTEN_BOOK && meta instanceof BookMeta bookMeta) {
            // 成书应该有页面内容
            if (bookMeta.getPageCount() == 0 || !bookMeta.hasAuthor() || !bookMeta.hasTitle()) {
                needsFix = true;
                reason = "空成书（没有页面内容或缺少作者/标题）";
                replacementType = Material.WRITABLE_BOOK; // 替换为书与笔
            }
        }
        
        // 检查空附魔书 - 替换为普通书 - 使用Java 16+模式匹配
        if (type == Material.ENCHANTED_BOOK && meta instanceof EnchantmentStorageMeta enchantMeta) {
            // 附魔书应该有存储的附魔
            if (!enchantMeta.hasStoredEnchants() || enchantMeta.getStoredEnchants().isEmpty()) {
                needsFix = true;
                reason = "空附魔书（没有存储的附魔）";
                replacementType = Material.BOOK; // 替换为普通书
            }
        }
        
        // 检查空地图 - 替换为空白地图 - 使用Java 16+模式匹配
        if (type == Material.FILLED_MAP && meta instanceof MapMeta mapMeta) {
            // 检查是否有地图视图
            try {
                if (!mapMeta.hasMapView()) {
                    needsFix = true;
                    reason = "空地图（没有地图数据）";
                    replacementType = Material.MAP; // 替换为空白地图
                }
            } catch (Exception e) {
                // 如果方法不存在，使用备用检查
                try {
                    java.lang.reflect.Method hasMapIdMethod = mapMeta.getClass().getMethod("hasMapId");
                    Boolean hasMapId = (Boolean) hasMapIdMethod.invoke(mapMeta);
                    if (!hasMapId) {
                        needsFix = true;
                        reason = "空地图（没有地图ID）";
                        replacementType = Material.MAP;
                    }
                } catch (Exception ex) {
                    // 忽略
                }
            }
        }
        
        // 检查知识之书 - 替换为普通书
        if (type == Material.KNOWLEDGE_BOOK) {
            try {
                Class<?> knowledgeBookMetaClass = Class.forName("org.bukkit.inventory.meta.KnowledgeBookMeta");
                if (knowledgeBookMetaClass.isInstance(meta)) {
                    java.lang.reflect.Method hasRecipesMethod = meta.getClass().getMethod("hasRecipes");
                    Boolean hasRecipes = (Boolean) hasRecipesMethod.invoke(meta);
                    if (!hasRecipes) {
                        needsFix = true;
                        reason = "空知识之书（没有配方数据）";
                        replacementType = Material.BOOK;
                    }
                }
            } catch (Exception e) {
                // 忽略
            }
        }
        
        // 如果需要修复，创建新物品替换
        // replacementType在needsFix为true时已经被赋值，所以不需要null检查
        if (needsFix) {
            plugin.getLogger().warning("检测到空数据物品: " + reason);
            
            // 创建新的替换物品
            ItemStack replacement = new ItemStack(replacementType, item.getAmount());
            
            plugin.getLogger().info("已将空数据物品 " + type.name() + " 替换为 " + replacementType.name());
            
            return replacement;
        }
        
        return item;
    }
}