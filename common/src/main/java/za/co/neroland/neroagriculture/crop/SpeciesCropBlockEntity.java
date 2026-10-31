package za.co.neroland.neroagriculture.crop;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.neroagriculture.registry.ModBlockEntities;

/** Non-ticking storage for a food/alien crop's species identity and genetics-preserving harvest history. */
public final class SpeciesCropBlockEntity extends BlockEntity {
    private Identifier species = Identifier.parse("neroagriculture:unknown");
    private int harvestCount;
    private za.co.neroland.neroagriculture.genetics.Genetics genetics = za.co.neroland.neroagriculture.genetics.Genetics.EMPTY;

    public SpeciesCropBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.SPECIES_CROP.get(), pos, state);
    }

    public SpeciesCropBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public Identifier species() { return species; }
    public int harvestCount() { return harvestCount; }
    public za.co.neroland.neroagriculture.genetics.Genetics genetics() { return genetics; }
    public void setGenetics(za.co.neroland.neroagriculture.genetics.Genetics genetics) {
        this.genetics = genetics == null ? za.co.neroland.neroagriculture.genetics.Genetics.EMPTY : genetics;
        setChanged();
    }

    public void setSpecies(Identifier species, int harvestCount) {
        int bounded = Math.max(0, harvestCount);
        boolean changed = !this.species.equals(species) || this.harvestCount != bounded;
        this.species = species;
        this.harvestCount = bounded;
        setChanged();
        // Species identity is client-visible (tint/model selection), so a real change must be broadcast.
        if (changed && level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void recordHarvest() {
        harvestCount = Math.min(1_000_000_000, harvestCount + 1);
        setChanged();
    }

    //? if >=1.21.6 {
    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
    //?} else {
    /*@Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ValueOutput output = ValueOutput.of(tag, registries);
    *///?}
        output.putString("Species", species.toString());
        output.putInt("HarvestCount", harvestCount);
        za.co.neroland.neroagriculture.genetics.GeneticsCodecs.save(output, genetics);
    }

    //? if >=1.21.6 {
    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
    //?} else {
    /*@Override protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ValueInput input = ValueInput.of(tag, registries);
    *///?}
        harvestCount = Math.max(0, input.getIntOr("HarvestCount", 0));
        try {
            species = Identifier.parse(input.getStringOr("Species", "neroagriculture:unknown"));
        } catch (RuntimeException e) {
            species = Identifier.parse("neroagriculture:unknown");
        }
        genetics = za.co.neroland.neroagriculture.genetics.GeneticsCodecs.load(input);
    }

    // Client sync (chunk load + explicit updates) so client-side rendering sees the real species.
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
}
