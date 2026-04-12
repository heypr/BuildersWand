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

    public static void initialize() {
        try {
            BuildersWand plugin = BuildersWand.getInstance();
            messagesFile = new File(plugin.getDataFolder(), "messages.yml");
            if (!messagesFile.exists()) {
                plugin.saveResource("messages.yml", true);
            }
            loadMessagesFromFile();
            String fileVersion = messages.getString("config_version", "unknown");
            if (!fileVersion.equals(CURRENT_VERSION)) {
                Util.error("OUTDATED messages.yml: Expected '" + CURRENT_VERSION + "' but found '" + fileVersion + "'.");
                Util.error("Please update your messages.yml to the latest version. A default messages.yml can be found on the plugin page and on GitHub.");
            }
            Util.debug("MessageManager initialized successfully!");
        }
        catch (Exception e) {
            Util.error("Failed to initialize MessageManager: " + e.getMessage());
        }
    }

    private static void loadMessagesFromFile() {
        if (messagesFile == null || !messagesFile.exists()) {
            throw new RuntimeException("messages.yml file not found at: " + messagesFile);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        Util.debug("Messages YAML loaded from disk");
    }

    public static void reload() {
        try {
            initialize();
            Util.debug("Messages reloaded successfully");
        }
        catch (Exception e) {
            Util.error("Failed to reload messages: " + e.getMessage());
        }
    }

    public enum Messages {
        PREFIX("prefix", "&7[&bBuildersWand&7] &r"),
        USAGE("command.usage", "&cUsage: /builderswand <reload|list|give> <id> [player]\" # <-- id and player are only needed for the give command"),
        RELOAD_SUCCESS("command.reload.success", "&aConfiguration has been reloaded."),
        NO_WANDS("command.no-wands", "&cNo wands found."),
        WAND_NOT_FOUND("command.wand-not-found", "&cNo wand found with ID: {id}"),
        WAND_RECEIVED("command.wand-received", "&aYou received a {id} wand!"),
        WAND_GIVEN("command.wand-given", "&aGave {id} to {player_name}."),
        ONLY_PLAYERS("command.only-players", "&cOnly players can give wands to themselves."),
        LIST_WANDS("command.wand-list", "&eWands: "),
        PLACING_BLOCKS("wand.placing-blocks", "&7Placing blocks... &e{remaining} &7left"),
        PLACEMENT_COMPLETE("wand.placement-complete", "&aBlock placement complete!"),
        MISCONFIGURED("wand.misconfigured", "&4The wand you had was misconfigured and has been removed. Please contact an administrator immediately."),
        NO_PERMISSION("wand.no-permission", "&4You do not have permission to use this wand."),
        STILL_PLACING("wand.still-placing", "&4Wand is still placing blocks, please wait..."),
        COOLDOWN_ACTIVE("wand.cooldown-active", "&4Please wait &c{seconds} &4seconds before using the wand again."),
        INSUFFICIENT_BLOCKS("wand.insufficient-blocks", "&4You need &c{needed} &4more &c{material} &4blocks."),
        PLACEMENT_DISALLOWED("wand.placement-disallowed", "&4Disallowed."),
        NOTHING_TO_UNDO("wand.nothing-to-undo", "&cNothing to undo!"),
        ACTION_UNDONE("wand.action-undone", "&aAction undone! &c{remaining} &aundoes remaining."),
        UPDATE_AVAILABLE("updater.available", "&aAn update for BuildersWand is available! Check console for more info.");

        private final String key;
        private final String defaultValue;

        Messages(String key, String defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        public String getKey() {
            return key;
        }

        public String getDefaultValue() {
            return defaultValue;
        }
    }

    public static TextComponent getPrefixedMessage(Messages message) {
        return Util.toPrefixedComponent(getMessage(message));
    }

    public static TextComponent getRegularMessage(Messages message) {
        return Util.toComponent(getMessage(message));
    }

    private static String getWandValue(Wand wand, String value) {
        if (wand == null) {
            return "";
        }
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
        if (wand == null) {
            return msg;
        }
        String[] keys = {"id", "name", "material", "durability", "wandtype", "staticlength", "staticwidth", "maxsize", "cooldown", "undohistorysize"};
        for (String key : keys) {
            String placeholder = "{" + key + "}";
            if (msg.contains(placeholder)) {
                msg = msg.replace(placeholder, getWandValue(wand, key));
            }
        }
        return msg;
    }

    public static String getWandSenderMessage(Messages message, Wand wand, CommandSender sender) {
        String msg = getWandMessage(message, wand);
        if (wand == null) {
            return msg;
        }
        if (sender == null) {
            return msg;
        }
        if (msg.contains("{player_name}")) {
            msg = msg.replace("{player_name}", sender.getName());
        }
        return msg;
    }

    public static String getMessage(Messages message) {
        if (messages == null) {
            Util.error("MessageManager not initialized! Get in touch via Discord to resolve this!");
            return "&4Missing: &c[" + message.getKey() + "]";
        }
        String msg = messages.getString(message.getKey());
        if (msg == null) {
            Util.error("Message key not found: '" + message.getKey() + "'! Using default value.");
            return message.getDefaultValue();
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
        if (player == null) {
            return;
        }
        player.sendActionBar(Util.toPrefixedComponent(getMessage(message)));
    }

    public static void sendActionBar(Player player, Messages message, String key, String value) {
        if (player == null) {
            return;
        }
        player.sendActionBar(Util.toPrefixedComponent(getMessage(message, key, value)));
    }

    public static void sendActionBar(Player player, Messages message, Object... placeholders) {
        if (player == null) {
            return;
        }
        player.sendActionBar(Util.toPrefixedComponent(getMessage(message, placeholders)));
    }

    public static void sendMessage(Player player, Messages message) {
        if (player == null) {
            return;
        }
        player.sendMessage(Util.toPrefixedComponent(getMessage(message)));
    }

    public static void sendMessage(Player player, Messages message, String key, String value) {
        if (player == null) {
            return;
        }
        player.sendMessage(Util.toPrefixedComponent(getMessage(message, key, value)));
    }

    public static void sendMessage(Player player, Messages message, Object... placeholders) {
        if (player == null) {
            return;
        }
        player.sendMessage(Util.toPrefixedComponent(getMessage(message, placeholders)));
    }

    public static void sendMessage(CommandSender sender, Messages message) {
        if (sender == null) {
            return;
        }
        sender.sendMessage(Util.toPrefixedComponent(getMessage(message)));
    }

    public static void sendMessage(CommandSender sender, Messages message, String key, String value) {
        if (sender == null) {
            return;
        }
        sender.sendMessage(Util.toPrefixedComponent(getMessage(message, key, value)));
    }

    public static void sendMessage(CommandSender sender, Messages message, Object... placeholders) {
        if (sender == null) {
            return;
        }
        sender.sendMessage(Util.toPrefixedComponent(getMessage(message, placeholders)));
    }

    public static void sendMessage(CommandSender sender, Messages message, Wand wand) {
        if (sender == null) {
            return;
        }
        sender.sendMessage(Util.toPrefixedComponent(getWandMessage(message, wand)));
    }

    public static void sendMessage(Player player, Messages message, Wand wand) {
        if (player == null) {
            return;
        }
        player.sendMessage(Util.toPrefixedComponent(getWandSenderMessage(message, wand, player)));
    }
}
