package dev.totemoptimizer.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.totemoptimizer.TotemOptimizerClient;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin {
    /** Called for every key press/release the moment it arrives from the OS. */
    @Inject(method = "set", at = @At("HEAD"))
    private static void totemoptimizer$onKeySet(InputConstants.Key key, boolean held, CallbackInfo ci) {
        TotemOptimizerClient.onKeyEvent(key, held);
    }

    /** Lets us drop vanilla's own queued swap click after we already sent the swap. */
    @Inject(method = "click", at = @At("RETURN"))
    private static void totemoptimizer$onClick(InputConstants.Key key, CallbackInfo ci) {
        TotemOptimizerClient.onKeyClicked(key);
    }
}
