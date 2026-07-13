# VoidRP Abyss

Server-side anarchy gameplay mod for **VoidRP: Abyss** (NeoForge 26.2, Java 25).

Bundles the combat-driven features of the anarchy server: death coordinates,
kill/death/playtime stat sync, diamond bounties, head trophies and a live killfeed.
Loads server-side only (`displayTest=IGNORE_ALL_VERSION`) — mod-less clients still
connect. Reuses the auth-bridge game secret, so no extra systemd flags are needed.

## Features

- **Death coordinates** — on death you get a private chat message with where you died
  (inventory is not kept on anarchy, so you know where to run).
- **Diamond bounties** — `/bounty <player> <amount>` escrows physical diamonds; a PvP
  kill claims **all** open bounties on the victim and gives the killer the summed
  diamonds. Self-bounties and suicides never pay out. `/bounty list` shows the board.
- **Head trophies** — a PvP kill drops the victim's player head (with their skin) as a
  trophy / proof of the kill.
- **No combat logging** — hitting or being hit by another player tags both "in combat"
  for a few seconds; disconnecting while tagged executes the player (loot drops, the
  last attacker is credited with the kill). NeoForge fires the logout event *before* the
  player is saved, so the dead/emptied state persists — no item duplication.
- **Notoriety / "wanted"** — a long PvP kill streak makes a player wanted: the server
  puts a growing, self-funded bounty on their head and announces it. Dying resets the
  streak and pays the reward to the killer (via the normal bounty claim). Current/best
  streaks sync to the site leaderboard; wanted targets are flagged on the bounty board.
- **Kill / death / playtime stats** — accumulated per player and flushed to the backend
  (`player_stat_cache`), feeding the site leaderboard (incl. K/D and best kill streak).
- **Killfeed** — every PvP kill is posted to the backend and shown in the live "Abyss
  Pulse" feed on the site (who killed whom + weapon — never coordinates).

The bounty, killfeed and stat features talk to the VoidRP backend
(`/api/v1/bounties/*`, `/api/v1/game-sync/kill-event`, `/api/v1/game-sync/player-stats`,
`X-Game-Auth-Secret`); the mod tolerates a missing backend (logs warnings).

## Build

```bash
./gradlew jar          # -> build/libs/voidrp_abyss-<ver>+mc26.2.jar
```

## Config (JVM system properties)

Reuses the auth-bridge secret/backend by default (no extra flags needed):

| Property | Default |
|---|---|
| `voidrp.abyss.backend` | `voidrp.auth.backend` → `https://api.void-rp.ru` |
| `voidrp.abyss.gameSecret` | `voidrp.auth.gameSecret` |
| `voidrp.abyss.serverSlug` | (empty → default server) |
| `voidrp.abyss.statFlushMinutes` | `3` |
| `voidrp.abyss.deathCoords` | `true` |
| `voidrp.abyss.headDrops` | `true` |
| `voidrp.abyss.combatLog` | `true` (punish combat logging) |
| `voidrp.abyss.combatTagSeconds` | `20` |
| `voidrp.abyss.notoriety` | `true` (wanted system) |
| `voidrp.abyss.notorietyThreshold` | `5` (kills before wanted) |
| `voidrp.abyss.notorietyBaseReward` | `4` (diamonds at threshold) |
| `voidrp.abyss.notorietyStepReward` | `2` (diamonds per further kill) |

## Commands

`/bounty <player> <amount>` — put a diamond bounty on a player ·
`/bounty list` — show active bounties

Companion mod: [voidrp_claims](https://github.com/VOIDRP-MINECRAFT/voidrp_claims)
(block-anchored claims + raid alerts) runs alongside it on Abyss.
