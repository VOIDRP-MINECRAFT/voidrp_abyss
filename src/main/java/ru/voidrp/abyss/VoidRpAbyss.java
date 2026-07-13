package ru.voidrp.abyss;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.voidrp.abyss.backend.AbyssBackendClient;
import ru.voidrp.abyss.command.BountyCommands;
import ru.voidrp.abyss.config.AbyssConfig;
import ru.voidrp.abyss.game.AbyssHooks;
import ru.voidrp.abyss.game.CombatTracker;
import ru.voidrp.abyss.stats.StatTracker;

/** Abyss anarchy gameplay: death coordinates, kill/death/playtime stat sync,
 *  and diamond bounties. Server-side; reuses the auth-bridge game secret. */
@Mod(VoidRpAbyss.MODID)
public final class VoidRpAbyss {

    public static final String MODID = "voidrp_abyss";
    public static final Logger LOGGER = LoggerFactory.getLogger("VoidRpAbyss");

    private static AbyssConfig config;
    private static AbyssBackendClient backend;
    private static final StatTracker STATS = new StatTracker();
    private static final CombatTracker COMBAT = new CombatTracker();

    public VoidRpAbyss(IEventBus modBus) {
        config = AbyssConfig.load();
        backend = new AbyssBackendClient(config);

        NeoForge.EVENT_BUS.register(AbyssHooks.class);
        NeoForge.EVENT_BUS.register(BountyCommands.class);

        LOGGER.info("VoidRP Abyss loaded — backend={}, statFlush={}min, deathCoords={}",
                config.backendBaseUrl(), config.statFlushMinutes(), config.deathCoords());
    }

    public static AbyssConfig config() {
        return config;
    }

    public static AbyssBackendClient backend() {
        return backend;
    }

    public static StatTracker stats() {
        return STATS;
    }

    public static CombatTracker combat() {
        return COMBAT;
    }
}
