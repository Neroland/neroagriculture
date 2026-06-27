package za.co.neroland.neroagriculture.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;

/** MinecraftForge entry point for NeroAgriculture. */
@Mod(NeroAgricultureCommon.MOD_ID)
public final class NeroAgricultureForge {

    public NeroAgricultureForge(FMLJavaModLoadingContext context) {
        NeroAgricultureCommon.LOGGER.info("[NeroAgriculture] Forge bootstrap");
        NeroAgricultureCommon.init();
    }
}
