package dev.heypr.buildersWand.commands.sub.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.commands.sub.Subcommand;
import dev.heypr.buildersWand.managers.io.ConfigManager;
import dev.heypr.buildersWand.managers.io.MessageManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Collection;

@SuppressWarnings("UnstableApiUsage")
public class ListCommand implements Subcommand {
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("list")
                .requires(stack -> stack.getSender().hasPermission("builderswand.list"))
                .executes(this::execute);
    }

    private int execute(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        Collection<Wand> wands = ConfigManager.getAllWands();
        if (wands.isEmpty()) {
            MessageManager.sendMessage(source.getSender(), MessageManager.Messages.NO_WANDS);
            return Command.SINGLE_SUCCESS;
        }
        TextComponent.Builder msg = Component.text().append(MessageManager.getPrefixedMessage(MessageManager.Messages.LIST_WANDS));
        for (Wand wand : wands) {
            msg.append(Component.text(wand.getId())
                    .color(NamedTextColor.YELLOW)
                    .hoverEvent(HoverEvent.showText(wand.getName()))
                    .clickEvent(ClickEvent.runCommand("/bw give " + wand.getId())));
        }
        source.getSender().sendMessage(msg.build());
        return Command.SINGLE_SUCCESS;
    }
}
