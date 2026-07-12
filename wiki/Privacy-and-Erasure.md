# Privacy and Data Erasure

NeroAgriculture is built to be POPIA- and GDPR-friendly: it stores the minimum player data, never a name,
and everything it does keep can be exported and erased through Neroland Core.

## What player data is stored

Only two kinds, both keyed by **UUID only** (never a username or display name):

- **Machine ownership** — the Planter, Harvester, Fertiliser Applicator and Terraforming Controller may
  record the placing player's UUID so a claims/protection mod can authorise their edits. This is opt-out
  via `automation.track_owner = false`, in which case no owner is stored at all.
- **Material milestones / research** — which materials and seeds a player has discovered or researched is
  tracked through Core's shared progression store, not by this mod directly.

Everything else — the material catalog, greenhouse and tower state, terraformed regions, cycle phases — is
**world data with no player attribution**.

## What is never stored

No usernames or display names, no chat, no location history, and no play-time analytics. Aggregate
world/environment data is never linked to a player.

## Anonymous crash reporting (opt-out)

Like the rest of the Neroland family, NeroAgriculture includes **opt-out** crash/error reporting via
Sentry (EU-region servers) so bugs can be fixed. It is disclosed here for the POPIA/GDPR and CurseForge
transparency rules:

- **Opt-out:** on by default, client-local. Set `telemetryEnabled = false` in
  `config/neroagriculture.properties` to switch it off (takes effect on restart).
- **NeroAgriculture errors only:** an event is sent only if its stack trace touches
  `za.co.neroland.neroagriculture`; everything else is dropped before sending.
- **No personal data:** never a name, UUID, IP, hostname or world data. Home-directory paths (which
  contain the OS account name) are scrubbed. The payload is a stack trace plus public version strings —
  mod / Minecraft / loader / OS / Java versions and the loaded-mod list.
- **Bounded:** per-session de-duplication and a hard cap of 10 events per session; nothing is stored on
  disk locally.

## Export and erasure

- **Milestones/research** export and erase through Core's shared `data.PlayerDataErasure` hook — a single
  erasure request purges a player across every Nero mod that registers with it.
- **Machine ownership** registers its own eraser with the same hook: an erasure request clears the owner
  UUID from every loaded owned machine (the machine simply becomes unowned). Breaking a machine also drops
  its owner.

## For administrators

Set `automation.track_owner = false` to disable ownership recording entirely. Player-data erasure is
performed through Core's mechanism; NeroAgriculture participates automatically once Core processes a request.

See also: [Administration](Administration.md), [Configuration](Configuration.md).
