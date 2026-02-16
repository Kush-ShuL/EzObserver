package top.mc_plfd_host.ezobserver.listener;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import top.mc_plfd_host.ezobserver.EzObserver;
import top.mc_plfd_host.ezobserver.checker.ItemChecker;
import top.mc_plfd_host.ezobserver.config.ConfigManager;
import top.mc_plfd_host.ezobserver.fixer.ItemFixer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.logging.Logger;

import net.kyori.adventure.text.minimessage.MiniMessage;

public class ItemMoveListener implements Listener {

    private final EzObserver plugin;
    private final ConfigManager configManager;
    private final ItemChecker itemChecker;
    private final ItemFixer itemFixer;
    private final Logger logger;

    public ItemMoveListener(EzObserver plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.itemChecker = new ItemChecker(plugin);
        this.itemFixer = new ItemFixer(plugin);
        this.logger = plugin.getLogger();
    }

    /**
     * 检查物品是否包含禁止的名称或Lore
     */
    private boolean hasBannedNameOrLore(ItemStack item) {
        if (!item.hasItemMeta()) {
            return false;
        }
        
        ItemMeta meta = item.getItemMeta();
        
        // 检查物品名称
        if (meta.hasDisplayName()) {
            String displayName = meta.getDisplayName();
            for (String keyword : configManager.getBannedNameKeywords()) {
                if (displayName.contains(keyword)) {
                    return true;
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
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }

    /**
     * 检查物品是否是不可修复的违规物品（必须删除）
     */
    private boolean isUnfixableViolation(ItemStack item) {
        // 禁止的物品类型
        if (configManager.isBannedMaterial(item.getType())) {
            return true;
        }
        
        // 禁止的刷怪蛋类型
        if (configManager.isBannedSpawnEgg(item.getType())) {
            return true;
        }
        
        // 禁止的名称或Lore
        if (hasBannedNameOrLore(item)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查是否应该删除违禁物品
     * 根据配置的 action-mode 决定
     */
    private boolean shouldDeleteBannedItem(ItemStack item) {
        // 如果是违禁物品
        if (itemChecker.isBannedItem(item)) {
            // 检查配置的处理模式
            return configManager.isBannedItemsDeleteMode();
        }
        return false;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!shouldProcessEvent(event)) return;
        
        ItemStack item = event.getCurrentItem();
        if (item == null) return;
        
        List<String> violations = itemChecker.checkItem(item);
        if (!violations.isEmpty()) {
            handleViolation(event.getWhoClicked() instanceof Player ? (Player) event.getWhoClicked() : null, item, violations);
            
            if (configManager.isConfiscateItems()) {
                handleConfiscation(event, item, event.getWhoClicked() instanceof Player ? (Player) event.getWhoClicked() : null);
            }
        }
    }
    
    private boolean shouldProcessEvent(InventoryClickEvent event) {
        return configManager.isEnabled();
    }
    
    private void handleConfiscation(InventoryClickEvent event, ItemStack item, Player player) {
        boolean unfixable = isUnfixableViolation(item);
        
        if (configManager.isDeleteMode() || unfixable) {
            event.setCurrentItem(null);
            event.setCancelled(true);
            if (unfixable) {
                logger.info("[EzObserver] 已删除禁止物品: " + item.getType().name());
            }
        } else if (configManager.isStoreMode()) {
            storeConfiscatedItem(player, item);
            event.setCurrentItem(null);
            event.setCancelled(true);
        } else if (configManager.isFixMode()) {
            ItemStack fixedItem = itemFixer.fixItem(item);
            event.setCurrentItem(fixedItem);
            logger.info("[EzObserver] 已修正违规物品");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryMoveItem(InventoryMoveItemEvent event) {
        if (!shouldProcessEvent(event)) return;
        
        ItemStack item = event.getItem();
        List<String> violations = itemChecker.checkItem(item);
        
        if (!violations.isEmpty()) {
            handleViolation(null, item, violations);
            
            if (configManager.isConfiscateItems()) {
                handleConfiscation(event, item, null);
            }
        }
    }
    
    private boolean shouldProcessEvent(InventoryMoveItemEvent event) {
        return configManager.isEnabled() && configManager.isStrictMode();
    }
    
    private void handleConfiscation(InventoryMoveItemEvent event, ItemStack item, Player player) {
        boolean unfixable = isUnfixableViolation(item);
        
        if (configManager.isDeleteMode() || unfixable) {
            event.setCancelled(true);
            if (unfixable) {
                logger.info("[EzObserver] 已阻止禁止物品移动: " + item.getType().name());
            }
        } else if (configManager.isStoreMode()) {
            storeConfiscatedItem(player, item);
            event.setCancelled(true);
        } else if (configManager.isFixMode()) {
            ItemStack fixedItem = itemFixer.fixItem(item);
            event.setItem(fixedItem);
            logger.info("[EzObserver] 已修正违规物品");
        }
    }

    // 严格模式：检测物品拖拽
    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!shouldProcessEvent(event)) return;
        
        for (ItemStack item : event.getNewItems().values()) {
            if (item == null) continue;
            
            List<String> violations = itemChecker.checkItem(item);
            if (!violations.isEmpty()) {
                handleViolation(event.getWhoClicked() instanceof Player ? (Player) event.getWhoClicked() : null, item, violations);
                
                if (configManager.isConfiscateItems()) {
                    handleConfiscation(event, item, event.getWhoClicked() instanceof Player ? (Player) event.getWhoClicked() : null);
                }
                
                event.setCancelled(true);
                break;
            }
        }
    }
    
    private boolean shouldProcessEvent(InventoryDragEvent event) {
        return configManager.isEnabled() && configManager.isStrictMode();
    }
    
    private void handleConfiscation(InventoryDragEvent event, ItemStack item, Player player) {
        boolean unfixable = isUnfixableViolation(item);
        
        if (configManager.isDeleteMode() || unfixable) {
            event.setCancelled(true);
            if (unfixable) {
                logger.info("[EzObserver] 已阻止禁止物品拖拽: " + item.getType().name());
            }
        } else if (configManager.isStoreMode()) {
            storeConfiscatedItem(player, item);
            event.setCancelled(true);
        } else if (configManager.isFixMode()) {
            ItemStack fixedItem = itemFixer.fixItem(item);
            // 由于拖拽事件无法直接修改物品，只能取消事件
            logger.info("[EzObserver] 拖拽物品包含违规内容，已阻止操作");
        }
    }

    // 严格模式：检测玩家丢弃物品
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (!shouldProcessEvent(event)) return;
        
        ItemStack item = event.getItemDrop().getItemStack();
        List<String> violations = itemChecker.checkItem(item);
        
        if (!violations.isEmpty()) {
            handleViolation(event.getPlayer(), item, violations);
            
            if (configManager.isConfiscateItems()) {
                handleConfiscation(event, item, event.getPlayer());
            }
            
            event.getItemDrop().remove();
            event.setCancelled(true);
        }
    }
    
    private boolean shouldProcessEvent(PlayerDropItemEvent event) {
        return configManager.isEnabled() && configManager.isStrictMode();
    }
    
    private void handleConfiscation(PlayerDropItemEvent event, ItemStack item, Player player) {
        boolean unfixable = isUnfixableViolation(item);
        
        if (configManager.isDeleteMode() || unfixable) {
            if (unfixable) {
                logger.info("[EzObserver] 已阻止丢弃禁止物品: " + item.getType().name());
            }
        } else if (configManager.isStoreMode()) {
            storeConfiscatedItem(player, item);
        } else if (configManager.isFixMode()) {
            ItemStack fixedItem = itemFixer.fixItem(item);
            event.getItemDrop().setItemStack(fixedItem);
            logger.info("[EzObserver] 已修正违规物品");
        }
    }

    // 严格模式：检测玩家切换手持物品
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        if (!shouldProcessEvent(event)) return;
        
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItem(event.getNewSlot());
        
        if (item == null) return;
        
        List<String> violations = itemChecker.checkItem(item);
        if (!violations.isEmpty()) {
            handleViolation(player, item, violations);
            
            if (configManager.isConfiscateItems()) {
                handleConfiscation(event, item, player);
            }
        }
    }
    
    private boolean shouldProcessEvent(PlayerItemHeldEvent event) {
        return configManager.isEnabled() && configManager.isStrictMode();
    }
    
    private void handleConfiscation(PlayerItemHeldEvent event, ItemStack item, Player player) {
        boolean unfixable = isUnfixableViolation(item);
        
        if (configManager.isDeleteMode() || unfixable) {
            player.getInventory().setItem(event.getNewSlot(), null);
            if (unfixable) {
                logger.info("[EzObserver] 已阻止使用禁止物品: " + item.getType().name());
            }
        } else if (configManager.isStoreMode()) {
            storeConfiscatedItem(player, item);
            player.getInventory().setItem(event.getNewSlot(), null);
        } else if (configManager.isFixMode()) {
            ItemStack fixedItem = itemFixer.fixItem(item);
            player.getInventory().setItem(event.getNewSlot(), fixedItem);
            logger.info("[EzObserver] 已修正违规物品");
        }
    }

    // 严格模式：检测玩家交换手中物品
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (!shouldProcessEvent(event)) return;
        
        ItemStack mainHand = event.getMainHandItem();
        ItemStack offHand = event.getOffHandItem();
        
        if (mainHand != null) {
            List<String> violations = itemChecker.checkItem(mainHand);
            if (!violations.isEmpty()) {
                handleViolation(event.getPlayer(), mainHand, violations);
                handleConfiscation(event, mainHand, event.getPlayer());
                event.setCancelled(true);
                return;
            }
        }
        
        if (offHand != null) {
            List<String> violations = itemChecker.checkItem(offHand);
            if (!violations.isEmpty()) {
                handleViolation(event.getPlayer(), offHand, violations);
                handleConfiscation(event, offHand, event.getPlayer());
                event.setCancelled(true);
            }
        }
    }
    
    private boolean shouldProcessEvent(PlayerSwapHandItemsEvent event) {
        return configManager.isEnabled() && configManager.isStrictMode();
    }
    
    private void handleConfiscation(PlayerSwapHandItemsEvent event, ItemStack item, Player player) {
        boolean unfixable = isUnfixableViolation(item);
        
        if (configManager.isDeleteMode() || unfixable) {
            if (unfixable) {
                logger.info("[EzObserver] 已阻止使用禁止物品: " + item.getType().name());
            }
        } else if (configManager.isStoreMode()) {
            storeConfiscatedItem(player, item);
        } else if (configManager.isFixMode()) {
            ItemStack fixedItem = itemFixer.fixItem(item);
            // 由于事件无法直接修改物品，只能取消事件
            logger.info("[EzObserver] 检测到违规物品，已阻止交换");
        }
    }

    // 严格模式：检测玩家使用物品
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!shouldProcessEvent(event)) return;
        
        ItemStack item = event.getItem();
        if (item == null) return;
        
        List<String> violations = itemChecker.checkItem(item);
        if (!violations.isEmpty()) {
            handleViolation(event.getPlayer(), item, violations);
            handleConfiscation(event, item, event.getPlayer());
            event.setCancelled(true);
        }
    }
    
    private boolean shouldProcessEvent(PlayerInteractEvent event) {
        return configManager.isEnabled() && configManager.isStrictMode();
    }
    
    private void handleConfiscation(PlayerInteractEvent event, ItemStack item, Player player) {
        boolean unfixable = isUnfixableViolation(item);
        
        if (configManager.isDeleteMode() || unfixable) {
            event.getPlayer().getInventory().setItemInMainHand(null);
            if (unfixable) {
                logger.info("[EzObserver] 已阻止使用禁止物品: " + item.getType().name());
            }
        } else if (configManager.isStoreMode()) {
            storeConfiscatedItem(player, item);
            event.getPlayer().getInventory().setItemInMainHand(null);
        } else if (configManager.isFixMode()) {
            ItemStack fixedItem = itemFixer.fixItem(item);
            event.getPlayer().getInventory().setItemInMainHand(fixedItem);
            logger.info("[EzObserver] 已修正违规物品");
        }
    }
    
    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (!configManager.isEnabled()) return;
        
        ItemStack item = event.getItem().getItemStack();
        List<String> violations = itemChecker.checkItem(item);
        
        if (!violations.isEmpty()) {
            handleViolation(event.getPlayer(), item, violations);
            
            if (configManager.isConfiscateItems()) {
                boolean unfixable = isUnfixableViolation(item);
                
                if (configManager.isDeleteMode() || unfixable) {
                    event.getItem().remove();
                    event.setCancelled(true);
                    if (unfixable) {
                        logger.info("已删除禁止物品: " + item.getType().name());
                    }
                } else if (configManager.isStoreMode()) {
                    storeConfiscatedItem(event.getPlayer(), item);
                    event.getItem().remove();
                    event.setCancelled(true);
                } else if (configManager.isFixMode()) {
                    ItemStack fixedItem = itemFixer.fixItem(item);
                    event.getItem().setItemStack(fixedItem);
                    logger.info("已修正违规物品");
                }
            }
        }
    }

    private void handleViolation(Player player, ItemStack item, List<String> violations) {
        String playerName = player != null ? player.getName() : "未知";
        String itemName = item.getType().name();
        String reason = String.join(", ", violations);

        // 异步处理日志和广播
        plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> {
            try {
                // 记录日志
                if (configManager.isLogViolations()) {
                    StringBuilder logMessage = new StringBuilder();
                    logMessage.append("[EzObserver] 检测到违规物品: ").append(itemName);

                    if (player != null) {
                        logMessage.append(" | 玩家: ").append(playerName);
                    }

                    logMessage.append(" | 违规原因: ");
                    for (String violation : violations) {
                        logMessage.append("\n  - ").append(violation);
                    }

                    logger.warning(logMessage.toString());
                }

                // 广播消息
                if (configManager.isBroadcastViolations()) {
                    java.util.Map<String, String> placeholders = new java.util.HashMap<>();
                    placeholders.put("player", playerName);
                    placeholders.put("item", itemName);
                    placeholders.put("reason", reason);

                    String mode = configManager.getConfiscateMode();
                    // 如果是违禁物品且配置了删除模式，覆盖 mode
                    if (isUnfixableViolation(item)) {
                        mode = "delete";
                    }

                    // 修正：我们需要根据 mode 获取对应的消息 key
                    String key = "broadcast-delete";
                    if ("store".equalsIgnoreCase(mode)) key = "broadcast-store";
                    else if ("fix".equalsIgnoreCase(mode)) key = "broadcast-fix";

                    plugin.getServer().broadcast(plugin.getMessageManager().getMessage(key, placeholders));
                }
            } catch (Exception e) {
                logger.severe("处理违规物品时发生错误: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    private void storeConfiscatedItem(Player player, ItemStack item) {
        try {
            File storageDir = new File(configManager.getConfiscateStoragePath());
            if (!storageDir.exists()) {
                storageDir.mkdirs();
            }

            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
            String playerName = player != null ? player.getName() : "unknown";
            String fileName = String.format("%s_%s_%s.yml", timestamp, playerName, item.getType().name());
            File file = new File(storageDir, fileName);

            YamlConfiguration config = new YamlConfiguration();
            config.set("item", item);
            config.set("player", playerName);
            config.set("timestamp", timestamp);
            config.set("type", item.getType().name());
            
            config.save(file);
            logger.info("已存储没收物品: " + fileName);
        } catch (IOException e) {
            logger.severe("存储没收物品失败: " + e.getMessage());
        }
    }
    private void logViolationToFile(Player player, ItemStack item, List<String> violations) {
        try {
            File logFile = new File(plugin.getDataFolder(), "violations.log");
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = sdf.format(new Date());
            
            String logEntry = String.format("[%s] 玩家: %s, 物品: %s, 违规原因: %s%n",
                timestamp, player != null ? player.getName() : "未知", item.getType().name(), String.join(", ", violations));
            
            Files.write(logFile.toPath(), logEntry.getBytes(StandardCharsets.UTF_8), StandardOpenOption.APPEND);
            
        } catch (IOException e) {
            logger.severe("写入违规日志时发生错误: " + e.getMessage());
        }
    }
}