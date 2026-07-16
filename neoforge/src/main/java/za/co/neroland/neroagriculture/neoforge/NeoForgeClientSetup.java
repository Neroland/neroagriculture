package za.co.neroland.neroagriculture.neoforge;

import java.util.List;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import za.co.neroland.neroagriculture.client.CropTintSource;
import za.co.neroland.neroagriculture.client.FoundationMachineScreen;
import za.co.neroland.neroagriculture.registry.ModBlocks;
import za.co.neroland.neroagriculture.registry.ModMenuTypes;

/** NeoForge client-only fabrication screen + colour-handler wiring. */
public final class NeoForgeClientSetup {
    private NeoForgeClientSetup() { }
    public static void init(IEventBus bus) {
        bus.addListener(NeoForgeClientSetup::screens);
        bus.addListener(NeoForgeClientSetup::blockTints);
        bus.addListener(NeoForgeClientSetup::fluidExtensions);
    }

    /** Give nutrient + biofuel their still/flow sprites (26.x fluid models, per-fluid registration). */
    private static void fluidExtensions(net.neoforged.neoforge.client.event.RegisterFluidModelsEvent event) {
        event.register(fluidModel("nutrient"),
                za.co.neroland.neroagriculture.fluid.ModFluids.NUTRIENT,
                za.co.neroland.neroagriculture.fluid.ModFluids.FLOWING_NUTRIENT);
        event.register(fluidModel("biofuel"),
                za.co.neroland.neroagriculture.fluid.ModFluids.BIOFUEL,
                za.co.neroland.neroagriculture.fluid.ModFluids.FLOWING_BIOFUEL);
    }

    private static net.minecraft.client.renderer.block.FluidModel.Unbaked fluidModel(String name) {
        return new net.minecraft.client.renderer.block.FluidModel.Unbaked(
                new net.minecraft.client.resources.model.sprite.Material(
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("neroagriculture", "block/" + name + "_still")),
                new net.minecraft.client.resources.model.sprite.Material(
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("neroagriculture", "block/" + name + "_flow")),
                null,
                (net.neoforged.neoforge.client.fluid.FluidTintSource) null);
    }
    private static void blockTints(RegisterColorHandlersEvent.BlockTintSources event) {
        event.register(List.of(new CropTintSource()), ModBlocks.RESOURCE_CROP.get());
    }
    private static void screens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.FOUNDATION_MACHINE.get(), FoundationMachineScreen::new);
        event.register(ModMenuTypes.GENETICS_STATION.get(),
                za.co.neroland.neroagriculture.client.GeneticsStationScreen::new);
        event.register(ModMenuTypes.CROP_TOWER_CONTROLLER.get(),
                za.co.neroland.neroagriculture.client.CropTowerScreen::new);
        event.register(ModMenuTypes.STATUS_CONTROLLER.get(),
                za.co.neroland.neroagriculture.client.StatusScreen::new);
        event.register(ModMenuTypes.AREA_MACHINE.get(),
                za.co.neroland.neroagriculture.client.AreaMachineScreen::new);
        event.register(ModMenuTypes.CONVERTER.get(),
                za.co.neroland.neroagriculture.client.ProcessorScreen::new);
        event.register(ModMenuTypes.PROCESSOR.get(),
                za.co.neroland.neroagriculture.client.ProcessorScreen::new);
        event.register(ModMenuTypes.GROW_BED.get(),
                za.co.neroland.neroagriculture.client.GrowBedScreen::new);
    }
}
