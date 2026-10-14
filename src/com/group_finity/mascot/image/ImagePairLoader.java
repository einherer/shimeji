package com.group_finity.mascot.image;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.imageio.ImageIO;



/**
 *　Bildpaar laden.
 */
public class ImagePairLoader {

	/**
	 * Bildpaar laden.
	 *
	 * Nach links gerichtetes Bild laden und nach rechts gerichtetes Bild automatisch erzeugen.
	 *
	 * @param name Zu ladendes Bild mit Blickrichtung nach links.
	 * @param center Mittelpunkt des Bildes.
	 * @return Geladenes Bildpaar.
	 */
	public static ImagePair load(final String name, final Point center) throws IOException {

		// Da manche Bilder beim Spiegeln offenbar ihre Transparenz verlieren,
		// wird für shime1.png das separat gespiegelte Bild shime1-r.png verwendet.
		String rightName = name.replaceAll("¥¥.[a-zA-Z]+$", "-r$0");

		final BufferedImage leftImage = ImageIO.read(ImagePairLoader.class.getResource(name));


		final BufferedImage rightImage;
		/*
		if ( ImagePairLoader.class.getResource(rightName)==null ) {
			rightImage = flip(leftImage);
		} else {
			rightImage = ImageIO.read(ImagePairLoader.class.getResource(rightName));
		}
		*/
		rightImage = flip(leftImage);

		return new ImagePair(new MascotImage(leftImage, center), new MascotImage(rightImage, new Point(rightImage
				.getWidth()
				- center.x, center.y)));
	}

	/**
	 * Bild horizontal spiegeln.
	 * @param src Horizontal zu spiegelndes Bild
	 * @return Horizontal gespiegeltes Bild
	 */
	private static BufferedImage flip(final BufferedImage src) {

		final BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);

		for (int y = 0; y < src.getHeight(); ++y) {
			for (int x = 0; x < src.getWidth(); ++x) {
				copy.setRGB(copy.getWidth() - x - 1, y, src.getRGB(x, y));
			}
		}
		return copy;
	}

}
