package dev.totemoptimizer;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Tiny properties-file config: .minecraft/config/totemoptimizer.properties */
public final class TotemOptimizerConfig {
    /** Send hotbar slot changes the same frame the key/scroll happens instead of waiting for the next tick. */
    public static volatile boolean instantHotbarSync = true;

    /** Consume the totem client-side the moment the server tells us we popped, so no ghost totem is left in hand. */
    public static volatile boolean predictPop = true;

    /** Send the offhand-swap (F) packet the moment the key event arrives instead of on the next tick. */
    public static volatile boolean instantOffhandSwap = true;

    private TotemOptimizerConfig() {
    }

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("totemoptimizer.properties");
        Properties props = new Properties();

        try {
            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    props.load(in);
                }
            }

            instantHotbarSync = Boolean.parseBoolean(props.getProperty("instantHotbarSync", "true"));
            predictPop = Boolean.parseBoolean(props.getProperty("predictPop", "true"));

            instantOffhandSwap = Boolean.parseBoolean(props.getProperty("instantOffhandSwap", "true"));

            props.setProperty("instantOffhandSwap", Boolean.toString(instantOffhandSwap));
            props.setProperty("instantHotbarSync", Boolean.toString(instantHotbarSync));
            props.setProperty("predictPop", Boolean.toString(predictPop));

            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, "Totem Optimizer settings (restart the game after editing)");
            }
        } catch (IOException e) {
            TotemOptimizerClient.LOGGER.warn("Could not read/write totemoptimizer.properties, using defaults", e);
        }
    }
}
