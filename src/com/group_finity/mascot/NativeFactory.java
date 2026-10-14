package com.group_finity.mascot;

import java.awt.image.BufferedImage;

import com.group_finity.mascot.environment.Environment;
import com.group_finity.mascot.image.NativeImage;
import com.group_finity.mascot.image.TranslucentWindow;
import com.sun.jna.Platform;

/**
 * Ermöglicht den Zugriff auf die native Umgebung.
 * {@link #getInstance()} liefert je nach Laufzeitumgebung eine Windows-spezifische oder allgemeine Unterklasse.
 * @author Yuki
 */
public abstract class NativeFactory {

	private static final NativeFactory instance;

	/**
	 * Instanz einer Unterklasse vorab erstellen.
	 */
	static {
		final String basePackage = NativeFactory.class.getName().substring(0, NativeFactory.class.getName().lastIndexOf('.'));

		final String subPackage = Platform.isWindows() ? "win" : (Platform.isX11() ? "x11" : "generic");

		try {
			final Class<? extends NativeFactory> impl = (Class<? extends NativeFactory>)Class.forName(basePackage+"."+subPackage+".NativeFactoryImpl");

			instance = impl.newInstance();

		} catch (final ClassNotFoundException e) {
			throw new RuntimeException(e);
		} catch (final InstantiationException e) {
			throw new RuntimeException(e);
		} catch (final IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Zur Laufzeitumgebung passende Instanz einer Unterklasse abrufen.
	 * @return
	 */
	public static NativeFactory getInstance() {
		return instance;
	}

	/**
	 * Umgebungsobjekt abrufen.
	 * @return Umgebungsobjekt.
	 */
	public abstract Environment getEnvironment();

	/**
	 * Zum angegebenen BufferedImage passendes natives Bild abrufen.
	 * Dieses Bild kann zur Maskierung eines {@link TranslucentWindow} verwendet werden.
	 * @param src
	 * @return
	 */
	public abstract NativeImage newNativeImage(BufferedImage src);

	/**
	 * Fenster mit halbtransparenter Darstellung erstellen.
	 * @return
	 */
	public abstract TranslucentWindow newTransparentWindow();
}
