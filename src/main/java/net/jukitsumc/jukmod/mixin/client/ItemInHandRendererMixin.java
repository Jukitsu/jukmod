package net.jukitsumc.jukmod.mixin.client;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.jukitsumc.jukmod.Jukmod;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Shadow @Final private ItemRenderer itemRenderer;




    @ModifyArg(method="renderArmWithItem",
            at=@At(value="INVOKE",
                    target="Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
                    ordinal = 1),
            index = 2
    )
    public ItemDisplayContext modifyContext(ItemDisplayContext context, @Local AbstractClientPlayer livingEntity, @Local PoseStack poseStack, @Local ItemStack itemStack, @Local int i) {
        if (!this.itemRenderer.getModel(itemStack, livingEntity.level(), livingEntity, i).isCustomRenderer()
                && !this.itemRenderer.getModel(itemStack, livingEntity.level(), livingEntity, i).isGui3d()
                && !this.itemRenderer.getModel(itemStack, livingEntity.level(), livingEntity, i).useAmbientOcclusion()
                && !itemStack.is(Items.FISHING_ROD)
                && !itemStack.is(Items.TRIDENT)
                && !itemStack.is(Items.SPYGLASS)) {
            poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));

            poseStack.scale(0.4F, 0.4F, 0.4F);
            poseStack.translate(0.58800083f, 0.36999986f, -0.77000016f);
            poseStack.translate(0.0F, -0.3F, 0.0F);
            poseStack.scale(1.5F, 1.5F, 1.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(50.0F));
            poseStack.mulPose(Axis.ZP.rotationDegrees(335.0F));
            poseStack.translate(-0.9375F, -0.0625F, 0.0F);
            poseStack.scale(-2, 2, -2);
            poseStack.scale(0.5f, 0.5f, 0.5f);

            return ItemDisplayContext.NONE;
        }

        return context;


    }


}
