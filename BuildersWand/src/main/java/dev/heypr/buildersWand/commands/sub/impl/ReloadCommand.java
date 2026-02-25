package dev.heypr.buildersWand.commands.sub.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.heypr.buildersWand.commands.sub.Subcommand;
import dev.heypr.buildersWand.managers.io.ConfigManager;
import dev.heypr.buildersWand.managers.io.MessageManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

@SuppressWarnings("UnstableApiUsage")
public class ReloadCommand implements Subcommand {

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("reload")
                .requires(stack -> stack.getSender().hasPermission("builderswand.reload"))
                .executes(this::execute);
    }
    private int execute(CommandContext<CommandSourceStack> ctx) {
        ConfigManager.reload();
        MessageManager.reload();
        MessageManager.sendMessage(ctx.getSource().getSender(), MessageManager.Messages.RELOAD_SUCCESS);
        return Command.SINGLE_SUCCESS;
    }
}
