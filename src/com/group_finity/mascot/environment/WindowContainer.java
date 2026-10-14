package com.group_finity.mascot.environment;

import java.awt.Rectangle;
import java.awt.Point;
import java.util.Hashtable;
import java.util.Iterator;

/*
 * Hashtable-Erweiterung zum Speichern von XWindow-IDs und Area-Objekten,
 * die die Fensterabmessungen darstellen. Die Methoden werden von
 * Maskottchen über MascotEnvironment aufgerufen, wenn neue Aktionen gewählt werden
 * um zu prüfen, ob sie sich auf einem Rand befinden.
 */

public class WindowContainer extends Hashtable<Number,Area> {
	
/*
 * onBorder, getBorder – Werden von Maskottchen aufgerufen, wenn
 * die nächste Aktion bestimmt wird. Alle Fenster durchlaufen,
 * den jeweiligen Randtyp ermitteln und dessen isOn()-Methode aufrufen; diese
 * prüft, ob der Ankerpunkt des Maskottchens auf dem Rand liegt. Eine Methode
 * für reine Wahrheitswertprüfungen, eine zum Abrufen des Randes,
 * wenn daraus Bewegungsziele bestimmt werden müssen.
 *
 */
	public boolean onLeft(final Point p) {
		Iterator<Area> temp = this.values().iterator();
		while (temp.hasNext()) {
			Wall w = new Wall(temp.next(),false);
			if (w.isOn(p)) return true;
		}
		return false;
	}
	
	public boolean onRight(final Point p) {
		Iterator<Area> temp = this.values().iterator();
		while (temp.hasNext()) {
			Wall w = new Wall(temp.next(),true);
			if (w.isOn(p)) return true;
		}
		return false;
	}
	
	public boolean onTop(final Point p) {
		Iterator<Area> temp = this.values().iterator();
		while (temp.hasNext()) {
			FloorCeiling w = new FloorCeiling(temp.next(),false);
			if (w.isOn(p)) return true;
		}
		return false;
	}
	
	public boolean onBottom(final Point p) {
		Iterator<Area> temp = this.values().iterator();
		while (temp.hasNext()) {
			FloorCeiling w = new FloorCeiling(temp.next(),true);
			if (w.isOn(p)) return true;
		}
		return false;
	}

	public Wall getLeft(final Point p) {
		Iterator<Area> temp = this.values().iterator();
		while (temp.hasNext()) {
			Wall w = new Wall(temp.next(),false);
			if (w.isOn(p)) return w;
		}
		return null;
	}
	
	public Wall getRight(final Point p) {
		Iterator<Area> temp = this.values().iterator();
		while (temp.hasNext()) {
			Wall w = new Wall(temp.next(),true);
			if (w.isOn(p)) return w;
		}
		return null;
	}
	
	public FloorCeiling getTop(final Point p) {
		Iterator<Area> temp = this.values().iterator();
		while (temp.hasNext()) {
			FloorCeiling w = new FloorCeiling(temp.next(),false);
			if (w.isOn(p)) return w;
		}
		return null;
	}
	
	public FloorCeiling getBottom(final Point p) {
		Iterator<Area> temp = this.values().iterator();
		while (temp.hasNext()) {
			FloorCeiling w = new FloorCeiling(temp.next(),true);
			if (w.isOn(p)) return w;
		}
		return null;
	}

}
