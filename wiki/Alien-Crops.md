# Alien Crops

Alien crops are natural (non-engineered) species with exotic products used in food and, in later stages,
genetics and processing. Their defining rule is that they are **found, not fabricated** — exploration,
not the machine chain, is how you earn their seeds.

## Natural vs derived strains

Each alien species is either **natural** or **derived**:

- **Natural strains** are wild species. They are discovered, never synthesized. The fabrication chain
  will refuse to produce a natural-strain seed regardless of research state.
- **Derived strains** are bred or engineered from discovered stock. Once researched, they behave like
  engineered foods and may be produced through the fabrication path.

This distinction is enforced server-side: `synthesizable()` is false for every natural strain, so no
manual, automated, or component-forged route can fabricate one before it is discovered in the world.

## Acquisition

Alien species are unlocked at the [Seed Research Bench](Seed-Research.md) from a discovery sample, which
records the milestone and returns the corresponding `Alien Seed`. The seed is then planted on a
sufficient-tier grow bed and cultivated like any other crop, dropping `Alien Produce` on harvest. This
path is fully standalone; where planet worldgen is available it adds further native sources without
changing the rule that natural strains are found rather than synthesized.

## Products and effects

Alien produce is edible and may carry a bounded [signature effect](Food-Effects.md) just like engineered
food, subject to the same server-authoritative potency and duration caps.

See also: [Engineered Foods](Engineered-Foods.md), [Planet Varieties](Planet-Varieties.md).
