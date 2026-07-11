package za.co.neroland.neroagriculture.registry;

import java.util.stream.Collectors;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

import za.co.neroland.neroagriculture.NeroAgricultureCommon;
import za.co.neroland.neroagriculture.machine.FoundationMachineBlockEntity;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.nerolandcore.registry.RegistrationProvider.RegistryEntry;

/** One compatible shell type spans the stable machine blocks until later stages specialize behavior. */
public final class ModBlockEntities {
    public static final RegistrationProvider<BlockEntityType<?>> BLOCK_ENTITIES =
            RegistrationProvider.get(Registries.BLOCK_ENTITY_TYPE, NeroAgricultureCommon.MOD_ID);

    public static final RegistryEntry<BlockEntityType<FoundationMachineBlockEntity>> FOUNDATION_MACHINE =
            BLOCK_ENTITIES.register("foundation_machine", key -> new BlockEntityType<>(FoundationMachineBlockEntity::new,
                    ModBlocks.ALL.stream().filter(entry -> entry.get() instanceof za.co.neroland.neroagriculture.machine.FoundationMachineBlock)
                            .map(RegistryEntry::get).collect(Collectors.toSet())));

    private ModBlockEntities() { }
    public static void init() { }
}
