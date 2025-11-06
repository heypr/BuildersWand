package dev.heypr.buildersWand.commands;

import dev.heypr.buildersWand.Util;
import dev.heypr.buildersWand.managers.ConfigManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadWandCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender.hasPermission("builderswand.reload"))) {
            sender.sendMessage(Util.toPrefixedComponent("&cYou don't have permission to reload the configuration."));
            return true;
        }

        ConfigManager.reload();
        sender.sendMessage(Util.toPrefixedComponent("Configuration has been reloaded."));
        return true;
    }
}
