package top.mc_plfd_host.ezobserver.command;

import org.bukkit.ChatColor;
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
            sender.sendMessage(messages.getNoPermission());
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
                sender.sendMessage(messages.getConfigReloaded());
                plugin.getLogger().info("配置已被 " + sender.getName() + " 重新加载");
                break;
            case "status":
                sendStatus(sender);
                break;
            case "scan":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(messages.getPlayerOnly());
                    return true;
                }
                plugin.getWorldScanner().startFullScan((Player) sender);
                break;
            case "check":
                if (!(sender instanceof Player)) {
                    sender.sendMessage(messages.getPlayerOnly());
                    return true;
                }
                checkHandItem((Player) sender);
                break;
            case "help":
            default:
                sendHelp(sender);
                break;
        }

        return true;
    }

    private void sendHelp(CommandSender sender) {
        MessageManager messages = plugin.getMessageManager();
        sender.sendMessage(messages.getMessage("help-header"));
        sender.sendMessage(messages.getMessage("help-reload"));
        sender.sendMessage(messages.getMessage("help-status"));
        sender.sendMessage(messages.getMessage("help-scan"));
        sender.sendMessage(messages.getMessage("help-check"));
        sender.sendMessage(messages.getMessage("help-footer"));
    }

    private void sendStatus(CommandSender sender) {
        MessageManager messages = plugin.getMessageManager();
        String statusOn = messages.getStatusOn();
        String statusOff = messages.getStatusOff();
        
        sender.sendMessage(messages.getMessage("status-header"));
        
        Map<String, String> placeholders = new HashMap<>();
        
        placeholders.put("status", plugin.getConfigManager().isEnabled() ? statusOn : statusOff);
        sender.sendMessage(messages.getMessage("status-enabled", placeholders));
        
        placeholders.put("status", plugin.getConfigManager().isLogViolations() ? statusOn : statusOff);
        sender.sendMessage(messages.getMessage("status-log-violations", placeholders));
        
        placeholders.put("status", plugin.getConfigManager().isConfiscateItems() ? statusOn : statusOff);
        sender.sendMessage(messages.getMessage("status-confiscate", placeholders));
        
        placeholders.put("mode", plugin.getConfigManager().getConfiscateMode());
        sender.sendMessage(messages.getMessage("status-confiscate-mode", placeholders));
        
        placeholders.put("status", plugin.getConfigManager().isBannedItemsEnabled() ? statusOn : statusOff);
        sender.sendMessage(messages.getMessage("status-banned-items", placeholders));
        
        placeholders.put("status", plugin.getConfigManager().isOpItemsEnabled() ? statusOn : statusOff);
        sender.sendMessage(messages.getMessage("status-op-items", placeholders));
        
        sender.sendMessage(messages.getMessage("status-footer"));
    }

    private void checkHandItem(Player player) {
        MessageManager messages = plugin.getMessageManager();
        ItemStack item = player.getInventory().getItemInMainHand();
        
        if (item == null || item.getType().isAir()) {
            player.sendMessage(messages.getPrefix() + messages.getMessage("check-no-item"));
            return;
        }
        
        List<String> violations = itemChecker.checkItem(item);
        
        if (violations.isEmpty()) {
            player.sendMessage(messages.getPrefix() + messages.getMessage("check-safe"));
        } else {
            player.sendMessage(messages.getPrefix() + messages.getMessage("check-violations"));
            Map<String, String> placeholders = new HashMap<>();
            for (String violation : violations) {
                placeholders.put("violation", violation);
                player.sendMessage(messages.getMessage("check-violation-item", placeholders));
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload", "status", "scan", "check", "help");
            String input = args[0].toLowerCase();
            for (String sub : subCommands) {
                if (sub.startsWith(input)) {
                    completions.add(sub);
                }
            }
        }
        
        return completions;
    }
}