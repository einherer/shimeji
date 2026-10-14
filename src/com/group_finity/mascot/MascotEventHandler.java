package com.group_finity.mascot;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import com.group_finity.mascot.exception.CantBeAliveException;

public class MascotEventHandler implements MouseListener {

	private static final Logger log = Logger.getLogger(MascotEventHandler.class.getName());

	/**
	 * Ob die Menüeinträge des Taskleistensymbols ebenfalls angezeigt werden sollen.
	 * Wenn kein Taskleistensymbol erstellt werden konnte, müssen dessen Menüeinträge hier ebenfalls angezeigt werden.
	 */
	private static boolean showSystemTrayMenu = false;

	public static void setShowSystemTrayMenu(boolean showSystemTrayMenu) {
		MascotEventHandler.showSystemTrayMenu = showSystemTrayMenu;
	}

	public static boolean isShowSystemTrayMenu() {
		return showSystemTrayMenu;
	}

	private final Mascot mascot;

	public MascotEventHandler(Mascot mascot) {
		this.mascot = mascot;
	}

	public void mousePressed(final MouseEvent event) {

		// Beim Drücken der Maustaste zur Ziehanimation wechseln
		if (getMascot().getBehavior() != null) {
			try {
				getMascot().getBehavior().mousePressed(event);
			} catch (final CantBeAliveException e) {
				log.log(Level.SEVERE, "Maskottchen kann in dieser Situation nicht weiterlaufen", e);
				getMascot().dispose();
			}
		}

	}

	public void mouseReleased(final MouseEvent event) {

		if (event.isPopupTrigger()) {
			SwingUtilities.invokeLater(new Runnable(){
				@Override
				public void run() {
					showPopup(event.getX(), event.getY());
				}
			});
		} else {
			if (getMascot().getBehavior() != null) {
				try {
					getMascot().getBehavior().mouseReleased(event);
				} catch (final CantBeAliveException e) {
					log.log(Level.SEVERE, "Maskottchen kann in dieser Situation nicht weiterlaufen", e);
					getMascot().dispose();
				}
			}
		}

	}

	private void showPopup(final int x, final int y) {
		final JPopupMenu popup = new JPopupMenu();

		popup.addPopupMenuListener(new PopupMenuListener() {
			@Override
			public void popupMenuCanceled(final PopupMenuEvent e) {
			}

			@Override
			public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
				getMascot().setAnimating(true);
			}

			@Override
			public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
				getMascot().setAnimating(false);
			}
		});

		final JMenuItem disposeMenu = new JMenuItem("Beenden");
		disposeMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				getMascot().dispose();
			}
		});

		popup.add(disposeMenu);

		popup.add(new JSeparator());

		final JMenuItem openWindowConfMenu = new JMenuItem("Einstellungen: window.conf öffnen");
		final JMenuItem openSettingsGuiMenu = new JMenuItem("Einstellungen öffnen");
		openSettingsGuiMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Main.getInstance().launchSettingsGui();
			}
		});

		openWindowConfMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Main.getInstance().openPath(System.getProperty("user.dir") + java.io.File.separator + "window.conf");
			}
		});

		final JMenuItem openTitlesConfMenu = new JMenuItem("Einstellungen: titles.conf öffnen");
		openTitlesConfMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Main.getInstance().openPath(System.getProperty("user.dir") + java.io.File.separator + "titles.conf");
			}
		});

		final JMenuItem openAppFolderMenu = new JMenuItem("Einstellungen: Programmordner öffnen");
		openAppFolderMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Main.getInstance().openPath(System.getProperty("user.dir"));
			}
		});

		final JMenuItem resetWindowConfMenu = new JMenuItem("Einstellungen: window.conf zurücksetzen");
		resetWindowConfMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e) {
				Main.getInstance().resetWindowConf();
			}
		});

		popup.add(openSettingsGuiMenu);
		popup.add(openWindowConfMenu);
		popup.add(openTitlesConfMenu);
		popup.add(openAppFolderMenu);
		popup.add(resetWindowConfMenu);

		if (MascotEventHandler.isShowSystemTrayMenu()) {

			popup.add(new JSeparator());

			// Menüeintrag „Maskottchen hinzufügen“
			final JMenuItem increaseMenu = new JMenuItem("Maskottchen hinzufügen");
			increaseMenu.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent event) {
					Main.getInstance().createMascot();
				}
			});

			// Menüeintrag „Versammeln“
			final JMenuItem gatherMenu = new JMenuItem("Versammeln!");
			gatherMenu.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent event) {
					getMascot().getManager().setBehaviorAll(Main.getInstance().getConfiguration(), Main.BEHAVIOR_GATHER);
				}
			});

			// Menüeintrag „Nur eines behalten“
			final JMenuItem oneMenu = new JMenuItem("Nur eines behalten");
			oneMenu.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent event) {
					getMascot().getManager().remainOne();
				}
			});

			// Menüeintrag „IE-Fenster wiederherstellen“
		/*	final JMenuItem restoreMenu = new JMenuItem("IE-Fenster wiederherstellen");
			restoreMenu.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent event) {
					NativeFactory.getInstance().getEnvironment().restoreIE();
				}
			}); */

			// Menüeintrag „Alle entfernen“
			final JMenuItem closeMenu = new JMenuItem("Beenden");
			closeMenu.addActionListener(new ActionListener() {
				public void actionPerformed(final ActionEvent e) {
					Main.getInstance().exit();
				}
			});

			popup.add(increaseMenu);
			popup.add(gatherMenu);
			popup.add(oneMenu);
	//		popup.add(restoreMenu);
			popup.add(new JSeparator());
			popup.add(closeMenu);
		}

		popup.show(getMascot().getWindow().asJWindow(), x, y);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO: Automatisch erzeugter Methodenrumpf

	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO: Automatisch erzeugter Methodenrumpf

	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO: Automatisch erzeugter Methodenrumpf

	}

	private Mascot getMascot() {
		return mascot;
	}

}
