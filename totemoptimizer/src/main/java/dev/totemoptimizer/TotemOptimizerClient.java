package dev.totemoptimizer;

import com.mojang.blaze3d.platform.InputConstants;
import dev.totemoptimizer.mixin.KeyMappingAccessor;
import dev.totemoptimizer.mixin.MultiPlayerGameModeAccessor;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TotemOptimizerClient implements ClientModInitializer {
    public static final String MOD_ID = "totemoptimizer";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Edge detection for the 9 hotbar keys, so a held key doesn't keep re-selecting. */
    private static final boolean[] WAS_DOWN = new boolean[9];

    @Override
    public void onInitializeClient() {
        TotemOptimizerConfig.load();
        LOGGER.info("Totem Optimizer loaded (instantHotbarSync={}, predictPop={})",
                TotemOptimizerConfig.instantHotbarSync, TotemOptimizerConfig.predictPop);
    }

    /**
     * Runs once per rendered frame (not once per 50 ms tick).
     *
     * Vanilla only reads hotbar keys inside the client tick and only tells the server about the new
     * slot on the following tick, which can leave the server on the OLD slot for 50-100 ms. If a
     * crystal/anchor hits you in that window, the server sees no totem in your hand.
     * Here we select the slot as soon as the key goes down and push the slot packet immediately.
     */
    public static void onFrame(Minecraft mc) {
        if (!TotemOptimizerConfig.instantHotbarSync) {
            return;
        }

        LocalPlayer player = mc.player;
        if (!isReady(mc)) {
            java.util.Arrays.fill(WAS_DOWN, false);
            return;
        }

        KeyMapping[] keys = mc.options.keyHotbarSlots;
        boolean canSelect = canSelect(mc, player);

        for (int i = 0; i < keys.length && i < WAS_DOWN.length; i++) {
            boolean down = keys[i].isDown();
            if (canSelect && down && !WAS_DOWN[i]) {
                player.getInventory().setSelectedSlot(i);
            }
            WAS_DOWN[i] = down;
        }

        // Sends ServerboundSetCarriedItemPacket right now if the selected slot changed (key OR scroll wheel).
        // Vanilla tracks the last sent slot itself, so this never sends duplicates.
        ((MultiPlayerGameModeAccessor) mc.gameMode).totemoptimizer$ensureHasSentCarriedItem();
    }

    /**
     * Fastest path: called from KeyMapping.set(), i.e. at the moment the key event arrives from the
     * OS, before the next frame or tick. Selects the slot and sends the packet in the same call.
     */
    public static void onKeyEvent(InputConstants.Key key, boolean held) {
        if (!TotemOptimizerConfig.instantHotbarSync) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (!isReady(mc)) {
            return;
        }

        KeyMapping[] keys = mc.options.keyHotbarSlots;
        for (int i = 0; i < keys.length && i < WAS_DOWN.length; i++) {
            if (!key.equals(((KeyMappingAccessor) keys[i]).totemoptimizer$getKey())) {
                continue;
            }

            if (!held) {
                WAS_DOWN[i] = false;
            } else if (!WAS_DOWN[i] && canSelect(mc, mc.player)) {
                WAS_DOWN[i] = true;
                mc.player.getInventory().setSelectedSlot(i);
                ((MultiPlayerGameModeAccessor) mc.gameMode).totemoptimizer$ensureHasSentCarriedItem();
            }
            return;
        }

        handleSwapKey(mc, key, held);
    }

    private static boolean swapWasDown;
    private static int swallowSwapClicks;

    /**
     * Vanilla sends the offhand swap from the next client tick. We send it right now and remove the
     * click vanilla queued, so the swap still happens exactly once.
     */
    private static void handleSwapKey(Minecraft mc, InputConstants.Key key, boolean held) {
        if (!TotemOptimizerConfig.instantOffhandSwap) {
            return;
        }

        KeyMappingAccessor swap = (KeyMappingAccessor) mc.options.keySwapOffhand;
        if (!key.equals(swap.totemoptimizer$getKey())) {
            return;
        }

        if (!held) {
            swapWasDown = false;
            swallowSwapClicks = 0;
            return;
        }

        if (swapWasDown || mc.screen != null || mc.getOverlay() != null || mc.player.isSpectator()) {
            return;
        }

        swapWasDown = true;
        mc.getConnection().send(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));

        // The click may be registered before or after set(); handle both orders.
        int clicks = swap.totemoptimizer$getClickCount();
        if (clicks > 0) {
            swap.totemoptimizer$setClickCount(clicks - 1);
        } else {
            swallowSwapClicks = 1;
        }
    }

    /** Called right after KeyMapping.click(key). */
    public static void onKeyClicked(InputConstants.Key key) {
        if (swallowSwapClicks <= 0) {
            return;
        }

        KeyMappingAccessor swap = (KeyMappingAccessor) Minecraft.getInstance().options.keySwapOffhand;
        if (!key.equals(swap.totemoptimizer$getKey())) {
            return;
        }

        swallowSwapClicks--;
        int clicks = swap.totemoptimizer$getClickCount();
        if (clicks > 0) {
            swap.totemoptimizer$setClickCount(clicks - 1);
        }
    }

    private static boolean isReady(Minecraft mc) {
        return mc.player != null && mc.level != null && mc.gameMode != null && mc.getConnection() != null;
    }

    private static boolean canSelect(Minecraft mc, LocalPlayer player) {
        return mc.screen == null
                && mc.getOverlay() == null
                && !player.isSpectator()
                && !mc.options.keySaveHotbarActivator.isDown()
                && !mc.options.keyLoadHotbarActivator.isDown();
    }

    /**
     * Mirrors what the server does on a totem pop (LivingEntity#checkTotemDeathProtection): it takes
     * the first totem it finds, main hand first, then off hand, and shrinks it by one. Doing it here
     * means the totem disappears the instant the pop packet arrives. The server's own slot update
     * arrives later with the same result, so a wrong guess is corrected automatically.
     */
    public static void consumeTotemLocally(LocalPlayer player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (!stack.isEmpty() && stack.get(DataComponents.DEATH_PROTECTION) != null) {
                stack.shrink(1);
                return;
            }
        }
    }
}
