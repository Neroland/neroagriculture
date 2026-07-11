package za.co.neroland.neroagriculture.catalog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.resources.Identifier;

/** Pure deterministic precedence/conflict/cap resolver. */
public final class CatalogResolver {
    public record Candidate(MaterialDefinition definition, CatalogSource source, String detail) { }

    private CatalogResolver() { }

    public static ResolvedCatalog resolve(Collection<Candidate> input, Set<Identifier> blacklist,
            Map<Identifier, MaterialOverride> overrides, int configuredCap, Collection<String> initialErrors) {
        List<String> errors = new ArrayList<>(initialErrors);
        Map<Identifier, List<Candidate>> grouped = new TreeMap<>(Comparator.comparing(Identifier::toString));
        input.forEach(candidate -> grouped.computeIfAbsent(candidate.definition().id(), key -> new ArrayList<>()).add(candidate));

        Map<Identifier, ResolvedMaterial> all = new LinkedHashMap<>();
        Comparator<Candidate> order = Comparator.comparingInt((Candidate c) -> c.source().priority())
                .thenComparing(Candidate::detail);
        for (Map.Entry<Identifier, List<Candidate>> entry : grouped.entrySet()) {
            List<Candidate> candidates = entry.getValue().stream().sorted(order).toList();
            Candidate winner = candidates.getFirst();
            MaterialDefinition definition = winner.definition();
            CatalogSource source = winner.source();
            String detail = winner.detail();
            MaterialOverride override = overrides.get(entry.getKey());
            if (override != null && winner.source() != CatalogSource.DATAPACK) {
                try {
                    definition = definition.withOverrides(override);
                    source = CatalogSource.CONFIG;
                    detail = "config override";
                } catch (IllegalArgumentException e) {
                    errors.add(entry.getKey() + ": invalid config override: " + e.getMessage());
                }
            } else if (override != null) {
                errors.add(entry.getKey() + ": datapack definition takes precedence over config override");
            }
            if (blacklist.contains(entry.getKey())) {
                definition = definition.disabled();
                source = CatalogSource.CONFIG;
                detail = "config blacklist";
            }
            List<String> shadowed = candidates.stream().skip(1)
                    .map(candidate -> candidate.source().name().toLowerCase() + ":" + candidate.detail()).toList();
            if (!shadowed.isEmpty()) {
                errors.add(entry.getKey() + ": selected " + source.name().toLowerCase() + ":" + detail
                        + "; shadowed " + String.join(", ", shadowed));
            }
            all.put(entry.getKey(), new ResolvedMaterial(definition, source, detail, shadowed));
        }

        int cap = Math.max(0, configuredCap);
        Map<Identifier, ResolvedMaterial> exposed = new LinkedHashMap<>();
        int capOmissions = 0;
        for (Map.Entry<Identifier, ResolvedMaterial> entry : all.entrySet()) {
            if (!entry.getValue().definition().enabled()) continue;
            if (exposed.size() >= cap) {
                capOmissions++;
                continue;
            }
            exposed.put(entry.getKey(), entry.getValue());
        }
        if (capOmissions > 0) {
            errors.add(capOmissions + " enabled material(s) omitted because discovery.scan_cap=" + cap);
        }
        return new ResolvedCatalog(all, exposed, errors);
    }
}
