package com.group_finity.mascot.behavior;

import java.awt.event.MouseEvent;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.exception.CantBeAliveException;


/**
 * Objekt, das das langfristige Verhalten eines Maskottchens beschreibt.
 * 
 * Wird von {@link Mascot#setBehavior(Behavior)} verwendet.
 */
public interface Behavior {

	/**
	 * Wird beim Start eines Verhaltens aufgerufen.
	 * @param mascot Zugeordnetes Maskottchen.
	 */
	public void init(Mascot mascot) throws CantBeAliveException;

	/**
	 * Maskottchen zum nächsten Frame weiterschalten.
	 */
	public void next() throws CantBeAliveException;
	
	/**
	 * Maustaste wurde gedrückt.
	 * @param mascot Angeklicktes Maskottchen.
	 */
	public void mousePressed(MouseEvent e) throws CantBeAliveException;

	/**
	 * Maustaste wurde losgelassen.
	 * @param mascot Maskottchen, bei dem die Maustaste losgelassen wurde.
	 */
	public void mouseReleased(MouseEvent e) throws CantBeAliveException;
}
