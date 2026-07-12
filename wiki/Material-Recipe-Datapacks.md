# Material Recipe Datapacks

Fabrication recipes live below `data/<namespace>/recipe/` and use one of five registered types:

- `neroagriculture:material_extraction`
- `neroagriculture:essence_infusing`
- `neroagriculture:seed_synthesizing`
- `neroagriculture:seed_researching`
- `neroagriculture:material_conversion`

Every type uses the same bounded shape. `ingredient` uses vanilla item/tag ingredient syntax; `result`
uses the vanilla item-stack template shape. `input_count` is 1–64, `energy` is 0–1,000,000 NF, and `ticks`
is 1–72,000. `family` and `material` are optional identifiers used for server-side tier and component
validation.

```json
{
  "type": "neroagriculture:seed_synthesizing",
  "ingredient": "#c:ores/iron",
  "result": { "id": "neroagriculture:resource_seed" },
  "energy": 2400,
  "ticks": 140,
  "material": "c:iron",
  "family": "industrial"
}
```

Recipes are selected deterministically by recipe id. Reloading can add, replace, or remove operations
without assigning numeric material ids. Extraction and synthesis still require a matching active catalog
entry. Conversion recipes additionally must meet the catalog conversion count and return its exact output,
so a datapack cannot use forged components or an unrelated result to bypass the material definition.

The shipped baseline covers Coal, Iron, Diamond, Nether Star, and Echo Shard as one standalone material per
tier, all four essence-condensation transitions, and charging for all five tiers. Packs can add recipes for
new catalog materials without registering new items.
