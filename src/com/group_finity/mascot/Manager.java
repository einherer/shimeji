package com.group_finity.mascot;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;

/**
 *
 * Verwaltet die Maskottchenliste und steuert die Zeitabläufe.
 * Diese Klasse synchronisiert alle Maskottchen, da asynchrone Bewegungen beispielsweise beim Werfen von Fenstern Probleme verursachen.
 * {@link #tick()} aktualisiert zunächst die Umgebungsinformationen und bewegt anschließend alle Maskottchen.
 *
 * @author Yuki Yamada
 */
public class Manager {

	private static final Logger log = Logger.getLogger(Manager.class.getName());

	/**
	 * Ausführungsintervall des Timers.
	 */
	public static final int TICK_INTERVAL = 20;

	/**
	 * Liste der Maskottchen.
	 */
	private final List<Mascot> mascots = new ArrayList<Mascot>();

	/**
	 * Liste der zum Hinzufügen vorgemerkten Maskottchen.
	 * Um eine {@link ConcurrentModificationException} zu vermeiden, werden neue Maskottchen bei jedem {@link #tick()} gemeinsam hinzugefügt.
	 */
	private final Set<Mascot> added = new LinkedHashSet<Mascot>();

	/**
	 * Liste der zum Entfernen vorgemerkten Maskottchen.
	 * Um eine {@link ConcurrentModificationException} zu vermeiden, werden Maskottchen bei jedem {@link #tick()} gemeinsam entfernt.
	 */
	private final Set<Mascot> removed = new LinkedHashSet<Mascot>();

	/**
	 * Ob das Programm beim Entfernen des letzten Maskottchens beendet werden soll.
	 * Wenn beispielsweise kein Taskleistensymbol erstellt werden konnte, muss das Programm beim Entfernen des letzten Maskottchens beendet werden, damit kein Prozess zurückbleibt.
	 */
	private boolean exitOnLastRemoved;

	/**
	 * Thread, der {@link #tick()} wiederholt aufruft.
	 */
	private transient Thread thread;

	public Manager() {

		// Dies umgeht einen Java-Fehler unter Windows
		// Häufige kurze Aufrufe von Thread.sleep können die Windows-Uhr beeinträchtigen
		// Längere Aufrufe von Thread.sleep umgehen dieses Problem.
		new Thread() {
			{
				this.setDaemon(true);
				this.start();
			}

			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(Integer.MAX_VALUE);
					} catch (final InterruptedException ex) {
					}
				}
			}
		};
	}

	/**
	 * Thread starten.
	 */
	public void start() {

		if ( thread!=null && thread.isAlive() ) {
			// Thread läuft bereits
			return;
		}

		thread = new Thread() {
			@Override
			public void run() {

				// Vorheriger Zeitpunkt
				long prev = System.nanoTime() / 1000000;
				try {
					for (;;) {
						for (;;) {
							// Aktueller Zeitpunkt
							// Schleife ausführen, bis TICK_INTERVAL verstrichen ist.
							final long cur = System.nanoTime() / 1000000;
							if (cur - prev >= TICK_INTERVAL) {
								if (cur > prev + TICK_INTERVAL * 2) {
									prev = cur;
								} else {
									prev += TICK_INTERVAL;
								}
								break;
							}
							Thread.sleep(1, 0);
						}

						// Maskottchen bewegen.
						tick();
					}
				} catch (final InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		};
		thread.setDaemon(false);

		thread.start();
	}

	/**
	 * Thread anhalten.
	 */
	public void stop() {
		if ( thread==null || !thread.isAlive() ) {
			// Thread läuft bereits nicht mehr
			return;
		}

		thread.interrupt();

		try {
			thread.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Maskottchen um einen Frame weiterschalten.
	 */
	private void tick() {

		// Zuerst Umgebungsinformationen aktualisieren
		NativeFactory.getInstance().getEnvironment().tick();

		synchronized (this.getMascots()) {

			// Zum Hinzufügen vorgemerkte Maskottchen hinzufügen
			for (final Mascot mascot : this.getAdded()) {
				this.getMascots().add(mascot);
			}
			this.getAdded().clear();

			// Zum Entfernen vorgemerkte Maskottchen entfernen
			for (final Mascot mascot : this.getRemoved()) {
				this.getMascots().remove(mascot);
			}
			this.getRemoved().clear();

			// Zeit des Maskottchens fortschreiben
			for (final Mascot mascot : this.getMascots()) {
				mascot.tick();
			}

			// Bild und Position des Maskottchens aktualisieren.
			for (final Mascot mascot : this.getMascots()) {
				mascot.apply();
			}
		}

		if (isExitOnLastRemoved() && this.getMascots().size() == 0) {
			// Beenden, da exitOnLastRemoved true ist und kein Maskottchen mehr vorhanden ist.
			Main.getInstance().exit();
		}
	}

	/**
	 * Maskottchen hinzufügen.
	 * Das Hinzufügen erfolgt beim nächsten Aufruf von {@link #tick()}.
	 * @param mascot Hinzuzufügendes Maskottchen.
	 */
	public void add(final Mascot mascot) {
		synchronized (this.getAdded()) {
			this.getAdded().add(mascot);
			this.getRemoved().remove(mascot);
		}
		mascot.setManager(this);
	}

	/**
	 * Maskottchen entfernen.
	 * Das Entfernen erfolgt beim nächsten Aufruf von {@link #tick()}.
	 * @param mascot Zu entfernendes Maskottchen.
	 */
	public void remove(final Mascot mascot) {
		synchronized (this.getAdded()) {
			this.getAdded().remove(mascot);
			this.getRemoved().add(mascot);
		}
		mascot.setManager(null);
	}

	/**
	 * Verhalten aller Maskottchen festlegen.
	 * @param configuration
	 * @param name
	 */
	public void setBehaviorAll(final Configuration configuration, final String name) {
		synchronized (this.getMascots()) {
			for (final Mascot mascot : this.getMascots()) {
				try {
					mascot.setBehavior(configuration.buildBehavior(name));
				} catch (final BehaviorInstantiationException e) {
					log.log(Level.SEVERE, "Nächstes Verhalten konnte nicht initialisiert werden", e);
					mascot.dispose();
				} catch (final CantBeAliveException e) {
					log.log(Level.SEVERE, "Maskottchen kann in dieser Situation nicht weiterlaufen", e);
					mascot.dispose();
				}
			}
		}
	}

	/**
	 * Ein Maskottchen behalten und alle anderen entfernen.
	 */
	public void remainOne() {
		synchronized (this.getMascots()) {
			for (int i = this.getMascots().size() - 1; i > 0; --i) {
				this.getMascots().get(i).dispose();
			}
		}
	}

	/**
	 * Alle Maskottchen entfernen.
	 */
	public void disposeAll() {
		synchronized (this.getMascots()) {
			for (int i = this.getMascots().size() - 1; i >= 0; --i) {
				this.getMascots().get(i).dispose();
			}
		}
	}

	/**
	 * Aktuelle Anzahl der Maskottchen abrufen.
	 * @return Aktuelle Anzahl der Maskottchen.
	 */
	public int getCount() {
		synchronized (this.getMascots()) {
			return this.getMascots().size();
		}
	}

	public void setExitOnLastRemoved(boolean exitOnLastRemoved) {
		this.exitOnLastRemoved = exitOnLastRemoved;
	}

	public boolean isExitOnLastRemoved() {
		return exitOnLastRemoved;
	}

	private List<Mascot> getMascots() {
		return this.mascots;
	}

	private Set<Mascot> getAdded() {
		return this.added;
	}

	private Set<Mascot> getRemoved() {
		return this.removed;
	}

}
