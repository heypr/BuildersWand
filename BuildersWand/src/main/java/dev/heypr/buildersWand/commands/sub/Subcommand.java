package dev.heypr.buildersWand.commands.sub;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;

@SuppressWarnings("UnstableApiUsage")
public interface Subcommand {
    LiteralArgumentBuilder<CommandSourceStack> build();
}
