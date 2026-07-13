package ru.voidrp.abyss.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import ru.voidrp.abyss.VoidRpAbyss;
import ru.voidrp.abyss.util.Msg;

/** /bounty &lt;player&gt; &lt;amount&gt; — pledge diamonds for a kill; /bounty list — the board. */
public final class BountyCommands {

    private BountyCommands() {
    }

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("bounty")
                        .then(Commands.literal("list").executes(BountyCommands::list))
                        .then(Commands.argument("target", StringArgumentType.word())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(BountyCommands::place)))
                        .executes(ctx -> {
                            ctx.getSource().sendSystemMessage(Msg.legacy(
                                    "§eИспользование: §f/bounty <ник> <алмазов>§e · §f/bounty list"));
                            return 1;
                        }));
    }

    private static int place(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer placer = ctx.getSource().getPlayerOrException();
        MinecraftServer server = ctx.getSource().getServer();
        String target = StringArgumentType.getString(ctx, "target");
        int amount = IntegerArgumentType.getInteger(ctx, "amount");
        String placerNick = placer.getGameProfile().name();

        if (target.equalsIgnoreCase(placerNick)) {
            Msg.to(placer, "§cНельзя назначить награду за собственную голову.");
            return 0;
        }

        int have = countDiamonds(placer.getInventory());
        if (have < amount) {
            Msg.to(placer, "§cНужно §b" + amount + "§c алмазов, у тебя §b" + have + "§c.");
            return 0;
        }
        removeDiamonds(placer.getInventory(), amount);

        VoidRpAbyss.backend().placeBountyAsync(target, placerNick, amount)
                .thenAccept(resp -> server.execute(() -> {
                    if (resp != null && resp.ok()) {
                        server.getPlayerList().broadcastSystemMessage(
                                Msg.legacy("§e☠ §f" + placerNick + " §eназначил §b" + amount
                                        + "§e алмазов за голову §f" + target
                                        + "§e! Всего на нём: §b" + resp.totalAmount() + "§e."),
                                false);
                    } else {
                        // Backend rejected — refund the diamonds.
                        refund(server, placerNick, amount);
                        Msg.to(placer, "§cНаграда не назначена: "
                                + (resp != null ? resp.error() : "нет ответа") + ". Алмазы возвращены.");
                    }
                }))
                .exceptionally(ex -> {
                    server.execute(() -> {
                        refund(server, placerNick, amount);
                        Msg.to(placer, "§cСервис наград недоступен, попробуй позже. Алмазы возвращены.");
                    });
                    return null;
                });
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        VoidRpAbyss.backend().boardAsync()
                .thenAccept(resp -> source.getServer().execute(() -> {
                    if (resp == null || resp.bounties() == null || resp.bounties().isEmpty()) {
                        source.sendSystemMessage(Msg.legacy("§7Активных наград нет."));
                        return;
                    }
                    source.sendSystemMessage(Msg.legacy("§6☠ Награды за головы:"));
                    int shown = 0;
                    for (var e : resp.bounties()) {
                        if (shown++ >= 15) {
                            break;
                        }
                        source.sendSystemMessage(Msg.legacy("  §f" + e.targetNick() + " §7— §b"
                                + e.totalAmount() + "§7 алмазов (§f" + e.contributorCount() + "§7)"));
                    }
                }))
                .exceptionally(ex -> {
                    source.getServer().execute(() -> source.sendSystemMessage(
                            Msg.legacy("§cНе удалось получить список наград.")));
                    return null;
                });
        return 1;
    }

    private static void refund(MinecraftServer server, String nick, int amount) {
        server.getCommands().performPrefixedCommand(
                server.createCommandSourceStack().withSuppressedOutput(),
                "give " + nick + " minecraft:diamond " + amount);
    }

    private static int countDiamonds(Inventory inv) {
        int total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.DIAMOND)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void removeDiamonds(Inventory inv, int amount) {
        int remaining = amount;
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(Items.DIAMOND)) {
                int take = Math.min(remaining, stack.getCount());
                inv.removeItem(i, take);
                remaining -= take;
            }
        }
    }
}
