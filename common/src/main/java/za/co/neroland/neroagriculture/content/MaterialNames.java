package za.co.neroland.neroagriculture.content;

import java.util.function.Predicate;

import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;

/**
 * The single resolve-or-fallback rule for catalogued material and species display names.
 *
 * <p>A catalogue entry carries a {@code displayKey} such as {@code material.c.iron}, but most of those
 * keys can <em>never</em> be shipped in a lang file: tag-discovered materials
 * ({@link za.co.neroland.neroagriculture.catalog.MaterialCatalog}) and Core meteor materials
 * ({@link za.co.neroland.neroagriculture.catalog.MeteorMaterialAdapter}) mint their key at runtime from
 * whatever ids the modpack happens to expose. So the fallback is the normal case, not an error path:
 * translate the key when the active language actually has it, otherwise title-case the id's leaf path
 * exactly the way the item names and tooltips already do.</p>
 *
 * <p>Screens, tooltips and item names all route through here so they can never disagree.</p>
 */
public final class MaterialNames {
    private MaterialNames() { }

    /**
     * Title-case an id's leaf path: {@code c:iron -> "Iron"}, {@code c:nether_star -> "Nether Star"},
     * {@code neroagriculture:food/earth_algae -> "Earth Algae"}.
     */
    public static String leafName(Identifier id) {
        String path = id.getPath();
        int slash = path.lastIndexOf('/');
        if (slash >= 0) path = path.substring(slash + 1);
        StringBuilder sb = new StringBuilder();
        for (String word : path.split("_")) {
            if (word.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return sb.toString();
    }

    /**
     * {@code displayKey} as a translatable component when a translation for it exists, otherwise the
     * title-cased leaf path of {@code id} as literal text. Never returns a raw translation key.
     */
    public static Component display(Identifier id, @Nullable String displayKey) {
        return display(id, displayKey, key -> Language.getInstance().has(key));
    }

    /**
     * Test seam for {@link #display(Identifier, String)}: {@code hasTranslation} decides whether the
     * catalogue's key resolves, so the rule can be exercised without a loaded language.
     */
    public static Component display(Identifier id, @Nullable String displayKey, Predicate<String> hasTranslation) {
        if (displayKey != null && !displayKey.isBlank() && hasTranslation.test(displayKey)) {
            return Component.translatable(displayKey);
        }
        return Component.literal(leafName(id));
    }
}
