package za.co.neroland.neroagriculture.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.event.OnDatapackSyncEvent;
import net.minecraftforge.event.RegisterCommandsEvent;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.catalog.CatalogSync;
import za.co.neroland.neroagriculture.command.AgricultureCommands;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;

/** MinecraftForge entry point for NeroAgriculture. */
@Mod(NeroAgricultureCommon.MOD_ID)
public final class NeroAgricultureForge {

    public NeroAgricultureForge(FMLJavaModLoadingContext context) {
        NeroAgricultureCommon.LOGGER.info("[NeroAgriculture] Forge bootstrap");
        NeroAgricultureCommon.init();
        za.co.neroland.neroagriculture.platform.ForgeFluidFactory.registerFluidTypes(context.getModBusGroup());
        RegistrationProvider.attach(context.getModBusGroup());
        ForgeNetwork.register();
        ForgeCapabilities.register();
        if (FMLEnvironment.dist == Dist.CLIENT) ForgeClientSetup.init(context.getModBusGroup());
        RegisterCommandsEvent.BUS.addListener(event -> AgricultureCommands.register(event.getDispatcher()));
        OnDatapackSyncEvent.BUS.addListener(event -> {
            if (event.getPlayer() == null) CatalogSync.reloadAndSync(event.getPlayerList().getServer());
            else CatalogSync.syncTo(event.getPlayer());
        });
    }
}
