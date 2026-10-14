import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.MascotScale;
import com.group_finity.mascot.image.ImagePair;
import com.group_finity.mascot.image.ImagePairLoader;
import com.group_finity.mascot.image.MascotImage;
import com.group_finity.mascot.win.WindowsNativeImage;

public class ScalingTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void setScale(Path path, String value, int expected) throws Exception {
        Files.write(path, ("scalePercent=" + value + "\n").getBytes(StandardCharsets.UTF_8));
        Thread.sleep(550);
        MascotScale.refresh();
        check(MascotScale.getPercent() == expected, "settings: " + value);
    }

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("shimeji-scale-test");
        Path settings = directory.resolve("settings.properties");
        System.setProperty("user.dir", directory.toString());
        Mascot left = null, right = null;
        try {
            MascotScale.refresh();
            check(MascotScale.getPercent() == 100, "missing settings default");
            ImagePair pair = ImagePairLoader.load("/shime1.png", new Point(64, 128));
            left = new Mascot();
            right = new Mascot();
            left.setAnchor(new Point(500, 600));
            right.setAnchor(new Point(900, 600));
            left.setImage(pair.getImage(false));
            right.setImage(pair.getImage(true));
            Dimension original = pair.getImage(false).getSize();
            for (int percent : new int[] {25, 137, 200, 100}) {
                setScale(settings, Integer.toString(percent), percent);
                for (Mascot mascot : new Mascot[] {left, right}) {
                    MascotImage image = mascot.getImage();
                    Rectangle bounds = mascot.getBounds();
                    check(bounds.width == Math.max(1, (int) Math.round(original.width * percent / 100.0)), "width");
                    check(bounds.height == Math.max(1, (int) Math.round(original.height * percent / 100.0)), "height");
                    check(bounds.x + image.getCenter().x == mascot.getAnchor().x, "horizontal anchor");
                    check(bounds.y + image.getCenter().y == 600, "feet stay on ground");
                    BufferedImage pixels = ((WindowsNativeImage) image.getImage()).getManagedImage();
                    check(pixels.getWidth() == bounds.width && pixels.getHeight() == bounds.height, "native window size");
                    check((pixels.getRGB(0, 0) >>> 24) == 0, "transparent corner");
                    check(mascot.getImage() == image, "shared cached variant");
                }
            }
            setScale(settings, "bad", 100);
            setScale(settings, "0", 25);
            setScale(settings, "999", 200);
            MascotImage tiny = new MascotImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB), new Point(0, 1));
            check(tiny.getScaled(25).getSize().equals(new Dimension(1, 1)), "tiny image stays valid");
            check(pair.getImage(false).getScaled(100) == pair.getImage(false), "original retained");
            System.out.println("Java scaling: live updates, both directions, bounds, anchors, transparency and defaults passed.");
        } finally {
            if (left != null) left.dispose();
            if (right != null) right.dispose();
            Files.deleteIfExists(settings);
            Files.deleteIfExists(directory);
        }
        System.exit(0);
    }
}
