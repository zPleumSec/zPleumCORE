// SecurityManager.java
package me.zpleum.zPleumCORE;

import net.luckperms.api.LuckPermsProvider;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.scheduler.BukkitTask;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import java.util.*;

import static org.bukkit.Bukkit.getLogger;

public class SecurityManager {
    private final ZPleumCORE plugin;
    private final ConfigManager configManager;
    private Map<String, Integer> loginAttempts;
    private Set<String> suspiciousIPs;
    private BukkitTask permissionCheckTask; // Track the task

    public SecurityManager(ZPleumCORE plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.loginAttempts = new HashMap<>();
        this.suspiciousIPs = new HashSet<>();

        startSecurityChecks();
        startPermissionChecks(); // Start a separate task for permission checks
    }

    public void addRateLimiting(String ip) {
        loginAttempts.merge(ip, 1, Integer::sum);

        int maxAttempts = configManager.getConfig().getInt("security.login.max-attempts");
        int blockTime = configManager.getConfig().getInt("security.login.block-time");

        if (loginAttempts.get(ip) > maxAttempts) {
            suspiciousIPs.add(ip);
            plugin.getLogger().warning("Blocked IP due to excessive attempts: " + ip);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                suspiciousIPs.remove(ip);
                loginAttempts.remove(ip);
            }, blockTime * 20L);
        }
    }

    private void startSecurityChecks() {
        int interval = configManager.getConfig().getInt("security.permissions.check-interval");

        // Convert to ticks (10 ticks = 0.5 second)
        long ticks = interval * 10L;

        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            performSecurityCheck();
        }, ticks, ticks);
    }

    // New method to start permission checks once
    private void startPermissionChecks() {
        // Run permission checks every second
        permissionCheckTask = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            if (configManager.getConfig().getBoolean("security.logging.enabled")) {
                for (Player player : plugin.getServer().getOnlinePlayers()) {
                    checkPlayerPermissions(player);
                }
            }
        }, 20L, 20L); // 20 ticks = 1 second
    }

    public void performSecurityCheck() {
        if (!configManager.getConfig().getBoolean("security.logging.enabled")) return;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            checkPlayerSecurity(player);
        }
    }

    public void checkPlayerSecurity(Player player) {
        List<String> allowedAdmins = configManager.getConfig().getStringList("security.permissions.allowed-admins");
        Set<String> verifiedOps = plugin.getVerifiedOps(); // ตรวจสอบ OP ที่ผ่าน zpleumcore.verify

        if (player.isOp() && !allowedAdmins.contains(player.getName()) && !verifiedOps.contains(player.getName())) {
            getLogger().warning("Removing OP from " + player.getName() +
                    " because they did not use zpleumcore.verify.");
            player.setOp(false);
            player.kickPlayer("§cคุณไม่ได้รับอนุญาตให้เป็น OP!");
            logSecurityEvent("Removed illegal OP status from " + player.getName() + " and kicked them out.");
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

        // No longer scheduling tasks here - the actual check is called from the scheduled task
    }

    public void checkPlayerPermissions(Player player) {
        LuckPerms luckPerms = LuckPermsProvider.get();

        List<String> allowedAdmins = configManager.getConfig()
                .getStringList("security.permissions.allowed-admins")
                .stream()
                .map(String::toLowerCase)
                .collect(Collectors.toList());

        if (allowedAdmins == null || allowedAdmins.isEmpty()) {
            plugin.getLogger().warning("Config security.permissions.allowed-admins is Null or Error!");
            return;
        }

        List<String> restrictedPerms = Arrays.asList(
                "*",
                "cmi.*",
                "luckperms.*",
                "minecraft.*",
                "bukkit.command.op",
                "minecraft.command.op",
                "bukkit.command.reload",
                "minecraft.command.reload"
        );

        if (!allowedAdmins.contains(player.getName().toLowerCase())) {
            User user = luckPerms.getUserManager().getUser(player.getUniqueId());
            if (user != null) {
                boolean hasRestrictedPerm = false;

                for (String perm : restrictedPerms) {
                    if (user.getCachedData().getPermissionData().checkPermission(perm).asBoolean()) {
                        user.data().remove(Node.builder(perm).build());
                        hasRestrictedPerm = true;
                    }
                }

                if (hasRestrictedPerm) {
                    luckPerms.getUserManager().saveUser(user); // บันทึกการเปลี่ยนแปลง
                    SecurityListeners.notifyAdmins(
                            "§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §cระบบป้องกันได้ลบ Perms จากผู้เล่น §4" +
                                    player.getName() +
                                    " §cเนื่องจากไม่ได้รับอนุญาติ"
                    );
                }
            }
        }
    }

    public void logSecurityEvent(String message) {
        if (configManager.getConfig().getBoolean("security.logging.enabled")) {
            plugin.getLogger().warning("( ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ ) " + message);
        }
    }

    public boolean isIPSuspicious(String ip) {
        return suspiciousIPs.contains(ip);
    }

    public void addLoginAttempt(String ip) {
        loginAttempts.merge(ip, 1, Integer::sum);

        int maxAttempts = configManager.getConfig().getInt("security.login.max-attempts");
        if (loginAttempts.get(ip) > maxAttempts) {
            suspiciousIPs.add(ip);
        }
    }

    // Add a cleanup method to cancel tasks when the plugin is disabled
    public void shutdown() {
        if (permissionCheckTask != null) {
            permissionCheckTask.cancel();
        }
    }
}