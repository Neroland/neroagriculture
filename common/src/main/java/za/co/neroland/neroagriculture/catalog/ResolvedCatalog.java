package za.co.neroland.neroagriculture.catalog;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.resources.Identifier;

/** Immutable complete + exposed catalog snapshot. Disabled/unknown variants remain distinguishable. */
public record ResolvedCatalog(Map<Identifier, ResolvedMaterial> all, Map<Identifier, ResolvedMaterial> exposed,
        List<String> errors) {
    public ResolvedCatalog {
        all = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(all));
        exposed = java.util.Collections.unmodifiableMap(new java.util.LinkedHashMap<>(exposed));
        errors = List.copyOf(errors);
    }

    public Lookup lookup(Identifier id) {
        ResolvedMaterial material = all.get(id);
        if (material == null) return new Lookup(Status.UNKNOWN, Optional.empty());
        return new Lookup(material.definition().enabled() && exposed.containsKey(id) ? Status.ACTIVE : Status.DISABLED,
                Optional.of(material));
    }

    public enum Status { ACTIVE, DISABLED, UNKNOWN }
    public record Lookup(Status status, Optional<ResolvedMaterial> material) {
        public boolean permitsGrowth() { return status == Status.ACTIVE; }
        public String warningKey() {
            return status == Status.UNKNOWN ? "warning.neroagriculture.unknown_material"
                    : status == Status.DISABLED ? "warning.neroagriculture.disabled_material" : "";
        }
    }
}
