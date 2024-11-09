package de.crafty.lifecompat.energy.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractPositionedMenu extends AbstractContainerMenu {

    protected final Container container;
    protected BlockPos menuPos;

    protected AbstractPositionedMenu(@Nullable MenuType<?> menuType, int i, Inventory inventory, Container container, BlockPos menuPos) {
        super(menuType, i);

        this.container = container;
        this.menuPos = menuPos;
    }

    public Container getContainer() {
        return this.container;
    }

    public void setMenuPos(BlockPos menuPos) {
        this.menuPos = menuPos;
    }

    public BlockPos getMenuPos() {
        return this.menuPos;
    }
}
