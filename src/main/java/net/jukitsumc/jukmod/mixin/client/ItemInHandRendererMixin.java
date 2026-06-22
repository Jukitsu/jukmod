package net.jukitsumc.jukmod.mixin.client;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.jukitsumc.jukmod.Jukmod;
import net.jukitsumc.jukmod.config.option.BooleanOption;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;



@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Unique
    private BooleanOption oldSwing;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initialize(CallbackInfo ci) {

        oldSwing = Jukmod.getInstance().getConfig().animations().oldSwing();
    }


    @ModifyArg(method="renderArmWithItem",
            at=@At(value="INVOKE",
                    target="Lnet/minecraft/client/renderer/ItemInHandRenderer;renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;I)V",
                    ordinal = 1),
            index = 2
    )
    public ItemDisplayContext modifyContext(ItemDisplayContext context,
                                            @Local AbstractClientPlayer livingEntity, @Local InteractionHand hand,
                                            @Local PoseStack poseStack, @Local ItemStack itemStack) {
        if (oldSwing.get() && !(itemStack.getItem() instanceof BlockItem)) {
            final HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? livingEntity.getMainArm() : livingEntity.getMainArm().getOpposite();
            final int direction = arm == HumanoidArm.RIGHT ? 1 : -1;

            final float scale = 0.7585F / 0.86F;
            poseStack.scale(scale, scale, scale);
            poseStack.translate(direction * -0.084F, 0.059F, 0.08F);
            poseStack.mulPose(Axis.YP.rotationDegrees(direction * 5.0F));

        }
        return context;

    }


}
