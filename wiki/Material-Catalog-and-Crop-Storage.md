# Material catalog and crop storage

NeroAgriculture resolves one deterministic, server-authoritative material catalog after item tags and
datapacks load. The precedence is:

1. `data/<namespace>/neroagriculture/materials/*.json`
2. server blacklist and overrides
3. Neroland Core meteor-material metadata
4. explicit vanilla/Core defaults
5. discovered `c:ores/*` tags; unknown third-party materials default to the configured
   `discovery.default_tier` (Orbite unless changed; unknown gems are always Orbite)

Core metadata is adapted read-only. Conflicts and validation failures are retained for operator
diagnostics rather than silently discarded.

## Datapack format

The file path defines the material id. For example,
`data/example/neroagriculture/materials/osmium.json` defines `example:osmium`:

```json
{
  "input": { "tag": "c:ores/osmium" },
  "output": "example:raw_osmium",
  "tier": "orbite",
  "yield": { "minimum": 1, "maximum": 5, "ramp_harvests": 96 },
  "conversion": 16,
  "display_key": "material.example.osmium",
  "color": "#7A94A8",
  "enabled": true,
  "dimension": "nerospace:station"
}
```

`input` contains exactly one `item` or `tag`. `gate` is optional and defaults from the tier; explicit
`null` means no gate. `dimension`, `enabled`, and `gate` are optional. Invalid identifiers, missing
items/tags, impossible yield ranges, conversion values, colors, and oversized display keys produce an
actionable catalog error.

`color` accepts a 24-bit RGB value in any of `#RRGGBB`, `0xRRGGBB`, bare hex, decimal-integer, or JSON
number form — datapack material and food definitions and the config override `color=` field all share one
parser, so the same input never parses differently by source.

The blacklist is a comma-separated material-id list. Overrides use semicolon-separated entries:

```text
example:osmium|tier=colonite|gate=neroagriculture:transmutation|yield=1:6:128|conversion=20|enabled=true
```

Use `/neroagriculture catalog list`, `show <material>`, or `errors` as an operator. These commands expose
material metadata only and contain no player data.

## Client and save safety

Clients receive only id, display key, tier, and RGB palette metadata. The packet is capped at 4,096
entries and 256 KiB. The configured discovery cap and blacklist apply before entries are exposed or
synced.

Seeds and crops store the literal material identifier, never a numeric catalog index. Removing or
disabling a definition therefore preserves the item/crop and its harvest history, displays a warning,
and prevents growth. Re-adding the same id makes it usable again without migration.

The resource-crop block entity has no ticker. The Gate 3 Java Object Layout benchmark modeled the
incremental retained crop entity/variant state for 4,096 crops at **352,296 bytes (86.0 bytes/crop)**.
Across the two supported versions, the 200,000-iteration empty idle loop measured **1.437–1.478 ms**
versus **1.648–1.997 ms** for the equivalent crop-architecture loop, with **zero scheduled crop
block-entity ticks**; reading all 4,096 identities took **0.086–0.498 ms**. Run it with:

```text
./gradlew :neoforge:26.2:cropStorageBenchmark
```

The memory figure covers the crop block-entity shallow layout plus the deep variant-state graph; shared
Minecraft block states and normal chunk-container overhead are baseline engine costs.
