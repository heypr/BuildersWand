package dev.heypr.buildersWand.commands;

import dev.heypr.buildersWand.managers.ConfigManager;
import dev.heypr.buildersWand.Wand;
import dev.heypr.buildersWand.managers.WandManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GiveWandCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args.length > 2) {
            sender.sendMessage(ChatColor.AQUA + "[BuildersWand] " + ChatColor.RED + "Usage: /givewand <wand_id> (player)");
            return true;
        }

        Player player = sender instanceof Player ? (Player) sender : null;

        if (player == null && args.length == 1) {
            return true;
        }

        if (args.length == 2) {
            Player target = player.getServer().getPlayer(args[1]);
            if (target == null) {
                sender.sendMessage(ChatColor.AQUA + "[BuildersWand] " + ChatColor.RED + "Player not found.");
                return true;
            }
            player = target;
        }

        int wandId;

        try {
            wandId = Integer.parseInt(args[0]);
        }
        catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.AQUA + "[BuildersWand] " + ChatColor.RED + "Invalid wand ID.");
            return true;
        }

        Wand wand = WandManager.getWandConfig(wandId);

        if (wand == null) {
            sender.sendMessage(ChatColor.AQUA + "[BuildersWand] " + ChatColor.RED + "No wand found with ID: " + wandId);
            return true;
        }

        player.getInventory().addItem(WandManager.createWandItem(wand));

        player.sendMessage(ChatColor.AQUA + "[BuildersWand] " + ChatColor.GREEN + "You have been given a wand!");
        return true;
    }


    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if (args.length == 1) {
            return ConfigManager.getAllWands()
                    .stream()
                    .map(wandConfig -> String.valueOf(wandConfig.getId()))
                    .toList();
        }
        return null;
    }
}
