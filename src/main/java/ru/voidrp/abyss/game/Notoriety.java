package ru.voidrp.abyss.game;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import ru.voidrp.abyss.VoidRpAbyss;
import ru.voidrp.abyss.util.Msg;

/**
 * The "wanted" / notoriety system: a player on a long kill streak gets a growing,
 * server-funded bounty on their own head, announced to everyone. Killing them ends
 * the spree and pays out the accumulated reward (handled by the normal bounty claim).
 */
public final class Notoriety {

    /** Placed as the bounty author for server-funded wanted bounties. */
    private static final String AUTHOR = "Abyss";

    private Notoriety() {
    }

    /**
     * Called after a PvP kill with the killer's new streak length. At/above the
     * threshold, escalates the wanted bounty on the killer and broadcasts it.
     */
    public static void onKill(MinecraftServer server, ServerPlayer killer, int streak) {
        var config = VoidRpAbyss.config();
        if (!config.notoriety() || streak < config.notorietyThreshold()) {
            return;
        }
        boolean firstTime = streak == config.notorietyThreshold();
        int reward = firstTime ? config.notorietyBaseReward() : config.notorietyStepReward();
        String wantedNick = killer.getGameProfile().name();

        VoidRpAbyss.backend().placeBountyAsync(wantedNick, AUTHOR, reward, "server")
                .thenAccept(resp -> server.execute(() -> {
                    if (resp == null || !resp.ok()) {
                        return;
                    }
                    int total = resp.totalAmount();
                    if (firstTime) {
                        server.getPlayerList().broadcastSystemMessage(Msg.legacy(
                                "§4☠ §c§lРОЗЫСК! §f" + wantedNick + " §cустроил бойню — §f" + streak
                                        + " §cубийств подряд. Награда за его голову: §b" + total + "💎"),
                                false);
                    } else {
                        server.getPlayerList().broadcastSystemMessage(Msg.legacy(
                                "§c" + wantedNick + " §7продолжает бойню (§f" + streak
                                        + "§7) — награда за голову выросла до §b" + total + "💎"),
                                false);
                    }
                }))
                .exceptionally(ex -> {
                    VoidRpAbyss.LOGGER.warn("Notoriety bounty failed for {}: {}", wantedNick, ex.getMessage());
                    return null;
                });
    }
}
