package dev.totemoptimizer.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("key")
    InputConstants.Key totemoptimizer$getKey();

    @Accessor("clickCount")
    int totemoptimizer$getClickCount();

    @Accessor("clickCount")
    void totemoptimizer$setClickCount(int count);
}
