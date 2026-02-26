package dev.heypr.buildersWand.commands.sub.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.commands.sub.Subcommand;
import dev.heypr.buildersWand.managers.io.ConfigManager;
import dev.heypr.buildersWand.managers.io.MessageManager;
import dev.heypr.buildersWand.managers.WandManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class GiveCommand implements Subcommand {
    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("give")
                .requires(stack -> stack.getSender().hasPermission("builderswand.give"))
                .then(Commands.argument("id", StringArgumentType.word())
                        .suggests(this::suggestWandIds)
                        .executes(this::executeSelf)
                        .then(Commands.argument("target", ArgumentTypes.player())
                                .executes(this::executeTarget)
                        )
                );
    }

    private CompletableFuture<Suggestions> suggestWandIds(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        ConfigManager.getAllWands().forEach(w -> builder.suggest(w.getId()));
        return builder.buildFuture();
    }

    private int executeSelf(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getSender() instanceof Player player)) {
            MessageManager.sendMessage(source.getSender(), MessageManager.Messages.ONLY_PLAYERS);
            return Command.SINGLE_SUCCESS;
        }
        String wandId = StringArgumentType.getString(ctx, "id");
        return giveWand(source, player, wandId);
    }

    private int executeTarget(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String wandId = StringArgumentType.getString(ctx, "id");
        List<Player> players;
        try {
            players = ctx.getArgument("target", PlayerSelectorArgumentResolver.class).resolve(source);
        }
        catch (CommandSyntaxException e) {
            return Command.SINGLE_SUCCESS;
        }
        if (players.isEmpty()) {
            return Command.SINGLE_SUCCESS;
        }
        for (Player player : players) {
            giveWand(source, player, wandId);
        }
        return Command.SINGLE_SUCCESS;
    }

    private int giveWand(CommandSourceStack source, Player target, String wandId) {
        Wand wand = WandManager.getWandConfig(wandId);
        if (wand == null) {
            MessageManager.sendMessage(target, MessageManager.Messages.WAND_NOT_FOUND, "wand_id", wandId);
            return Command.SINGLE_SUCCESS;
        }
        ItemStack item = WandManager.createWandItem(wand);
        target.getInventory().addItem(item);
        MessageManager.sendMessage(target, MessageManager.Messages.WAND_RECEIVED, wand);
        if (target != source.getSender()) {
            MessageManager.sendMessage(target, MessageManager.Messages.WAND_GIVEN, wand);
        }
        return Command.SINGLE_SUCCESS;
    }
}
