# NeroQuests integration

NeroAgriculture ships a **quest pack** for NeroQuests inside its own jar. It is plain data (JSON plus
language keys under `data/neroagriculture/neroquests/`), so it adds **no dependency**: with NeroQuests
installed the chapter appears in the quest book; without it the files are simply never read.

## Chapter: Field Ledger

Chapter id `neroagriculture:field_ledger`, 15 quests, all player-scope except the last.

| Quest | How to complete it | Leads to |
| --- | --- | --- |
| First Compost | Craft four Fertiliser | Quick Growth, Heavy Harvest, First Reaping, Ledger Opened |
| Quick Growth | Craft two Speed Fertiliser | Ledger Opened |
| Heavy Harvest | Craft two Yield Fertiliser | Ledger Opened |
| Biomass Stock | Craft eight Biomass | Ledger Opened |
| Nothing Wasted | Craft four Crop Waste | Ledger Opened |
| Ledger Opened | Finish the five crafts above | Seed Scholar |
| First Reaping | Harvest a mature NeroAgriculture crop by hand | Under Glass |
| Seed Scholar | Complete a research on the Seed Research Bench | — |
| Under Glass | Open a Greenhouse Controller while its greenhouse is formed and running | Vertical Farm |
| Vertical Farm | Open a Crop Tower Controller while the tower is formed | — |
| Refinement, Synthesis, Transmutation, Ascension | Reach each tier of the fragment ladder (the native gates of the same name) | the next tier |
| Green World (server-wide) | Any terraforming project in the world reaches Complete | — |

Rewards are quest XP and NeroQuests currency; First Compost also hands out bone meal. Machine output
never counts as a craft, so the five crafting quests want the hand-crafted vanilla-ingredient recipes.

## What the mod publishes

Everything below is Neroland Core API and works for any Core listener, not only NeroQuests.

### Record gates (personal milestones)

Four player-scope progression gates, defined under `data/neroagriculture/neroland_gates/`, that the mod
opens once for a player at the moment of the milestone. They have no prerequisites and gate nothing:
no machine, recipe or tier ever checks them.

| Gate | Opened when |
| --- | --- |
| `neroagriculture:first_research` | the player completes a Seed Research Bench research |
| `neroagriculture:first_harvest` | the player hand-harvests a mature Prospora, resource or engineered crop |
| `neroagriculture:greenhouse_keeper` | the player opens a Greenhouse Controller that is formed and running |
| `neroagriculture:tower_grower` | the player opens a Crop Tower Controller whose tower is formed |

The greenhouse and tower milestones are recorded on interaction so that neither controller needs to
store an owner. The tier gates `refinement`, `synthesis`, `transmutation` and `ascension` are the real
progression gates and are documented in [Resource-seed economy](Resource-Seed-Economy.md).

### Threshold channels (world milestones)

| Channel | Scope | Meaning |
| --- | --- | --- |
| `neroagriculture:tower_output` | dimension id | crop-tower harvest in the last in-game hour crosses the configured threshold; see [Crop Towers](Crop-Towers.md#output-threshold-event) |
| `neroagriculture:terraform_stage` | dimension id | a terraforming project changed stage; value = the stage (Complete is 4); see [Terraforming](Terraforming.md#stage-threshold-event) |

Both are published with `neroagriculture` as the source mod. A channel scope is always a place or a
system, never a player, a UUID or coordinates.

## Privacy

Opening a gate stores only the gate id in Neroland Core's progression store, keyed by player UUID like
every other Core gate and erased by Core's erasure hook. Nothing is logged or broadcast; the threshold
channels carry no player data at all. See [Privacy and erasure](Privacy-and-Erasure.md).

## Writing your own quests against this mod

Use `neroquests:gate_open` with any of the gate ids above, `neroquests:custom_event` with either channel
(server-scope quests only), and `neroquests:craft_item` or `collect_item` with NeroAgriculture item ids.
Items are listed on the [Crafting recipes](Crafting-Recipes.md) page; the item tags
`#neroagriculture:fragments`, `#neroagriculture:fertilisers` and `#neroagriculture:biofuels` work in
`collect_item` too.
