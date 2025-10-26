package dev.heypr.buildersWand.api;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class Util {

    public static final TextComponent PREFIX = toComponent("&7[&bBuildersWand&7] &r");

    public static TextComponent toComponent(String string) {
        if (string == null) return Component.empty();
        LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();
        return legacy.deserialize(legacy.serialize(MiniMessage.miniMessage().deserialize(string).asComponent())).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static TextComponent toPrefixedComponent(String string) {
        return PREFIX.append(toComponent(string));
    }
}
