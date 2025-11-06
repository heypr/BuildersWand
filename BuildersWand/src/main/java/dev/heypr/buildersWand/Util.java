package dev.heypr.buildersWand;

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
}
