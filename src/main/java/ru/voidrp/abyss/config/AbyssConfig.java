package ru.voidrp.abyss.config;

import java.net.URI;

/**
 * Runtime configuration via JVM system properties. Reuses the auth-bridge's game
 * secret / backend by default (same as voidrp_claims), so no extra systemd flags:
 *   -Dvoidrp.auth.gameSecret=...  -Dvoidrp.auth.backend=https://api.void-rp.ru
 * Abyss-specific overrides: -Dvoidrp.abyss.backend / -Dvoidrp.abyss.gameSecret /
 *   -Dvoidrp.abyss.serverSlug / -Dvoidrp.abyss.statFlushMinutes.
 */
public final class AbyssConfig {

    private final URI backendBaseUrl;
    private final String gameAuthSecret;
    private final String serverSlug;
    private final int statFlushMinutes;
    private final boolean deathCoords;
    private final boolean headDrops;
    private final boolean combatLog;
    private final int combatTagSeconds;
    private final boolean notoriety;
    private final int notorietyThreshold;
    private final int notorietyBaseReward;
    private final int notorietyStepReward;

    private AbyssConfig(URI backendBaseUrl, String gameAuthSecret, String serverSlug,
                        int statFlushMinutes, boolean deathCoords, boolean headDrops,
                        boolean combatLog, int combatTagSeconds, boolean notoriety,
                        int notorietyThreshold, int notorietyBaseReward, int notorietyStepReward) {
        this.backendBaseUrl = backendBaseUrl;
        this.gameAuthSecret = gameAuthSecret;
        this.serverSlug = serverSlug;
        this.statFlushMinutes = statFlushMinutes;
        this.deathCoords = deathCoords;
        this.headDrops = headDrops;
        this.combatLog = combatLog;
        this.combatTagSeconds = combatTagSeconds;
        this.notoriety = notoriety;
        this.notorietyThreshold = notorietyThreshold;
        this.notorietyBaseReward = notorietyBaseReward;
        this.notorietyStepReward = notorietyStepReward;
    }

    public static AbyssConfig load() {
        String backend = prop("voidrp.abyss.backend", prop("voidrp.auth.backend", "https://api.void-rp.ru"));
        String secret = prop("voidrp.abyss.gameSecret", prop("voidrp.auth.gameSecret", ""));
        String slug = prop("voidrp.abyss.serverSlug", "");
        int flush = intProp("voidrp.abyss.statFlushMinutes", 3);
        boolean death = boolProp("voidrp.abyss.deathCoords", true);
        boolean heads = boolProp("voidrp.abyss.headDrops", true);
        boolean combatLog = boolProp("voidrp.abyss.combatLog", true);
        int combatTag = Math.max(1, intProp("voidrp.abyss.combatTagSeconds", 20));
        boolean noto = boolProp("voidrp.abyss.notoriety", true);
        int notoThreshold = Math.max(2, intProp("voidrp.abyss.notorietyThreshold", 5));
        int notoBase = Math.max(1, intProp("voidrp.abyss.notorietyBaseReward", 4));
        int notoStep = Math.max(1, intProp("voidrp.abyss.notorietyStepReward", 2));
        return new AbyssConfig(URI.create(backend), secret, slug, Math.max(1, flush), death, heads,
                combatLog, combatTag, noto, notoThreshold, notoBase, notoStep);
    }

    private static String prop(String key, String def) {
        String v = System.getProperty(key);
        return (v == null || v.isBlank()) ? def : v.trim();
    }

    private static int intProp(String key, int def) {
        try {
            String v = System.getProperty(key);
            return (v == null || v.isBlank()) ? def : Integer.parseInt(v.trim());
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    private static boolean boolProp(String key, boolean def) {
        String v = System.getProperty(key);
        return (v == null || v.isBlank()) ? def : Boolean.parseBoolean(v.trim());
    }

    public URI backendBaseUrl() {
        return backendBaseUrl;
    }

    public String gameAuthSecret() {
        return gameAuthSecret;
    }

    public String serverSlug() {
        return serverSlug;
    }

    public int statFlushMinutes() {
        return statFlushMinutes;
    }

    public boolean deathCoords() {
        return deathCoords;
    }

    public boolean headDrops() {
        return headDrops;
    }

    public boolean combatLog() {
        return combatLog;
    }

    public int combatTagSeconds() {
        return combatTagSeconds;
    }

    public boolean notoriety() {
        return notoriety;
    }

    public int notorietyThreshold() {
        return notorietyThreshold;
    }

    public int notorietyBaseReward() {
        return notorietyBaseReward;
    }

    public int notorietyStepReward() {
        return notorietyStepReward;
    }
}
