package dev.heypr.buildersWand.managers.io;

import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.utility.ComponentUtil;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class MessageManager {
    private static final String CURRENT_VERSION = "1.6.0";
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
                ComponentUtil.error("OUTDATED messages.yml: Expected '" + CURRENT_VERSION + "' but found '" + fileVersion + "'.");
                ComponentUtil.error("Please update your messages.yml to the latest version. A default messages.yml can be found on the plugin page and on GitHub.");
            }
            ComponentUtil.debug("MessageManager initialized successfully!");
        }
        catch (Exception e) {
            ComponentUtil.error("Failed to initialize MessageManager: " + e.getMessage());
        }
    }

    private static void loadMessagesFromFile() {
        if (messagesFile == null || !messagesFile.exists()) {
            throw new RuntimeException("messages.yml file not found at: " + messagesFile);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        ComponentUtil.debug("Messages YAML loaded from disk");
    }

    public static void reload() {
        try {
            initialize();
            ComponentUtil.debug("Messages reloaded successfully");
        }
        catch (Exception e) {
            ComponentUtil.error("Failed to reload messages: " + e.getMessage());
        }
    }

    public enum Messages {
        PREFIX("prefix", "&7[&bBuildersWand&7] &r"),
        USAGE("command.usage", "&cUsage: /builderswand <reload|list|give|storage> <id> [player]\" # <-- id and player are only needed for the give command"),
        RELOAD_SUCCESS("command.reload.success", "&aConfiguration has been reloaded."),
        NO_WANDS("command.no-wands", "&cNo wands found."),
        WAND_NOT_FOUND("command.wand-not-found", "&cNo wand found with ID: {id}"),
        WAND_RECEIVED("command.wand-received", "&aYou received a {id} wand!"),
        WAND_GIVEN("command.wand-given", "&aGave {id} to {player_name}."),
        ONLY_PLAYERS("command.only-players", "&cOnly players can give wands to themselves."),
        LIST_WANDS("command.wand-list", "&eWands: "),
        STORAGE_UNAVAILABLE("command.storage.unavailable", "&cWand storage is not enabled."),
        STORAGE_NOT_FOUND("command.storage.not-found", "&cNo wand storage found with UUID: {uuid}"),
        STORAGE_SUMMARY("command.storage.list-result", "&aStorage files: &e{total} &a(&e{empty} &aempty, &e{nonempty} &anon-empty) &a- &e{size} &aon disk. Oldest: &e{oldest}&a, newest: &e{newest}&a."),
        PRUNE_RESULT("command.prune.result", "&aPruned &c{deleted} &aempty storage file(s). &c{stale} &astale but non-empty storage(s) found (left untouched)."),
        PRUNE_STALE_LIST("command.prune.stale-list", "&7Stale non-empty storage UUIDs: &e{uuids}"),
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
        return ComponentUtil.toPrefixedComponent(getMessage(message));
    }

    public static TextComponent getRegularMessage(Messages message) {
        return ComponentUtil.toComponent(getMessage(message));
    }

    public static String getMessage(Messages message) {
        if (messages == null) {
            ComponentUtil.error("MessageManager not initialized! Get in touch via Discord to resolve this!");
            return "&4Missing: &c[" + message.getKey() + "]";
        }
        String msg = messages.getString(message.getKey());
        if (msg == null) {
            ComponentUtil.error("Message key not found: '" + message.getKey() + "'! Using default value.");
            return message.getDefaultValue();
        }
        return msg;
    }

    public static String getMessage(Messages message, String key, String value) {
        return resolve(message, Map.of(key, value));
    }

    public static String getMessage(Messages message, Object... placeholders) {
        if (placeholders.length % 2 != 0) {
            ComponentUtil.error("Invalid placeholder count for: " + message.getKey());
            return getMessage(message);
        }
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = 0; i < placeholders.length; i += 2) {
            map.put(String.valueOf(placeholders[i]), String.valueOf(placeholders[i + 1]));
        }
        return resolve(message, map);
    }

    public static String getWandMessage(Messages message, Wand wand) {
        return resolve(message, wandPlaceholders(wand));
    }

    public static String getWandSenderMessage(Messages message, Wand wand, CommandSender sender) {
        Map<String, String> placeholders = new LinkedHashMap<>(wandPlaceholders(wand));
        if (sender != null) {
            placeholders.put("player_name", sender.getName());
        }
        return resolve(message, placeholders);
    }

    private static String resolve(Messages message, Map<String, String> placeholders) {
        String msg = getMessage(message);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return msg;
    }

    private static Map<String, String> wandPlaceholders(Wand wand) {
        if (wand == null) {
            return Collections.emptyMap();
        }
        Map<String, String> map = new LinkedHashMap<>();
        map.put("id", wand.getId());
        map.put("name", wand.getRawName());
        map.put("material", wand.getMaterial().name());
        map.put("durability", String.valueOf(wand.getDurabilityAmount()));
        map.put("wandtype", wand.getWandType().name());
        map.put("staticlength", String.valueOf(wand.getStaticLength()));
        map.put("staticwidth", String.valueOf(wand.getStaticWidth()));
        map.put("maxsize", String.valueOf(wand.getMaxSize()));
        map.put("cooldown", String.valueOf(wand.getCooldown()));
        map.put("undohistorysize", String.valueOf(wand.getUndoHistorySize()));
        return map;
    }

    public static void sendActionBar(Player player, Messages message) {
        if (player == null) {
            return;
        }
        player.sendActionBar(ComponentUtil.toPrefixedComponent(getMessage(message)));
    }

    public static void sendActionBar(Player player, Messages message, String key, String value) {
        if (player == null) {
            return;
        }
        player.sendActionBar(ComponentUtil.toPrefixedComponent(getMessage(message, key, value)));
    }

    public static void sendActionBar(Player player, Messages message, Object... placeholders) {
        if (player == null) {
            return;
        }
        player.sendActionBar(ComponentUtil.toPrefixedComponent(getMessage(message, placeholders)));
    }

    public static void sendMessage(Player player, Messages message) {
        if (player == null) {
            return;
        }
        player.sendMessage(ComponentUtil.toPrefixedComponent(getMessage(message)));
    }

    public static void sendMessage(Player player, Messages message, String key, String value) {
        if (player == null) {
            return;
        }
        player.sendMessage(ComponentUtil.toPrefixedComponent(getMessage(message, key, value)));
    }

    public static void sendMessage(Player player, Messages message, Object... placeholders) {
        if (player == null) {
            return;
        }
        player.sendMessage(ComponentUtil.toPrefixedComponent(getMessage(message, placeholders)));
    }

    public static void sendMessage(CommandSender sender, Messages message) {
        if (sender == null) {
            return;
        }
        sender.sendMessage(ComponentUtil.toPrefixedComponent(getMessage(message)));
    }

    public static void sendMessage(CommandSender sender, Messages message, String key, String value) {
        if (sender == null) {
            return;
        }
        sender.sendMessage(ComponentUtil.toPrefixedComponent(getMessage(message, key, value)));
    }

    public static void sendMessage(CommandSender sender, Messages message, Object... placeholders) {
        if (sender == null) {
            return;
        }
        sender.sendMessage(ComponentUtil.toPrefixedComponent(getMessage(message, placeholders)));
    }

    public static void sendMessage(CommandSender sender, Messages message, Wand wand) {
        if (sender == null) {
            return;
        }
        sender.sendMessage(ComponentUtil.toPrefixedComponent(getWandMessage(message, wand)));
    }

    public static void sendMessage(Player player, Messages message, Wand wand) {
        if (player == null) {
            return;
        }
        player.sendMessage(ComponentUtil.toPrefixedComponent(getWandSenderMessage(message, wand, player)));
    }
}
