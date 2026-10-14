package com.group_finity.mascot.image;


/**
 * Bildpaar eines Maskottchens für beide Blickrichtungen.
 * 
 * Bilder für beide Blickrichtungen eines Maskottchens gemeinsam verwalten.
 */
public class ImagePair {

	/**
	 * Bild mit Blickrichtung nach links.
	 */
	private MascotImage leftImage;

	/**
	 * Bild mit Blickrichtung nach rechts.
	 */
	private MascotImage rightImage;

	/**
	 * Bildpaar aus zwei vorhandenen Bildern erstellen.
	 * @param leftImage Bild mit Blickrichtung nach links.
	 * @param rightImage Bild mit Blickrichtung nach rechts.
	 */
	public ImagePair(
			final MascotImage leftImage, final MascotImage rightImage) {
		this.leftImage = leftImage;
		this.rightImage = rightImage;
	}

	/**
	 * Bild mit der angegebenen Blickrichtung abrufen.
	 * @param lookRight Ob das nach rechts gerichtete Bild zurückgegeben werden soll.
	 * @return Bild mit der angegebenen Blickrichtung.
	 */
	public MascotImage getImage(final boolean lookRight) {
		return lookRight ? this.getRightImage() : this.getLeftImage();
	}

	private MascotImage getLeftImage() {
		return this.leftImage;
	}
	
	private MascotImage getRightImage() {
		return this.rightImage;
	}
}
