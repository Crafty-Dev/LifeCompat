package de.crafty.lifecompat.energy.blockentity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import de.crafty.lifecompat.LifeCompat;
import de.crafty.lifecompat.energy.block.BaseEnergyBlock;
import de.crafty.lifecompat.init.LifeCompatModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;

public abstract class SimpleEnergyBlockRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

    public static final ResourceLocation IO_LOCATION = ResourceLocation.fromNamespaceAndPath(LifeCompat.MODID, "textures/entity/machines/energy_io.png");

    protected final ModelPart noneModel, inputModel, outputModel, ioModel;

    public SimpleEnergyBlockRenderer(BlockEntityRendererProvider.Context ctx) {
        this.noneModel = ctx.bakeLayer(LifeCompatModelLayers.ENERGY_IO_NONE).getChild("main");
        this.inputModel = ctx.bakeLayer(LifeCompatModelLayers.ENERGY_IO_INPUT).getChild("main");
        this.outputModel = ctx.bakeLayer(LifeCompatModelLayers.ENERGY_IO_OUTPUT).getChild("main");
        this.ioModel = ctx.bakeLayer(LifeCompatModelLayers.ENERGY_IO_IO).getChild("main");
    }

    @Override
    public abstract void render(T blockEntity, float f, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, int overlay);


    protected void renderIOSideCentered(BlockState state, Direction side, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, int overlay){
        //1.0F / 16.0F is the size of one pixel
        this.renderIOSide(state, side, 0.5F - 1.0F / 16.0F, 0.5F - 1.0F / 16.0F, poseStack, multiBufferSource, light, overlay);
    }

    //Renders the default I/O icons at the given coordinates
    protected void renderIOSide(BlockState state, Direction side, float x, float y, PoseStack poseStack, MultiBufferSource multiBufferSource, int light, int overlay){
        if(!(state.getBlock() instanceof BaseEnergyBlock))
            return;

        Direction facing = Direction.NORTH;

        if(state.hasProperty(BaseEnergyBlock.HORIZONTAL_FACING))
            facing = state.getValue(BaseEnergyBlock.HORIZONTAL_FACING);

        if(state.hasProperty(BaseEnergyBlock.FACING))
            facing = state.getValue(BaseEnergyBlock.FACING);

        EnumProperty<BaseEnergyBlock.IOMode> sideMode = BaseEnergyBlock.calculateIOSide(facing, side);
        if(!state.hasProperty(sideMode))
            return;

        BaseEnergyBlock.IOMode mode = state.getValue(sideMode);
        poseStack.pushPose();
        this.translateByDirection(side, poseStack);
        poseStack.translate(x, y, -0.000125F);

        VertexConsumer vertexConsumer = multiBufferSource.getBuffer(RenderType.entitySolid(IO_LOCATION));

        if(mode == BaseEnergyBlock.IOMode.NONE)
            this.noneModel.render(poseStack, vertexConsumer, light, overlay);
        if(mode == BaseEnergyBlock.IOMode.INPUT)
            this.inputModel.render(poseStack, vertexConsumer, light, overlay);
        if(mode == BaseEnergyBlock.IOMode.OUTPUT)
            this.outputModel.render(poseStack, vertexConsumer, light, overlay);
        if(mode == BaseEnergyBlock.IOMode.IO)
            this.ioModel.render(poseStack, vertexConsumer, light, overlay);

        poseStack.popPose();
    }

    protected void translateByDirection(Direction direction, PoseStack poseStack){
        if (direction == Direction.EAST) {
            poseStack.mulPose(Axis.YP.rotationDegrees(270));
            poseStack.translate(0.0F, 0.0F, -1.0F);
        }
        if (direction == Direction.SOUTH) {
            poseStack.translate(1.0F, 0.0F, 1.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(180));
        }
        if (direction == Direction.WEST) {
            poseStack.translate(0.0F, 0.0F, 1.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(90));
        }
        if(direction == Direction.UP){
            poseStack.translate(0.0F, 1.0F, 0.0F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90));
        }
        if(direction == Direction.DOWN){
            poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            poseStack.translate(0.0F, -1.0F, 0.0F);
        }
    }

    public static LayerDefinition createIONoneLayer() {
        return withDefinedUV(0, 0);
    }

    public static LayerDefinition createOutputLayer() {
        return withDefinedUV(2, 0);
    }

    public static LayerDefinition createInputLayer() {
        return withDefinedUV(4, 0);
    }

    public static LayerDefinition createIOLayer() {
        return withDefinedUV(6, 0);
    }


    private static LayerDefinition withDefinedUV(int u, int v) {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        partdefinition.addOrReplaceChild("main", CubeListBuilder.create().texOffs(u, v).addBox(0.0F, 0.0F, 0.0F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));

        return LayerDefinition.create(meshdefinition, 8, 8);
    }
}

