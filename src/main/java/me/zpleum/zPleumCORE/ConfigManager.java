// ConfigManager.java
package me.zpleum.zPleumCORE;

import org.bukkit.configuration.file.FileConfiguration;
import java.util.List;
import java.util.ArrayList;

public class ConfigManager {
    private final ZPleumCORE plugin;
    private FileConfiguration config;

    public ConfigManager(ZPleumCORE plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();

        // Command Protection
        config.addDefault("security.commands.blocked", getDefaultBlockedCommands());
        config.addDefault("security.commands.rcon-blocked", getDefaultRconBlockedCommands());

        // Permission Protection
        config.addDefault("security.permissions.allowed-admins", new ArrayList<String>());
        config.addDefault("security.permissions.check-interval", 10); // seconds
        config.addDefault("security.permissions.force-survival-mode", true);
        config.addDefault("security.permissions.prevent-op-commands", true);

        // Login Protection
        config.addDefault("security.login.max-attempts", 5);
        config.addDefault("security.login.block-time", 3600); // seconds
        config.addDefault("security.login.allowed-username-regex", "^[a-zA-Z0-9_]{3,16}$");

        // Command Block Protection
        config.addDefault("security.blocks.prevent-command-blocks", true);

        // Plugin Protection
        config.addDefault("security.plugins.blocked", getDefaultBlockedPlugins());
        config.addDefault("security.plugins.hide-plugin-commands", true);

        // Logging
        config.addDefault("security.logging.enabled", true);
        config.addDefault("security.logging.detailed", true);

        config.options().copyDefaults(true);
        plugin.saveConfig();
    }

    private List<String> getDefaultBlockedCommands() {
        return new ArrayList<String>() {{
            add("lp");
            add("perms");
            add("luckperms");
            add("op");
            add("deop");
            add("kick");
            add("plugman");
            add("minecraft:op");
            add("bukkit:op");
            add("spigot:op");
            add("rl");
            add("reload");
            add("restart");
            add("stop");
            add("plugins");
            add("pl");
            add("?");
            add("timings");
            add("tps");
        }};
    }

    private List<String> getDefaultRconBlockedCommands() {
        return new ArrayList<String>() {{
            add("op");
            add("deop");
            add("kick");
            add("ban");
            add("pardon");
            add("whitelist");
            add("reload");
            add("stop");
            add("restart");
            add("luckperms");
        }};
    }

    private List<String> getDefaultBlockedPlugins() {
        return new ArrayList<String>() {{
            add("Plugmanx");
            add("Plugman");
            add("SecurityX");
            add("Skript");
            add("CMI");
            add("CMILib");
            add("ProtocolLib");
            add("zPleumCORE");
            add("zPleumAUTH");
        }};
    }

    public FileConfiguration getConfig() {
        return config;
    }
}