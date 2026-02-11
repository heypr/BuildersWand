package dev.heypr.buildersWand.managers;

import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.utility.Util;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class MessageManager {

    private Player player;

    public MessageManager(Player player) {
        this.player = player;
    }

    public enum Messages {
        COMMAND_USAGE("&cUsage: /builderswand <reload|list|give>"),
        PLACING_BLOCK("&7Placing blocks... &e{remaining} &7left"),
        PLACEMENT_COMPLETE("&aBlock placement complete!"),
        WAND_MISCONFIGURED("&4The wand you had was misconfigured and has been removed. Please contact an administrator immediately.");

        private String value;

        Messages(String value) {
            this.value = value;
        }
    }

    //TODO: add cool placeholder replacement logic
    private static String getWandValue(Wand wand, String value) {
        value = value.toLowerCase();
        return switch (value) {
            case "id" -> String.valueOf(wand.getId());
            case "name" -> String.valueOf(wand.getName());
            case "material" -> String.valueOf(wand.getMaterial());
            case "durability" -> String.valueOf(wand.getDurabilityAmount());
            case "wandtype" -> wand.getWandType().toString();
            case "staticlength" -> String.valueOf(wand.getStaticLength());
            case "staticwidth" -> String.valueOf(wand.getStaticWidth());
            case "maxsize" -> String.valueOf(wand.getMaxSize());
            case "cooldown" -> String.valueOf(wand.getCooldown());
            case "undohistorysize" -> String.valueOf(wand.getUndoHistorySize());
            default -> "";
        };
    }

    public static void sendActionBar(Player player, Messages message) {
        player.sendActionBar(Util.toPrefixedComponent(message.value));
    }

    public static void sendActionBar(Player player, Messages message, String replacementVar, String replacementMsg) {
        String regex = "{" + replacementVar + "}";
        String finalMessage = message.value.replaceAll(regex, replacementMsg);
        player.sendActionBar(Util.toPrefixedComponent(finalMessage));
    }

    public static void sendMessage(Player player, Messages message) {
        player.sendMessage(Util.toPrefixedComponent(message.value));
    }

    public static void sendMessage(Player player, Messages message, String replacementVar, String replacementMsg) {
        String regex = "{" + replacementVar + "}";
        String finalMessage = message.value.replaceAll(regex, replacementMsg);
        player.sendMessage(Util.toPrefixedComponent(finalMessage));
    }

    public static void sendMessage(CommandSender sender, Messages message) {
        sender.sendMessage(Util.toPrefixedComponent(message.value));
    }

    public static void sendMessage(CommandSender sender, Messages message, String replacementVar, String replacementMsg) {
        String regex = "{" + replacementVar + "}";
        String finalMessage = message.value.replaceAll(regex, replacementMsg);
        sender.sendMessage(Util.toPrefixedComponent(finalMessage));
    }

    public void sendActionBar(Messages message) {
        if (player == null) {
            Util.error("Unable to send message to player!");
            return;
        }
        sendActionBar(player, message);
    }

    public void sendActionBar(Messages message, String replacementVar, String replacementMsg) {
        if (player == null) {
            Util.error("Unable to send message to player!");
            return;
        }
        sendActionBar(player, message, replacementVar, replacementMsg);
    }

    public void sendMessage(Messages message) {
        if (player == null) {
            Util.error("Unable to send message to player!");
            return;
        }
        sendMessage(player, message);
    }

    public void sendMessage(Messages message, String replacementVar, String replacementMsg) {
        if (player == null) {
            Util.error("Unable to send message to player!");
            return;
        }
        sendMessage(player, message, replacementVar, replacementMsg);
    }
}
