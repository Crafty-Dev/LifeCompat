package de.crafty.lifecompat.energy.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import de.crafty.lifecompat.LifeCompat;
import de.crafty.lifecompat.energy.menu.AbstractEnergyContainerMenu;
import de.crafty.lifecompat.util.EnergyUnitConverter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public abstract class AbstractEnergyContainerScreen<T extends AbstractEnergyContainerMenu> extends AbstractContainerScreen<T> {

    private static final ResourceLocation ENERGY_BAR = ResourceLocation.fromNamespaceAndPath(LifeCompat.MODID, "textures/gui/container/energy_bar_horizontal.png");

    private int currentEnergy = 0;
    private int energyCapacity = 0;
    private float fillStatus = 0.0F;

    private int energyBarTick = 0;
    private boolean energyBarTickUp = true;

    public AbstractEnergyContainerScreen(T abstractContainerMenu, Inventory inventory, Component component) {
        super(abstractContainerMenu, inventory, component);
    }


    @Override
    protected void containerTick() {

        if (this.energyBarTickUp)
            this.energyBarTick++;
        else
            this.energyBarTick--;

        if (this.energyBarTick <= 0)
            this.energyBarTickUp = true;

        if (this.energyBarTick >= 31)
            this.energyBarTickUp = false;

        this.currentEnergy = this.getMenu().getStoredEnergy();
        this.energyCapacity = this.getMenu().getCapacity();

        this.fillStatus = (float) this.currentEnergy / (float) this.energyCapacity;
    }

    public int getCurrentEnergy() {
        return this.currentEnergy;
    }

    public int getEnergyCapacity() {
        return this.energyCapacity;
    }

    public float getFillStatus() {
        return this.fillStatus;
    }

    protected void renderHorizontalEnergyBar(GuiGraphics guiGraphics, int x, int y, float partialTicks) {
        float scale = 0.75F;

        float barX = this.width / 2.0F - (152 * scale) / 2;
        float barY = y - (18 * scale) - 2;

        PoseStack poseStack = guiGraphics.pose();
        poseStack.pushPose();
        poseStack.translate(barX, barY, 0);
        poseStack.scale(scale, scale, 1.0F);
        guiGraphics.blit(ENERGY_BAR, 0, 0, 0, 0, 152, 18);
        guiGraphics.blit(ENERGY_BAR, 0, 0, 0, 18 + (this.energyBarTick / 4) * 18, 2 + Math.round(148 * this.getFillStatus()), 18);
        poseStack.popPose();

        //Info Rndering
        float infoScale = 0.5F;

        float infoWidth = 96 * infoScale;
        float infoHeight = 18 * infoScale;

        float infoX = (this.width - infoWidth) / 2.0F;
        float infoY = barY - infoHeight - 0.75F;

        poseStack.pushPose();
        poseStack.translate(infoX, infoY, 0);
        poseStack.scale(infoScale, infoScale, 1.0F);
        guiGraphics.blit(ENERGY_BAR, 0, 0, 152, 0, 96, 18);
        poseStack.popPose();

        float fontScale = 0.5F;

        Component energy = Component.literal(EnergyUnitConverter.formatRaw(this.getCurrentEnergy())).withStyle(ChatFormatting.GRAY)
                .append("/")
                .append(Component.literal(EnergyUnitConverter.format(this.getEnergyCapacity())).withStyle(ChatFormatting.RED));

        poseStack.pushPose();
        poseStack.translate(this.width / 2.0F - this.font.width(energy) / 2.0F * fontScale, infoY + (infoHeight / 2.0F - this.font.lineHeight / 2.0F * fontScale) + 0.5F, 0.0F);
        poseStack.scale(fontScale, fontScale, 1.0F);
        guiGraphics.drawString(this.font, energy, 0, 0, ChatFormatting.DARK_GRAY.getColor());
        poseStack.popPose();
    }
}
