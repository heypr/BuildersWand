package dev.heypr.buildersWand;

import dev.heypr.buildersWand.managers.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.Objects;

public class Util {

    public static TextComponent PREFIX;

    public static TextComponent toComponent(String string) {
        if (string == null) return Component.empty();
        LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
        return legacy.deserialize(legacy.serialize(MiniMessage.miniMessage().deserialize(string).asComponent())).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static TextComponent toPrefixedComponent(String string) {
        return Objects.requireNonNullElseGet(PREFIX, () -> toComponent("&7[&bBuildersWand&7] &r")).append(toComponent(string));
    }

    public static void debug(String string) {
        if (!ConfigManager.getDebugMode()) return;
        BuildersWand.getInstance().getComponentLogger().info(Util.toComponent("[DEBUG] " + string));
    }

    public static void log(String string) {
        BuildersWand.getInstance().getComponentLogger().info(Util.toComponent(string));
    }

    public static void error(String string) {
        BuildersWand.getInstance().getComponentLogger().error(Util.toComponent(string));
    }
}
