# Engineered Foods

Engineered food crops are a parallel seed family to resource crops. Where resource crops feed the
material economy, food crops feed the player: each carries a **nutrition value**, a **saturation
value**, and — for most species — a bounded **signature effect** applied when eaten.

## Species identity

Like resource crops, food crops keep the registered item catalogue finite. One `Food Seed` item, one
`Engineered Food Crop` block, and one `Engineered Food` produce item all carry their species in a
`species_variant` data component rather than as separate registered items. The server-owned food
catalogue resolves that id to a full definition (nutrition, saturation, effect, caps, tier, planet
theme). Datapacks may add or replace species under
`data/neroagriculture/neroagriculture/foods/food/<name>.json`.

Item **names** are derived from the species id's leaf path alone (e.g. `neroagriculture:food/starleaf`
reads as "Starleaf") — the same rule seeds use — so the name a dedicated server computes for container
titles, `/give` feedback and anvils always matches what the client renders. The catalogue's translated
display name still appears in the seed tooltip.

## Growing and harvesting

Food seeds are planted on a grow bed whose tier meets or exceeds the species tier, exactly like
resource crops — see [Grow Beds](Grow-Beds.md) and [Growth Conditions](Growth-Conditions.md). Growth is
server-authoritative and fail-closed: an unknown species, an under-tier bed, low light, or (for powered
tiers) missing NF/nutrient all stop growth. Right-clicking a mature crop yields one `Engineered Food`
stamped with the species; breaking the crop returns the seed and preserves its harvest history for the
genetics work in later stages.

## Nutrition and effects

Eating an `Engineered Food` restores hunger and saturation and, if the species defines one, applies its
[signature effect](Food-Effects.md). Effect potency (amplifier) and duration are always clamped to the
per-species caps recorded in the definition, and the effect is re-derived from the server catalogue on
consumption — a forged component can never grant an out-of-cap effect. Vanilla handles renewal and
removal.

## Acquisition

New food species are unlocked at the [Seed Research Bench](Seed-Research.md): researching a species from
its discovery sample records the milestone and hands back the corresponding seed. From there the crop is
cultivated and replanted like any other.

See also: [Alien Crops](Alien-Crops.md), [Planet Varieties](Planet-Varieties.md).
