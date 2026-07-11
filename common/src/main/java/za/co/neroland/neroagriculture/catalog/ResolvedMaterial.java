package za.co.neroland.neroagriculture.catalog;

import java.util.List;

/** Effective definition plus provenance for diagnostics. */
public record ResolvedMaterial(MaterialDefinition definition, CatalogSource source, String sourceDetail,
        List<String> shadowedSources) {
    public ResolvedMaterial {
        shadowedSources = List.copyOf(shadowedSources);
    }
}
