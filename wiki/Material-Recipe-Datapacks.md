# Material Recipe Datapacks

Fabrication recipes live below `data/<namespace>/recipe/` and use one of five registered types:

- `neroagriculture:material_extraction`
- `neroagriculture:fragment_infusing`
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
  "family": "forgite"
}
```

Recipes are selected deterministically by recipe id. Reloading can add, replace, or remove operations
without assigning numeric material ids. Extraction and synthesis still require a matching active catalog
entry. Conversion recipes additionally must meet the catalog conversion count and return its exact output,
so a datapack cannot use forged components or an unrelated result to bypass the material definition.

The shipped baseline covers the 15 built-in materials, all four fragment-condensation transitions, and
charging for all five tiers. When no material-specific recipe exists, catalog definitions supplied by a
datapack or config override, and the common tagged metals tin, lead, silver,
nickel, zinc and aluminum/aluminium receive conservative fallback extraction, research and synthesis.
Unknown tagged entries outside that list require a datapack definition or recipes. Fallback processing
still consumes a real matching sample, checks research and gates, and does not replace an explicit recipe
whose input or other requirements are unmet. It uses the tier's normal extraction energy and time;
synthesis costs twice that energy, 40 more ticks, matching tier fragments and a Prospora Seed. These
server-generated fallbacks do not currently appear as individual recipe-viewer entries.
