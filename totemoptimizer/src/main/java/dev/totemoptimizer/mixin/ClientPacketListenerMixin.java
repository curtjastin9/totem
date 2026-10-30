package dev.totemoptimizer.mixin;

import dev.totemoptimizer.TotemOptimizerClient;
import dev.totemoptimizer.TotemOptimizerConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Shadow
    private Minecraft minecraft;

    @Shadow
    private ClientLevel level;

    /** Entity event 35 = totem of undying activated. TAIL so vanilla's pop animation still finds the totem first. */
    @Inject(method = "handleEntityEvent", at = @At("TAIL"))
    private void totemoptimizer$predictPop(ClientboundEntityEventPacket packet, CallbackInfo ci) {
        if (!TotemOptimizerConfig.predictPop || packet.getEventId() != 35) {
            return;
        }

        LocalPlayer player = this.minecraft.player;
        if (player == null || this.level == null) {
            return;
        }

        Entity entity = packet.getEntity(this.level);
        if (entity == player) {
            TotemOptimizerClient.consumeTotemLocally(player);
        }
    }
}
