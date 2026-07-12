package za.co.neroland.neroagriculture.automation;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jetbrains.annotations.Nullable;

import za.co.neroland.neroagriculture.config.AgricultureConfig;
import za.co.neroland.nerolandcore.data.PlayerDataErasure;

/**
 * POPIA/GDPR-safe owner tracking for automation machines. Only a bare UUID is stored (never a name), the
 * feature is opt-out via config, and a Core {@link PlayerDataErasure} eraser clears the owner from any
 * loaded owned machine when a player requests erasure.
 */
public final class AutomationOwner {
    /** Implemented by automation block entities that hold an owner UUID. */
    public interface Owned {
        @Nullable UUID automationOwner();
        void clearAutomationOwner();
    }

    private static final Set<Owned> TRACKED = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private AutomationOwner() { }

    public static void register() {
        PlayerDataErasure.register((server, uuid) -> {
            for (Owned owned : TRACKED) {
                if (uuid.equals(owned.automationOwner())) owned.clearAutomationOwner();
            }
        });
    }

    public static boolean trackingEnabled() {
        return AgricultureConfig.AUTOMATION_TRACK_OWNER.get();
    }

    public static void track(Owned owned) { TRACKED.add(owned); }
    public static void untrack(Owned owned) { TRACKED.remove(owned); }

    public static void save(ValueOutput output, @Nullable UUID owner) {
        if (owner != null && trackingEnabled()) output.putString("Owner", owner.toString());
    }

    @Nullable
    public static UUID load(ValueInput input) {
        String raw = input.getStringOr("Owner", "");
        if (raw.isBlank()) return null;
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
