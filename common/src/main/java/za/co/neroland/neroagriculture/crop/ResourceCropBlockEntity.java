package za.co.neroland.neroagriculture.crop;

import net.minecraft.core.BlockPos;
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
    public void setVariant(CropVariantState variant) { this.variant = variant; setChanged(); }
    public za.co.neroland.neroagriculture.genetics.Genetics genetics() { return genetics; }
    public void setGenetics(za.co.neroland.neroagriculture.genetics.Genetics genetics) {
        this.genetics = genetics == null ? za.co.neroland.neroagriculture.genetics.Genetics.EMPTY : genetics;
        setChanged();
    }

    public ResolvedCatalog.Lookup catalogState() {
        if (level == null || level.getServer() == null) return MaterialCatalog.current().lookup(variant.material());
        return MaterialCatalog.forServer(level.getServer()).lookup(variant.material());
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
                    za.co.neroland.neroagriculture.content.EssenceFamily.valueOf(
                            input.getStringOr("Family", "ORBITAL")), harvests);
        } catch (RuntimeException e) {
            // Preserve the block and fail closed; malformed legacy data becomes an explicit unknown id.
            variant = CropVariantState.fresh(Identifier.parse("neroagriculture:unknown"));
        }
        genetics = GeneticsCodecs.load(input);
    }
}
