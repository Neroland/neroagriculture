package za.co.neroland.neroagriculture.fertiliser;

/**
 * A timed fertiliser dose held on a grow bed. {@code amount} is the accumulated potency (clamped to a
 * configured cap) and {@code expiryTick} is the game time at which it lapses. Doses of different types are
 * tracked independently so speed and yield can never combine past their individual caps.
 */
public record FertiliserDose(FertiliserType type, int amount, long expiryTick) {

    /** Add {@code add} potency (capped) and refresh the expiry; pass the current dose or null for none. */
    public static FertiliserDose applied(FertiliserDose current, FertiliserType type, int add, long now,
            int durationTicks, int maxDose) {
        int base = current != null && current.type == type && current.active(now) ? current.amount : 0;
        int amount = Math.max(0, Math.min(maxDose, base + Math.max(0, add)));
        return new FertiliserDose(type, amount, now + Math.max(1, durationTicks));
    }

    public boolean active(long now) {
        return amount > 0 && now < expiryTick;
    }
}
