package ru.voidrp.abyss.game;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * Tracks who is currently "in combat" on the anarchy server. When a player damages
 * another player both are tagged for a few seconds; logging out while tagged is
 * punished by death (see {@link AbyssHooks}). Server-thread confined.
 */
public final class CombatTracker {

    /** A live combat tag: expiry time + the last opponent (for kill attribution). */
    public record Tag(long expiresAtMillis, UUID foeUuid, String foeNick) {
        boolean active(long now) {
            return now < expiresAtMillis;
        }
    }

    private final Map<UUID, Tag> tags = new HashMap<>();

    /**
     * Tags both fighters. Returns {@code true} if the victim was not already in
     * combat (so the caller can show a one-time "you are in combat" warning).
     */
    public boolean tag(ServerPlayer victim, ServerPlayer attacker, int seconds) {
        long now = System.currentTimeMillis();
        long until = now + seconds * 1000L;
        Tag prev = tags.get(victim.getUUID());
        boolean wasActive = prev != null && prev.active(now);

        tags.put(victim.getUUID(), new Tag(until, attacker.getUUID(), attacker.getGameProfile().name()));
        tags.put(attacker.getUUID(), new Tag(until, victim.getUUID(), victim.getGameProfile().name()));
        return !wasActive;
    }

    /** Whether the player currently has an active combat tag. */
    public boolean isTagged(UUID uuid) {
        Tag t = tags.get(uuid);
        return t != null && t.active(System.currentTimeMillis());
    }

    /** Returns the active tag (or null) and removes it. */
    public Tag consume(UUID uuid) {
        Tag t = tags.remove(uuid);
        return (t != null && t.active(System.currentTimeMillis())) ? t : null;
    }

    /** Drops any tag for a player (e.g. on a normal death). */
    public void clear(UUID uuid) {
        tags.remove(uuid);
    }
}
