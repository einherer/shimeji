package com.group_finity.mascot.image;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.image.BufferedImage;

import com.group_finity.mascot.NativeFactory;



public class MascotImage {

	private final NativeImage image;

	private BufferedImage source;
	private int scaledPercent = 100;
	private MascotImage scaledImage;

	private final Point center;

	private final Dimension size;

	public MascotImage(final NativeImage image, final Point center, final Dimension size) {
		this.image = image;
		this.center = center;
		this.size = size;
	}

	public MascotImage(final BufferedImage image, final Point center) {
		this(NativeFactory.getInstance().newNativeImage(image), center, new Dimension(image.getWidth(), image.getHeight()));
		this.source = image;
	}

	/** Cache one scaled variant per pose, shared by every mascot using it. */
	public synchronized MascotImage getScaled(final int percent) {
		if (percent == 100 || source == null) {
			return this;
		}
		if (scaledImage == null || scaledPercent != percent) {
			final double factor = Math.max(25, Math.min(200, percent)) / 100.0;
			final int width = Math.max(1, (int) Math.round(size.width * factor));
			final int height = Math.max(1, (int) Math.round(size.height * factor));
			final BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			final Graphics2D graphics = resized.createGraphics();
			try {
				graphics.setComposite(AlphaComposite.Src);
				graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
				graphics.drawImage(source, 0, 0, width, height, null);
			} finally {
				graphics.dispose();
			}
			final Point scaledCenter = new Point((int) Math.round(center.x * factor),
					(int) Math.round(center.y * factor));
			scaledImage = new MascotImage(NativeFactory.getInstance().newNativeImage(resized),
					scaledCenter, new Dimension(width, height));
			scaledPercent = percent;
		}
		return scaledImage;
	}

	public NativeImage getImage() {
		return this.image;
	}

	public Point getCenter() {
		return this.center;
	}

	public Dimension getSize() {
		return this.size;
	}

}
