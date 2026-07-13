package ru.voidrp.abyss.game;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.voidrp.abyss.VoidRpAbyss;
import ru.voidrp.abyss.util.Msg;

/** Death coordinates, kill/death/playtime tracking, and bounty payout on PvP kills. */
public final class AbyssHooks {

    private AbyssHooks() {
    }

    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        Entity killerEntity = event.getSource().getEntity();
        ServerPlayer killer = killerEntity instanceof ServerPlayer sp ? sp : null;

        if (victim instanceof ServerPlayer deadPlayer) {
            VoidRpAbyss.stats().addDeath(deadPlayer);
            if (VoidRpAbyss.config().deathCoords()) {
                sendDeathCoords(deadPlayer);
            }
            if (killer != null && killer != deadPlayer) {
                VoidRpAbyss.stats().addPvpKill(killer);
                tryClaimBounty(deadPlayer, killer);
                if (VoidRpAbyss.config().headDrops()) {
                    dropHead(deadPlayer);
                }
                postKillEvent(deadPlayer, killer);
            }
        } else if (killer != null) {
            // A player killed a non-player living entity → mob kill.
            VoidRpAbyss.stats().addMobKill(killer);
        }
    }

    /** Drops the victim's head (a player skull carrying their skin) as a trophy. */
    private static void dropHead(ServerPlayer victim) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(victim.getGameProfile()));
        Block.popResource(victim.level(), victim.blockPosition(), head);
    }

    /** Records the PvP kill in the public killfeed (fire-and-forget). */
    private static void postKillEvent(ServerPlayer victim, ServerPlayer killer) {
        String weapon = weaponId(killer);
        VoidRpAbyss.backend()
                .postKillEventAsync(killer.getGameProfile().name(), victim.getGameProfile().name(), weapon)
                .exceptionally(ex -> {
                    VoidRpAbyss.LOGGER.warn("Kill-event post failed: {}", ex.getMessage());
                    return null;
                });
    }

    private static String weaponId(ServerPlayer p) {
        ItemStack held = p.getMainHandItem();
        if (held.isEmpty()) {
            return "";
        }
        return BuiltInRegistries.ITEM.getKey(held.getItem()).toString();
    }

    private static void sendDeathCoords(ServerPlayer player) {
        BlockPos p = player.blockPosition();
        String dim = player.level().dimension().identifier().toString();
        Msg.to(player, "§7☠ Ты погиб на §f" + p.getX() + " " + p.getY() + " " + p.getZ() + " §7(" + dim + ")");
    }

    private static void tryClaimBounty(ServerPlayer victim, ServerPlayer killer) {
        MinecraftServer server = killer.level().getServer();
        String targetNick = victim.getGameProfile().name();
        String killerNick = killer.getGameProfile().name();
        VoidRpAbyss.backend().claimBountyAsync(targetNick, killerNick)
                .thenAccept(resp -> server.execute(() -> {
                    if (resp == null || !resp.ok() || resp.totalAmount() <= 0) {
                        return;
                    }
                    int reward = resp.totalAmount();
                    // Give physical diamonds via a console give command (handles stacking).
                    server.getCommands().performPrefixedCommand(
                            server.createCommandSourceStack().withSuppressedOutput(),
                            "give " + killerNick + " minecraft:diamond " + reward);
                    server.getPlayerList().broadcastSystemMessage(
                            Msg.legacy("§6☠ §f" + killerNick + " §6получил награду за голову §f"
                                    + targetNick + " §6— §b" + reward + " алмазов§6!"),
                            false);
                }))
                .exceptionally(ex -> {
                    VoidRpAbyss.LOGGER.warn("Bounty claim failed for {} -> {}: {}",
                            targetNick, killerNick, ex.getMessage());
                    return null;
                });
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            VoidRpAbyss.stats().onJoin(player);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            VoidRpAbyss.stats().onLeave(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        int interval = VoidRpAbyss.config().statFlushMinutes() * 60 * 20;
        if (++tickCounter >= interval) {
            tickCounter = 0;
            VoidRpAbyss.stats().flush(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onStopping(ServerStoppingEvent event) {
        VoidRpAbyss.stats().flush(event.getServer());
    }
}
