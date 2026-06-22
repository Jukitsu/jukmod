package net.jukitsumc.jukmod.mixin.client;

import com.mojang.authlib.GameProfile;
import net.jukitsumc.jukmod.Jukmod;
import net.jukitsumc.jukmod.config.option.BooleanOption;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RemotePlayer.class)
public class RemotePlayerMixin extends AbstractClientPlayer {

    @Unique
    private BooleanOption lerpPlayerVelocity;
    @Unique
    private BooleanOption remotePlayerPhysics;

    public RemotePlayerMixin(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void initialize(CallbackInfo ci) {
        lerpPlayerVelocity = Jukmod.getInstance().getConfig().entities().lerpPlayerVelocity();
        remotePlayerPhysics = Jukmod.getInstance().getConfig().entities().remotePlayerPhysics();
    }

    /**
     * @author Jukitsu
     * @reason Linear Interpolation doesn't seem to affect rendering. We'll just bring back the old behaviour.
     */
    @Inject(method = "lerpMotion", at = @At("HEAD"), cancellable = true)
    public void onLerpMotion(Vec3 vec3, CallbackInfo ci) {
        if (!this.lerpPlayerVelocity.get()) {
            this.setDeltaMovement(vec3);
            ci.cancel();
        }
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/RemotePlayer;calculateEntityAnimation(Z)V"))
    public void calculateEntityAnimation(RemotePlayer instance, boolean b) {
        if (!this.remotePlayerPhysics.get())
            instance.calculateEntityAnimation(b);

    }

    @Inject(method = "aiStep", at = @At(value = "HEAD"), cancellable = true)
    public void clientPlayerPhysics(CallbackInfo ci) {
        if (this.remotePlayerPhysics.get()) {
            super.aiStep();
            this.updateSwingTime();
            ci.cancel();
        }


    }
}
