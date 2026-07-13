# Fusion and alloy seeds

**Fusion** combines two resources into a **combined/alloy seed** in the Fragment Infuser — the way to
obtain alloys that do not exist as ores. It is a **data-driven** recipe type
(`neroagriculture:fragment_fusion`), fully datapack-extendable.

## How it works

Put a **Resource Fragment** (the primary input) in the Infuser's primary slot and the **second input**
(an item or tag) in the secondary slot. Unlike ordinary synthesis, fusion does **not** require the
alloy resource itself — it invents the seed. The alloy seed takes the alloy resource's **own catalog
tier**, so the downstream grow → fragments → resource loop stays consistent.

## Curated built-in alloys

| Alloy | Inputs | Notes |
| ----- | ------ | ----- |
| **Nero Alloy** | Iron Fragment + Redstone | Works standalone — Core always provides Nero Alloy |
| **Steel** | Iron Fragment + Coal | Active when a mod supplies `c:ingots/steel` |
| **Energized Steel** | Steel Fragment + Redstone | Active when a mod supplies energized steel |

Steel and Energized Steel are **dormant-safe**: they only activate when a mod supplies the alloy
resource, and never produce a broken recipe when it is absent.

## Recipe format

```json
{
  "type": "neroagriculture:fragment_fusion",
  "ingredient": "neroagriculture:resource_fragment",
  "material": "c:iron",
  "input_count": 4,
  "secondary": "minecraft:coal",
  "secondary_count": 4,
  "result_material": "c:steel",
  "result": { "id": "neroagriculture:resource_seed" },
  "energy": 4000,
  "ticks": 160
}
```

## Config

- `fusion.enabled` — master toggle.
- `fusion.max_tier` — alloys whose resource sits above this tier cannot be fused.
