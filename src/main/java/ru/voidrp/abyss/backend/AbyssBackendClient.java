package ru.voidrp.abyss.backend;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import ru.voidrp.abyss.backend.AbyssDtos.BountyActionResponse;
import ru.voidrp.abyss.backend.AbyssDtos.BountyBoardResponse;
import ru.voidrp.abyss.backend.AbyssDtos.BountyClaimRequest;
import ru.voidrp.abyss.backend.AbyssDtos.BountyPlaceRequest;
import ru.voidrp.abyss.backend.AbyssDtos.KillEventRequest;
import ru.voidrp.abyss.backend.AbyssDtos.KillEventResponse;
import ru.voidrp.abyss.backend.AbyssDtos.PlayerStatDelta;
import ru.voidrp.abyss.backend.AbyssDtos.PlayerStatsBatchRequest;
import ru.voidrp.abyss.backend.AbyssDtos.PlayerStatsBatchResponse;
import ru.voidrp.abyss.config.AbyssConfig;

/** Async HTTP client for the abyss gameplay endpoints (game-auth secret). */
public final class AbyssBackendClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final AbyssConfig config;
    private final Gson gson;
    private final HttpClient http;

    public AbyssBackendClient(AbyssConfig config) {
        this.config = config;
        this.gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .disableHtmlEscaping()
                .create();
        this.http = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    }

    public CompletableFuture<BountyActionResponse> placeBountyAsync(String target, String placedBy, int amount) {
        return placeBountyAsync(target, placedBy, amount, "player");
    }

    public CompletableFuture<BountyActionResponse> placeBountyAsync(
            String target, String placedBy, int amount, String source) {
        return sendAsync(jsonPost("/api/v1/bounties/place",
                new BountyPlaceRequest(target, placedBy, amount, source)), BountyActionResponse.class);
    }

    public CompletableFuture<BountyActionResponse> claimBountyAsync(String target, String killer) {
        return sendAsync(jsonPost("/api/v1/bounties/claim",
                new BountyClaimRequest(target, killer)), BountyActionResponse.class);
    }

    public CompletableFuture<BountyBoardResponse> boardAsync() {
        return sendAsync(request("/api/v1/bounties/board").GET().build(), BountyBoardResponse.class);
    }

    public CompletableFuture<PlayerStatsBatchResponse> syncStatsAsync(List<PlayerStatDelta> players) {
        return sendAsync(jsonPost("/api/v1/game-sync/player-stats",
                new PlayerStatsBatchRequest(players)), PlayerStatsBatchResponse.class);
    }

    public CompletableFuture<KillEventResponse> postKillEventAsync(String killer, String victim, String weapon) {
        return sendAsync(jsonPost("/api/v1/game-sync/kill-event",
                new KillEventRequest(killer, victim, weapon, "pvp")), KillEventResponse.class);
    }

    // ── internals ────────────────────────────────────────────────────────
    private HttpRequest.Builder request(String path) {
        URI uri = config.backendBaseUrl().resolve(path);
        HttpRequest.Builder b = HttpRequest.newBuilder(uri)
                .timeout(TIMEOUT)
                .header("X-Game-Auth-Secret", config.gameAuthSecret());
        if (!config.serverSlug().isBlank()) {
            b.header("X-Server-Slug", config.serverSlug());
        }
        return b;
    }

    private HttpRequest jsonPost(String path, Object body) {
        return request(path)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body), StandardCharsets.UTF_8))
                .build();
    }

    private <T> CompletableFuture<T> sendAsync(HttpRequest req, Class<T> type) {
        return http.sendAsync(req, HttpResponse.BodyHandlers.ofString())
                .thenApply(resp -> {
                    if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                        return gson.fromJson(resp.body(), type);
                    }
                    throw new RuntimeException("http_" + resp.statusCode() + ": " + resp.body());
                });
    }
}
