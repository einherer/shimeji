package com.group_finity.mascot;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.group_finity.mascot.action.Action;
import com.group_finity.mascot.behavior.Behavior;
import com.group_finity.mascot.environment.*;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.image.MascotImage;
import com.group_finity.mascot.image.TranslucentWindow;

/**
 * Maskottchenobjekt.
 *
 * Ein Maskottchen wird durch {@link Behavior} für langfristiges, komplexes Verhalten und
 * {@link Action} für kurzfristige, einfache Bewegungen gesteuert.
 *
 * Ein interner Timer ruft {@link Action} in regelmäßigen Abständen auf.
 * Eine {@link Action} kann über {@link #animate(Point, MascotImage, boolean)} und ähnliche Methoden das
 * Maskottchen animieren.
 *
 * Wenn eine {@link Action} endet oder ein anderes vorgesehenes Ereignis eintritt, wird {@link Behavior} aufgerufen und die nächste {@link Action} gestartet.
 *
 */
public class Mascot {

	private static final long serialVersionUID = 1L;
	// Aktuellen Rand und zugehöriges IE-Fenster für Bedingungsprüfungen speichern.
	private Area curIE = new Area();
	private FloorCeiling curFC;
	private Wall curW;

	private static final Logger log = Logger.getLogger(Mascot.class.getName());

	/**
	 * ID des zuletzt erzeugten Maskottchens.
	 */
	private static AtomicInteger lastId = new AtomicInteger();

	/**
	 * ID des Maskottchens.
	 * Dient ausschließlich der besseren Lesbarkeit von Diagnoseprotokollen.
	 */
	private final int id;

	/**
	 * Fenster zur Anzeige des Maskottchens.
	 */
	private final TranslucentWindow window = NativeFactory.getInstance().newTransparentWindow();

	/**
	 * Manager, der das Maskottchen verwaltet.
	 */
	private Manager manager = null;

	/**
	 * Ankerposition des Maskottchens.
	 * Zum Beispiel die Füße oder beim Hängen die Hände.
	 * Dieser Punkt dient als Mittelpunkt bei der Bildanzeige.
	 */
	private Point anchor = new Point(0, 0);

	/**
	 * Anzuzeigendes Bild.
	 */
	private MascotImage image = null;

	/**
	 * Ob das Maskottchen nach rechts blickt.
	 * Das Originalbild blickt nach links; bei true wird es gespiegelt gezeichnet.
	 */
	private boolean lookRight = false;

	/**
	 * Objekt, das das langfristige Verhalten beschreibt.
	 */
	private Behavior behavior = null;

	/**
	 * Zeitwert, der bei jedem Timer-Tick erhöht wird.
	 */
	private int time = 0;

	/**
	 * Ob die Animation gerade läuft.
	 */
	private boolean animating = true;

	/**
	 * Anzeigeumgebung des Maskottchens.
	 */
	
	private MascotEnvironment environment = new MascotEnvironment(this);

	public Mascot() {
		this.id = lastId.incrementAndGet();

		log.log(Level.INFO, "Maskottchen erstellt ({0})", this);

		// Immer im Vordergrund anzeigen
		getWindow().asJWindow().setAlwaysOnTop(true);
		//getWindow().asJWindow().setBounds(0,0,128,128);

		// Mausereignisbehandlung registrieren
		getWindow().asJWindow().addMouseListener(new MascotEventHandler(this));

	}

	@Override
	public String toString() {
		return "Maskottchen" + this.id;
	}
	void tick() {
	// Zugeordnetes IE-Fenster anhand der Boden- und Wandprüfungen aktualisieren
		setCurIE();
		if (isAnimating()) {
			if (getBehavior() != null) {

				try {
					getBehavior().next();
				} catch (final CantBeAliveException e) {
					log.log(Level.SEVERE, "Maskottchen kann in dieser Situation nicht weiterlaufen", e);
					dispose();
				}

				setTime(getTime() + 1);
			}
		}

	}

	public void apply() {
		if (isAnimating()) {
			// Ohne darstellbares Bild ist keine Aktion möglich
			if (getImage() != null) {

				// Bild festlegen
				getWindow().setImage(getImage().getImage());

				// Fensterbereich festlegen
				getWindow().asJWindow().setBounds(getBounds());

				// Anzeigen
				if (!getWindow().asJWindow().isVisible()) {
					getWindow().asJWindow().setVisible(true);
				}
				getWindow().asJWindow().setAlwaysOnTop(true);
				getWindow().asJWindow().toFront();

				// Neu zeichnen
				getWindow().updateImage();
			} else {
				if (getWindow().asJWindow().isVisible()) {
					getWindow().asJWindow().setVisible(false);
				}
			}
			// Auf modernen Desktops für das Maskottchen keinen DOCK-Fenstertyp erzwingen.
			// Dadurch kann es auf einer unerwarteten Ebene unter oder zwischen Fenstern mit Rahmen landen.
			// Die alte X11-Behelfslösung blendete das Fenster bei bestimmten Ticks aus. Das kann
			// bei modernen Compositors sichtbar flackern. Das Maskottchen sichtbar lassen und
			// den regulären Ablauf zur Bildaktualisierung verwenden.
		}
	}

	public void dispose() {
		log.log(Level.INFO, "Maskottchen entfernt ({0})", this);

		getWindow().asJWindow().dispose();
		if (getManager() != null) {
			getManager().remove(Mascot.this);
		}
	}

	public Manager getManager() {
		return this.manager;
	}

	public void setManager(final Manager manager) {
		this.manager = manager;
	}

	public Point getAnchor() {
		return this.anchor;
	}
	
	public Area getCurIE() {
		return this.curIE;
	}

	public void setAnchor(final Point anchor) {
		this.anchor = anchor;
	}

	public MascotImage getImage() {
		return this.image == null ? null : this.image.getScaled(MascotScale.getPercent());
	}

	public void setImage(final MascotImage image) {
		this.image = image;
	}

	public boolean isLookRight() {
		return this.lookRight;
	}

	public void setLookRight(final boolean lookRight) {
		this.lookRight = lookRight;
	}

	public Rectangle getBounds() {

		// Fensterbereich aus Ankerposition und Bildmittelpunkt berechnen.
		final int top = getAnchor().y - getImage().getCenter().y;
		final int left = getAnchor().x - getImage().getCenter().x;

		final Rectangle result = new Rectangle(left, top, getImage().getSize().width, getImage().getSize().height);

		return result;
	}

	public int getTime() {
		return this.time;
	}

	private void setTime(final int time) {
		this.time = time;
	}
	public void setCurIE() {
		Area temp = new Area();
		if (this.curW != null) {
			this.curIE = curW.getArea();
					} else {
			if (this.curFC != null) {
				this.curIE = curFC.getArea();
			} else {
				this.curIE = new Area();
			}
		}
		if (temp.toRectangle().getWidth() < 5) return;
		if (temp.toRectangle().toString() != curIE.toRectangle().toString()) {
			curIE = temp;
		}
		
	}
	
	public void setCurW(Wall w) {
		this.curW = w;
	}

	public boolean onBorder() {
		return (this.curW != null || this.curFC != null);
	}
	
	public void setCurFC(FloorCeiling fc) {
		this.curFC = fc;
	}
		
	public Behavior getBehavior() {
		return this.behavior;
	}

	public void setBehavior(final Behavior behavior) throws CantBeAliveException {
		this.behavior = behavior;
		this.behavior.init(this);
	}

	public int getTotalCount() {
		return getManager().getCount();
	}

	private boolean isAnimating() {
		return this.animating;
	}

	void setAnimating(final boolean animating) {
		this.animating = animating;
	}

	TranslucentWindow getWindow() {
		return this.window;
	}

	public MascotEnvironment getEnvironment() {
		return environment;
	}

	public int getID() {
		return this.id;
	}

	public void move(int x, int y) {
		anchor.setLocation(anchor.getX()+x,anchor.getY()+y);
	}

}
