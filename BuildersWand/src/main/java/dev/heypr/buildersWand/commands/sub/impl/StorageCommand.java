package dev.heypr.buildersWand.commands.sub.impl;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.heypr.buildersWand.BuildersWand;
import dev.heypr.buildersWand.commands.sub.Subcommand;
import dev.heypr.buildersWand.managers.WandStorage;
import dev.heypr.buildersWand.managers.WandStorageManager;
import dev.heypr.buildersWand.managers.io.ConfigManager;
import dev.heypr.buildersWand.managers.io.MessageManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public class StorageCommand implements Subcommand {
    private static final int MAX_LISTED = 10;

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("storage")
                .then(Commands.literal("list")
                        .requires(stack -> stack.getSender().hasPermission("builderswand.storage.list"))
                        .executes(this::executeList))
                .then(Commands.literal("inspect")
                        .requires(stack -> stack.getSender().hasPermission("builderswand.storage.inspect"))
                        .then(Commands.argument("uuid", StringArgumentType.string())
                                .executes(this::executeInspect)))
                .then(Commands.literal("prune")
                        .requires(stack -> stack.getSender().hasPermission("builderswand.storage.prune"))
                        .executes(this::executePrune));
    }

    private int executeList(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        WandStorageManager manager = BuildersWand.getStorageManager();
        if (manager == null) {
            MessageManager.sendMessage(sender, MessageManager.Messages.STORAGE_UNAVAILABLE);
            return Command.SINGLE_SUCCESS;
        }
        WandStorageManager.StorageSummary summary = manager.summarize();
        String oldest = summary.totalFiles() == 0 ? "-" : formatAge(summary.oldestModifiedMillis());
        String newest = summary.totalFiles() == 0 ? "-" : formatAge(summary.newestModifiedMillis());
        MessageManager.sendMessage(sender, MessageManager.Messages.STORAGE_SUMMARY, "total", summary.totalFiles(), "empty", summary.emptyFiles(), "nonempty", summary.totalFiles() - summary.emptyFiles(), "size", formatBytes(summary.totalBytes()), "oldest", oldest, "newest", newest);
        return Command.SINGLE_SUCCESS;
    }

    private int executeInspect(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player admin)) {
            MessageManager.sendMessage(sender, MessageManager.Messages.ONLY_PLAYERS);
            return Command.SINGLE_SUCCESS;
        }
        WandStorageManager manager = BuildersWand.getStorageManager();
        if (manager == null) {
            MessageManager.sendMessage(sender, MessageManager.Messages.STORAGE_UNAVAILABLE);
            return Command.SINGLE_SUCCESS;
        }
        String uuid = StringArgumentType.getString(ctx, "uuid");
        Optional<WandStorage> storage = manager.findByUUID(uuid);
        if (storage.isEmpty()) {
            MessageManager.sendMessage(sender, MessageManager.Messages.STORAGE_NOT_FOUND, "uuid", uuid);
            return Command.SINGLE_SUCCESS;
        }
        storage.get().openForAdmin(admin);
        return Command.SINGLE_SUCCESS;
    }

    private int executePrune(CommandContext<CommandSourceStack> ctx) {
        CommandSender sender = ctx.getSource().getSender();
        WandStorageManager manager = BuildersWand.getStorageManager();
        if (manager == null) {
            MessageManager.sendMessage(sender, MessageManager.Messages.STORAGE_UNAVAILABLE);
            return Command.SINGLE_SUCCESS;
        }
        long staleAfterMillis = ConfigManager.getPruneStaleAfterDays() * 24L * 60L * 60L * 1000L;
        WandStorageManager.PruneResult result = manager.pruneStale(staleAfterMillis);
        MessageManager.sendMessage(sender, MessageManager.Messages.PRUNE_RESULT, "deleted", result.deletedEmpty(), "stale", result.staleNonEmpty().size());
        if (!result.staleNonEmpty().isEmpty()) {
            List<String> shown = result.staleNonEmpty().stream().limit(MAX_LISTED).toList();
            String suffix = result.staleNonEmpty().size() > MAX_LISTED ? ", ..." : "";
            MessageManager.sendMessage(sender, MessageManager.Messages.PRUNE_STALE_LIST, "uuids", String.join(", ", shown) + suffix);
        }
        return Command.SINGLE_SUCCESS;
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    private static String formatAge(long modifiedMillis) {
        long ageMillis = System.currentTimeMillis() - modifiedMillis;
        long days = ageMillis / 86_400_000L;
        if (days > 0) return days + "d ago";
        long hours = ageMillis / 3_600_000L;
        if (hours > 0) return hours + "h ago";
        return "just now";
    }
}
