package notmistfalls.conquest;

import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import notmistfalls.conquest.config.ConfigException;
import notmistfalls.conquest.config.ConquestConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NotMistFallsConquest implements ModInitializer {
    public static final String MOD_ID = "notmistfalls_conquest";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static volatile ConquestConfig config = ConquestConfig.defaults();

    /** The active configuration (defaults if the config file could not be read). */
    public static ConquestConfig config() {
        return config;
    }

    @Override
    public void onInitialize() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve(MOD_ID + ".properties");
        try {
            config = ConquestConfig.load(file);
            LOGGER.info("Loaded config from {}", file);
        } catch (ConfigException e) {
            config = ConquestConfig.defaults();
            LOGGER.error("Invalid config ({}). Using built-in defaults; the file was not changed.",
                    e.getMessage());
        }
        LOGGER.info("NotMistFall: Conquest initialized.");
    }
}
