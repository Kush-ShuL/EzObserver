package top.mc_plfd_host.ezobserver.checker;

import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Piston;
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
            
            // 检查不可破坏属性
            violations.addAll(checkUnbreakable(meta, item.getType()));
        }
        
        // 检查附魔
        violations.addAll(checkEnchantments(item));
        
        // 检查非法附魔（附魔不能应用到不允许的物品上）
        violations.addAll(checkIllegalEnchantments(item));
        
        // 检查冲突附魔
        violations.addAll(checkConflictingEnchantments(item));
        
        // 检查OP物品
        violations.addAll(checkOpItem(item));
        
        // 检查药水
        violations.addAll(checkPotion(item));
        
        // 检查刷怪蛋NBT
        violations.addAll(checkSpawnEgg(item));
        
        // 检查烟花火箭
        violations.addAll(checkFireworkRocket(item));
        
        // 检查无头活塞
        violations.addAll(checkExtendedPiston(item));
        
        // 检查收纳袋（Bundle）内容（1.21.4+）
        violations.addAll(checkBundle(item));
        
        // 检查空数据物品（空成书、空附魔书、空地图）
        violations.addAll(checkEmptyDataItem(item));
        
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
     * 检查非法附魔（附魔不能应用到不允许的物品上）
     * 使用 Enchantment.canEnchantItem() 来检查附魔是否可以应用到物品上
     */
    private List<String> checkIllegalEnchantments(ItemStack item) {
        List<String> violations = new ArrayList<>();
        
        Map<Enchantment, Integer> enchantments = item.getEnchantments();
        if (enchantments.isEmpty()) {
            return violations;
        }
        
        // 检查每个附魔是否可以应用到此物品
        for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
            Enchantment enchant = entry.getKey();
            String enchantName = enchant.getKey().getKey();
            
            // 使用 canEnchantItem 检查附魔是否可以应用到此物品
            // 注意：这个方法对于已经附魔的物品可能返回 false，所以我们需要创建一个干净的物品来测试
            ItemStack cleanItem = new ItemStack(item.getType());
            
            if (!enchant.canEnchantItem(cleanItem)) {
                violations.add(String.format("非法附魔: %s 不能应用到 %s 上",
                    enchantName, item.getType().name()));
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

    /**
     * 检查不可破坏属性
     * 普通物品不应该有不可破坏属性（除非是特定的物品）
     */
    private List<String> checkUnbreakable(ItemMeta meta, Material type) {
        List<String> violations = new ArrayList<>();
        
        // 检查配置是否启用不可破坏属性检测
        if (!configManager.isRemoveUnbreakable()) {
            return violations;
        }
        
        if (meta.isUnbreakable()) {
            violations.add(String.format("物品 %s 具有不可破坏属性 (疑似作弊物品)", type.name()));
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
        
        // 检查禁止的物品类型
        if (configManager.isBannedMaterial(item.getType())) {
            violations.add(String.format("禁止的物品类型: %s", item.getType().name()));
        }
        
        // 检查禁止的刷怪蛋类型
        if (configManager.isBannedSpawnEgg(item.getType())) {
            violations.add(String.format("禁止的刷怪蛋类型: %s", item.getType().name()));
        }
        
        return violations;
    }
    
    /**
     * 检查物品是否是违禁物品（需要直接删除的物品）
     * 违禁物品包括：禁止的物品类型、禁止的刷怪蛋类型
     */
    public boolean isBannedItem(ItemStack item) {
        if (item == null || !configManager.isBannedItemsEnabled()) {
            return false;
        }
        
        return configManager.isBannedMaterial(item.getType()) ||
               configManager.isBannedSpawnEgg(item.getType());
    }
    
    /**
     * 检查是否应该删除违禁物品
     */
    public boolean shouldDeleteBannedItem() {
        return configManager.isBannedItemsDeleteMode();
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
        int durationTicks = effect.getDuration();
        int duration = durationTicks / 20; // 转换为秒
        
        // 检查是否是禁止的药水效果
        if (configManager.isBannedPotionEffect(effectName)) {
            violations.add(String.format("禁止的药水效果: %s", effectName));
            return violations;
        }
        
        // 检查是否是极端作弊效果（超高等级或超长时间）
        // amplifier >= 117 或 duration 接近 Integer.MAX_VALUE 被视为极端作弊
        if (amplifier >= 117 || durationTicks >= 2147483640) {
            violations.add(String.format("极端作弊药水效果: %s 等级 %d 持续时间 %d ticks (疑似作弊物品)",
                effectName, amplifier + 1, durationTicks));
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
     *
     * 典型的作弊刷怪蛋示例：
     * - entity_data.id: "tnt_minecart" (应该是 allay)
     * - explosion_power: 32 (正常TNT是4)
     * - fuse: 0 (立即爆炸)
     * - unbreakable: {} (无法破坏)
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
        
        // 1. 检查是否有无法破坏属性（刷怪蛋不应该有）
        if (meta.isUnbreakable()) {
            violations.add(String.format("刷怪蛋 %s 具有无法破坏属性 (疑似作弊物品)", item.getType().name()));
        }
        
        // 2. 检查是否有自定义NBT数据（通过PersistentDataContainer或其他方式）
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
                        violations.add(String.format("刷怪蛋NBT被篡改: 物品类型为 %s，但实际会生成 %s (疑似作弊物品)",
                            item.getType().name(), actualTypeName));
                    }
                }
            } catch (Exception e) {
                // 方法不存在或调用失败，使用备用检测方法
            }
            
            // 尝试通过 getSpawnedEntity() 方法检查（Paper 1.20.4+）
            try {
                java.lang.reflect.Method getSpawnedEntityMethod = spawnEggMeta.getClass().getMethod("getSpawnedEntity");
                Object spawnedEntity = getSpawnedEntityMethod.invoke(spawnEggMeta);
                
                if (spawnedEntity != null) {
                    // 检查实体快照中的数据
                    violations.add(String.format("刷怪蛋 %s 包含自定义实体数据 (疑似作弊物品)", item.getType().name()));
                }
            } catch (Exception e) {
                // 方法不存在或调用失败
            }
        }
        
        // 3. 检查是否有额外的NBT数据（如EntityTag）
        // 这些数据可能被用于生成非预期的实体或携带恶意数据
        if (hasCustomEntityTag(item)) {
            violations.add(String.format("刷怪蛋 %s 包含自定义EntityTag NBT数据，可能被篡改", item.getType().name()));
        }
        
        // 4. 检查是否有附魔（刷怪蛋不应该有附魔）
        if (!item.getEnchantments().isEmpty()) {
            violations.add(String.format("刷怪蛋 %s 具有附魔 (疑似作弊物品)", item.getType().name()));
        }
        
        // 5. 检查是否有属性修饰符（刷怪蛋不应该有）
        if (meta.hasAttributeModifiers()) {
            violations.add(String.format("刷怪蛋 %s 具有属性修饰符 (疑似作弊物品)", item.getType().name()));
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

    /**
     * 检查烟花火箭是否有非法的飞行时间
     * 正常飞行时间为 1-3，超过 3 的就是非法的
     */
    private List<String> checkFireworkRocket(ItemStack item) {
        List<String> violations = new ArrayList<>();
        
        // 检查是否是烟花火箭
        if (item.getType() != Material.FIREWORK_ROCKET) {
            return violations;
        }
        
        if (!item.hasItemMeta() || !(item.getItemMeta() instanceof FireworkMeta)) {
            return violations;
        }
        
        FireworkMeta fireworkMeta = (FireworkMeta) item.getItemMeta();
        int power = fireworkMeta.getPower();
        
        // 正常飞行时间为 1-3（power 0-2 对应飞行时间 1-3）
        // 超过 3 的就是非法的
        if (power > 3) {
            violations.add(String.format("烟花火箭飞行时间 %d 超过限制 3 (疑似作弊物品)", power));
        }
        
        return violations;
    }

    /**
     * 检查活塞是否处于伸出状态（无头活塞）
     * 物品形式的活塞不应该有 extended=true 的状态
     */
    private List<String> checkExtendedPiston(ItemStack item) {
        List<String> violations = new ArrayList<>();
        
        // 检查是否是活塞
        Material type = item.getType();
        if (type != Material.PISTON && type != Material.STICKY_PISTON) {
            return violations;
        }
        
        if (!item.hasItemMeta()) {
            return violations;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // 检查是否有 BlockStateMeta
        if (meta instanceof BlockStateMeta) {
            BlockStateMeta blockStateMeta = (BlockStateMeta) meta;
            
            // 尝试获取方块数据
            if (blockStateMeta.hasBlockState()) {
                try {
                    BlockData blockData = blockStateMeta.getBlockState().getBlockData();
                    if (blockData instanceof Piston) {
                        Piston piston = (Piston) blockData;
                        if (piston.isExtended()) {
                            violations.add(String.format("检测到无头活塞: %s 处于伸出状态 (疑似作弊物品)", type.name()));
                        }
                    }
                } catch (Exception e) {
                    // 忽略异常
                }
            }
        }
        
        // 检查是否有无法破坏属性（活塞不应该有）
        if (meta.isUnbreakable()) {
            violations.add(String.format("活塞 %s 具有无法破坏属性 (疑似作弊物品)", type.name()));
        }
        
        return violations;
    }

    /**
     * 检查收纳袋（Bundle）内容
     * 收纳袋可能包含违规物品，需要递归检查
     *
     * 典型的作弊收纳袋示例：
     * - 包含违禁物品（如光源方块）
     * - 包含作弊药水（amplifier: 124, duration: 2147483647）
     * - 包含篡改的物品展示框（entity_data.Invisible: 1b）
     * - 包含特殊盔甲架（ShowArms, Small等属性）
     */
    private List<String> checkBundle(ItemStack item) {
        List<String> violations = new ArrayList<>();
        
        // 检查是否是收纳袋
        if (item.getType() != Material.BUNDLE) {
            return violations;
        }
        
        if (!item.hasItemMeta()) {
            return violations;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // 检查是否是 BundleMeta（1.21.4+）
        if (!(meta instanceof BundleMeta)) {
            return violations;
        }
        
        BundleMeta bundleMeta = (BundleMeta) meta;
        
        // 获取收纳袋中的物品
        List<ItemStack> contents = bundleMeta.getItems();
        
        if (contents == null || contents.isEmpty()) {
            return violations;
        }
        
        int violatingItemCount = 0;
        List<String> contentViolations = new ArrayList<>();
        
        // 递归检查每个物品
        for (int i = 0; i < contents.size(); i++) {
            ItemStack contentItem = contents.get(i);
            if (contentItem == null) continue;
            
            // 检查物品是否违规
            List<String> itemViolations = checkItem(contentItem);
            
            if (!itemViolations.isEmpty()) {
                violatingItemCount++;
                String itemName = contentItem.getType().name();
                contentViolations.add(String.format("  [%d] %s: %s", i + 1, itemName,
                    String.join("; ", itemViolations)));
            }
            
            // 检查是否是违禁物品
            if (isBannedItem(contentItem)) {
                violatingItemCount++;
                contentViolations.add(String.format("  [%d] 违禁物品: %s", i + 1, contentItem.getType().name()));
            }
            
            // 检查是否有自定义实体数据（物品展示框、盔甲架等）
            if (hasCustomEntityData(contentItem)) {
                violatingItemCount++;
                contentViolations.add(String.format("  [%d] 包含自定义实体数据: %s", i + 1, contentItem.getType().name()));
            }
        }
        
        if (violatingItemCount > 0) {
            violations.add(String.format("收纳袋包含 %d 个违规物品 (共 %d 个物品):",
                violatingItemCount, contents.size()));
            violations.addAll(contentViolations);
        }
        
        return violations;
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
            if (meta.getPersistentDataContainer() != null && !meta.getPersistentDataContainer().isEmpty()) {
                return true;
            }
            
            // 尝试通过反射检查 entity_data
            try {
                // 检查是否有自定义显示名称（通常被篡改的物品会有特殊名称）
                if (meta.hasDisplayName()) {
                    String name = meta.getDisplayName();
                    // 检查是否包含可疑关键词
                    if (name.contains("隐形") || name.contains("Invisible") ||
                        name.contains("arms") || name.contains("small")) {
                        return true;
                    }
                }
            } catch (Exception e) {
                // 忽略异常
            }
        }
        
        return false;
    }

    /**
     * 检查空数据物品
     * 这些物品缺少必要的数据，是作弊物品
     *
     * 典型示例：
     * - 空成书 (WRITTEN_BOOK) - 没有页面内容
     * - 空附魔书 (ENCHANTED_BOOK) - stored_enchantments: {} 为空
     * - 空地图 (FILLED_MAP) - 没有 map_id
     */
    private List<String> checkEmptyDataItem(ItemStack item) {
        List<String> violations = new ArrayList<>();
        
        Material type = item.getType();
        
        if (!item.hasItemMeta()) {
            // 某些物品必须有 ItemMeta，没有的话也是异常
            if (type == Material.WRITTEN_BOOK || type == Material.ENCHANTED_BOOK ||
                type == Material.FILLED_MAP) {
                violations.add(String.format("空数据物品: %s 缺少必要的元数据", type.name()));
            }
            return violations;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // 检查空成书
        if (type == Material.WRITTEN_BOOK) {
            if (meta instanceof BookMeta) {
                BookMeta bookMeta = (BookMeta) meta;
                
                // 成书应该有页面内容
                if (bookMeta.getPageCount() == 0) {
                    violations.add("空成书: 没有页面内容 (疑似作弊物品)");
                }
                
                // 成书应该有作者和标题
                if (!bookMeta.hasAuthor() || !bookMeta.hasTitle()) {
                    violations.add("空成书: 缺少作者或标题 (疑似作弊物品)");
                }
            }
        }
        
        // 检查空附魔书
        if (type == Material.ENCHANTED_BOOK) {
            if (meta instanceof EnchantmentStorageMeta) {
                EnchantmentStorageMeta enchantMeta = (EnchantmentStorageMeta) meta;
                
                // 附魔书应该有存储的附魔
                if (!enchantMeta.hasStoredEnchants() || enchantMeta.getStoredEnchants().isEmpty()) {
                    violations.add("空附魔书: 没有存储的附魔 (疑似作弊物品)");
                }
            }
        }
        
        // 检查空地图
        if (type == Material.FILLED_MAP) {
            if (meta instanceof MapMeta) {
                MapMeta mapMeta = (MapMeta) meta;
                
                // 已填充的地图应该有地图视图
                // 注意：在某些版本中，可能需要使用不同的方法来检查
                try {
                    // 尝试检查是否有地图视图
                    if (!mapMeta.hasMapView()) {
                        violations.add("空地图: 没有地图数据 (疑似作弊物品)");
                    }
                } catch (Exception e) {
                    // 如果方法不存在，使用备用检查
                    // 检查是否有地图 ID（通过反射）
                    try {
                        java.lang.reflect.Method hasMapIdMethod = mapMeta.getClass().getMethod("hasMapId");
                        Boolean hasMapId = (Boolean) hasMapIdMethod.invoke(mapMeta);
                        if (!hasMapId) {
                            violations.add("空地图: 没有地图ID (疑似作弊物品)");
                        }
                    } catch (Exception ex) {
                        // 忽略
                    }
                }
            }
        }
        
        // 检查知识之书
        if (type == Material.KNOWLEDGE_BOOK) {
            // 知识之书应该有配方数据
            // 在 Bukkit API 中，KnowledgeBookMeta 用于检查
            try {
                Class<?> knowledgeBookMetaClass = Class.forName("org.bukkit.inventory.meta.KnowledgeBookMeta");
                if (knowledgeBookMetaClass.isInstance(meta)) {
                    java.lang.reflect.Method hasRecipesMethod = meta.getClass().getMethod("hasRecipes");
                    Boolean hasRecipes = (Boolean) hasRecipesMethod.invoke(meta);
                    if (!hasRecipes) {
                        violations.add("空知识之书: 没有配方数据 (疑似作弊物品)");
                    }
                }
            } catch (Exception e) {
                // 忽略
            }
        }
        
        // 检查是否有异常的 enchantment_glint_override（发光效果）
        // 正常物品不应该有这个属性，除非是附魔物品
        // 注意：hasEnchantGlint() 是 1.21+ 的新方法，需要使用反射兼容旧版本
        try {
            java.lang.reflect.Method hasEnchantGlintMethod = meta.getClass().getMethod("hasEnchantGlint");
            Boolean hasEnchantGlint = (Boolean) hasEnchantGlintMethod.invoke(meta);
            
            if (hasEnchantGlint != null && hasEnchantGlint) {
                // 如果物品有发光效果但没有附魔，可能是作弊物品
                if (item.getEnchantments().isEmpty()) {
                    // 对于附魔书，检查存储的附魔
                    if (type == Material.ENCHANTED_BOOK) {
                        if (meta instanceof EnchantmentStorageMeta) {
                            EnchantmentStorageMeta enchantMeta = (EnchantmentStorageMeta) meta;
                            if (!enchantMeta.hasStoredEnchants() || enchantMeta.getStoredEnchants().isEmpty()) {
                                violations.add("异常发光效果: 物品有发光但没有附魔 (疑似作弊物品)");
                            }
                        }
                    } else {
                        // 其他物品有发光但没有附魔
                        violations.add("异常发光效果: 物品有发光但没有附魔 (疑似作弊物品)");
                    }
                }
            }
        } catch (Exception e) {
            // 方法不存在（旧版本），忽略此检查
        }
        
        return violations;
    }
}