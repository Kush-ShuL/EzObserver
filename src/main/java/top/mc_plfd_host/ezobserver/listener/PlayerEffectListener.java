package top.mc_plfd_host.ezobserver.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;
import top.mc_plfd_host.ezobserver.EzObserver;
import top.mc_plfd_host.ezobserver.config.PotionEffectLimitManager;

/**
 * 玩家药水效果监听器
 * 仅在玩家状态效果更新时执行检测，确保高性能
 * 
 * @author Kush_ShuL
 */
public class PlayerEffectListener implements Listener {
    
    private final EzObserver plugin;
    private final PotionEffectLimitManager potionEffectLimitManager;
    
    public PlayerEffectListener(EzObserver plugin) {
        this.plugin = plugin;
        this.potionEffectLimitManager = plugin.getPotionEffectLimitManager();
    }
    
    /**
     * 监听玩家获得药水效果事件
     * 只在效果添加或升级时检测，确保高性能
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityPotionEffect(EntityPotionEffectEvent event) {
        // 只处理玩家
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        
        // 只处理效果添加或升级的情况
        EntityPotionEffectEvent.Action action = event.getAction();
        if (action != EntityPotionEffectEvent.Action.ADDED && 
            action != EntityPotionEffectEvent.Action.CHANGED) {
            return;
        }
        
        Player player = (Player) event.getEntity();
        PotionEffect newEffect = event.getNewEffect();
        
        if (newEffect == null) {
            return;
        }
        
        // 检查新效果是否超限
        String effectName = newEffect.getType().getName();
        int amplifier = newEffect.getAmplifier();
        
        if (potionEffectLimitManager != null && potionEffectLimitManager.isOverLimit(effectName, amplifier)) {
            // 取消事件，阻止超限效果被应用
            event.setCancelled(true);
            
            int limitLevel = potionEffectLimitManager.getLimitLevel(effectName);
            boolean isPotionObtainable = potionEffectLimitManager.isPotionObtainable(effectName);
            
            if (isPotionObtainable) {
                plugin.getLogger().info(String.format("阻止玩家 %s 获得超限药水效果: %s 等级 %d (限制 %d)",
                    player.getName(), effectName, amplifier + 1, limitLevel + 1));
            } else {
                plugin.getLogger().info(String.format("阻止玩家 %s 获得超限非药水效果: %s 等级 %d (限制 %d)",
                    player.getName(), effectName, amplifier + 1, limitLevel + 1));
            }
        }
    }
}