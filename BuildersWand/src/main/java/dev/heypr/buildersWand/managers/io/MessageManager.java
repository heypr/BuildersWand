package dev.heypr.buildersWand.managers.io;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.utility.Util;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;

public class MessageManager {

    private static final String CURRENT_VERSION = "1.5.0";
    private static FileConfiguration messages;
    private static File messagesFile;
    private Player player;

    public MessageManager(Player player) {
        this.player = player;
    }

    public static void initialize() {
        try {
            BuildersWand plugin = BuildersWand.getInstance();
            messagesFile = new File(plugin.getDataFolder(), "messages.yml");

            if (!messagesFile.exists()) {
                plugin.saveResource("messages.yml", false);
            }

            loadMessagesFromFile();

            String fileVersion = messages.getString("config-version", "unknown");
            if (!fileVersion.equals(CURRENT_VERSION)) {
                Util.error("OUTDATED messages.yml: Expected " + CURRENT_VERSION + " but found " + fileVersion);
                Util.error("Please update your messages.yml to the latest version. A default messages.yml can be found on the plugin page or on GitHub.");
            }

            Util.log("MessageManager initialized successfully!");
        }
        catch (Exception e) {
            Util.error("Failed to initialize MessageManager: " + e.getMessage());
        }
    }

    private static void loadMessagesFromFile() {
        if (messagesFile == null || !messagesFile.exists()) {
            throw new RuntimeException("messages.yml file not found at: " + messagesFile);
        }

        String fileVersion = messages.getString("config-version", "unknown");
        if (!fileVersion.equals(CURRENT_VERSION)) {
            Util.error("OUTDATED messages.yml: Expected " + CURRENT_VERSION + " but found " + fileVersion);
            Util.error("Please update your messages.yml to the latest version. A default messages.yml can be found on the plugin page or on GitHub. If you need help, please get in touch via the support Discord.");
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);
        Util.debug("Messages YAML loaded from disk");
    }

    public static void reload() {
        try {
            loadMessagesFromFile();
            Util.debug("Messages reloaded successfully");
        }
        catch (Exception e) {
            Util.error("Failed to reload messages: " + e.getMessage());
        }
    }

    public enum Messages {
        USAGE("command.usage"),
        RELOAD_SUCCESS("command.reload.success"),
        NO_WANDS("command.no-wands"),
        WAND_NOT_FOUND("command.wand-not-found"),
        WAND_RECEIVED("command.wand-received"),
        WAND_GIVEN("command.wand-given"),
        ONLY_PLAYERS("command.only-players"),
        LIST_WANDS("command.wand-list"),

        PLACING_BLOCKS("wand.placing-blocks"),
        PLACEMENT_COMPLETE("wand.placement-complete"),

        MISCONFIGURED("wand.misconfigured"),
        NO_PERMISSION("wand.no-permission"),
        STILL_PLACING("wand.still-placing"),
        COOLDOWN_ACTIVE("wand.cooldown-active"),
        INSUFFICIENT_BLOCKS("wand.insufficient-blocks"),
        PLACEMENT_DISALLOWED("wand.placement-disallowed"),
        NOTHING_TO_UNDO("wand.nothing-to-undo"),
        ACTION_UNDONE("wand.action-undone"),

        UPDATE_AVAILABLE("updater.available");

        private final String key;

        Messages(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    public static TextComponent getPrefixedMessage(Messages message) {
        return Util.toPrefixedComponent(getMessage(message));
    }

    private static String getWandValue(Wand wand, String value) {
        if (wand == null) return "";

        return switch (value.toLowerCase()) {
            case "id" -> wand.getId();
            case "name" -> wand.getRawName();
            case "material" -> wand.getMaterial().name();
            case "durability" -> String.valueOf(wand.getDurabilityAmount());
            case "wandtype" -> wand.getWandType().name();
            case "staticlength" -> String.valueOf(wand.getStaticLength());
            case "staticwidth" -> String.valueOf(wand.getStaticWidth());
            case "maxsize" -> String.valueOf(wand.getMaxSize());
            case "cooldown" -> String.valueOf(wand.getCooldown());
            case "undohistorysize" -> String.valueOf(wand.getUndoHistorySize());
            default -> "";
        };
    }

    public static String getWandMessage(Messages message, Wand wand) {
        String msg = getMessage(message);
        if (wand == null) return msg;

        String[] keys = {"id", "name", "material", "durability", "wandtype",
                "staticlength", "staticwidth", "maxsize", "cooldown", "undohistorysize"};

        for (String key : keys) {
            String placeholder = "{" + key + "}";
            if (msg.contains(placeholder)) {
                msg = msg.replace(placeholder, getWandValue(wand, key));
            }
        }
        return msg;
    }

    public static String getMessage(Messages message) {
        if (messages == null) {
            Util.error("MessageManager not initialized!");
            return "&c[Missing: " + message.getKey() + "]";
        }

        String msg = messages.getString(message.getKey());
        if (msg == null) {
            Util.debug("Message key not found: " + message.getKey());
            return "&c[Missing: " + message.getKey() + "]";
        }

        return msg;
    }

    public static String getMessage(Messages message, String key, String value) {
        String msg = getMessage(message);
        return msg.replace("{" + key + "}", value);
    }

    public static String getMessage(Messages message, Object... placeholders) {
        String msg = getMessage(message);

        if (placeholders.length % 2 != 0) {
            Util.error("Invalid placeholder count for: " + message.getKey());
            return msg;
        }

        for (int i = 0; i < placeholders.length; i += 2) {
            String key = placeholders[i].toString();
            String value = placeholders[i + 1].toString();
            msg = msg.replace("{" + key + "}", value);
        }

        return msg;
    }

    public static void sendActionBar(Player player, Messages message) {
        if (player == null) return;
        player.sendActionBar(Util.toPrefixedComponent(getMessage(message)));
    }

    public static void sendActionBar(Player player, Messages message, String key, String value) {
        if (player == null) return;
        player.sendActionBar(Util.toPrefixedComponent(getMessage(message, key, value)));
    }

    public static void sendActionBar(Player player, Messages message, Object... placeholders) {
        if (player == null) return;
        player.sendActionBar(Util.toPrefixedComponent(getMessage(message, placeholders)));
    }

    public static void sendMessage(Player player, Messages message) {
        if (player == null) return;
        player.sendMessage(Util.toPrefixedComponent(getMessage(message)));
    }

    public static void sendMessage(Player player, Messages message, String key, String value) {
        if (player == null) return;
        player.sendMessage(Util.toPrefixedComponent(getMessage(message, key, value)));
    }

    public static void sendMessage(Player player, Messages message, Object... placeholders) {
        if (player == null) return;
        player.sendMessage(Util.toPrefixedComponent(getMessage(message, placeholders)));
    }

    public static void sendMessage(CommandSender sender, Messages message) {
        if (sender == null) return;
        sender.sendMessage(Util.toPrefixedComponent(getMessage(message)));
    }

    public static void sendMessage(CommandSender sender, Messages message, String key, String value) {
        if (sender == null) return;
        sender.sendMessage(Util.toPrefixedComponent(getMessage(message, key, value)));
    }

    public static void sendMessage(CommandSender sender, Messages message, Object... placeholders) {
        if (sender == null) return;
        sender.sendMessage(Util.toPrefixedComponent(getMessage(message, placeholders)));
    }

    public static void sendMessage(CommandSender sender, Messages message, Wand wand) {
        if (sender == null) return;
        sender.sendMessage(Util.toPrefixedComponent(getWandMessage(message, wand)));
    }

    public static void sendMessage(Player player, Messages message, Wand wand) {
        if (player == null) return;
        player.sendMessage(Util.toPrefixedComponent(getWandMessage(message, wand)));
    }

    public void sendActionBar(Messages message) {
        sendActionBar(player, message);
    }

    public void sendActionBar(Messages message, String key, String value) {
        sendActionBar(player, message, key, value);
    }

    public void sendActionBar(Messages message, Object... placeholders) {
        sendActionBar(player, message, placeholders);
    }

    public void sendMessage(Messages message) {
        sendMessage(player, message);
    }

    public void sendMessage(Messages message, String key, String value) {
        sendMessage(player, message, key, value);
    }

    public void sendMessage(Messages message, Object... placeholders) {
        sendMessage(player, message, placeholders);
    }
}
