package za.co.neroland.neroagriculture.crop;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import za.co.neroland.neroagriculture.catalog.MaterialCatalog;
import za.co.neroland.neroagriculture.catalog.ResolvedCatalog;
import za.co.neroland.neroagriculture.genetics.GeneticsCodecs;
import za.co.neroland.neroagriculture.registry.ModBlockEntities;

/** Non-ticking storage for material identity/history. Unknown ids are preserved and growth fails closed. */
public final class ResourceCropBlockEntity extends BlockEntity {
    private CropVariantState variant = CropVariantState.fresh(Identifier.parse("neroagriculture:unknown"));
    private za.co.neroland.neroagriculture.genetics.Genetics genetics = za.co.neroland.neroagriculture.genetics.Genetics.EMPTY;

    public ResourceCropBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESOURCE_CROP.get(), pos, state);
    }

    public CropVariantState variant() { return variant; }
    public void setVariant(CropVariantState variant) {
        boolean changed = !this.variant.equals(variant);
        this.variant = variant;
        setChanged();
        // The client tints the crop from this BE's material, so a real change must be broadcast.
        if (changed && level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    public za.co.neroland.neroagriculture.genetics.Genetics genetics() { return genetics; }
    public void setGenetics(za.co.neroland.neroagriculture.genetics.Genetics genetics) {
        this.genetics = genetics == null ? za.co.neroland.neroagriculture.genetics.Genetics.EMPTY : genetics;
        setChanged();
    }

    /**
     * Server side this is the authoritative catalog. Client side (dedicated-server clients included,
     * where {@code level.getServer()} is always {@code null}) the only truthful source is the synced
     * display catalog — {@code MaterialCatalog.current()} would be builtins-only there, so datapack
     * materials would wrongly resolve UNKNOWN. The client lookup carries status only (no definition):
     * the server never syncs rules, and client callers only ever ask {@link #permitsGrowth()}.
     */
    public ResolvedCatalog.Lookup catalogState() {
        if (level != null && level.getServer() != null) {
            return MaterialCatalog.forServer(level.getServer()).lookup(variant.material());
        }
        return new ResolvedCatalog.Lookup(
                za.co.neroland.neroagriculture.catalog.ClientMaterialCatalog.entries().containsKey(variant.material())
                        ? ResolvedCatalog.Status.ACTIVE : ResolvedCatalog.Status.UNKNOWN,
                java.util.Optional.empty());
    }

    public boolean permitsGrowth() { return catalogState().permitsGrowth(); }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Format", variant.formatVersion());
        output.putString("Material", variant.material().toString());
        output.putString("Family", variant.family().name());
        output.putInt("HarvestCount", variant.harvestCount());
        GeneticsCodecs.save(output, genetics);
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        int format = input.getIntOr("Format", CropVariantState.CURRENT_FORMAT);
        int harvests = input.getIntOr("HarvestCount", 0);
        try {
            variant = new CropVariantState(format,
                    Identifier.parse(input.getStringOr("Material", "neroagriculture:unknown")),
                    za.co.neroland.neroagriculture.content.FragmentTier.valueOf(
                            input.getStringOr("Family", "ORBITE")), harvests);
        } catch (RuntimeException e) {
            // Preserve the block and fail closed; malformed legacy data becomes an explicit unknown id.
            variant = CropVariantState.fresh(Identifier.parse("neroagriculture:unknown"));
        }
        genetics = GeneticsCodecs.load(input);
    }

    // Client sync (chunk load + explicit updates) so in-world tints see the real material, not the default.
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveCustomOnly(registries); }
}
