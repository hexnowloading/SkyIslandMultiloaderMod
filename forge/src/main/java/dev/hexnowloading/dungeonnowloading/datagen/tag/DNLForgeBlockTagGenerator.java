package dev.hexnowloading.dungeonnowloading.datagen.tag;

import dev.hexnowloading.dungeonnowloading.DungeonNowLoading;
import dev.hexnowloading.dungeonnowloading.registry.DNLBlocks;
import dev.hexnowloading.dungeonnowloading.registry.DNLItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class DNLForgeBlockTagGenerator extends BlockTagsProvider {

    public DNLForgeBlockTagGenerator(DataGenerator generator, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(generator.getPackOutput(), lookupProvider, DungeonNowLoading.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
                DNLBlocks.COILING_STONE_PILLAR.get(),
                DNLBlocks.CHISELED_COILING_STONE_PILLAR.get(),
                DNLBlocks.COILING_STONE_PILLAR_CAPITAL.get(),
                DNLBlocks.COILING_STONE_PILLAR_STAIRS.get(),
                DNLBlocks.COILING_STONE_PILLAR_SLAB.get(),
                DNLBlocks.COILING_STONE_PILLAR_WALL.get(),
                DNLBlocks.STONE_TILES.get(),
                DNLBlocks.CRACKED_STONE_TILES.get(),
                DNLBlocks.STONE_TILE_STAIRS.get(),
                DNLBlocks.STONE_TILE_SLAB.get(),
                DNLBlocks.STONE_TILE_WALL.get(),
                DNLBlocks.SIGNALING_STONE_EMBLEM.get(),
                DNLBlocks.DUELING_STONE_EMBLEM.get(),
                DNLBlocks.PUZZLING_STONE_EMBLEM.get(),
                DNLBlocks.COBBLESTONE_PEBBLES.get(),
                DNLBlocks.MOSSY_COBBLESTONE_PEBBLES.get(),
                DNLBlocks.IRON_INGOT_PILE.get(),
                DNLBlocks.GOLD_INGOT_PILE.get(),
                DNLBlocks.CHAOS_SPAWNER_EDGE.get(),
                DNLBlocks.CHAOS_SPAWNER_DIAMOND_EDGE.get(),
                DNLBlocks.CHAOS_SPAWNER_DIAMOND_VERTEX.get(),
                DNLBlocks.SPIKES.get(),
                DNLBlocks.FAIRKEEEPER_SPAWNER.get()
        );

        this.tag(BlockTags.MINEABLE_WITH_AXE).add(
                DNLBlocks.WOODEN_WALL_RACK.get(),
                DNLBlocks.WOODEN_WALL_PLATFORM.get(),
                DNLBlocks.FAIRKEEPER_CHEST.get()
        );

        this.tag(BlockTags.WALLS).add(
                DNLBlocks.COILING_STONE_PILLAR_WALL.get(),
                DNLBlocks.STONE_TILE_WALL.get()
        );
    }
}