package top.mc_plfd_host.ezobserver.command;

import net.kyori.adventure.text.Component;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import top.mc_plfd_host.ezobserver.EzObserver;
import top.mc_plfd_host.ezobserver.checker.ItemChecker;
import top.mc_plfd_host.ezobserver.config.MessageManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EzObserverCommand implements CommandExecutor, TabCompleter {

    private final EzObserver plugin;
    private final ItemChecker itemChecker;

    public EzObserverCommand(EzObserver plugin) {
        this.plugin = plugin;
        this.itemChecker = new ItemChecker(plugin);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        MessageManager messages = plugin.getMessageManager();
        
        if (!sender.hasPermission("ezobserver.admin")) {
            sendMessage(sender, messages.getNoPermission());
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.getConfigManager().reloadConfig();
                plugin.getMessageManager().reloadMessages();
                sendMessage(sender, messages.getConfigReloaded());
                plugin.getLogger().info("配置已被 " + sender.getName() + " 重新加载");
                break;
            case "status":
                sendStatus(sender);
                break;
            case "scan":
                if (!(sender instanceof Player)) {
                    sendMessage(sender, messages.getPlayerOnly());
                    return true;
                }
                plugin.getWorldScanner().startFullScan((Player) sender);
                break;
            case "check":
                if (!(sender instanceof Player)) {
                    sendMessage(sender, messages.getPlayerOnly());
                    return true;
                }
                checkHandItem((Player) sender);
                break;
            case "whitelist":
                handleWhitelistCommand(sender, args);
                break;
            case "help":
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendMessage(CommandSender sender, Component component) {
        plugin.adventure().sender(sender).sendMessage(component);
    }

    private void sendHelp(CommandSender sender) {
        MessageManager messages = plugin.getMessageManager();
        sendMessage(sender, messages.getMessage("help-header"));
        sendMessage(sender, messages.getMessage("help-reload"));
        sendMessage(sender, messages.getMessage("help-status"));
        sendMessage(sender, messages.getMessage("help-scan"));
        sendMessage(sender, messages.getMessage("help-check"));
        sendMessage(sender, messages.getMessage("help-whitelist"));
        sendMessage(sender, messages.getMessage("help-footer"));
    }

    private void sendStatus(CommandSender sender) {
        MessageManager messages = plugin.getMessageManager();
        String statusOn = messages.getStatusOn();
        String statusOff = messages.getStatusOff();
        
        sendMessage(sender, messages.getMessage("status-header"));
        
        Map<String, String> placeholders = new HashMap<>();
        
        placeholders.put("status", plugin.getConfigManager().isEnabled() ? statusOn : statusOff);
        sendMessage(sender, messages.getMessage("status-enabled", placeholders));
        
        placeholders.put("status", plugin.getConfigManager().isLogViolations() ? statusOn : statusOff);
        sendMessage(sender, messages.getMessage("status-log-violations", placeholders));
        
        placeholders.put("status", plugin.getConfigManager().isConfiscateItems() ? statusOn : statusOff);
        sendMessage(sender, messages.getMessage("status-confiscate", placeholders));
        
        placeholders.put("mode", plugin.getConfigManager().getConfiscateMode());
        sendMessage(sender, messages.getMessage("status-confiscate-mode", placeholders));
        
        placeholders.put("status", plugin.getConfigManager().isBannedItemsEnabled() ? statusOn : statusOff);
        sendMessage(sender, messages.getMessage("status-banned-items", placeholders));
        
        placeholders.put("status", plugin.getConfigManager().isOpItemsEnabled() ? statusOn : statusOff);
        sendMessage(sender, messages.getMessage("status-op-items", placeholders));
        
        sendMessage(sender, messages.getMessage("status-footer"));
    }

    private void checkHandItem(Player player) {
        MessageManager messages = plugin.getMessageManager();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType().isAir()) {
            sendMessage(player, messages.getPrefix().append(messages.getMessage("check-no-item")));
            return;
        }
        
        List<String> violations = itemChecker.checkItem(item);
        
        if (violations.isEmpty()) {
            sendMessage(player, messages.getPrefix().append(messages.getMessage("check-safe")));
        } else {
            sendMessage(player, messages.getPrefix().append(messages.getMessage("check-violations")));
            Map<String, String> placeholders = new HashMap<>();
            for (String violation : violations) {
                placeholders.put("violation", violation);
                sendMessage(player, messages.getMessage("check-violation-item", placeholders));
            }
        }
    }

    private void handleWhitelistCommand(CommandSender sender, String[] args) {
        MessageManager messages = plugin.getMessageManager();
        
        if (args.length < 2) {
            sendWhitelistHelp(sender);
            return;
        }
        
        switch (args[1].toLowerCase()) {
            case "add":
                if (!(sender instanceof Player)) {
                    sendMessage(sender, messages.getPlayerOnly());
                    return;
                }
                addToWhitelist((Player) sender);
                break;
            case "remove":
                if (!(sender instanceof Player)) {
                    sendMessage(sender, messages.getPlayerOnly());
                    return;
                }
                removeFromWhitelist((Player) sender);
                break;
            case "list":
                listWhitelist(sender);
                break;
            case "reload":
                plugin.getConfigManager().getWhitelistManager().reloadWhitelist();
                sendMessage(sender, messages.getMessage("whitelist-reloaded"));
                break;
            default:
                sendWhitelistHelp(sender);
                break;
        }
    }
    
    private void sendWhitelistHelp(CommandSender sender) {
        MessageManager messages = plugin.getMessageManager();
        sendMessage(sender, messages.getMessage("whitelist-help"));
    }
    
    private void addToWhitelist(Player player) {
        MessageManager messages = plugin.getMessageManager();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType().isAir()) {
            sendMessage(player, messages.getMessage("whitelist-no-item"));
            return;
        }
        
        plugin.getConfigManager().getWhitelistManager().addToWhitelist(item);
        sendMessage(player, messages.getMessage("whitelist-added"));
    }
    
    private void removeFromWhitelist(Player player) {
        MessageManager messages = plugin.getMessageManager();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType().isAir()) {
            sendMessage(player, messages.getMessage("whitelist-no-item"));
            return;
        }
        
        plugin.getConfigManager().getWhitelistManager().removeFromWhitelist(item);
        sendMessage(player, messages.getMessage("whitelist-removed"));
    }
    
    private void listWhitelist(CommandSender sender) {
        MessageManager messages = plugin.getMessageManager();
        List<String> whitelist = plugin.getConfigManager().getWhitelistManager().getWhitelistEntries();
        
        if (whitelist.isEmpty()) {
            sendMessage(sender, messages.getMessage("whitelist-empty"));
            return;
        }
        
        sendMessage(sender, messages.getMessage("whitelist-list-header"));
        for (String entry : whitelist) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("entry", entry);
            sendMessage(sender, messages.getMessage("whitelist-list-item", placeholders));
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload", "status", "scan", "check", "whitelist", "help");
            String input = args[0].toLowerCase();
            for (String sub : subCommands) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("whitelist")) {
            List<String> whitelistCommands = Arrays.asList("add", "remove", "list", "reload");
            String input = args[1].toLowerCase();
            for (String sub : whitelistCommands) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        }
        
        return completions;
    }
}