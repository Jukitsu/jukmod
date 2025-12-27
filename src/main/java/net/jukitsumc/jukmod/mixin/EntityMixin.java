package net.jukitsumc.jukmod.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class EntityMixin {

    @Shadow
    private Level level;

    @Shadow
    protected abstract BlockPos getPrimaryStepSoundBlockPos(BlockPos blockPos);
    @Shadow public abstract void playSound(SoundEvent soundEvent, float f, float g);

    @Redirect(method="walkingStepSound", at=@At(value="INVOKE", target="Lnet/minecraft/world/entity/Entity;playStepSound(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void onPlayStepSound(Entity entity, BlockPos blockPos, BlockState blockState) {
        this.playStepSound(blockPos, blockState);
        boolean isEntityDimed = entity instanceof Spider || entity instanceof Silverfish || entity instanceof Bee;
        boolean isInsideFluid = !blockState.getFluidState().isEmpty();

        if (isInsideFluid)
            return;

        SoundType soundType = blockState.getSoundType();
        this.playSound(soundType.getStepSound(), soundType.getVolume() * 0.15F * (isEntityDimed ? 0.5F : 1.0F), soundType.getPitch());

    }

    @Overwrite
    public void playStepSound(BlockPos blockPos, BlockState blockState) {

    }
}
