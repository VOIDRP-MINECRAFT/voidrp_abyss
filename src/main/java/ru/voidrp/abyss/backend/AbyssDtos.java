package ru.voidrp.abyss.backend;

import java.util.List;

/** Request/response DTOs mirroring the backend schemas. Gson uses
 *  LOWER_CASE_WITH_UNDERSCORES, so {@code placedByNick} ↔ {@code placed_by_nick}. */
public final class AbyssDtos {

    private AbyssDtos() {
    }

    // ── bounties ─────────────────────────────────────────────────────────
    public record BountyPlaceRequest(String targetNick, String placedByNick, int amount) {
    }

    public record BountyClaimRequest(String targetNick, String killerNick) {
    }

    public record BountyActionResponse(boolean ok, String error, int totalAmount) {
    }

    public record BountyBoardEntry(String targetNick, int totalAmount, int contributorCount, String lastUpdated) {
    }

    public record BountyBoardResponse(List<BountyBoardEntry> bounties) {
    }

    // ── stat sync ────────────────────────────────────────────────────────
    public record PlayerStatDelta(
            String nick,
            int pvpKills,
            int mobKills,
            int deaths,
            int playtimeMinutes) {
    }

    public record PlayerStatsBatchRequest(List<PlayerStatDelta> players) {
    }

    public record PlayerStatsBatchResponse(boolean ok, int updated) {
    }
}
