package de.crafty.lifecompat.energy.block;

import de.crafty.lifecompat.util.EnergyUnitConverter;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class BaseEnergyBlock extends BaseEntityBlock {

    public static final List<EnumProperty<IOMode>> IO_MODE_PROPERTIES = new ArrayList<>();

    public static final EnumProperty<IOMode> IO_FRONT = registerIOMode("front");
    public static final EnumProperty<IOMode> IO_LEFT = registerIOMode("left");
    public static final EnumProperty<IOMode> IO_BACK = registerIOMode("back");
    public static final EnumProperty<IOMode> IO_RIGHT = registerIOMode("right");
    public static final EnumProperty<IOMode> IO_TOP = registerIOMode("top");
    public static final EnumProperty<IOMode> IO_BOTTOM = registerIOMode("bottom");

    private static EnumProperty<IOMode> registerIOMode(String name) {
        EnumProperty<IOMode> prop = EnumProperty.create(name, IOMode.class);
        IO_MODE_PROPERTIES.add(prop);
        return prop;
    }

    public static final DirectionProperty HORIZONTAL_FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    //A map of all facings with their associated relative sides
    private static final HashMap<Direction, List<Direction>> RELATIVE_DIRECTIONS = BaseEnergyBlock.preGenerateRelativeDirections();

    protected BaseEnergyBlock(Properties properties) {
        super(properties);
    }

    public List<IOMode> validIOModes() {
        return List.of(IOMode.values());
    }

    @Override
    protected RenderShape getRenderShape(BlockState blockState) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState blockState, Level level, BlockPos blockPos, Player player, BlockHitResult blockHitResult) {

        DirectionProperty facingProp = null;
        if (blockState.hasProperty(HORIZONTAL_FACING))
            facingProp = HORIZONTAL_FACING;
        if (blockState.hasProperty(FACING))
            facingProp = FACING;

        Direction facing = facingProp == null ? Direction.NORTH : blockState.getValue(facingProp);

        Direction side = blockHitResult.getDirection();
        if (player.isCrouching() || player.isShiftKeyDown()) {
            EnumProperty<IOMode> sideMode = BaseEnergyBlock.calculateIOSide(facing, side);
            if (sideMode != null && blockState.hasProperty(sideMode)) {
                IOMode next = blockState.getValue(sideMode).next(this);
                player.displayClientMessage(
                        Component.translatable("energy.lifecompat.io." + sideMode.getName())
                                .append(": ")
                                .withStyle(ChatFormatting.GRAY)
                                .append(Component.translatable("energy.lifecompat.io." + next.getSerializedName()).withStyle(ChatFormatting.BLUE)),
                        true);

                level.setBlock(blockPos, blockState.setValue(sideMode, next), Block.UPDATE_ALL);
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        return InteractionResult.PASS;
    }

    @Override
    protected @NotNull List<ItemStack> getDrops(BlockState blockState, LootParams.Builder builder) {
        List<ItemStack> drops = super.getDrops(blockState, builder);
        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);

        BlockState state = builder.getOptionalParameter(LootContextParams.BLOCK_STATE);
        drops.forEach(stack -> {
            if (stack.is(this.asItem()) && blockEntity != null)
                BlockItem.setBlockEntityData(stack, blockEntity.getType(), blockEntity.saveCustomOnly(blockEntity.getLevel().registryAccess()));

            if (state != null)
                stack.update(DataComponents.BLOCK_STATE, BlockItemStateProperties.EMPTY, properties -> {
                    for (EnumProperty<IOMode> property : IO_MODE_PROPERTIES) {
                        if (state.hasProperty(property))
                            properties = properties.with(property, state.getValue(property));

                    }
                    return properties;
                });
        });
        return drops;
    }

    public enum IOMode implements StringRepresentable {
        NONE,
        INPUT,
        OUTPUT,
        IO;

        public IOMode next(BaseEnergyBlock block) {
            if (block.validIOModes().isEmpty())
                return IOMode.values()[this.ordinal()];

            IOMode mode = IOMode.values()[(this.ordinal() + 1) % IOMode.values().length];
            if (!block.validIOModes().contains(mode))
                return mode.next(block);

            return mode;
        }

        public boolean isInput() {
            return this == INPUT || this == IO;
        }

        public boolean isOutput() {
            return this == OUTPUT || this == IO;
        }

        @Override
        public @NotNull String getSerializedName() {
            return this.name().toLowerCase();
        }
    }


    //Retrieves the IOMode Property depending on the side
    public static EnumProperty<IOMode> calculateIOSide(Direction facing, Direction side) {

        List<Direction> sides = RELATIVE_DIRECTIONS.get(facing);
        List<EnumProperty<IOMode>> modes = List.of(IO_FRONT, IO_LEFT, IO_BACK, IO_RIGHT, IO_TOP, IO_BOTTOM);

        return sides.contains(side) ? modes.get(sides.indexOf(side)) : null;
    }

    //Pre-generates a map of all possible facings with their associated relative sides (left, right, back, front, etc... are relative to the facing)
    private static HashMap<Direction, List<Direction>> preGenerateRelativeDirections() {
        HashMap<Direction, List<Direction>> relativeDirections = new HashMap<>();
        for (Direction facing : Direction.values()) {
            relativeDirections.put(facing, BaseEnergyBlock.calculateRelativeSides(facing));
        }

        return relativeDirections;
    }

    private static List<Direction> calculateRelativeSides(Direction facing) {

        Direction front = Direction.fromAxisAndDirection(facing.getAxis(), facing.getAxisDirection());
        Direction back = front.getOpposite();
        Direction left = null;
        Direction right;
        Direction top = null;
        Direction bottom;

        if (facing.getAxis() == Direction.Axis.Z) {
            left = Direction.fromAxisAndDirection(Direction.Axis.X, facing.getOpposite().getAxisDirection());
            top = Direction.UP;
        }
        if (facing.getAxis() == Direction.Axis.X) {
            left = Direction.fromAxisAndDirection(Direction.Axis.Z, facing.getAxisDirection());
            top = Direction.UP;
        }

        if (facing.getAxis() == Direction.Axis.Y) {
            left = Direction.fromAxisAndDirection(Direction.Axis.X, facing.getAxisDirection());
            top = Direction.fromAxisAndDirection(Direction.Axis.Z, facing.getAxisDirection());
        }

        right = left.getOpposite();
        bottom = top.getOpposite();

        return List.of(front, left, back, right, top, bottom);
    }

    //Returns the actual IOMode for the given side (or IOMode.NONE if the given state has no IO functionality at this side)
    public static IOMode getIOMode(BlockState state, Direction side) {
        if (!(state.getBlock() instanceof BaseEnergyBlock))
            return IOMode.NONE;

        Direction facing = Direction.NORTH;
        if (state.hasProperty(HORIZONTAL_FACING))
            facing = state.getValue(HORIZONTAL_FACING);
        if (state.hasProperty(FACING))
            facing = state.getValue(FACING);

        EnumProperty<IOMode> modeProp = BaseEnergyBlock.calculateIOSide(facing, side);

        return modeProp == null || !state.hasProperty(modeProp) ? IOMode.NONE : state.getValue(modeProp);
    }

    @Override
    public void appendHoverText(ItemStack itemStack, Item.TooltipContext tooltipContext, List<Component> list, TooltipFlag tooltipFlag) {
        CustomData data = itemStack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag tag = data == null ? new CompoundTag() : data.copyTag();

        int energy = 0;

        if (tag.contains("energy"))
            energy = tag.getInt("energy");

        if (energy > 0)
            list.add(Component.translatable("energy.lifecompat.stored").append(": ").withStyle(ChatFormatting.GRAY).append(Component.literal(EnergyUnitConverter.format(energy)).withStyle(ChatFormatting.DARK_PURPLE)));
    }
}
