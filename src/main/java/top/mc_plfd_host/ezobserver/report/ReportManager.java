package top.mc_plfd_host.ezobserver.report;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import top.mc_plfd_host.ezobserver.EzObserver;
import top.mc_plfd_host.ezobserver.monitor.RealTimeMonitor;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ReportManager {
    
    private final EzObserver plugin;
    private final RealTimeMonitor realTimeMonitor;
    // reports集合用于缓存报告，虽然主要操作是添加，但保留以便未来扩展查询功能
    //noinspection MismatchedCollectionQueryUpdate
    private final Map<String, Report> reports = new ConcurrentHashMap<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final File reportsDir;
    
    public ReportManager(EzObserver plugin, RealTimeMonitor realTimeMonitor) {
        this.plugin = plugin;
        this.realTimeMonitor = realTimeMonitor;
        this.reportsDir = new File(plugin.getDataFolder(), "reports");
        if (!reportsDir.exists()) {
            if (!reportsDir.mkdirs()) {
                plugin.getLogger().warning("无法创建报告目录: " + reportsDir.getAbsolutePath());
            }
        }
    }
    
    /**
     * 生成服务器报告
     */
    public Report generateServerReport() {
        Report report = new Report("server", "Server-Wide Report");
        
        // 添加服务器统计
        Map<String, Object> serverStats = realTimeMonitor.getServerStats();
        report.addSection("Server Statistics", serverStats);
        
        // 添加违规排行榜（转换格式）
        Map<String, Object> leaderboardData = new HashMap<>();
        List<Map.Entry<UUID, Integer>> leaderboard = realTimeMonitor.getViolationLeaderboard();
        
        for (int i = 0; i < leaderboard.size(); i++) {
            Map.Entry<UUID, Integer> entry = leaderboard.get(i);
            String playerName = getPlayerName(entry.getKey());
            leaderboardData.put("rank_" + (i + 1),
                Map.of("player", playerName, "violations", entry.getValue()));
        }
        report.addSection("Violation Leaderboard", leaderboardData);
        
        // 添加时间戳
        report.addSection("Generated", Map.of("timestamp", System.currentTimeMillis(),
                                            "date", dateFormat.format(new Date())));
        
        reports.put("server_" + System.currentTimeMillis(), report);
        saveReport(report);
        
        return report;
    }
    
    /**
     * 生成玩家报告
     */
    public Report generatePlayerReport(Player player) {
        if (player == null) return null;
        
        Report report = new Report(player.getUniqueId().toString(), "Player Report: " + player.getName());
        
        // 添加玩家统计
        Map<String, Object> playerStats = realTimeMonitor.getPlayerStats(player.getUniqueId());
        report.addSection("Player Statistics", playerStats);
        
        // 添加玩家信息
        Map<String, Object> playerInfo = new HashMap<>();
        playerInfo.put("name", player.getName());
        playerInfo.put("uuid", player.getUniqueId().toString());
        playerInfo.put("firstJoin", player.getFirstPlayed());
        playerInfo.put("lastSeen", player.getLastPlayed());
        report.addSection("Player Information", playerInfo);
        
        // 添加时间戳
        report.addSection("Generated", Map.of("timestamp", System.currentTimeMillis(), 
                                            "date", dateFormat.format(new Date())));
        
        reports.put("player_" + player.getUniqueId() + "_" + System.currentTimeMillis(), report);
        saveReport(report);
        
        return report;
    }
    
    /**
     * 生成详细违规报告
     */
    public Report generateViolationReport(String type, String description, Map<String, Object> data) {
        Report report = new Report(type, description);
        
        // 添加违规详情
        report.addSection("Violation Details", data);
        
        // 添加服务器信息
        Map<String, Object> serverInfo = new HashMap<>();
        serverInfo.put("serverVersion", Bukkit.getVersion());
        serverInfo.put("onlinePlayers", Bukkit.getOnlinePlayers().size());
        serverInfo.put("maxPlayers", Bukkit.getMaxPlayers());
        report.addSection("Server Information", serverInfo);
        
        // 添加时间戳
        report.addSection("Generated", Map.of("timestamp", System.currentTimeMillis(), 
                                            "date", dateFormat.format(new Date())));
        
        reports.put("violation_" + type + "_" + System.currentTimeMillis(), report);
        saveReport(report);
        
        return report;
    }
    
    /**
     * 保存报告到文件
     */
    private void saveReport(Report report) {
        try {
            String filename = report.getId() + ".json";
            File reportFile = new File(reportsDir, filename);
            
            try (PrintWriter writer = new PrintWriter(new FileWriter(reportFile))) {
                writer.println(report.toJson());
            }
            
        } catch (IOException e) {
            plugin.getLogger().warning("保存报告失败: " + e.getMessage());
        }
    }
    
    /**
     * 加载报告
     */
    public Report loadReport(String reportId) {
        File reportFile = new File(reportsDir, reportId + ".json");
        if (!reportFile.exists()) return null;
        
        try (BufferedReader reader = new BufferedReader(new FileReader(reportFile))) {
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            
            return Report.fromJson(json.toString());
        } catch (IOException e) {
            plugin.getLogger().warning("加载报告失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取所有报告列表
     */
    public List<String> getReportList() {
        List<String> reportList = new ArrayList<>();
        File[] files = reportsDir.listFiles((dir, name) -> name.endsWith(".json"));
        
        if (files != null) {
            for (File file : files) {
                reportList.add(file.getName().replace(".json", ""));
            }
        }
        
        reportList.sort(Collections.reverseOrder());
        return reportList;
    }
    
    /**
     * 删除报告
     */
    public boolean deleteReport(String reportId) {
        File reportFile = new File(reportsDir, reportId + ".json");
        return reportFile.delete();
    }
    
    /**
     * 清理旧报告
     */
    public int cleanupOldReports(long maxAgeMillis) {
        File[] files = reportsDir.listFiles((dir, name) -> name.endsWith(".json"));
        int deleted = 0;
        
        if (files != null) {
            long cutoffTime = System.currentTimeMillis() - maxAgeMillis;
            
            for (File file : files) {
                String filename = file.getName().replace(".json", "");
                String[] parts = filename.split("_");
                
                if (parts.length >= 2) {
                    try {
                        long timestamp = Long.parseLong(parts[parts.length - 1]);
                        if (timestamp < cutoffTime) {
                            if (file.delete()) {
                                deleted++;
                            }
                        }
                    } catch (NumberFormatException e) {
                        // 忽略格式错误的文件名
                    }
                }
            }
        }
        
        return deleted;
    }
    
    /**
     * 获取玩家名称（如果玩家在线则返回真实名称，否则返回UUID）
     */
    private String getPlayerName(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            return player.getName();
        }
        return playerId.toString();
    }
    
    /**
     * 报告类
     */
    public static class Report {
        private final String id;
        private final String title;
        private final Map<String, Map<String, Object>> sections = new HashMap<>();
        private final long createdAt;
        
        public Report(String id, String title) {
            this.id = id;
            this.title = title;
            this.createdAt = System.currentTimeMillis();
        }
        
        public void addSection(String name, Map<String, Object> data) {
            sections.put(name, new HashMap<>(data));
        }
        
        public Map<String, Object> getSection(String name) {
            return sections.get(name);
        }
        
        public String getId() {
            return id;
        }
        
        public String getTitle() {
            return title;
        }
        
        public long getCreatedAt() {
            return createdAt;
        }
        
        public Map<String, Map<String, Object>> getAllSections() {
            return new HashMap<>(sections);
        }
        
        public String toJson() {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            json.append("  \"id\": \"").append(id).append("\",\n");
            json.append("  \"title\": \"").append(title).append("\",\n");
            json.append("  \"createdAt\": ").append(createdAt).append(",\n");
            json.append("  \"sections\": {\n");
            
            boolean first = true;
            for (Map.Entry<String, Map<String, Object>> entry : sections.entrySet()) {
                if (!first) json.append(",\n");
                json.append("    \"").append(entry.getKey()).append("\": ");
                json.append(mapToJson(entry.getValue()));
                first = false;
            }
            
            json.append("\n  }\n}");
            return json.toString();
        }
        
        public static Report fromJson(String json) {
            // 简化的JSON解析，实际项目中应使用JSON库
            String id = extractValue(json, "\"id\": \"", "\"");
            String title = extractValue(json, "\"title\": \"", "\"");
            Report report = new Report(id, title);
            
            // 这里简化处理，实际应该完整解析
            return report;
        }
        
        private static String extractValue(String json, String key, String end) {
            int start = json.indexOf(key) + key.length();
            int endIndex = json.indexOf(end, start);
            return json.substring(start, endIndex);
        }
        
        private String mapToJson(Map<String, Object> map) {
            StringBuilder json = new StringBuilder();
            json.append("{\n");
            
            boolean first = true;
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (!first) json.append(",\n");
                json.append("      \"").append(entry.getKey()).append("\": ");
                
                Object value = entry.getValue();
                if (value instanceof String) {
                    json.append("\"").append(value).append("\"");
                } else if (value instanceof Number) {
                    json.append(value);
                } else if (value instanceof Boolean) {
                    json.append(value);
                } else {
                    json.append("\"").append(value.toString()).append("\"");
                }
                
                first = false;
            }
            
            json.append("\n    }");
            return json.toString();
        }
    }
}