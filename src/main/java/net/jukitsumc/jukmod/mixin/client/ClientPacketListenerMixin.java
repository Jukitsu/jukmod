package net.jukitsumc.jukmod.mixin.client;

import net.jukitsumc.jukmod.Jukmod;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.Entity;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin {


    @Redirect(method = "handleMoveEntity", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;lerpTo(DDDFFIZ)V"))
    private void properlyLerpEntityMovement(Entity entity,
                                            double x, double y, double z,
                                            float g, float h,
                                            int three, boolean idk) {
        entity.lerpTo(x, y, z, g, h, 3, idk);

    }

    @Redirect(method = "handleRotateMob", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;lerpHeadTo(FI)V"))
    private void properlyLerpEntityRotation(Entity entity, float yRot, int three) {
        entity.lerpHeadTo(yRot, 3);
    }


}
