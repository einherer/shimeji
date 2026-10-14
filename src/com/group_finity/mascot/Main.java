package com.group_finity.mascot;

import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.Desktop;
import java.io.IOException;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.group_finity.mascot.config.Configuration;
import com.group_finity.mascot.config.Entry;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.ConfigurationException;
import com.sun.jna.Platform;

/**
 * Einstiegspunkt des Programms.
 */
public class Main {

	private static final Logger log = Logger.getLogger(Main.class.getName());

	static final String BEHAVIOR_GATHER = "マウスの周りに集まる";

	static {
		try {
			LogManager.getLogManager().readConfiguration(Main.class.getResourceAsStream("/logging.properties"));
		} catch (final SecurityException e) {
			e.printStackTrace();
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private static Main instance = new Main();

	public static Main getInstance() {
		return instance;
	}

	private final Manager manager = new Manager();

	private final Configuration configuration = new Configuration();

	private static final String APP_ROOT = System.getProperty("user.dir");
	private static final String WINDOW_CONF = APP_ROOT + File.separator + "window.conf";
	private static final String TITLES_CONF = APP_ROOT + File.separator + "titles.conf";
	private static final String SETTINGS_SCRIPT = APP_ROOT + File.separator + "run-settings.sh";
	private static final String SETTINGS_PROPERTIES = APP_ROOT + File.separator + "settings.properties";

	public static void main(final String[] args) {
		System.setProperty("jna.nosys", "true");
		getInstance().run();
	}

	public void run() {

		// Einstellungen laden
		loadConfiguration();

		// Taskleistensymbol erstellen
		if (SystemTray.isSupported()) {
			createTrayIcon();
		}

		// Ein Maskottchen erstellen
		createMascot();
		getManager().start();
	}

	/**
	 * Konfigurationsdatei laden.
	 */
	private void loadConfiguration() {

		try {
			log.log(Level.INFO, "Konfigurationsdatei wird geladen ({0})", "/Behavior.xml");

			final Document actions = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
					Main.class.getResourceAsStream("/Behavior.xml"));

			log.log(Level.INFO, "Konfigurationsdatei wird geladen ({0})", "/Actions.xml");

			this.getConfiguration().load(new Entry(actions.getDocumentElement()));

			final Document behaviors = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
					Main.class.getResourceAsStream("/Actions.xml"));

			this.getConfiguration().load(new Entry(behaviors.getDocumentElement()));

			this.getConfiguration().validate();

		} catch (final IOException e) {
			log.log(Level.SEVERE, "Konfigurationsdatei konnte nicht geladen werden", e);
			exit();
		} catch (final SAXException e) {
			log.log(Level.SEVERE, "Konfigurationsdatei konnte nicht geladen werden", e);
			exit();
		} catch (final ParserConfigurationException e) {
			log.log(Level.SEVERE, "Konfigurationsdatei konnte nicht geladen werden", e);
			exit();
		} catch (final ConfigurationException e) {
			log.log(Level.SEVERE, "Konfigurationsdatei enthält Fehler", e);
			exit();
		}
	}

	/**
	 * Taskleistensymbol erstellen.
	 * @throws AWTException
	 * @throws IOException
	 */
	private void createTrayIcon() {

		log.log(Level.INFO, "Taskleistensymbol wird erstellt");

		if ( SystemTray.getSystemTray()==null ) {
			return;
		}

		// Menüeintrag „Maskottchen hinzufügen“
		final MenuItem increaseMenu = new MenuItem("Maskottchen hinzufügen");
		increaseMenu.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				createMascot();
			}
		});

		// Menüeintrag „Versammeln“
		final MenuItem gatherMenu = new MenuItem("Versammeln");
		gatherMenu.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				gatherAll();
			}
		});

		// Menüeintrag „Nur eines behalten“
		final MenuItem oneMenu = new MenuItem("Nur eines behalten");
		oneMenu.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				remainOne();
			}
		});

		final MenuItem openWindowConfMenu = new MenuItem("Einstellungen: window.conf öffnen");
		openWindowConfMenu.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				openPath(WINDOW_CONF);
			}
		});

		final MenuItem openTitlesConfMenu = new MenuItem("Einstellungen: titles.conf öffnen");
		openTitlesConfMenu.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				openPath(TITLES_CONF);
			}
		});

		final MenuItem openSettingsGuiMenu = new MenuItem("Einstellungen öffnen");
		openSettingsGuiMenu.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				launchSettingsGui();
			}
		});

		final MenuItem openAppFolderMenu = new MenuItem("Einstellungen: Programmordner öffnen");
		openAppFolderMenu.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				openPath(APP_ROOT);
			}
		});

		final MenuItem resetWindowConfMenu = new MenuItem("Einstellungen: window.conf zurücksetzen");
		resetWindowConfMenu.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent event) {
				resetWindowConf();
			}
		});

		//  Wiederherstellung des IE-Fensters noch nicht implementiert
		//final MenuItem restoreMenu = new MenuItem("IE-Fenster wiederherstellen");
		//restoreMenu.addActionListener(new ActionListener() {
			//public void actionPerformed(final ActionEvent event) {
				//restoreIE();
			//}
		//});

		// Menüeintrag „Beenden“
		final MenuItem closeMenu = new MenuItem("Beenden");
		closeMenu.addActionListener(new ActionListener() {
			public void actionPerformed(final ActionEvent e) {
				exit();
			}
		});

		// Kontextmenü erstellen
		final PopupMenu trayPopup = new PopupMenu();
		trayPopup.add(increaseMenu);
		trayPopup.add(gatherMenu);
		trayPopup.add(oneMenu);
		trayPopup.add(new MenuItem("-"));
		trayPopup.add(openSettingsGuiMenu);
		trayPopup.add(openWindowConfMenu);
		trayPopup.add(openTitlesConfMenu);
		trayPopup.add(openAppFolderMenu);
		trayPopup.add(resetWindowConfMenu);
//		trayPopup.add(restoreMenu);
		trayPopup.add(new MenuItem("-"));
		trayPopup.add(closeMenu);

		try {
			// Taskleistensymbol erstellen
			final TrayIcon icon = new TrayIcon(ImageIO.read(Main.class.getResource("/icon.png")), "Shimeji", trayPopup);
			icon.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(final MouseEvent e) {
					// Auch beim Doppelklick auf das Symbol ein Maskottchen hinzufügen
					if (SwingUtilities.isLeftMouseButton(e)) {
						createMascot();
					}
				}
			});

			// Taskleistensymbol anzeigen
			SystemTray.getSystemTray().add(icon);

		} catch (final IOException e) {
			log.log(Level.SEVERE, "Taskleistensymbol konnte nicht erstellt werden", e);
			exit();

		} catch (final AWTException e) {
//			log.log(Level.SEVERE, "Taskleistensymbol konnte nicht erstellt werden", e);
			MascotEventHandler.setShowSystemTrayMenu(true);
			getManager().setExitOnLastRemoved(true);
		}

	}

	/**
	 * Ein Maskottchen erstellen.
	 */
	public void createMascot() {

		log.log(Level.INFO, "Maskottchen wird erstellt");

		// Ein Maskottchen erstellen
		final Mascot mascot = new Mascot();

		// Außerhalb des sichtbaren Bereichs beginnen
		mascot.setAnchor(new Point(-1000, -1000));
		// Zufällige Blickrichtung wählen
		mascot.setLookRight(Math.random() < 0.5);

		try {
			mascot.setBehavior(getConfiguration().buildBehavior(null, mascot));

			this.getManager().add(mascot);

		} catch (final BehaviorInstantiationException e) {
			log.log(Level.SEVERE, "Anfangsverhalten konnte nicht initialisiert werden", e);
			mascot.dispose();
		} catch (final CantBeAliveException e) {
			log.log(Level.SEVERE, "Maskottchen kann in dieser Situation nicht weiterlaufen", e);
			mascot.dispose();
		}

	}

	public void gatherAll() {
		Main.this.getManager().setBehaviorAll(Main.this.getConfiguration(), BEHAVIOR_GATHER);
	}

	public void remainOne() {
		Main.this.getManager().remainOne();
	}

	public void restoreIE() {
		NativeFactory.getInstance().getEnvironment().restoreIE();
	}

	public void exit() {

		this.getManager().disposeAll();
		this.getManager().stop();

		System.exit(0);
	}

	public void openPath(final String path) {
		try {
			final Desktop desktop = Desktop.getDesktop();
			final File file = new File(path);
			if (file.isDirectory()) {
				desktop.open(file);
			} else if (file.exists()) {
				desktop.edit(file);
			}
		} catch (final Exception e) {
			log.log(Level.WARNING, "Pfad konnte nicht geöffnet werden: " + path, e);
		}
	}

	public void resetWindowConf() {
		try {
			java.nio.file.Files.write(
				java.nio.file.Paths.get(WINDOW_CONF),
				("Fensterversatz in den folgenden Zeilen angeben: x, y, Breitenzuschlag, Höhenzuschlag. Leere Angaben entsprechen 0.\n0\n0\n0\n0\n").getBytes(java.nio.charset.StandardCharsets.UTF_8)
			);
		} catch (final IOException e) {
			log.log(Level.WARNING, "window.conf konnte nicht zurückgesetzt werden", e);
		}
	}

	public void launchSettingsGui() {
		try {
			new ProcessBuilder(SETTINGS_SCRIPT).directory(new File(APP_ROOT)).start();
		} catch (final IOException e) {
			log.log(Level.WARNING, "Einstellungen konnten nicht geöffnet werden", e);
		}
	}

	public boolean isSelfCloningEnabled() {
		java.util.Properties props = new java.util.Properties();
		java.io.File settingsFile = new java.io.File(SETTINGS_PROPERTIES);
		if (!settingsFile.exists()) {
			return true;
		}
		try (java.io.FileInputStream in = new java.io.FileInputStream(settingsFile)) {
			props.load(in);
			return Boolean.parseBoolean(props.getProperty("selfCloningEnabled", "true"));
		} catch (final IOException e) {
			log.log(Level.WARNING, "settings.properties konnte nicht gelesen werden; Standardwert selfCloningEnabled=true wird verwendet", e);
			return true;
		}
	}

	public Configuration getConfiguration() {
		return this.configuration;
	}

	private Manager getManager() {
		return this.manager;
	}

}
