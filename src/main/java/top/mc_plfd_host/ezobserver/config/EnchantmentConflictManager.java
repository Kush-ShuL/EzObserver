package top.mc_plfd_host.ezobserver.config;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

/**
 * 附魔冲突管理器
 * 负责管理不同物品类型的附魔冲突规则
 * 根据Minecraft原版冲突表实现
 * 
 * @author Kush_ShuL
 */
@SuppressWarnings("FieldCanBeLocal")
public class EnchantmentConflictManager {
    
    // configManager字段在构造函数中初始化后不再修改，但保留为实例字段以便未来扩展
    private final ConfigManager configManager;
    private boolean conflictDetectionEnabled = true;
    
    // 附魔冲突矩阵 - 存储每个物品类型的冲突关系
    private final Map<Material, Map<String, Set<String>>> conflictMatrix = new HashMap<>();
    
    public EnchantmentConflictManager(ConfigManager configManager) {
        this.configManager = configManager;
        initializeConflictMatrix();
    }
    
    /**
     * 初始化冲突矩阵
     */
    private void initializeConflictMatrix() {
        
        // 剑类物品的冲突
        addSwordConflicts(conflictMatrix);
        
        // 斧类物品的冲突
        addAxeConflicts(conflictMatrix);
        
        // 镐、锹类物品的冲突
        addToolConflicts(conflictMatrix);
        
        // 弓的冲突
        addBowConflicts(conflictMatrix);
        
        // 三叉戟的冲突
        addTridentConflicts(conflictMatrix);
        
        // 弩的冲突
        addCrossbowConflicts(conflictMatrix);
        
        // 盔甲类物品的冲突
        addArmorConflicts(conflictMatrix);
    }
    
    /**
     * 添加剑类冲突
     * 根据用户提供的冲突表：
     * - 亡灵杀手与节肢杀手、锋利互相冲突
     * - 节肢杀手与亡灵杀手、锋利互相冲突
     * - 锋利与亡灵杀手、节肢杀手互相冲突
     */
    private void addSwordConflicts(Map<Material, Map<String, Set<String>>> conflictMatrix) {
        Set<Material> swords = new HashSet<>(Arrays.asList(
            Material.WOODEN_SWORD, Material.STONE_SWORD, Material.IRON_SWORD,
            Material.GOLDEN_SWORD, Material.DIAMOND_SWORD, Material.NETHERITE_SWORD
        ));
        
        for (Material sword : swords) {
            Map<String, Set<String>> conflicts = conflictMatrix.computeIfAbsent(sword, k -> new HashMap<>());
            
            // 锋利、亡灵杀手、节肢杀手互相冲突（表格中显示"否"）
            conflicts.put("sharpness", new HashSet<>(Arrays.asList("smite", "bane_of_arthropods")));
            conflicts.put("smite", new HashSet<>(Arrays.asList("sharpness", "bane_of_arthropods")));
            conflicts.put("bane_of_arthropods", new HashSet<>(Arrays.asList("sharpness", "smite")));
        }
    }
    
    /**
     * 添加斧类冲突
     * 根据用户提供的冲突表：
     * - 时运与精准采集冲突
     * - 亡灵杀手与节肢杀手、锋利互相冲突
     * - 节肢杀手与亡灵杀手、锋利互相冲突
     * - 锋利与亡灵杀手、节肢杀手互相冲突
     */
    private void addAxeConflicts(Map<Material, Map<String, Set<String>>> conflictMatrix) {
        Set<Material> axes = new HashSet<>(Arrays.asList(
            Material.WOODEN_AXE, Material.STONE_AXE, Material.IRON_AXE,
            Material.GOLDEN_AXE, Material.DIAMOND_AXE, Material.NETHERITE_AXE
        ));
        
        for (Material axe : axes) {
            Map<String, Set<String>> conflicts = conflictMatrix.computeIfAbsent(axe, k -> new HashMap<>());
            
            // 时运与精准采集冲突
            conflicts.put("fortune", new HashSet<>(Collections.singletonList("silk_touch")));
            conflicts.put("silk_touch", new HashSet<>(Collections.singletonList("fortune")));
            
            // 锋利、亡灵杀手、节肢杀手互相冲突
            conflicts.put("sharpness", new HashSet<>(Arrays.asList("smite", "bane_of_arthropods")));
            conflicts.put("smite", new HashSet<>(Arrays.asList("sharpness", "bane_of_arthropods")));
            conflicts.put("bane_of_arthropods", new HashSet<>(Arrays.asList("sharpness", "smite")));
        }
    }
    
    /**
     * 添加工具类冲突（镐、锹）
     * 根据用户提供的冲突表：
     * - 时运与精准采集冲突
     */
    private void addToolConflicts(Map<Material, Map<String, Set<String>>> conflictMatrix) {
        Set<Material> pickaxes = new HashSet<>(Arrays.asList(
            Material.WOODEN_PICKAXE, Material.STONE_PICKAXE, Material.IRON_PICKAXE,
            Material.GOLDEN_PICKAXE, Material.DIAMOND_PICKAXE, Material.NETHERITE_PICKAXE
        ));
        
        Set<Material> shovels = new HashSet<>(Arrays.asList(
            Material.WOODEN_SHOVEL, Material.STONE_SHOVEL, Material.IRON_SHOVEL,
            Material.GOLDEN_SHOVEL, Material.DIAMOND_SHOVEL, Material.NETHERITE_SHOVEL
        ));
        
        // 处理镐
        for (Material pickaxe : pickaxes) {
            Map<String, Set<String>> conflicts = conflictMatrix.computeIfAbsent(pickaxe, k -> new HashMap<>());
            
            // 时运与精准采集冲突
            conflicts.put("fortune", new HashSet<>(Collections.singletonList("silk_touch")));
            conflicts.put("silk_touch", new HashSet<>(Collections.singletonList("fortune")));
        }
        
        // 处理锹（与镐相同的冲突规则）
        for (Material shovel : shovels) {
            Map<String, Set<String>> conflicts = conflictMatrix.computeIfAbsent(shovel, k -> new HashMap<>());
            
            // 时运与精准采集冲突
            conflicts.put("fortune", new HashSet<>(Collections.singletonList("silk_touch")));
            conflicts.put("silk_touch", new HashSet<>(Collections.singletonList("fortune")));
        }
    }
    
    /**
     * 添加弓的冲突
     * 根据用户提供的冲突表：
     * - 无限与经验修补冲突
     */
    private void addBowConflicts(Map<Material, Map<String, Set<String>>> conflictMatrix) {
        Map<String, Set<String>> conflicts = conflictMatrix.computeIfAbsent(Material.BOW, k -> new HashMap<>());
        
        // 无限与经验修补冲突
        conflicts.put("infinity", new HashSet<>(Collections.singletonList("mending")));
        conflicts.put("mending", new HashSet<>(Collections.singletonList("infinity")));
    }
    
    /**
     * 添加三叉戟的冲突
     * 根据用户提供的冲突表：
     * - 引雷与激流冲突
     * - 忠诚与激流冲突
     */
    private void addTridentConflicts(Map<Material, Map<String, Set<String>>> conflictMatrix) {
        Map<String, Set<String>> conflicts = conflictMatrix.computeIfAbsent(Material.TRIDENT, k -> new HashMap<>());
        
        // 引雷与激流冲突
        conflicts.put("channeling", new HashSet<>(Collections.singletonList("riptide")));
        
        // 忠诚与激流冲突
        conflicts.put("loyalty", new HashSet<>(Collections.singletonList("riptide")));
        
        // 激流与引雷、忠诚冲突
        conflicts.put("riptide", new HashSet<>(Arrays.asList("channeling", "loyalty")));
    }
    
    /**
     * 添加弩的冲突
     * 根据用户提供的冲突表：
     * - 多重射击与穿透冲突
     */
    private void addCrossbowConflicts(Map<Material, Map<String, Set<String>>> conflictMatrix) {
        Map<String, Set<String>> conflicts = conflictMatrix.computeIfAbsent(Material.CROSSBOW, k -> new HashMap<>());

        // 多重射击与穿透冲突
        conflicts.put("multishot", new HashSet<>(Collections.singletonList("piercing")));
        conflicts.put("piercing", new HashSet<>(Collections.singletonList("multishot")));
    }

    /**
     * 添加盔甲类冲突
     */
    private void addArmorConflicts(Map<Material, Map<String, Set<String>>> conflictMatrix) {
        // 头盔类（包括海龟壳）
        Set<Material> helmets = new HashSet<>(Arrays.asList(
            Material.LEATHER_HELMET, Material.CHAINMAIL_HELMET, Material.IRON_HELMET,
            Material.GOLDEN_HELMET, Material.DIAMOND_HELMET, Material.NETHERITE_HELMET
        ));
        
        // 尝试添加海龟壳（不同版本可能有不同的名称）
        try {
            helmets.add(Material.valueOf("TURTLE_HELMET"));
        } catch (IllegalArgumentException e) {
            // 忽略，可能是旧版本
        }
        
        for (Material helmet : helmets) {
            addHelmetConflicts(helmet, conflictMatrix);
        }
        
        // 胸甲类
        Set<Material> chestplates = new HashSet<>(Arrays.asList(
            Material.LEATHER_CHESTPLATE, Material.CHAINMAIL_CHESTPLATE, Material.IRON_CHESTPLATE,
            Material.GOLDEN_CHESTPLATE, Material.DIAMOND_CHESTPLATE, Material.NETHERITE_CHESTPLATE
        ));
        
        for (Material chestplate : chestplates) {
            addChestplateLeggingsConflicts(chestplate, conflictMatrix);
        }
        
        // 护腿类
        Set<Material> leggings = new HashSet<>(Arrays.asList(
            Material.LEATHER_LEGGINGS, Material.CHAINMAIL_LEGGINGS, Material.IRON_LEGGINGS,
            Material.GOLDEN_LEGGINGS, Material.DIAMOND_LEGGINGS, Material.NETHERITE_LEGGINGS
        ));
        
        for (Material leg : leggings) {
            addChestplateLeggingsConflicts(leg, conflictMatrix);
        }
        
        // 靴子类
        Set<Material> boots = new HashSet<>(Arrays.asList(
            Material.LEATHER_BOOTS, Material.CHAINMAIL_BOOTS, Material.IRON_BOOTS,
            Material.GOLDEN_BOOTS, Material.DIAMOND_BOOTS, Material.NETHERITE_BOOTS
        ));
        
        for (Material boot : boots) {
            addBootsConflicts(boot, conflictMatrix);
        }
    }
    
    /**
     * 为头盔添加冲突
     * 根据用户提供的冲突表：
     * - 保护与弹射物保护、火焰保护、爆炸保护互相冲突
     */
    private void addHelmetConflicts(Material helmet, Map<Material, Map<String, Set<String>>> conflictMatrix) {
        Map<String, Set<String>> conflicts = conflictMatrix.computeIfAbsent(helmet, k -> new HashMap<>());
        
        // 保护类型附魔互相冲突
        conflicts.put("protection", new HashSet<>(Arrays.asList(
            "projectile_protection", "fire_protection", "blast_protection"
        )));
        conflicts.put("projectile_protection", new HashSet<>(Arrays.asList(
            "protection", "fire_protection", "blast_protection"
        )));
        conflicts.put("fire_protection", new HashSet<>(Arrays.asList(
            "protection", "projectile_protection", "blast_protection"
        )));
        conflicts.put("blast_protection", new HashSet<>(Arrays.asList(
            "protection", "projectile_protection", "fire_protection"
        )));
    }
    
    /**
     * 为胸甲和护腿添加冲突
     * 根据用户提供的冲突表：
     * - 保护与弹射物保护、火焰保护、爆炸保护互相冲突
     */
    private void addChestplateLeggingsConflicts(Material armorPiece, Map<Material, Map<String, Set<String>>> conflictMatrix) {
        Map<String, Set<String>> conflicts = conflictMatrix.computeIfAbsent(armorPiece, k -> new HashMap<>());
        
        // 保护类型附魔互相冲突
        conflicts.put("protection", new HashSet<>(Arrays.asList(
            "projectile_protection", "fire_protection", "blast_protection"
        )));
        conflicts.put("projectile_protection", new HashSet<>(Arrays.asList(
            "protection", "fire_protection", "blast_protection"
        )));
        conflicts.put("fire_protection", new HashSet<>(Arrays.asList(
            "protection", "projectile_protection", "blast_protection"
        )));
        conflicts.put("blast_protection", new HashSet<>(Arrays.asList(
            "protection", "projectile_protection", "fire_protection"
        )));
    }
    
    /**
     * 为靴子添加冲突
     * 根据用户提供的冲突表：
     * - 保护与弹射物保护、火焰保护、爆炸保护互相冲突
     * - 深海探索者与冰霜行者冲突
     */
    private void addBootsConflicts(Material boot, Map<Material, Map<String, Set<String>>> conflictMatrix) {
        Map<String, Set<String>> conflicts = conflictMatrix.computeIfAbsent(boot, k -> new HashMap<>());
        
        // 保护类型附魔互相冲突
        conflicts.put("protection", new HashSet<>(Arrays.asList(
            "projectile_protection", "fire_protection", "blast_protection"
        )));
        conflicts.put("projectile_protection", new HashSet<>(Arrays.asList(
            "protection", "fire_protection", "blast_protection"
        )));
        conflicts.put("fire_protection", new HashSet<>(Arrays.asList(
            "protection", "projectile_protection", "blast_protection"
        )));
        conflicts.put("blast_protection", new HashSet<>(Arrays.asList(
            "protection", "projectile_protection", "fire_protection"
        )));
        
        // 深海探索者与冰霜行者冲突
        conflicts.put("depth_strider", new HashSet<>(Collections.singletonList("frost_walker")));
        conflicts.put("frost_walker", new HashSet<>(Collections.singletonList("depth_strider")));
    }
    
    /**
     * 检查两个附魔是否冲突
     */
    public boolean areConflicting(String enchantName1, String enchantName2, Material material) {
        Map<String, Set<String>> materialConflicts = conflictMatrix.get(material);
        if (materialConflicts == null) {
            return false;
        }
        
        Set<String> conflicts1 = materialConflicts.get(enchantName1);
        Set<String> conflicts2 = materialConflicts.get(enchantName2);
        
        return (conflicts1 != null && conflicts1.contains(enchantName2)) ||
               (conflicts2 != null && conflicts2.contains(enchantName1));
    }
    
    /**
     * 获取与指定附魔冲突的所有附魔
     */
    public Set<String> getConflictingEnchantments(String enchantName, Material material) {
        Map<String, Set<String>> materialConflicts = conflictMatrix.get(material);
        if (materialConflicts == null) {
            return new HashSet<>();
        }
        
        Set<String> conflicts = new HashSet<>(materialConflicts.getOrDefault(enchantName, new HashSet<>()));
        
        // 双向检查，确保获取所有冲突关系
        for (Map.Entry<String, Set<String>> entry : materialConflicts.entrySet()) {
            if (entry.getValue().contains(enchantName)) {
                conflicts.add(entry.getKey());
            }
        }
        
        return conflicts;
    }
    
    /**
     * 检测物品中的冲突附魔
     * @return 返回冲突附魔组的列表，每个组包含互相冲突的附魔名称
     */
    public List<Set<String>> findConflictingEnchantments(ItemStack item) {
        List<Set<String>> conflictGroups = new ArrayList<>();
        
        if (item == null || !item.hasItemMeta()) {
            return conflictGroups;
        }
        
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasEnchants()) {
            return conflictGroups;
        }
        
        Map<Enchantment, Integer> enchantments = item.getEnchantments();
        Set<String> processedEnchantments = new HashSet<>();
        
        for (Enchantment enchant : enchantments.keySet()) {
            String enchantName = enchant.getKey().getKey();
            if (processedEnchantments.contains(enchantName)) {
                continue;
            }
            
            Set<String> conflictGroup = new HashSet<>();
            conflictGroup.add(enchantName);
            
            // 查找与当前附魔冲突的所有附魔
            for (Enchantment otherEnchant : enchantments.keySet()) {
                String otherEnchantName = otherEnchant.getKey().getKey();
                if (enchantName.equals(otherEnchantName) || processedEnchantments.contains(otherEnchantName)) {
                    continue;
                }
                
                if (areConflicting(enchantName, otherEnchantName, item.getType())) {
                    conflictGroup.add(otherEnchantName);
                }
            }
            
            if (conflictGroup.size() > 1) {
                conflictGroups.add(conflictGroup);
                processedEnchantments.addAll(conflictGroup);
            }
        }
        
        return conflictGroups;
    }
    
    /**
     * 从物品中移除冲突的附魔组
     * 如果两个附魔冲突，则两个附魔都会被移除
     * @return 返回被移除的附魔名称列表
     */
    public List<String> removeConflictingEnchantments(ItemStack item) {
        List<String> removedEnchantments = new ArrayList<>();
        List<Set<String>> conflictGroups = findConflictingEnchantments(item);
        
        for (Set<String> conflictGroup : conflictGroups) {
            // 移除冲突组中的所有附魔
            for (String enchantName : conflictGroup) {
                Enchantment enchant = findEnchantmentByName(enchantName);
                if (enchant != null && item.containsEnchantment(enchant)) {
                    item.removeEnchantment(enchant);
                    removedEnchantments.add(enchantName);
                }
            }
        }
        
        return removedEnchantments;
    }
    
    /**
     * 根据附魔名称查找Enchantment对象
     */
    private Enchantment findEnchantmentByName(String name) {
        for (Enchantment enchant : Enchantment.values()) {
            if (enchant.getKey().getKey().equals(name)) {
                return enchant;
            }
        }
        return null;
    }
    
    /**
     * 检查是否启用冲突检测
     */
    public boolean isConflictDetectionEnabled() {
        return conflictDetectionEnabled;
    }
    
    /**
     * 设置是否启用冲突检测
     */
    public void setConflictDetectionEnabled(boolean enabled) {
        this.conflictDetectionEnabled = enabled;
    }
}