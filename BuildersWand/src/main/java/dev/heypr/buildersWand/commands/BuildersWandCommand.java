package dev.heypr.buildersWand.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.heypr.buildersWand.commands.sub.Subcommand;
import dev.heypr.buildersWand.commands.sub.impl.GiveCommand;
import dev.heypr.buildersWand.commands.sub.impl.ListCommand;
import dev.heypr.buildersWand.commands.sub.impl.ReloadCommand;
import dev.heypr.buildersWand.managers.io.MessageManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class BuildersWandCommand {

    private final List<Subcommand> subcommands = List.of(
            new ReloadCommand(),
            new ListCommand(),
            new GiveCommand()
    );

    public void register(Commands commands) {
        LiteralArgumentBuilder<CommandSourceStack> baseNode = Commands.literal("builderswand")
                .requires(stack -> stack.getSender().hasPermission("builderswand.admin"))
                .executes(ctx -> {
                    MessageManager.sendMessage(ctx.getSource().getSender(), MessageManager.Messages.USAGE);
                    return Command.SINGLE_SUCCESS;
                });

        for (Subcommand subcommand : subcommands) {
            baseNode.then(subcommand.build());
        }

        commands.register(
                baseNode.build(),
                "Base builders wand command.",
                List.of("bw", "wand")
        );
    }
}
