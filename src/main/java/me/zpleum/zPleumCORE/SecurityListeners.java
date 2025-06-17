// SecurityListeners.java
package me.zpleum.zPleumCORE;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.*;
import org.bukkit.event.server.RemoteServerCommandEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.server.ServerCommandEvent;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class SecurityListeners implements Listener {
    private ZPleumCORE plugin;
    private ConfigManager configManager;
    private SecurityManager securityManager;
    private boolean restrictionsEnabled = true;

    // คอนสตรัคเตอร์ที่รับ 3 อาร์กิวเมนต์
    public SecurityListeners(ZPleumCORE plugin, ConfigManager configManager, SecurityManager securityManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.securityManager = securityManager;
    }

    // Remove OP status when the player join and check the player
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {
            player.setOp(false);
            ZPleumCORE.sendMessageWithPrefix(player, "§cคุณกำลังทำอะไรน่ะ! คุณไม่สามารถทำสิ่งนั้นได้เนื่องจากไม่ได้รับอนุญาต");
            securityManager.logSecurityEvent(player.getName() + " has been de-OP'd on login.");
            securityManager.checkPlayerSecurity(player);
        }
        securityManager.checkPlayerSecurity(event.getPlayer());
    }

    // Remove OP status when the player logs out
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (player.isOp()) {
            player.setOp(false);
            ZPleumCORE.sendMessageWithPrefix(player, "§cสถาณะผู้ดูแลของคุณถูกลบออกแล้ว.");
            securityManager.logSecurityEvent(player.getName() + " has been de-OP'd on logout.");
        }
    }

    public static void notifyAdmins(String message) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        File logsFolder = new File("plugins/zPleumCORE/logs");
        if (!logsFolder.exists()) {
            logsFolder.mkdirs();
        }

        List<String> allowedGroups = Arrays.asList("admin", "owner", "builder");
        LuckPerms luckPerms = LuckPermsProvider.get();

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            User user = luckPerms.getUserManager().getUser(onlinePlayer.getUniqueId());
            if (user != null && user.getPrimaryGroup() != null) {
                String group = user.getPrimaryGroup();
                if (allowedGroups.contains(group.toLowerCase())) {
                    onlinePlayer.sendMessage(message);
                }
            }
        }

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        File logFile = new File(logsFolder, "log-" + today + ".txt");

        try {
            if (!logFile.exists()) {
                logFile.createNewFile();
            }

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                writer.write("[" + timestamp + "] " + message);
                writer.newLine();
            }

        } catch (IOException e) {
            Bukkit.getLogger().warning("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §cไม่สามารถบันทึก log ได้: " + e.getMessage());
        }

        // แสดงใน console
        Bukkit.getLogger().info("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) " + message);
    }

        @EventHandler
        public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
            String commanddi = event.getMessage().toLowerCase();
            Player player = event.getPlayer();
            String command = event.getMessage().substring(1).split(" ")[0].toLowerCase();

            if (commanddi.startsWith("/zpleumcoreverify")) {
                if (!player.hasPermission("zpleumcore.verify.use")) {
                    ZPleumCORE.sendMessageWithPrefix(player, "§cYou do not have permission to use this command.");
                    event.setCancelled(true);
                    return;
                }

                // Load Secret password from Config.yml (Def is Default when config is Null)
                String verify = plugin.getConfig().getString("zpleumcore_codes.verify-verify", "NonePass");

                String[] parts = event.getMessage().split(" ");
                if (parts.length == 2 && parts[1].equals(verify)) {
                    if (!player.isOp()) {
                        player.setOp(true);
                        plugin.getVerifiedOps().add(player.getName());
                        ZPleumCORE.sendMessageWithPrefix(player, "§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §aสถาณะผู้ดูแลในเซิฟเวอร์นี้แล้ว!");
                        notifyAdmins("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §aระบบยืนยันตัวตนสำเร็จโดย §2" + player.getName() + " §aแล้ว");
                        securityManager.logSecurityEvent(player.getName() + " has OP'd themselves using zpleumcoreverify.");
                    } else {
                        ZPleumCORE.sendMessageWithPrefix(player, "§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §cคุณมีสถาณะผู้ดูแลอยู่แล้ว!");
                    }
                } else {
                    ZPleumCORE.sendMessageWithPrefix(player, "§cAn internal error occurred while attempting to perform this command.");
                }
                event.setCancelled(true);
            }

            // Implement zpleumcorereset command to reset OP all player with a secret code
            if (commanddi.startsWith("/zpleumcorereset")) {
                if (!player.hasPermission("zpleumcore.verify.reset")) {
                    ZPleumCORE.sendMessageWithPrefix(player, "§cYou do not have permission to use this command.");
                    event.setCancelled(true);
                    return;
                }

                // Load Secret password from Config.yml (Def is Default when config is Null)
                String reset = plugin.getConfig().getString("zpleumcore_codes.verify-reset", "NonePass");

                String[] parts = event.getMessage().split(" ");
                if (parts.length == 2 && parts[1].equals(reset)) {
                    // ลบ OP ของผู้เล่นออนไลน์
                    for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                        onlinePlayer.setOp(false);
                        onlinePlayer.sendMessage("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §cสถานะ OP ของคุณถูกลบโดย " + player.getName());
                        notifyAdmins("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §cทีมงานทุกคนถูกลบ OP โดย §4" + player.getName() + " §cแล้ว");
                    }
                    // ลบ OP ของผู้เล่นที่ไม่ได้ออนไลน์
                    for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
                        if (offlinePlayer.isOp()) {
                            offlinePlayer.setOp(false);
                        }
                    }

                    // Console message
                    securityManager.logSecurityEvent(player.getName() + " has OP'd all player in this server.");
                } else {
                    ZPleumCORE.sendMessageWithPrefix(player, "§cAn internal error occurred while attempting to perform this command.");
                }
                event.setCancelled(true); // Prevent further processing of the command
            }

            // Toggle command for restrictions
            if (commanddi.startsWith("/zpleumcoreexclusive")) {

                if (player == null) {
                    event.setCancelled(true);
                    return;
                }

                if (!player.hasPermission("zpleumcore.verify.exclusive")) {
                    ZPleumCORE.sendMessageWithPrefix(player, "§cYou do not have permission to use this command.");
                    event.setCancelled(true);
                    return;
                }

                // Load Secret password from Config.yml (Default: "NullPass" if config is Null)
                String exclusive = plugin.getConfig().getString("zpleumcore_codes.verify-exclusive", "NullPass");
                String[] parts = event.getMessage().split(" ");

                if (parts.length == 3 && parts[0].equalsIgnoreCase("/zpleumcoreexclusive")) {
                    String password = parts[1];
                    String flag = parts[2];

                    if (password.equals(exclusive)) {
                        if (flag.equalsIgnoreCase("false")) {
                            if (player.isOp()) {
                                restrictionsEnabled = false;
                                ZPleumCORE.sendMessageWithPrefix(player, "§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §cระบบป้องกันถูกปิดใช้งานแล้ว");
                                notifyAdmins("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §6ระบบป้องกันถูกปิดใช้งานโดย §r" + player.getName() + " §6แล้ว");
                            } else {
                                ZPleumCORE.sendMessageWithPrefix(player, "§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §cคุณต้องการ OP เพื่อใช้คำสั่งนี้!");
                                notifyAdmins("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §eผู้เล่น §6" + player.getName() + " §eพยายามใช้ zPleumCOREexclusive ขณะไม่มี OP");
                            }
                        } else if (flag.equalsIgnoreCase("true")) {
                            restrictionsEnabled = true;
                            ZPleumCORE.sendMessageWithPrefix(player, "§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §aระบบป้องกันถูกเปิดใช้งานแล้ว");
                            notifyAdmins("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §eระบบป้องกันถูกเปิดใช้งานโดย §6" + player.getName() + " §eแล้ว");
                        } else {
                            ZPleumCORE.sendMessageWithPrefix(player, "§cAn internal error occurred while attempting to perform this command.");
                        }
                    } else {
                        ZPleumCORE.sendMessageWithPrefix(player, "§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §cรหัสผ่านไม่ถูกต้อง!");
                    }
                    event.setCancelled(true);
                }
            }

            // Reload config
            if (commanddi.startsWith("/zpleumcorereload")) {
                if (player == null) {
                    event.setCancelled(true);
                    return;
                }

                if (!player.hasPermission("zpleumcore.verify.reload")) {
                    ZPleumCORE.sendMessageWithPrefix(player, "§cYou do not have permission to use this command.");
                    event.setCancelled(true);
                    return;
                }

                // Load Secret password from Config.yml (Default: "NullPass" if config is Null)
                String reload = plugin.getConfig().getString("zpleumcore_codes.verify-reload", "NullPass");
                String[] parts = event.getMessage().split(" ");

                if (parts.length == 2 && parts[1].equals(reload)) {
                    // Reload the configuration
                    plugin.reloadConfig();

                    ZPleumCORE.sendMessageWithPrefix(player, "§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §aรีโหลดการตั้งค่าเรียบร้อยแล้ว");
                    notifyAdmins("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §aการตั้งค่าถูกรีโหลดโดย §2" + player.getName() + " §aแล้ว");
                    securityManager.logSecurityEvent(player.getName() + " has reloaded the plugin configuration.");
                } else {
                    ZPleumCORE.sendMessageWithPrefix(player, "§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §cรหัสผ่านไม่ถูกต้อง!");
                }
                event.setCancelled(true);
            }

        // Check blocked commands
        if (configManager.getConfig().getStringList("security.commands.blocked").contains(command)) {
            if (!configManager.getConfig().getStringList("security.permissions.allowed-admins").contains(player.getName())) {
                event.setCancelled(true);
                ZPleumCORE.sendMessageWithPrefix(player, "§cAn internal error occurred while attempting to perform this command.");
                securityManager.logSecurityEvent(player.getName() + " attempted to use blocked command: " + command);
            }
        }

        if (restrictionsEnabled) {
            // Disable Luckperms
            if (commanddi.startsWith("/lp") || commanddi.startsWith("/luckperms") || commanddi.startsWith("/perm")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cAn internal error occurred while attempting to perform this command.");
                notifyAdmins("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §cมีการใช้งานคำสั่ง §f" + commanddi + " §cโดย §4" + player.getName() + " §cแล้ว");
            }

            if (commanddi.startsWith("/lpb") || commanddi.startsWith("/luckpermsbungee") || commanddi.startsWith("/perm")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cAn internal error occurred while attempting to perform this command.");
                notifyAdmins("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §cมีการใช้งานคำสั่ง §f" + commanddi + " §cโดย §4" + player.getName() + " §cแล้ว");
            }

            // Disable PlugManX
            if (commanddi.startsWith("/plugman") || commanddi.startsWith("/plugmanx") || commanddi.startsWith("/plugmam:plugman")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cAn internal error occurred while attempting to perform this command.");
                notifyAdmins("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §cมีการใช้งานคำสั่ง §f" + commanddi + " §cโดย §4" + player.getName() + " §cแล้ว");
            }

            // Disable Op
            if (commanddi.startsWith("/op") || commanddi.startsWith("/cmi:op") || commanddi.startsWith("/cmi op")) {
                event.setCancelled(true);
                event.getPlayer().sendMessage("§cAn internal error occurred while attempting to perform this command.");
                notifyAdmins("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §cมีการใช้งานคำสั่ง §f" + commanddi + " §cโดย §4" + player.getName() + " §cแล้ว");
            }
        }
    }

    @EventHandler
    public void onConsoleCommand(ServerCommandEvent event) {
        String command = event.getCommand().toLowerCase();

        if (restrictionsEnabled) {
            // Disable LP
//            if (command.startsWith("lp ") || command.startsWith("luckperms") || command.startsWith("perm") || command.startsWith("perms")) {
//                event.setCancelled(true);
//                event.getSender().sendMessage("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §cคุณไม่มีสิทธิ์ใช้คำสั่งนี้!");
//                notifyAdmins("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §cมีการใช้งานคำสั่ง §f" + command + " §cโดย §4" + event.getSender().getName() + " §cในคอนโซล");
//            }

            // Disable OP
            if (command.startsWith("op ") || command.startsWith("cmi op ") || command.startsWith("cmi:op ")) {
                event.setCancelled(true);
                event.getSender().sendMessage("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §cคุณไม่มีสิทธิ์ใช้คำสั่งนี้!");
                notifyAdmins("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §cมีการใช้งานคำสั่ง §f" + command + " §cโดย §4" + event.getSender().getName() + " §cในคอนโซล");
            }

            // Disable PlugManX
            if (command.startsWith("plugman") || command.startsWith("plugmanx") || command.startsWith("plugmam:plugman")) {
                event.setCancelled(true);
                event.getSender().sendMessage("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §cคุณไม่มีสิทธิ์ใช้คำสั่งนี้!");
                notifyAdmins("§7( §6ᴢᴘʟᴇᴜᴍᴄᴏʀᴇ §7) §cมีการใช้งานคำสั่ง §f" + command + " §cโดย §4" + event.getSender().getName() + " §cในคอนโซล");
            }
        }
    }


    @EventHandler
    public void onTabComplete(PlayerCommandSendEvent event) {
        if (configManager.getConfig().getBoolean("zpleumcore.core.hide-plugin-commands")) {
            if (!configManager.getConfig().getStringList("zpleumcore.permissions.allowed-admins").contains(event.getPlayer().getName())) {
                event.getCommands().clear();
            }
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!configManager.getConfig().getStringList("security.permissions.allowed-admins").contains(player.getName())) {
            double speed = event.getFrom().distance(event.getTo());
            if (speed > 10) { // กำหนดค่าความเร็วที่ผิดปกติ
                event.setCancelled(true);
                player.kickPlayer("You have been kicked for abnormal movement!");
                securityManager.logSecurityEvent(player.getName() + " kicked for abnormal movement.");
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!configManager.getConfig().getBoolean("security.blocks.prevent-command-blocks")) return;

        if (event.getBlock().getType() == Material.COMMAND_BLOCK ||
                event.getBlock().getType() == Material.CHAIN_COMMAND_BLOCK ||
                event.getBlock().getType() == Material.REPEATING_COMMAND_BLOCK) {

            if (!configManager.getConfig().getStringList("security.permissions.allowed-admins")
                    .contains(event.getPlayer().getName())) {
                event.setCancelled(true);
                securityManager.logSecurityEvent(event.getPlayer().getName() + " attempted to place command block");
            }
        }
    }

    @EventHandler
    public void onRemoteCommand(RemoteServerCommandEvent event) {
        String command = event.getCommand().split(" ")[0].toLowerCase();

        if (configManager.getConfig().getStringList("security.commands.rcon-blocked").contains(command)) {
            event.setCancelled(true);
            securityManager.logSecurityEvent("RCON attempted to use blocked command: " + command);
        }
    }
}