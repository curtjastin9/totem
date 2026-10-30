package dev.totemoptimizer.mixin;

import dev.totemoptimizer.TotemOptimizerClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    /** Per-frame hook, so hotbar input is handled between ticks too. */
    @Inject(method = "runTick", at = @At("HEAD"))
    private void totemoptimizer$onFrame(boolean advanceGameTime, CallbackInfo ci) {
        TotemOptimizerClient.onFrame((Minecraft) (Object) this);
    }
}
