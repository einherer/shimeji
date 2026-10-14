package com.group_finity.mascot;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/** Global figure size, refreshed once per animation tick for all mascots. */
public final class MascotScale {
    private static final File SETTINGS = new File(System.getProperty("user.dir"), "settings.properties");
    private static volatile int percent = 100;
    private static long lastCheck;

    private MascotScale() {
    }

    public static int getPercent() {
        return percent;
    }

    public static void refresh() {
        final long now = System.nanoTime();
        if (lastCheck != 0 && now - lastCheck < 500000000L) {
            return;
        }
        lastCheck = now;
        if (!SETTINGS.exists()) {
            percent = 100;
            return;
        }
        final Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(SETTINGS)) {
            props.load(in);
            percent = Math.max(25, Math.min(200,
                    Integer.parseInt(props.getProperty("scalePercent", "100").trim())));
        } catch (final NumberFormatException e) {
            percent = 100;
        } catch (final IOException e) {
            // Keep the previous size if the settings are temporarily unavailable.
        }
    }
}
