package dev.heypr.buildersWand.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.heypr.buildersWand.api.Wand;
import dev.heypr.buildersWand.managers.ConfigManager;
import dev.heypr.buildersWand.managers.MessageManager;
import dev.heypr.buildersWand.managers.WandManager;
import dev.heypr.buildersWand.utility.Util;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class WandCommand {

    public void register(Commands commands) {
        commands.register(
                Commands.literal("builderswand")
                        .requires(stack -> stack.getSender().hasPermission("builderswand.admin"))
                        .executes(ctx -> {
                            MessageManager.sendMessage(ctx.getSource().getSender(), MessageManager.Messages.COMMAND_USAGE);
                            return Command.SINGLE_SUCCESS;
                        })

                        .then(Commands.literal("reload")
                                .requires(stack -> stack.getSender().hasPermission("builderswand.reload"))
                                .executes(ctx -> reloadConfig(ctx.getSource()))
                        )

                        .then(Commands.literal("list")
                                .requires(stack -> stack.getSender().hasPermission("builderswand.list"))
                                .executes(ctx -> listWands(ctx.getSource()))
                        )

                        .then(Commands.literal("give")
                                .requires(stack -> stack.getSender().hasPermission("builderswand.give"))
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .suggests((ctx, builder) -> {
                                            ConfigManager.getAllWands().forEach(w -> builder.suggest(w.getId()));
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> {
                                            if (!(ctx.getSource().getSender() instanceof Player player)) {
                                                ctx.getSource().getSender().sendMessage(Util.toPrefixedComponent("&cOnly players can give wands to themselves."));
                                                return Command.SINGLE_SUCCESS;
                                            }
                                            return giveWand(ctx.getSource(), player, StringArgumentType.getString(ctx, "wand id"));
                                        })
                                        .then(Commands.argument("target", ArgumentTypes.player())
                                                .executes(ctx -> {
                                                    String wandId = StringArgumentType.getString(ctx, "wand id");
                                                    List<Player> players = ctx.getArgument("target", PlayerSelectorArgumentResolver.class).resolve(ctx.getSource());

                                                    if (players.isEmpty()) return Command.SINGLE_SUCCESS;

                                                    for (Player player : players) {
                                                        giveWand(ctx.getSource(), player, wandId);
                                                    }
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )
                        .build(),
                "Base command.",
                List.of("bw", "wand")
        );
    }

    private int reloadConfig(CommandSourceStack source) {
        ConfigManager.reload();
        source.getSender().sendMessage(Util.toPrefixedComponent("&aConfiguration has been reloaded."));
        return Command.SINGLE_SUCCESS;
    }

    private int listWands(CommandSourceStack source) {
        Collection<Wand> wands = ConfigManager.getAllWands();
        if (wands.isEmpty()) {
            source.getSender().sendMessage(Util.toPrefixedComponent("&cNo wands found."));
            return Command.SINGLE_SUCCESS;
        }

        TextComponent.Builder msg = Component.text().append(Util.toPrefixedComponent("&aWands: "));
        int i = 0;
        for (Wand wand : wands) {
            msg.append(Component.text(wand.getId())
                    .color(NamedTextColor.YELLOW)
                    .hoverEvent(HoverEvent.showText(wand.getName()))
                    .clickEvent(ClickEvent.suggestCommand("/bw give " + wand.getId())));

            if (++i < wands.size()) msg.append(Component.text(", ").color(NamedTextColor.GRAY));
        }
        source.getSender().sendMessage(msg.build());
        return Command.SINGLE_SUCCESS;
    }

    private int giveWand(CommandSourceStack source, Player target, String wandId) {
        Wand wand = WandManager.getWandConfig(wandId);
        if (wand == null) {
            source.getSender().sendMessage(Util.toPrefixedComponent("&cNo wand found with ID: " + wandId));
            return Command.SINGLE_SUCCESS;
        }

        ItemStack item = WandManager.createWandItem(wand);
        target.getInventory().addItem(item);

        target.sendMessage(Util.toPrefixedComponent("&aYou received a " + wandId + " wand!"));
        if (target != source.getSender()) {
            source.getSender().sendMessage(Util.toPrefixedComponent("&aGave " + wandId + " to " + target.getName()));
        }
        return Command.SINGLE_SUCCESS;
    }
}
