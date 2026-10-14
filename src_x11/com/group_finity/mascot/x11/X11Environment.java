package com.group_finity.mascot.x11;

import java.awt.Rectangle;
import java.awt.Point;
import java.util.Vector;
import jnacontrib.x11.api.X.Display;
import jnacontrib.x11.api.X.X11Exception;
import jnacontrib.x11.api.X.Window;
import com.group_finity.mascot.environment.Area;
import com.group_finity.mascot.environment.WindowContainer;
import com.group_finity.mascot.environment.Environment;
import com.sun.jna.platform.unix.X11;
import java.io.*;
import java.util.ArrayList;
import java.util.Set;
import java.util.Iterator;
import java.util.Random;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

class X11Environment extends Environment {
	private static final Logger log = Logger.getLogger(X11Environment.class.getName());
	private static final Set<Integer> loggedWindowIds = new HashSet<Integer>();
	
// X-Anzeige. Siehe X.java
	private Display display = new Display(); 

// Hashtable zum Speichern der aktiven Fenster
	public WindowContainer IE = new WindowContainer();

// Zufällig ausgewähltes Zielfenster für Sprünge
	public Area activeIE = new Area();

// Aktueller Bildschirm; bleibt nach der ersten Zuweisung unverändert
// Environment.java und ComplexArea.java übernehmen die Erkennung
// und das Verhalten bei zwei Monitoren.
	public static final Area workArea = new Area();

	private boolean checkTitles = true;
	private boolean newRandom, updateOnNext = false;

// Zählvariable für Vorgänge, die seltener als
// bei jedem Tick stattfinden sollen. Mit 400 initialisieren, um
// frühzeitig eine activeIE-Auswahl auszulösen.
	private int q = 400;
	private int z = 0;
	private Number markedForDeletion;

// Variablen für Konfigurationsoptionen
	private int xoffset,yoffset,wmod,hmod = 0;
	private ArrayList<String> titles = new ArrayList<String>();


// Zufallszahlengenerator zur Auswahl eines Zielfensters für Sprünge
	private Random RNG = new Random();

// Speicher für Fenster-IDs; dient nur zum Vergleich beim Entfernen
// vom Nutzer geschlossener Fenster
	private ArrayList<Number> curActiveWin = new ArrayList<Number>();
	private ArrayList<Number> curVisibleWin = new ArrayList<Number>();

// Speicher für Werte bestimmter Zustands- und Typatome der aktuellen Anzeige
	private ArrayList<Number> badStateList = new ArrayList<Number>();
	private ArrayList<Number> badTypeList = new ArrayList<Number>();
	private int minimizedValue;
	private int dockValue;
		
// init() – Arbeitsbereich festlegen und Konfigurationsdateien lesen
	X11Environment() {
		workArea.set(getWorkAreaRect());
		badStateList.add(Integer.decode(display.getAtom("_NET_WM_STATE_MODAL").toString()));
		badStateList.add(Integer.decode(display.getAtom("_NET_WM_STATE_HIDDEN").toString()));
		minimizedValue = Integer.decode(display.getAtom("_NET_WM_STATE_HIDDEN").toString());
		badStateList.add(Integer.decode(display.getAtom("_NET_WM_STATE_ABOVE").toString()));
		badTypeList.add(Integer.decode(display.getAtom("_NET_WM_WINDOW_TYPE_DOCK").toString()));
		dockValue = Integer.decode(display.getAtom("_NET_WM_WINDOW_TYPE_DOCK").toString());
		badTypeList.add(Integer.decode(display.getAtom("_NET_WM_WINDOW_TYPE_MENU").toString()));
		badTypeList.add(Integer.decode(display.getAtom("_NET_WM_WINDOW_TYPE_SPLASH").toString()));
		badTypeList.add(Integer.decode(display.getAtom("_NET_WM_WINDOW_TYPE_DIALOG").toString()));
		badTypeList.add(Integer.decode(display.getAtom("_NET_WM_WINDOW_TYPE_DESKTOP").toString()));
		try {
			FileInputStream fstream = new FileInputStream("window.conf");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			int z = 0;
			while ((strLine = br.readLine()) != null) {
				z++;
				switch (z) {
					case 1: break;
					case 2: this.xoffset = Integer.parseInt(strLine.trim()); break;
					case 3: this.yoffset = Integer.parseInt(strLine.trim()); break;
					case 4: this.wmod = Integer.parseInt(strLine.trim()); break;
					case 5: this.hmod = Integer.parseInt(strLine.trim()); break;
					default : break;
				}
			}
			br.close();
			in.close();
			fstream.close();
		} catch (Exception e) {}
		try {
			FileInputStream fstream = new FileInputStream("titles.conf");
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			while ((strLine = br.readLine()) != null) {
				titles.add(strLine.trim().toLowerCase());
			}
			br.close();
			in.close();
			fstream.close();
		} catch (Exception e) {}
		if (titles.size() == 0) checkTitles = false;
	}
	

// tick() – Wird alle x Millisekunden ausgeführt; Intervall in Manager.java definiert
	@Override
	public void tick() {
		super.tick();
		if (q%5==0 || updateOnNext) update();
	// Alle 1000 Ticks ein neues Zielfenster für Sprünge wählen
		if (q == 1000) {
			getRandomIE();
			q = 0;
		}
		q++;
	}

// update() – Fensterverwaltung; wird bei jedem Tick ausgeführt
	private void update() {
		Window[] allWindows = null;
		Window ie = null;
		int x,y,w,h,id,curDesktop = 0;
		Rectangle r = new Rectangle();
		Area a = new Area();
		if (curVisibleWin != null) curVisibleWin.clear();
		curActiveWin = new ArrayList<Number>();
		if (display == null) return;
		updateOnNext = false;
		try {
		// Alle Fenster der X-Anzeige abrufen
			allWindows = display.getWindows();
			curDesktop = display.getActiveDesktopNumber();
			uguu:	
				for (int i=0;i<allWindows.length;i++) {
				// Schleife abbrechen, wenn der Fenstertitel nicht zur Konfiguration passt.
					if (checkTitles) {
						if (!isIE(allWindows[i].getTitle())) continue uguu;
					}
				// Fensterattribute abrufen.
					id = allWindows[i].getID();
					Rectangle frameBounds = allWindows[i].getBounds();
					Window.Geometry geometry = allWindows[i].getGeometry();
					w = frameBounds.width + wmod;
					h = frameBounds.height + hmod;
					x = frameBounds.x + xoffset;
					y = frameBounds.y + yoffset;
					if (!loggedWindowIds.contains(id)) {
						loggedWindowIds.add(id);
						log.log(Level.INFO,
							"Fensterdiagnose ID={0} Titel={1} Geometrie=({2},{3},{4},{5}) Rahmen=({6},{7},{8},{9}) Ergebnis=({10},{11},{12},{13})",
							new Object[] { id, allWindows[i].getTitle(), geometry.x, geometry.y, geometry.width, geometry.height,
								frameBounds.x, frameBounds.y, frameBounds.width, frameBounds.height,
								x, y, w, h });
					}
					if (IE.containsKey(id)) {
						a = IE.get(id);
						int desktop = allWindows[i].getDesktop();
						boolean badDesktop = ((desktop != curDesktop)&&(desktop != -1));
						boolean badState = checkState(allWindows[i].getState());
						if (!checkTitles) {
							boolean badType = checkType(allWindows[i].getType());
							if (badDesktop||badType||badState) {
								IE.get(id).setVisible(false);
							} else {
								IE.get(id).setVisible(true);
								curVisibleWin.add(id);
							}
						} else {
							if (badDesktop||badState) {
								IE.get(id).setVisible(false);
							} else {
								IE.get(id).setVisible(true);
								curVisibleWin.add(id);
							}
						}
						r = a.toRectangle();
						Rectangle newRect = new Rectangle(x,y,w,h);
						if (!r.equals(newRect)) {
							updateOnNext = true;
						}
						a.set(newRect);
						curActiveWin.add(id);
						continue uguu;
					} else {
						r = new Rectangle(x,y,w,h);
						a = new Area();
						a.set(r);
						a.setVisible(false);
						IE.put(id,a);
						curActiveWin.add(id);
					}
				}
		} catch (X11Exception e) {}
	// Vom Nutzer geschlossene Fenster bei jedem fünften Tick aus dem Container entfernen
		Iterator<Number> keys = IE.keySet().iterator();
		while (keys.hasNext()) {
			Number i = keys.next();
			if (!curActiveWin.contains(i)) {
				IE.get(i).setVisible(false);
				IE.remove(i);
				break;
			}
		}
	}
	
	private boolean isIE(String titlebar) {
		for (String s : titles) {
			if (titlebar.toLowerCase().contains(s)) return true;
		}
		return false;
	} 

	private boolean checkState(int state) {
		if (!checkTitles) {
			if (badStateList.contains(state)) return true;
			return false;
		} else {
			if (state == minimizedValue) return true;
			return false;
		}
	}

	private boolean checkType(int type) {
		if (badTypeList.contains(type)) return true;	
		return false;
	}

	private Rectangle getWorkAreaRect() {
		Rectangle r1 = getScreen().toRectangle();
		return r1;
	}

// getRandomIE() – Neues, zufällig ausgewähltes activeIE-Fenster zuweisen
// als Ziel für Sprungaktionen.
	private void getRandomIE() {
		ArrayList<Area> visibleWin = new ArrayList<Area>();
		if (curVisibleWin == null) return;
		for (Number n : curVisibleWin) {
			if (n != null) visibleWin.add(IE.get(n));
		}
		if (visibleWin.size() == 0) return;
		activeIE = visibleWin.get(RNG.nextInt(visibleWin.size()));
	}

	public int getDockValue() {
		return dockValue;
	}

	public ArrayList<Number> getVisible() {
		return this.curVisibleWin;
	}

	@Override
	public Area getActiveIE() {
		return this.activeIE;
	}
	
	@Override
	public WindowContainer getIE() {
		return this.IE;
	}
	
	@Override
	public Area getWorkArea() {
		return this.workArea;
	}

}
	
