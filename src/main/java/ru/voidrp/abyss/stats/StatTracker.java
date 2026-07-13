package ru.voidrp.abyss.stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import ru.voidrp.abyss.VoidRpAbyss;
import ru.voidrp.abyss.backend.AbyssDtos.PlayerStatDelta;

/**
 * Accumulates per-player stat deltas (kills/deaths/playtime) between periodic
 * flushes to the backend. Server-thread confined. Playtime is accrued in whole
 * minutes from each player's session, carrying the sub-minute remainder.
 */
public final class StatTracker {

    private static final class Stat {
        String nick;
        int pvpKills;
        int mobKills;
        int deaths;
        int playtimeMinutes;

        boolean isEmpty() {
            return pvpKills == 0 && mobKills == 0 && deaths == 0 && playtimeMinutes == 0;
        }
    }

    /** key = nick lowercase */
    private final Map<String, Stat> pending = new HashMap<>();
    /** persistent per-nick kill streak: [current, peakSinceFlush]. Survives flushes. */
    private final Map<String, int[]> streaks = new HashMap<>();
    /** per-online-player timestamp up to which playtime has already been credited */
    private final Map<UUID, Long> accruedUntil = new HashMap<>();
    private final Map<UUID, String> nickByUuid = new HashMap<>();

    private Stat stat(String nickLower, String rawNick) {
        Stat s = pending.computeIfAbsent(nickLower, k -> new Stat());
        s.nick = rawNick;
        return s;
    }

    /** Increments the killer's PvP kill streak and returns the new streak length. */
    public int addPvpKill(ServerPlayer killer) {
        stat(key(killer), killer.getGameProfile().name()).pvpKills++;
        int[] st = streaks.computeIfAbsent(key(killer), k -> new int[2]);
        st[0]++;
        st[1] = Math.max(st[1], st[0]);
        return st[0];
    }

    public void addMobKill(ServerPlayer killer) {
        stat(key(killer), killer.getGameProfile().name()).mobKills++;
    }

    public void addDeath(ServerPlayer victim) {
        stat(key(victim), victim.getGameProfile().name()).deaths++;
        // Any death breaks the streak (peak is kept until the next flush).
        int[] st = streaks.get(key(victim));
        if (st != null) {
            st[0] = 0;
        }
    }

    /** Current PvP kill streak for a nick (0 if none). */
    public int killStreak(String nickLower) {
        int[] st = streaks.get(nickLower);
        return st == null ? 0 : st[0];
    }

    public void onJoin(ServerPlayer player) {
        accruedUntil.put(player.getUUID(), System.currentTimeMillis());
        nickByUuid.put(player.getUUID(), player.getGameProfile().name());
    }

    public void onLeave(ServerPlayer player) {
        accruePlaytime(player.getUUID(), System.currentTimeMillis());
        accruedUntil.remove(player.getUUID());
        nickByUuid.remove(player.getUUID());
    }

    private void accruePlaytime(UUID uuid, long now) {
        Long last = accruedUntil.get(uuid);
        String nick = nickByUuid.get(uuid);
        if (last == null || nick == null) {
            return;
        }
        long minutes = (now - last) / 60_000L;
        if (minutes > 0) {
            stat(nick.toLowerCase(Locale.ROOT), nick).playtimeMinutes += (int) minutes;
            accruedUntil.put(uuid, last + minutes * 60_000L);
        }
    }

    /** Credits current sessions, then POSTs a non-empty batch and clears counters. */
    public void flush(MinecraftServer server) {
        long now = System.currentTimeMillis();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            accruePlaytime(p.getUUID(), now);
        }
        if (pending.isEmpty()) {
            return;
        }
        List<PlayerStatDelta> batch = new ArrayList<>();
        for (Stat s : pending.values()) {
            if (!s.isEmpty()) {
                String nickLower = s.nick.toLowerCase(Locale.ROOT);
                int[] st = streaks.get(nickLower);
                int current = st == null ? 0 : st[0];
                int peak = st == null ? 0 : st[1];
                batch.add(new PlayerStatDelta(
                        s.nick, s.pvpKills, s.mobKills, s.deaths, s.playtimeMinutes, current, peak));
                // Peak resets to the current streak for the next window; drop dead entries.
                if (st != null) {
                    if (st[0] == 0) {
                        streaks.remove(nickLower);
                    } else {
                        st[1] = st[0];
                    }
                }
            }
        }
        pending.clear();
        if (batch.isEmpty()) {
            return;
        }
        VoidRpAbyss.backend().syncStatsAsync(batch)
                .exceptionally(ex -> {
                    VoidRpAbyss.LOGGER.warn("Stat sync failed ({} players dropped): {}",
                            batch.size(), ex.getMessage());
                    return null;
                });
    }

    private static String key(ServerPlayer p) {
        return p.getGameProfile().name().toLowerCase(Locale.ROOT);
    }
}
