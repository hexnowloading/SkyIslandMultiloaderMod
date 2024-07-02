package dev.hexnowloading.dungeonnowloading.block.entity;

import dev.hexnowloading.dungeonnowloading.block.FairkeeperChestBlock;
import dev.hexnowloading.dungeonnowloading.block.property.ChestStates;
import dev.hexnowloading.dungeonnowloading.platform.Services;
import dev.hexnowloading.dungeonnowloading.registry.DNLBlockEntityTypes;
import dev.hexnowloading.dungeonnowloading.registry.DNLBlocks;
import dev.hexnowloading.dungeonnowloading.registry.DNLProperties;
import dev.hexnowloading.dungeonnowloading.util.DNLMath;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FairkeeperChestBlockEntity extends RandomizableContainerBlockEntity implements MenuProvider {
    public static final String LOOT_TABLE_TAG = "LootTable";
    public static final String LOOT_TABLE_SEED_TAG = "LootTableSeed";
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);
    protected ResourceLocation lootTable;
    protected long lootTableSeed;
    private ResourceLocation combatLootTable;
    private long combatLootTableSeed;
    private List<BlockPos> spawnerLocationList;
    private BlockPos oldBlockPos; // This block position will be different from the actual fairkeeper chest block pos if it was generated through structure.
    private BlockPos lastSpawner;
    private int startUpTick;
    private int actualRegion1X;
    private int actualRegion1Y;
    private int actualRegion1Z;
    private int actualRegion2X;
    private int actualRegion2Y;
    private int actualRegion2Z;
    private BlockPos maxRegion;
    private BlockPos minRegion;
    private int playerCount;
    private boolean disabled;
    private static final double PLAYER_RANGE = 32.0D;
    private static final int START_UP_TICK = 60;
    private static final int OPEN_CLOSE_ANIMATION_DURATION = 10;
    private int openCloseAnimationProgress = 0;
    private int prevOpenCloseAnimationProgress = 0;

    public FairkeeperChestBlockEntity(BlockPos pos, BlockState state) {
        super(DNLBlockEntityTypes.FAIRKEEPER_CHEST.get(), pos, state);
        this.disabled = false;
    }

    // Saves the nbt when player leaves the world.
    @Override
    protected void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
        if (!this.trySaveLootTable(nbt)) {
            ContainerHelper.saveAllItems(nbt, this.items);
        } else {
            if (this.combatLootTable != null) {
                nbt.putString("CombatLootTable", this.combatLootTable.toString());
                if (this.combatLootTableSeed != 0L) {
                    nbt.putLong("CombatLootTableSeed", this.combatLootTableSeed);
                }
            }
        }
        if (this.spawnerLocationList != null) {
            ListTag listTag = new ListTag();
            this.spawnerLocationList.forEach(blockPos -> listTag.add(this.newIntList(blockPos.getX(), blockPos.getY(), blockPos.getZ())));
            nbt.put("SpawnerLocations", listTag);
        }
        if (lastSpawner != null) {
            nbt.put("LastSpawner", this.newIntList(this.lastSpawner.getX(), this.lastSpawner.getY(), this.lastSpawner.getZ()));
        }
        nbt.put("OldBlockPos", this.newIntList(this.getBlockPos().getX(), this.getBlockPos().getY(), this.getBlockPos().getZ()));
        nbt.putInt("StartUpTick", this.startUpTick);
        nbt.putInt("PlayerCount", this.playerCount);
        nbt.putBoolean("Disabled", this.disabled);
        if (maxRegion != null) {
            nbt.put("MaxRegion", this.newIntList(this.maxRegion.getX(), this.maxRegion.getY(), this.maxRegion.getZ()));
        }
        if (minRegion != null) {
            nbt.put("MinRegion", this.newIntList(this.minRegion.getX(), this.minRegion.getY(), this.minRegion.getZ()));
        }
    }

    private ListTag newIntList(int ... ints) {
        ListTag listTag = new ListTag();
        for (int i : ints) {
            listTag.add(IntTag.valueOf(i));
        }
        return listTag;
    }

    // Loads the nbt when player joins the world.
    @Override
    public void load(CompoundTag nbt) {
        super.load(nbt);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        if (!this.tryLoadLootTable(nbt)) {
            ContainerHelper.loadAllItems(nbt,this.items);
        } else {
            if (nbt.contains("CombatLootTable", CompoundTag.OBJECT_HEADER)) {
                this.combatLootTable = new ResourceLocation(nbt.getString("CombatLootTable"));
                this.combatLootTableSeed = nbt.getLong("CombatLootTableSeed");
            }
        }
        if (nbt.contains("SpawnerLocations", CompoundTag.TAG_LIST)) {
            ListTag listTag = nbt.getList("SpawnerLocations", CompoundTag.TAG_LIST);
            if (this.spawnerLocationList == null) {
                this.spawnerLocationList = new ArrayList<>();
            }
            for (int a = 0; a < listTag.size(); ++a) {
                this.spawnerLocationList.add(new BlockPos(listTag.getInt(0), listTag.getInt(1), listTag.getInt(2)));
            }
        }
        if (nbt.contains("LastSpawner", CompoundTag.TAG_LIST)) {
            this.lastSpawner = new BlockPos(nbt.getList("LastSpawner", CompoundTag.TAG_LIST).getInt(0), nbt.getList("LastSpawner", CompoundTag.TAG_LIST).getInt(1), nbt.getList("LastSpawner", CompoundTag.TAG_LIST).getInt(2));
        }
        this.startUpTick = nbt.getInt("StartUpTick");
        this.playerCount = nbt.getInt("PlayerCount");
        this.disabled = nbt.getBoolean("Disabled");
        this.oldBlockPos = new BlockPos(nbt.getList("OldBlockPos", CompoundTag.TAG_LIST).getInt(0), nbt.getList("OldBlockPos", CompoundTag.TAG_LIST).getInt(1), nbt.getList("OldBlockPos", CompoundTag.TAG_LIST).getInt(2));
        if (nbt.contains("MaxRegion", CompoundTag.TAG_LIST)) {
            this.maxRegion = new BlockPos(nbt.getList("MaxRegion", CompoundTag.TAG_LIST).getInt(0), nbt.getList("MaxRegion", CompoundTag.TAG_LIST).getInt(1), nbt.getList("MaxRegion", CompoundTag.TAG_LIST).getInt(2));
        }
        if (nbt.contains("MinRegion", CompoundTag.TAG_LIST)) {
            this.minRegion = new BlockPos(nbt.getList("MinRegion", CompoundTag.TAG_LIST).getInt(0), nbt.getList("MinRegion", CompoundTag.TAG_LIST).getInt(1), nbt.getList("MinRegion", CompoundTag.TAG_LIST).getInt(2));
        }
    }

    public boolean hasSpawnerLocations() {
        if (this.spawnerLocationList == null) {
            return false;
        }
        return !this.spawnerLocationList.isEmpty();
    }

    public CompoundTag setCombatLootTable(FairkeeperChestBlockEntity blockEntity) {
        CompoundTag nbt = blockEntity.saveWithFullMetadata().copy();
        if (nbt.contains("CombatLootTable", CompoundTag.OBJECT_HEADER)) {
            nbt.putString("LootTable", blockEntity.combatLootTable.toString());
            nbt.putLong("LootTableSeed", blockEntity.combatLootTableSeed);
            nbt.remove("CombatLootTable");
            nbt.remove("CombatLootTableSeed");
        }
        return nbt;
    }

    public AABB getBoundaryAABB() {
        return new AABB(this.actualRegion1X, this.actualRegion1Y, this.actualRegion1Z, this.actualRegion2X, this.actualRegion2Y, this.actualRegion2Z);
    }

    public boolean hasLastSpawner(Level level, FairkeeperChestBlockEntity blockEntity) {
        if(blockEntity.lastSpawner != null) {
            return level.getBlockState(blockEntity.lastSpawner).is(DNLBlocks.FAIRKEEEPER_SPAWNER.get());
        }
        return false;
    }

    private void removeLootTable() {
        if (this.lootTable != null) {
            this.lootTable = null;
        }
    }

    public void setDisabled(boolean b) {
        this.disabled = b;
    }

    public boolean getDisabled() {
        return this.disabled;
    }
    /*protected boolean trySaveLootTable(CompoundTag nbt) {
        if (this.lootTable == null) {
            return false;
        } else {
            nbt.putString("LootTable", this.lootTable.toString());
            if (this.lootTableSeed != 0L) {
                nbt.putLong("LootTableSeed", this.lootTableSeed);
            }
            return true;
        }
    }

    protected boolean tryLoadLootTable(CompoundTag nbt) {
        if (nbt.contains("LootTable", 8)) {
            this.lootTable = new ResourceLocation(nbt.getString("LootTable"));
            this.lootTableSeed = nbt.getLong("LootTableSeed");
            return true;
        } else {
            return false;
        }
    }*/

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.dungeonnowloading.fairkeeper_chest");
    }

    @Override
    protected AbstractContainerMenu createMenu(int i, Inventory inventory) {
        return ChestMenu.threeRows(i, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return 27;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> itemStacks) {
        this.items = itemStacks;
    }

    @Override
    public void startOpen(Player player) {
        if (!this.remove && !player.isSpectator()) {
            FairkeeperChestBlock.setFairkeeperChest(this.getLevel(), this.getBlockPos(), ChestStates.OPENING);
            FairkeeperChestBlockEntity.playSound(this.getLevel(), this.getBlockPos(), SoundEvents.CHEST_OPEN);
            FairkeeperChestBlockEntity.destroySpawners(this.getLevel(), this);
        }
    }

    @Override
    public void stopOpen(Player player) {
        if (!this.remove && !player.isSpectator()) {
            FairkeeperChestBlock.setFairkeeperChest(this.getLevel(), this.getBlockPos(), ChestStates.CLOSING);
            FairkeeperChestBlockEntity.playSound(this.getLevel(), this.getBlockPos(), SoundEvents.CHEST_CLOSE);
        }
    }

    public static void playSound(Level level, BlockPos blockPos, SoundEvent soundEvent) {
        level.playSound((Player) null, blockPos, soundEvent, SoundSource.BLOCKS, 0.5F, level.random.nextFloat() * 0.1f + 0.9f);
    }

    // Fairkeeper Chest Alert Mechanic

    public static void setMaxMinRegion(Level level, BlockPos blockPos, FairkeeperChestBlockEntity blockEntity) {
        int xp = 0, xn = 0, yp = 0, yn = 0, zp = 0, zn = 0;
        for (int i = 0; i < PLAYER_RANGE; i++) {
            if (level.getBlockState(blockPos.offset(0, i, 0)).is(Blocks.REDSTONE_BLOCK) && yp == 0) {
                yp = i;
            }
            if (level.getBlockState(blockPos.offset(0, 0, i)).is(Blocks.REDSTONE_BLOCK) && zp == 0) {
                zp = i;
            }
            if (level.getBlockState(blockPos.offset(i, 0, 0)).is(Blocks.REDSTONE_BLOCK) && xp == 0) {
                xp = i;
            }
            if (level.getBlockState(blockPos.offset(0, -i, 0)).is(Blocks.REDSTONE_BLOCK) && yn == 0) {
                yn = -i;
            }
            if (level.getBlockState(blockPos.offset(0, 0, -i)).is(Blocks.REDSTONE_BLOCK) && zn == 0) {
                zn = -i;
            }
            if (level.getBlockState(blockPos.offset(-i, 0, 0)).is(Blocks.REDSTONE_BLOCK) && xn == 0) {
                xn = -i;
            }
        }
        blockEntity.maxRegion = new BlockPos(xp, yp, zp);
        blockEntity.minRegion = new BlockPos(xn, yn, zn);

        int tempActualRegion1X = blockPos.getX() + blockEntity.maxRegion.getX();
        int tempActualRegion1Y = blockPos.getY() + blockEntity.maxRegion.getY();
        int tempActualRegion1Z = blockPos.getZ() + blockEntity.maxRegion.getZ();
        int tempActualRegion2X = blockPos.getX() + blockEntity.minRegion.getX();
        int tempActualRegion2Y = blockPos.getY() + blockEntity.minRegion.getY();
        int tempActualRegion2Z = blockPos.getZ() + blockEntity.minRegion.getZ();

        blockEntity.actualRegion1X = Math.max(tempActualRegion1X, tempActualRegion2X) + 1;
        blockEntity.actualRegion1Y = Math.max(tempActualRegion1Y, tempActualRegion2Y) + 1;
        blockEntity.actualRegion1Z = Math.max(tempActualRegion1Z, tempActualRegion2Z) + 1;
        blockEntity.actualRegion2X = Math.min(tempActualRegion1X, tempActualRegion2X);
        blockEntity.actualRegion2Y = Math.min(tempActualRegion1Y, tempActualRegion2Y);
        blockEntity.actualRegion2Z = Math.min(tempActualRegion1Z, tempActualRegion2Z);
    }

    private static void rotationalSetRegion(Level level, BlockPos pos, BlockState state, FairkeeperChestBlockEntity blockEntity) {
        BlockPos actualMax = new BlockPos(
                Math.max(blockEntity.maxRegion.getX(), blockEntity.minRegion.getX()) + 1,
                Math.max(blockEntity.maxRegion.getY(), blockEntity.minRegion.getY()) + 1,
                Math.max(blockEntity.maxRegion.getZ(), blockEntity.minRegion.getZ()) + 1
        );
        BlockPos actualMin = new BlockPos(
                Math.min(blockEntity.maxRegion.getX(), blockEntity.minRegion.getX()),
                Math.min(blockEntity.maxRegion.getY(), blockEntity.minRegion.getY()),
                Math.min(blockEntity.maxRegion.getZ(), blockEntity.minRegion.getZ())
        );

        BlockPos tempRegion1;
        BlockPos tempRegion2;
        switch (state.getValue(FairkeeperChestBlock.FACING)) {
            case NORTH:
            default:
                //tempRegion1 = blockEntity.region1.rotate(Rotation.COUNTERCLOCKWISE_90);
                //tempRegion2 = blockEntity.region2.rotate(Rotation.COUNTERCLOCKWISE_90);
                tempRegion1 = DNLMath.rotateVector(actualMax, Direction.Axis.Y, Math.toRadians(90)).south();
                tempRegion2 = DNLMath.rotateVector(actualMin, Direction.Axis.Y, Math.toRadians(90)).south();
                break;
            case EAST:
                tempRegion1 = actualMax;
                tempRegion2 = actualMin;
                break;
            case SOUTH:
                //tempRegion1 = blockEntity.region1.rotate(Rotation.CLOCKWISE_90);
                //tempRegion2 = blockEntity.region2.rotate(Rotation.CLOCKWISE_90);
                tempRegion1 = DNLMath.rotateVector(actualMax, Direction.Axis.Y, Math.toRadians(-90)).east();
                tempRegion2 = DNLMath.rotateVector(actualMin, Direction.Axis.Y, Math.toRadians(-90)).east();
                break;
            case WEST:
                //tempRegion1 = blockEntity.region1.rotate(Rotation.CLOCKWISE_180);
                //tempRegion2 = blockEntity.region2.rotate(Rotation.CLOCKWISE_180);
                tempRegion1 = DNLMath.rotateVector(actualMax, Direction.Axis.Y, Math.toRadians(180)).south().east();
                tempRegion2 = DNLMath.rotateVector(actualMin, Direction.Axis.Y, Math.toRadians(180)).south().east();
                break;
        }

        int tempActualRegion1X = pos.getX() + tempRegion1.getX();
        int tempActualRegion1Y = pos.getY() + tempRegion1.getY();
        int tempActualRegion1Z = pos.getZ() + tempRegion1.getZ();
        int tempActualRegion2X = pos.getX() + tempRegion2.getX();
        int tempActualRegion2Y = pos.getY() + tempRegion2.getY();
        int tempActualRegion2Z = pos.getZ() + tempRegion2.getZ();

        blockEntity.actualRegion1X = Math.max(tempActualRegion1X, tempActualRegion2X);
        blockEntity.actualRegion1Y = Math.max(tempActualRegion1Y, tempActualRegion2Y);
        blockEntity.actualRegion1Z = Math.max(tempActualRegion1Z, tempActualRegion2Z);
        blockEntity.actualRegion2X = Math.min(tempActualRegion1X, tempActualRegion2X);
        blockEntity.actualRegion2Y = Math.min(tempActualRegion1Y, tempActualRegion2Y);
        blockEntity.actualRegion2Z = Math.min(tempActualRegion1Z, tempActualRegion2Z);
    }

    private static void updateActualRegion(Level level, BlockPos pos, BlockState state, FairkeeperChestBlockEntity blockEntity) {
        if (pos != blockEntity.oldBlockPos) {
            setMaxMinRegion(level, pos, blockEntity);
            blockEntity.oldBlockPos = pos;
        }
    }

    private static void triggerSpawners(Level level, BlockPos fairkeeperChestPos, FairkeeperChestBlockEntity blockEntity) {

        //if (blockEntity.actualRegion1X > brokenBlockPos.getX() && blockEntity.actualRegion1Y > brokenBlockPos.getY() && blockEntity.actualRegion1Z > brokenBlockPos.getZ() && blockEntity.actualRegion2X <= brokenBlockPos.getX() && blockEntity.actualRegion2Y <= brokenBlockPos.getY() && blockEntity.actualRegion2Z <= brokenBlockPos.getZ()) {

        blockEntity.startUpTick = START_UP_TICK;
        CompoundTag compoundTag = blockEntity.setCombatLootTable(blockEntity);
        FairkeeperChestBlock.setFairkeeperAlert(level, fairkeeperChestPos, Boolean.TRUE);
        BlockEntity newBlockEntity = level.getBlockEntity(fairkeeperChestPos);
        newBlockEntity.load(compoundTag);
        AABB aabb = new AABB(blockEntity.actualRegion1X, blockEntity.actualRegion1Y, blockEntity.actualRegion1Z, blockEntity.actualRegion2X, blockEntity.actualRegion2Y, blockEntity.actualRegion2Z);
        List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, aabb);
        blockEntity.playerCount = nearbyPlayers.size();

        Map<BlockPos, BlockEntity> map = new HashMap<>();
        int chunkMinX = SectionPos.blockToSectionCoord(blockEntity.actualRegion2X);
        int chunkMinZ = SectionPos.blockToSectionCoord(blockEntity.actualRegion2Z);
        int chunkMaxX = SectionPos.blockToSectionCoord(blockEntity.actualRegion1X);
        int chunkMaxZ = SectionPos.blockToSectionCoord(blockEntity.actualRegion1Z);
        for (int x = 0; chunkMinX + x <= chunkMaxX; x++) {
            for (int z = 0; chunkMinZ + z <= chunkMaxZ; z++) {
                map.putAll(level.getChunk(chunkMinX + x, chunkMinZ + z).getBlockEntities());
            }
        }

        Map<BlockPos, BlockEntity> filtered = map.entrySet()
                .stream()
                .filter(e -> e.getValue() instanceof FairkeeperSpawnerBlockEntity)
                .filter(e -> e.getKey().getX() < blockEntity.actualRegion1X && e.getKey().getX() >= blockEntity.actualRegion2X && e.getKey().getY() < blockEntity.actualRegion1Y && e.getKey().getY() >= blockEntity.actualRegion2Y && e.getKey().getZ() < blockEntity.actualRegion1Z && e.getKey().getZ() >= blockEntity.actualRegion2Z)
                .filter(e -> !e.getValue().getBlockState().getValue(DNLProperties.FAIRKEEPER_ALERT))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        //filtered.forEach(((blockPosEntry, blockEntityEntry) -> ((FairkeeperSpawnerBlockEntity) blockEntityEntry).alert(nearbyPlayers.size() == 0 ? 1 : nearbyPlayers.size(), blockPosEntry, (FairkeeperSpawnerBlockEntity) blockEntityEntry)));
        blockEntity.spawnerLocationList = new ArrayList<>(filtered.keySet());
    }

    private static void destroySpawners(Level level, FairkeeperChestBlockEntity blockEntity) {
        Map<BlockPos, BlockEntity> map = new HashMap<>();
        int chunkMinX = SectionPos.blockToSectionCoord(blockEntity.actualRegion2X);
        int chunkMinZ = SectionPos.blockToSectionCoord(blockEntity.actualRegion2Z);
        int chunkMaxX = SectionPos.blockToSectionCoord(blockEntity.actualRegion1X);
        int chunkMaxZ = SectionPos.blockToSectionCoord(blockEntity.actualRegion1Z);
        for (int x = 0; chunkMinX + x <= chunkMaxX; x++) {
            for (int z = 0; chunkMinZ + z <= chunkMaxZ; z++) {
                map.putAll(level.getChunk(chunkMinX + x, chunkMinZ + z).getBlockEntities());
            }
        }

        Map<BlockPos, BlockEntity> filtered = map.entrySet()
                .stream()
                .filter(e -> e.getValue() instanceof FairkeeperSpawnerBlockEntity)
                .filter(e -> e.getKey().getX() < blockEntity.actualRegion1X && e.getKey().getX() >= blockEntity.actualRegion2X && e.getKey().getY() < blockEntity.actualRegion1Y && e.getKey().getY() >= blockEntity.actualRegion2Y && e.getKey().getZ() < blockEntity.actualRegion1Z && e.getKey().getZ() >= blockEntity.actualRegion2Z)
                .filter(e -> !e.getValue().getBlockState().getValue(DNLProperties.FAIRKEEPER_ALERT))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        filtered.forEach(((blockPosEntry, blockEntityEntry) -> ((FairkeeperSpawnerBlockEntity) blockEntityEntry).setDisabled(true)));
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, FairkeeperChestBlockEntity blockEntity) {
        blockEntity.prevOpenCloseAnimationProgress = blockEntity.openCloseAnimationProgress;
        if (state.getValue(DNLProperties.CHEST_STATES) == ChestStates.OPENING) {
            if (blockEntity.openCloseAnimationProgress == OPEN_CLOSE_ANIMATION_DURATION) {
                FairkeeperChestBlock.setFairkeeperChest(level, pos, ChestStates.OPENED);
            } else {
                blockEntity.openCloseAnimationProgress++;
            }
        } else if (state.getValue(DNLProperties.CHEST_STATES) == ChestStates.CLOSING) {
            if (blockEntity.openCloseAnimationProgress == 0) {
                FairkeeperChestBlock.setFairkeeperChest(level, pos, ChestStates.CLOSED);
            } else {
                blockEntity.openCloseAnimationProgress--;
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FairkeeperChestBlockEntity blockEntity) {
        if (!blockEntity.disabled) {
            if (blockEntity.startUpTick > 0) {
                blockEntity.startUpTick--;
            } else {
                blockEntity.startUpTick = 20;
                if (level.hasNearbyAlivePlayer(pos.getX(), pos.getY(), pos.getZ(), 32.0D)) {
                    updateActualRegion(level, pos, state, blockEntity);
                    AABB aabb = new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ()).inflate(32.0D);
                    List<Player> nearbyPlayers = level.getEntitiesOfClass(Player.class, aabb);
                    nearbyPlayers.forEach(player -> Services.DATA.addFairkeeperChestPositionList(player, blockEntity.getBlockPos()));
                }
                if (state.getValue(DNLProperties.FAIRKEEPER_ALERT)) {
                    if (blockEntity.spawnerLocationList != null && blockEntity.spawnerLocationList.size() == 1) {
                        blockEntity.lastSpawner = blockEntity.spawnerLocationList.get(0);
                    }
                    alertTick(level, pos, state, blockEntity);
                }
            }
            double x = blockEntity.actualRegion2X + (blockEntity.actualRegion1X - blockEntity.actualRegion2X) * level.random.nextFloat();
            double y = blockEntity.actualRegion2Y + (blockEntity.actualRegion1Y - blockEntity.actualRegion2Y) * level.random.nextFloat();
            double z = blockEntity.actualRegion2Z + (blockEntity.actualRegion1Z - blockEntity.actualRegion2Z) * level.random.nextFloat();
            ((ServerLevel) level).sendParticles(DustParticleOptions.REDSTONE, x, blockEntity.actualRegion1Y, blockEntity.actualRegion1Z, 1, 0, 0, 0, 0);
            ((ServerLevel) level).sendParticles(DustParticleOptions.REDSTONE, x, blockEntity.actualRegion2Y, blockEntity.actualRegion1Z, 1, 0, 0, 0, 0);
            ((ServerLevel) level).sendParticles(DustParticleOptions.REDSTONE, x, blockEntity.actualRegion1Y, blockEntity.actualRegion2Z, 1, 0, 0, 0, 0);
            ((ServerLevel) level).sendParticles(DustParticleOptions.REDSTONE, x, blockEntity.actualRegion2Y, blockEntity.actualRegion2Z, 1, 0, 0, 0, 0);
            ((ServerLevel) level).sendParticles(DustParticleOptions.REDSTONE, blockEntity.actualRegion1X, y, blockEntity.actualRegion1Z, 1, 0, 0, 0, 0);
            ((ServerLevel) level).sendParticles(DustParticleOptions.REDSTONE, blockEntity.actualRegion2X, y, blockEntity.actualRegion1Z, 1, 0, 0, 0, 0);
            ((ServerLevel) level).sendParticles(DustParticleOptions.REDSTONE, blockEntity.actualRegion1X, y, blockEntity.actualRegion2Z, 1, 0, 0, 0, 0);
            ((ServerLevel) level).sendParticles(DustParticleOptions.REDSTONE, blockEntity.actualRegion2X, y, blockEntity.actualRegion2Z, 1, 0, 0, 0, 0);
            ((ServerLevel) level).sendParticles(DustParticleOptions.REDSTONE, blockEntity.actualRegion1X, blockEntity.actualRegion1Y, z, 1, 0, 0, 0, 0);
            ((ServerLevel) level).sendParticles(DustParticleOptions.REDSTONE, blockEntity.actualRegion2X, blockEntity.actualRegion1Y, z, 1, 0, 0, 0, 0);
            ((ServerLevel) level).sendParticles(DustParticleOptions.REDSTONE, blockEntity.actualRegion1X, blockEntity.actualRegion2Y, z, 1, 0, 0, 0, 0);
            ((ServerLevel) level).sendParticles(DustParticleOptions.REDSTONE, blockEntity.actualRegion2X, blockEntity.actualRegion2Y, z, 1, 0, 0, 0, 0);
        }
    }

    private static void alertTick(Level level, BlockPos pos, BlockState state, FairkeeperChestBlockEntity blockEntity) {
        if (blockEntity.spawnerLocationList == null) {
            blockEntity.spawnerLocationList = new ArrayList<>();
            return;
        }
        if (blockEntity.spawnerLocationList.isEmpty()) {
            return;
        }
        BlockPos spawnerBlockPos = blockEntity.spawnerLocationList.get(0);
        BlockEntity spawnerBlockEntity = level.getBlockEntity(spawnerBlockPos);
        if (spawnerBlockEntity instanceof FairkeeperSpawnerBlockEntity fairkeeperSpawnerBlockEntity) {
            fairkeeperSpawnerBlockEntity.alert(blockEntity.playerCount == 0 ? 1 : blockEntity.playerCount, spawnerBlockPos, fairkeeperSpawnerBlockEntity);
        }
        redstoneBeam(level, pos, spawnerBlockPos);
        blockEntity.spawnerLocationList.remove(0);
    }

    private static void redstoneBeam(Level level, BlockPos originPos, BlockPos targetPos) {
        double d = (double) (targetPos.getX() - originPos.getX());
        double e = (double) (targetPos.getY() - originPos.getY());
        double f = (double) (targetPos.getZ() - originPos.getZ());
        double s = Math.sqrt(d * d + e * e + f * f);
        d /= s;
        e /= s;
        f /= s;
        double r = level.random.nextDouble();
        while (r < s) {
            r += 0.2;
            ((ServerLevel) level).sendParticles(DustParticleOptions.REDSTONE, (double) originPos.getX() + 0.5D + d * r, (double) originPos.getY() + 0.5D + e * r, (double) originPos.getZ() + 0.5D + f * r, 1, 0.0D, 0.0, 0.0, 0.0);
            //level.addParticle(DustParticleOptions.REDSTONE, originPos.getX(), originPos.getY() + r, originPos.getZ(), 0.0, 0.0, 0.0);
        }
    }

    public static boolean scanFairkeeperChestPositions(Level level, BlockPos fairkeeperChestPos, BlockPos brokenBlockPos) {
        if (fairkeeperChestToBrokenBlockDistance(fairkeeperChestPos, brokenBlockPos) > PLAYER_RANGE) {
            return false;
        }
        BlockState fairkeeperChestState = level.getBlockState(fairkeeperChestPos);
        if (!fairkeeperChestState.is(DNLBlocks.FAIRKEEPER_CHEST.get())) {
            return false;
        }
        if (level.getBlockEntity(fairkeeperChestPos) instanceof FairkeeperChestBlockEntity fairkeeperChestBlockEntity) {
            if (fairkeeperChestBlockEntity.disabled) {
                return false;
            }
            if (fairkeeperChestBlockEntity.actualRegion1X > brokenBlockPos.getX() && fairkeeperChestBlockEntity.actualRegion1Y > brokenBlockPos.getY() && fairkeeperChestBlockEntity.actualRegion1Z > brokenBlockPos.getZ() && fairkeeperChestBlockEntity.actualRegion2X <= brokenBlockPos.getX() && fairkeeperChestBlockEntity.actualRegion2Y <= brokenBlockPos.getY() && fairkeeperChestBlockEntity.actualRegion2Z <= brokenBlockPos.getZ()) {
                fairkeeperChestBlockEntity.removeLootTable();
                redstoneBeam(level, fairkeeperChestPos, brokenBlockPos);
                outlineCauseBlock(level, brokenBlockPos);
                playSound(level, brokenBlockPos, SoundEvents.WITHER_SHOOT);
                triggerSpawners(level, fairkeeperChestPos, fairkeeperChestBlockEntity);
                return false;
            }
        }
        return true;
    }

    private static void outlineCauseBlock(Level level, BlockPos blockPos) {
        double d = (double) blockPos.getX() + 0.5D;
        double e = (double) blockPos.getY() + 0.5D;
        double f = (double) blockPos.getZ() + 0.5D;
        ((ServerLevel) level).sendParticles(DustParticleOptions.REDSTONE, d, e, f, 10, 0.0D, 0.5, 0.5, 0.5);
    }

    private static float fairkeeperChestToBrokenBlockDistance(BlockPos fairkeeperChestPos, BlockPos brokenBlockPos) {
        float f = (float)(fairkeeperChestPos.getX() - brokenBlockPos.getX());
        float g = (float)(fairkeeperChestPos.getY() - brokenBlockPos.getY());
        float h = (float)(fairkeeperChestPos.getZ() - brokenBlockPos.getZ());
        return Mth.sqrt(f * f + g * g + h * h);
    }

    public float getOpenProgress(float partialTicks) {
        return (prevOpenCloseAnimationProgress + (openCloseAnimationProgress - prevOpenCloseAnimationProgress) * partialTicks) / OPEN_CLOSE_ANIMATION_DURATION;
    }
}