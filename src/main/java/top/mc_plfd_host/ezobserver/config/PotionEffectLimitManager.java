package top.mc_plfd_host.ezobserver.config;

import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 药水效果限制管理器
 * 管理每种药水效果的最大等级限制
 * 
 * @author Kush_ShuL
 */
public class PotionEffectLimitManager {
    
    // 可通过药水获得的效果及其最大等级（0表示I级，1表示II级）
    private final Map<String, Integer> potionObtainableEffects;
    
    // 所有药水效果的默认最大等级（非药水可获得的效果）
    private static final int DEFAULT_NON_POTION_MAX_LEVEL = 9; // 10级（0-9）
    
    // 超过正常最大等级的容许值
    private static final int LEVEL_TOLERANCE = 2;
    
    public PotionEffectLimitManager() {
        this.potionObtainableEffects = new HashMap<>();
        initializePotionLimits();
    }
    
    /**
     * 初始化药水效果的最大等级
     * 根据Minecraft原版药水表设置
     */
    private void initializePotionLimits() {
        // 正面效果
        // 再生 - 最高II级
        potionObtainableEffects.put("REGENERATION", 1);
        
        // 迅捷 - 最高II级
        potionObtainableEffects.put("SPEED", 1);
        
        // 抗火 - 只有I级
        potionObtainableEffects.put("FIRE_RESISTANCE", 0);
        
        // 治疗 - 最高II级
        potionObtainableEffects.put("INSTANT_HEALTH", 1);
        potionObtainableEffects.put("HEAL", 1); // 旧版本名称
        
        // 夜视 - 只有I级
        potionObtainableEffects.put("NIGHT_VISION", 0);
        
        // 力量 - 最高II级
        potionObtainableEffects.put("STRENGTH", 1);
        potionObtainableEffects.put("INCREASE_DAMAGE", 1); // 旧版本名称
        
        // 跳跃提升 - 最高II级
        potionObtainableEffects.put("JUMP_BOOST", 1);
        potionObtainableEffects.put("JUMP", 1); // 旧版本名称
        
        // 水下呼吸 - 只有I级
        potionObtainableEffects.put("WATER_BREATHING", 0);
        
        // 隐身 - 只有I级
        potionObtainableEffects.put("INVISIBILITY", 0);
        
        // 缓降 - 只有I级
        potionObtainableEffects.put("SLOW_FALLING", 0);
        
        // 幸运 - 只有I级（仅Java版）
        potionObtainableEffects.put("LUCK", 0);
        
        // 负面效果
        // 中毒 - 最高II级
        potionObtainableEffects.put("POISON", 1);
        
        // 虚弱 - 只有I级
        potionObtainableEffects.put("WEAKNESS", 0);
        
        // 缓慢 - 最高IV级（神龟药水II）
        potionObtainableEffects.put("SLOWNESS", 5); // 神龟药水II提供VI级
        potionObtainableEffects.put("SLOW", 5); // 旧版本名称
        
        // 伤害 - 最高II级
        potionObtainableEffects.put("INSTANT_DAMAGE", 1);
        potionObtainableEffects.put("HARM", 1); // 旧版本名称
        
        // 凋零/衰变 - 只有I级（仅基岩版）
        potionObtainableEffects.put("WITHER", 0);
        
        // 混合效果药水中的效果
        // 抗性提升 - 最高IV级（神龟药水II）
        potionObtainableEffects.put("RESISTANCE", 3);
        potionObtainableEffects.put("DAMAGE_RESISTANCE", 3); // 旧版本名称
        
        // 1.21新增效果
        // 蓄风 - 只有I级
        potionObtainableEffects.put("WIND_CHARGED", 0);
        
        // 盘丝 - 只有I级
        potionObtainableEffects.put("WEAVING", 0);
        
        // 渗浆 - 只有I级
        potionObtainableEffects.put("OOZING", 0);
        
        // 寄生 - 只有I级
        potionObtainableEffects.put("INFESTED", 0);
    }
    
    /**
     * 检查药水效果是否可以通过药水获得
     */
    public boolean isPotionObtainable(String effectName) {
        return potionObtainableEffects.containsKey(effectName.toUpperCase());
    }
    
    /**
     * 获取药水效果的最大等级
     * @param effectName 效果名称
     * @return 最大等级（0表示I级）
     */
    public int getMaxLevel(String effectName) {
        String upperName = effectName.toUpperCase();
        if (potionObtainableEffects.containsKey(upperName)) {
            return potionObtainableEffects.get(upperName);
        }
        // 非药水可获得的效果，返回默认最大等级
        return DEFAULT_NON_POTION_MAX_LEVEL;
    }
    
    /**
     * 检查药水效果等级是否超限
     * @param effectName 效果名称
     * @param amplifier 效果等级（0表示I级）
     * @return 是否超限
     */
    public boolean isOverLimit(String effectName, int amplifier) {
        int maxLevel = getMaxLevel(effectName);
        // 允许超过正常最大等级2级
        return amplifier > maxLevel + LEVEL_TOLERANCE;
    }
    
    /**
     * 获取药水效果的限制等级（包含容许值）
     * @param effectName 效果名称
     * @return 限制等级
     */
    public int getLimitLevel(String effectName) {
        return getMaxLevel(effectName) + LEVEL_TOLERANCE;
    }
    
    /**
     * 获取所有可通过药水获得的效果名称
     */
    public Set<String> getPotionObtainableEffects() {
        return new HashSet<>(potionObtainableEffects.keySet());
    }
}