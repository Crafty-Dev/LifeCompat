package de.crafty.lifecompat.energy.menu;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractEnergyContainerMenu extends AbstractContainerMenu {

    protected AbstractEnergyContainerMenu(@Nullable MenuType<?> menuType, int i) {
        super(menuType, i);
    }

    public abstract int getStoredEnergy();

    public abstract int getCapacity();
}
