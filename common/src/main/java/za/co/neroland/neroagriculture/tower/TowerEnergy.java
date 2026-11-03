package za.co.neroland.neroagriculture.tower;

import za.co.neroland.nerolandcore.energy.EnergyBuffer;

final class TowerEnergy {
    private TowerEnergy() { }

    static boolean has(EnergyBuffer energy, int cost) {
        return cost <= 0 || energy.has(cost);
    }

    static void consume(EnergyBuffer energy, int cost) {
        if (cost > 0) energy.consume(cost);
    }
}
