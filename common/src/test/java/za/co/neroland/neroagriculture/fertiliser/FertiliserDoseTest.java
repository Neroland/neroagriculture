package za.co.neroland.neroagriculture.fertiliser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FertiliserDoseTest {
    private static final int MAX = 8;
    private static final int DURATION = 100;

    @Test
    void firstApplicationSetsAmountAndExpiry() {
        FertiliserDose dose = FertiliserDose.applied(null, FertiliserType.SPEED, 3, 0, DURATION, MAX);
        assertEquals(3, dose.amount());
        assertEquals(DURATION, dose.expiryTick());
        assertTrue(dose.active(0));
        assertFalse(dose.active(DURATION), "expired at the expiry tick");
    }

    @Test
    void repeatedApplicationAccumulatesButNeverExceedsTheCap() {
        FertiliserDose dose = FertiliserDose.applied(null, FertiliserType.YIELD, 5, 0, DURATION, MAX);
        dose = FertiliserDose.applied(dose, FertiliserType.YIELD, 5, 10, DURATION, MAX);
        assertEquals(MAX, dose.amount(), "amount must be clamped to the cap");
        assertEquals(10 + DURATION, dose.expiryTick(), "expiry refreshes on re-application");
    }

    @Test
    void switchingTypeStartsFromZeroSoEffectsDoNotCombinePastTheirCaps() {
        FertiliserDose speed = FertiliserDose.applied(null, FertiliserType.SPEED, 8, 0, DURATION, MAX);
        FertiliserDose asYield = FertiliserDose.applied(speed, FertiliserType.YIELD, 2, 5, DURATION, MAX);
        assertEquals(2, asYield.amount(), "a different type does not inherit the previous amount");
        assertEquals(FertiliserType.YIELD, asYield.type());
    }

    @Test
    void expiredDoseIsTreatedAsZeroBase() {
        FertiliserDose expired = FertiliserDose.applied(null, FertiliserType.SPEED, 6, 0, DURATION, MAX);
        FertiliserDose renewed = FertiliserDose.applied(expired, FertiliserType.SPEED, 3, DURATION + 1, DURATION, MAX);
        assertEquals(3, renewed.amount(), "an expired dose does not stack onto the new one");
    }
}
