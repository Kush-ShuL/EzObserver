package top.mc_plfd_host.ezobserver.scanner;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import top.mc_plfd_host.ezobserver.EzObserver;
import top.mc_plfd_host.ezobserver.checker.ItemChecker;
import top.mc_plfd_host.ezobserver.config.ConfigManager;
import top.mc_plfd_host.ezobserver.config.MessageManager;
import top.mc_plfd_host.ezobserver.fixer.ItemFixer;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class WorldScanner {

    private final EzObserver plugin;
    private final ConfigManager configManager;
    private final ItemChecker itemChecker;
    private final ItemFixer itemFixer;
    private final Logger logger;
    
    private boolean isScanning = false;
    private AtomicInteger scannedPlayers = new AtomicInteger(0);
    private AtomicInteger scannedOfflinePlayers = new AtomicInteger(0);
    private AtomicInteger scannedContainers = new AtomicInteger(0);
    private AtomicInteger violationsFound = new AtomicInteger(0);
    private AtomicInteger itemsFixed = new AtomicInteger(0);
    private AtomicInteger itemsDeleted = new AtomicInteger(0);

    public WorldScanner(EzObserver plugin) {
        this.plugin = plugin;
        this.configManager = plugin.getConfigManager();
        this.itemChecker = new ItemChecker(plugin);
        this.itemFixer = new ItemFixer(plugin);
        this.logger = plugin.getLogger();
    }

    public boolean isScanning() {
        return isScanning;
    }

    public void startFullScan(Player executor) {
        MessageManager messages = plugin.getMessageManager();
        if (isScanning) {
            sendMessage(executor, messages.getScanAlreadyRunning());
            return;
        }

        isScanning = true;
        resetCounters();
        
        sendMessage(executor, messages.getScanStarted());
        sendMessage(executor, messages.getScanWarningFolia());
        logger.info("开始全服扫描，执行者: " + executor.getName());

        try {
            // 扫描在线玩家背包
            scanOnlinePlayers();
            
            // 扫描离线玩家数据
            scanOfflinePlayers();
            
            // 暂时跳过世界容器扫描（Folia线程限制）
            sendMessage(executor, messages.getScanSkippedContainers());
            
            // 完成扫描
            isScanning = false;
            sendScanSummary(executor);
            logger.info("全服扫描完成");
            
        } catch (Exception e) {
            isScanning = false;
            logger.severe("扫描过程中发生错误: " + e.getMessage());
            e.printStackTrace();
            sendMessage(executor, messages.getScanError(e.getMessage()));
        }
    }

    private void sendMessage(Player player, Component component) {
        plugin.adventure().player(player).sendMessage(component);
    }

    private void resetCounters() {
        scannedPlayers.set(0);
        scannedOfflinePlayers.set(0);
        scannedContainers.set(0);
        violationsFound.set(0);
        itemsFixed.set(0);
        itemsDeleted.set(0);
    }

    private void scanOnlinePlayers() {
        logger.info("开始扫描在线玩家背包...");
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("ezobserver.bypass")) {
                continue;
            }
            
            scannedPlayers.incrementAndGet();
            PlayerInventory inventory = player.getInventory();
            
            // 扫描主手物品
            scanInventorySlot(inventory, player.getInventory().getHeldItemSlot(), player.getName() + " 主手");
            
            // 扫描副手物品
            ItemStack offHand = inventory.getItemInOffHand();
            if (offHand != null && offHand.getType() != Material.AIR) {
                scanSingleItem(offHand, player.getName() + " 副手");
            }
            
            // 扫描装备栏
            scanInventorySlot(inventory, 36, player.getName() + " 头盔");
            scanInventorySlot(inventory, 37, player.getName() + " 胸甲");
            scanInventorySlot(inventory, 38, player.getName() + " 护腿");
            scanInventorySlot(inventory, 39, player.getName() + " 靴子");
            
            // 扫描背包栏
            for (int i = 0; i < 27; i++) {
                int slot = i + 9; // 背包栏从第9格开始
                scanInventorySlot(inventory, slot, player.getName() + " 背包");
            }
        }
        
        logger.info("在线玩家背包扫描完成");
    }

    private void scanInventorySlot(Inventory inventory, int slot, String source) {
        ItemStack item = inventory.getItem(slot);
        if (item != null && item.getType() != Material.AIR) {
            scanSingleItem(item, source);
        }
    }

    private void scanSingleItem(ItemStack item, String source) {
        List<String> violations = itemChecker.checkItem(item);
        if (!violations.isEmpty()) {
            violationsFound.incrementAndGet();
            
            String violationLog = String.format("发现违规物品 - 来源: %s, 物品: %s, 原因: %s",
                source, item.getType().name(), String.join(", ", violations));
            logger.warning(violationLog);
            
            // 注意：在Folia中修改玩家背包可能需要特殊处理
            // 这里仅记录，不自动修改以避免线程安全问题
        }
    }

    private void scanOfflinePlayers() {
        logger.info("开始扫描离线玩家数据...");
        
        File playerDataFolder = new File(Bukkit.getWorlds().get(0).getWorldFolder(), "playerdata");
        if (!playerDataFolder.exists()) {
            return;
        }

        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (offlinePlayer.isOnline()) {
                continue;
            }

            UUID uuid = offlinePlayer.getUniqueId();
            File playerFile = new File(playerDataFolder, uuid.toString() + ".dat");
            
            if (playerFile.exists()) {
                scannedOfflinePlayers.incrementAndGet();
                // 注意：直接读取离线玩家数据需要NBT操作，这里简化处理
                logger.info("检测到离线玩家数据: " + offlinePlayer.getName() + " (UUID: " + uuid + ")");
            }
        }
        
        logger.info("离线玩家数据扫描完成");
    }

    private void sendScanSummary(Player executor) {
        MessageManager messages = plugin.getMessageManager();
        sendMessage(executor, messages.getMessage("scan-complete"));
        
        Map<String, String> placeholders = new HashMap<>();
        
        placeholders.put("count", String.valueOf(scannedPlayers.get()));
        sendMessage(executor, messages.getMessage("scan-players", placeholders));
        
        placeholders.put("count", String.valueOf(scannedContainers.get()));
        sendMessage(executor, messages.getMessage("scan-chunks", placeholders)); // 借用 scan-chunks
        
        placeholders.put("count", String.valueOf(violationsFound.get()));
        sendMessage(executor, messages.getMessage("scan-violations", placeholders));
        
        if (configManager.isDeleteMode()) {
            placeholders.put("count", String.valueOf(itemsDeleted.get()));
            sendMessage(executor, messages.getMessage("scan-deleted", placeholders));
        } else if (configManager.isFixMode()) {
            placeholders.put("count", String.valueOf(itemsFixed.get()));
            sendMessage(executor, messages.getMessage("scan-fixed", placeholders));
        }
    }

    public int getScannedPlayers() {
        return scannedPlayers.get();
    }

    public int getScannedOfflinePlayers() {
        return scannedOfflinePlayers.get();
    }

    public int getScannedContainers() {
        return scannedContainers.get();
    }

    public int getViolationsFound() {
        return violationsFound.get();
    }
}