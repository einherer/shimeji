package com.group_finity.mascot.x11;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.group_finity.mascot.image.NativeImage;


/**
 * Bild mit Alpha-Werten zur Verwendung in einem durchscheinenden Fenster.
 * 
 * Das Bild stellt die Pixeldaten für das durchscheinende Fenster bereit.
 * Die Pixeldaten werden aus einem vorhandenen {@link BufferedImage} übernommen.
 * 
 * Ursprünglicher Autor: Yuki Yamada von Group Finity (http://www.group-finity.com/Shimeji/)
 * Wird derzeit von der Shimeji-ee Group entwickelt.
 */
class X11NativeImage implements NativeImage {

	/**
	 * Java-Bildobjekt.
	 */
	private final BufferedImage managedImage;
	
	private final Icon icon;

	public X11NativeImage(final BufferedImage image) {
		this.managedImage = image;
		this.icon = new ImageIcon(image);
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
	}

	public void flush() {
		this.getManagedImage().flush();
	}

	public Graphics getGraphics() {
		return this.getManagedImage().createGraphics();
	}

	public int getHeight() {
		return this.getManagedImage().getHeight();
	}

	public int getWidth() {
		return this.getManagedImage().getWidth();
	}

	public int getHeight(final ImageObserver observer) {
		return this.getManagedImage().getHeight(observer);
	}

	public Object getProperty(final String name, final ImageObserver observer) {
		return this.getManagedImage().getProperty(name, observer);
	}

	public ImageProducer getSource() {
		return this.getManagedImage().getSource();
	}

	public int getWidth(final ImageObserver observer) {
		return this.getManagedImage().getWidth(observer);
	}

	BufferedImage getManagedImage() {
		return this.managedImage;
	}

	public Icon getIcon() {
		return this.icon;
	}

}
