package me.zpleum.zPleumCORE;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.GameMode;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class ZPleumCORE extends JavaPlugin {
    private ConfigManager configManager;
    private Map<String, Integer> loginAttempts;
    private Set<String> suspiciousIPs;
    private ZPleumCORE plugin;
    private SecurityManager securityManager;
    private ProtocolManager protocolManager;
    private Set<String> verifiedOps = new HashSet<>();

    public static final String PREFIX = "§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) ";

    public static String prefixMessage(String message) {
        return PREFIX + message;
    }

    public static void sendMessageWithPrefix(Player player, String message) {
        player.sendMessage(prefixMessage(message));
    }

    public Set<String> getVerifiedOps() {
        return verifiedOps;
    }

    private String getVersionFromWeb() {
        String latestVersion = "Unknown";
        try {
            URL url = new URL("https://zpleum.site/api/get-version/zpleumcore");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            JSONObject jsonResponse = new JSONObject(response.toString());
            latestVersion = jsonResponse.getString("version");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return latestVersion;
    }

    @Override
    public void onEnable() {
        plugin = this;

        // Initialize ConfigManager first
        this.configManager = new ConfigManager(this);

        // Initialize SecurityManager with the plugin instance and config
        this.securityManager = new SecurityManager(this, configManager);

        // Register event listeners with all required instances
        getServer().getPluginManager().registerEvents(
                new SecurityListeners(this, configManager, securityManager),
                this
        );

        // Initialize managers
        configManager = new ConfigManager(this);

        // Initialize security-related variables
        loginAttempts = new HashMap<>();
        suspiciousIPs = new HashSet<>();

        protocolManager = ProtocolLibrary.getProtocolManager();

        new BukkitRunnable() {
            @Override
            public void run() {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    securityManager.checkPlayerSecurity(player);
                }
            }
        }.runTaskTimer(plugin, 0L, 20L); // ตรวจสอบทุก 1 วินาที

        // ลงทะเบียน event listener
        getServer().getPluginManager().registerEvents(new SecurityListeners(plugin, configManager, securityManager), this);


        String currentVersion = "1.2.4";
        String latestVersion = getVersionFromWeb();

        getLogger().info("zPleumCORE has been successfully enabled!");
        getLogger().info("Current version " + currentVersion + " Latest " + latestVersion);
    }

    @Override
    public void onDisable() {
        String currentVersion = "1.2.4";
        String latestVersion = getVersionFromWeb();

        getLogger().info("zMysticQuest has been successfully disabled!");
        getLogger().info("Current version " + currentVersion + " Latest " + latestVersion);
    }

    public SecurityManager getSecurityManager() {
        return securityManager;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    @EventHandler
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        String command = event.getMessage().split(" ")[0].toLowerCase();

        // ตรวจสอบคำสั่งที่บล็อก
        List<String> blockedCommands = getConfig().getStringList("security.blocked-commands");
        if (blockedCommands.contains(command)) {
            event.setCancelled(true);

            // ตรวจสอบว่าเป็น admin หรือไม่
            List<String> allowedAdmins = getConfig().getStringList("security.allowed-admins");
            if (!allowedAdmins.contains(player.getName())) {
                // ลบสิทธิ์จากผู้เล่น
                if (getConfig().getBoolean("security.actions.clear-permissions")) {
                    PermissionAttachment attachment = player.addAttachment(this);
                    attachment.remove(); // ลบ permission จากผู้เล่น
                }

                // ยกเลิก OP
                if (getConfig().getBoolean("security.actions.clear-op")) {
                    player.setOp(false);
                }

                // ใช้คำสั่งเตะจากคอนโซล
                if (getConfig().getBoolean("security.actions.kick-player")) {
                    String kickMessage = "Tried to use a restricted command.";
                    player.kickPlayer("REASON " + kickMessage);
                }

                // ใช้คำสั่งแบนจากคอนโซล
                if (getConfig().getBoolean("security.actions.ban-player")) {
                    String kickMessage = "Tried to use a restricted command.";
                    player.kickPlayer("REASON " + kickMessage);
                }
            }
        }
    }

    public void checkPlayerSecurity(Player player) {
        List<String> allowedAdmins = configManager.getConfig().getStringList("security.permissions.allowed-admins");

        // Check OP status
        if (player.isOp() && !allowedAdmins.contains(player.getName())) {
            getLogger().warning("[Debug] About to remove OP from " + player.getName() +
                    " because they are not in allowed-admins list");
            player.setOp(false);
            logSecurityEvent("Removed illegal OP status from " + player.getName());
            SecurityListeners.notifyAdmins("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §cระบบป้องกันได้ลบ OP จากผู้เล่น §4" + player.getName() + " §cเนื่องจากไม่ได้รับอณุญาติ");
        }

        // Check gamemode
        if (configManager.getConfig().getBoolean("security.permissions.force-survival-mode") &&
                !allowedAdmins.contains(player.getName()) &&
                player.getGameMode() != GameMode.SURVIVAL) {
            player.setGameMode(GameMode.SURVIVAL);
            logSecurityEvent(player.getName() + " gamemode was reset to survival");
            SecurityListeners.notifyAdmins("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §cระบบป้องกันได้ลบ GM จากผู้เล่น §4" + player.getName() + " §cเนื่องจากไม่ได้รับอณุญาติ");
        }
        // Check permissions
        checkPlayerPermissions(player);
    }

    // Log security event
    private void logSecurityEvent(String message) {
        getLogger().info("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) " + message);
    }

    // Check permissions (Implement this method based on your permission system)
    private void checkPlayerPermissions(Player player) {
        // Add permission checks if necessary
    }
}
