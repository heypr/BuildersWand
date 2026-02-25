package dev.heypr.buildersWand.commands.sub;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;

public interface Subcommand {
    @SuppressWarnings("UnstableApiUsage")
    LiteralArgumentBuilder<CommandSourceStack> build();
}
