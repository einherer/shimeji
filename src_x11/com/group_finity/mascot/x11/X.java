/* Copyright (c) 2008 Stefan Endrullis, All Rights Reserved
 *
 * Diese Bibliothek ist freie Software; Sie dürfen sie weitergeben und/oder
 * unter den Bedingungen der GNU Lesser General Public
 * License, wie von der Free Software Foundation veröffentlicht, entweder
 * Version 2.1 der Lizenz oder nach Ihrer Wahl einer späteren Version.
 * <p/>
 * Diese Bibliothek wird in der Hoffnung verteilt, dass sie nützlich ist,
 * jedoch OHNE JEGLICHE GEWÄHRLEISTUNG, auch ohne die stillschweigende Gewährleistung der
 * MARKTGÄNGIGKEIT oder EIGNUNG FÜR EINEN BESTIMMTEN ZWECK. Die GNU
 * Weitere Einzelheiten enthält die GNU Lesser General Public License.
 */
package jnacontrib.x11.api;

import com.sun.jna.Native;
import com.sun.jna.NativeLong;
import com.sun.jna.Pointer;
import com.sun.jna.Memory;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.NativeLongByReference;
import com.sun.jna.ptr.PointerByReference;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.ArrayList;
import java.awt.Rectangle;

import com.sun.jna.platform.unix.X11;
import com.sun.jna.platform.unix.X11.Atom;

/**
 * Objektorientiertes X-Fenstersystem.
 * <p/>
 * Einige Codeabschnitte dieser Klasse basieren auf dem Programm
 * wmctrl von Tomas Styblo &lt;tripie@cpan.org&gt; (unter GPLv2 lizenziert).
 * Vielen Dank, Tomas!
 *
 * @author Stefan Endrullis
 */
public class X {
    /** Eigenschaft entfernen oder zurücksetzen. */
    public static final int _NET_WM_STATE_REMOVE = 0;
    /** Eigenschaft hinzufügen oder setzen. */
    public static final int _NET_WM_STATE_ADD = 1;
    /** Eigenschaft umschalten. */
    public static final int _NET_WM_STATE_TOGGLE = 2;
    /** Maximale Länge eines Eigenschaftswerts. */
    public static final int MAX_PROPERTY_VALUE_LEN = 4096;

    private static final X11 x11 = X11.INSTANCE;

    private static int bytesToInt(byte[] prop) {
        return ((prop[3] & 0xff) << 24)
            | ((prop[2] & 0xff) << 16)
            | ((prop[1] & 0xff) << 8)
            | ((prop[0] & 0xff));
    }

    private static int bytesToInt(byte[] prop, int offset) {
        return ((prop[3 + offset] & 0xff) << 24)
            | ((prop[2 + offset] & 0xff) << 16)
            | ((prop[1 + offset] & 0xff) << 8)
            | ((prop[offset] & 0xff));
    }


    /**
     * X-Anzeige.
     */
    public static class Display {
        /**
         * Anzeige Öffnen.
         */
        private X11.Display x11Display;
        /**
         * HashMap<String,X11.Atom>.
         */
        private HashMap<String, Atom> atomsHash = new HashMap<String, Atom>();

        /**
         * Erstellt OOWindowUtils mit der Standardanzeige.
         */
        public Display() {
            x11Display = x11.XOpenDisplay(null);

            if (x11Display == null) {
                throw new Error("X-Anzeige kann nicht geöffnet werden");
            }
        }

        /**
         * Erstellt OOWindowUtils mit der angegebenen Anzeige.
         *
         * @param x11Display Geöffnete Anzeige
         */
        public Display(X11.Display x11Display) {
            this.x11Display = x11Display;

            if (x11Display == null) {
                throw new Error("X-Anzeige ist null");
            }
        }

		public void getStackingOrder() {
			X11.WindowByReference root = new X11.WindowByReference();
			X11.WindowByReference parent = new X11.WindowByReference();
			PointerByReference children = new PointerByReference();
			IntByReference childCount = new IntByReference();
			x11.XQueryTree(this.getX11Display(), this.getRootWindow().getX11Window(), root, parent, children, childCount);
			Pointer ret_prop = children.getValue();
			int ret_nitems = childCount.getValue();

			int length = Math.min(ret_nitems * 8, MAX_PROPERTY_VALUE_LEN);
			System.out.println(ret_prop);
			byte[] ret = ret_prop.getByteArray(0, length);
			//System.out.println(root);
			//System.out.println(parent);
			//System.out.println(childCount);
			for (byte b : ret) {
				System.out.println(b);
			}
		}

        /**
         * Schließt die Anzeige.
         */
        public void close() {
            x11.XCloseDisplay(x11Display);
        }

        /**
         * Leert den Ausgabepuffer und die Ereigniswarteschlange.
         */
        public void flush() {
            x11.XFlush(x11Display);
        }

        /**
         * Gibt die X11-Anzeige zurück.
         *
         * @return X11-Anzeige
         */
        public X11.Display getX11Display() {
            return x11Display;
        }

        /**
         * Interne Atome anhand ihres Namens abrufen.
         *
         * @param name Name des Atoms
         * @return Atom
         */
        public X11.Atom getAtom(String name) {
            X11.Atom atom = atomsHash.get(name);
            if (atom == null) {
                atom = x11.XInternAtom(x11Display, name, false);
                atomsHash.put(name, atom);
            }
            return atom;
        }

        /**
         * Gibt Informationen zum Fenstermanager als Fenster zurück.
         *
         * @return Informationen zum Fenstermanager als Fenster
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public Window getWindowManagerInfo() throws X11Exception {
            Window rootWindow = getRootWindow();

            try {
                return rootWindow.getWindowProperty(X11.XA_WINDOW, "_NET_SUPPORTING_WM_CHECK");
            } catch (X11Exception e) {
                try {
                    return rootWindow.getWindowProperty(X11.XA_CARDINAL, "_WIN_SUPPORTING_WM_CHECK");
                } catch (X11Exception e1) {
                    throw new X11Exception("Informationen zum Fenstermanager können nicht abgerufen werden (_NET_SUPPORTING_WM_CHECK oder _WIN_SUPPORTING_WM_CHECK)");
                }
            }
        }

        /**
         * Gibt das Wurzelfenster zurück.
         *
         * @return Wurzelfenster
         */
        public Window getRootWindow() {
            return new Window(this, x11.XDefaultRootWindow(x11Display));
        }

        /**
         * Gibt das aktuell aktive Fenster zurück.
         *
         * @return Aktuell aktives Fenster
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public Window getActiveWindow() throws X11Exception {
            return getRootWindow().getWindowProperty(X11.XA_WINDOW, "_NET_ACTIVE_WINDOW");
        }

        /**
         * Gibt alle vom Fenstermanager verwalteten Fenster zurück.
         *
         * @return Alle vom Fenstermanager verwalteten Fenster
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public Window[] getWindows() throws X11Exception {
            byte[] bytes;
            Window rootWindow = getRootWindow();

            try {
                bytes = rootWindow.getProperty(X11.XA_WINDOW, "_NET_CLIENT_LIST_STACKING");
            } catch (X11Exception e) {
                try {
                    bytes = rootWindow.getProperty(X11.XA_CARDINAL, "_WIN_CLIENT_LIST");
                } catch (X11Exception e1) {
                    throw new X11Exception("Fensterliste kann nicht abgerufen werden (_NET_CLIENT_LIST oder _WIN_CLIENT_LIST)");
                }
            }

            Window[] windowList = new Window[bytes.length / X11.Window.SIZE];

            for (int i = 0; i < windowList.length; i++) {
                windowList[i] = new Window(this, new X11.Window(bytesToInt(bytes, X11.XID.SIZE * i)));
            }

            return windowList;
        }

        /**
         * Gibt die Anzahl der Arbeitsflächen zurück.
         *
         * @return Anzahl der Arbeitsflächen
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public int getDesktopCount() throws X11Exception {
            Window root = getRootWindow();

            try {
                return root.getIntProperty(X11.XA_CARDINAL, "_NET_NUMBER_OF_DESKTOPS");
            } catch (X11Exception e) {
                try {
                    return root.getIntProperty(X11.XA_CARDINAL, "_WIN_WORKSPACE_COUNT");
                } catch (X11Exception e1) {
                    throw new X11Exception("Anzahl der Arbeitsflächen kann nicht abgerufen werden (_NET_NUMBER_OF_DESKTOPS oder _WIN_WORKSPACE_COUNT)");
                }
            }
        }

        /**
         * Gibt die Nummer der aktiven Arbeitsfläche zurück.
         *
         * @return Nummer der aktiven Arbeitsfläche
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public int getActiveDesktopNumber() throws X11Exception {
            Window root = getRootWindow();
            int cur_desktop;

            try {
                cur_desktop = root.getIntProperty(X11.XA_CARDINAL, "_NET_CURRENT_DESKTOP");
            } catch (X11Exception e) {
                try {
                    cur_desktop = root.getIntProperty(X11.XA_CARDINAL, "_WIN_WORKSPACE");
                } catch (X11Exception e1) {
                    throw new X11Exception("Aktuelle Arbeitsfläche kann nicht abgerufen werden (_NET_CURRENT_DESKTOP oder _WIN_WORKSPACE)");
                }
            }

            return cur_desktop;
        }

        /**
         * Gibt die verfügbaren Arbeitsflächen zurück.
         *
         * @return Verfügbare Arbeitsflächen
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public Desktop[] getDesktops() throws X11Exception {
            Window root = getRootWindow();
            String[] desktopNames;
            try {
                desktopNames = root.getUtf8ListProperty(getAtom("UTF8_STRING"), "_NET_DESKTOP_NAMES");
            } catch (X11Exception e) {
                try {
                    desktopNames = root.getStringListProperty(X11.XA_STRING, "_WIN_WORKSPACE_NAMES");
                } catch (X11Exception e1) {
                    throw new X11Exception("Namen der Arbeitsflächen können nicht abgerufen werden (_NET_DESKTOP_NAMES oder _WIN_WORKSPACE_NAMES)");
                }
            }

            Desktop[] desktops = new Desktop[getDesktopCount()];
            for (int i = 0; i < desktops.length; i++) {
                desktops[i] = new Desktop(this, i, desktopNames[i]);
            }

            return desktops;
        }

        /**
         * Wechselt zur angegebenen Arbeitsfläche.
         *
         * @param nr Nummer der Arbeitsfläche
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public void switchDesktop(int nr) throws X11Exception {
            getRootWindow().clientMsg("_NET_CURRENT_DESKTOP", nr, 0, 0, 0, 0);
        }

        /**
         * Legt fest, ob die Arbeitsfläche angezeigt wird.
         *
         * @param state true, wenn die Arbeitsfläche angezeigt werden soll
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public void showingDesktop(boolean state) throws X11Exception {
            getRootWindow().clientMsg("_NET_SHOWING_DESKTOP", state ? 1 : 0, 0, 0, 0, 0);
        }

        /**
         * Aktiviert oder deaktiviert die automatische Wiederholung gedrückter Tasten.
         *
         * @param on true, wenn die automatische Tastenwiederholung aktiviert werden soll
         */
        public void setKeyAutoRepeat(boolean on) {
           if (on) {
               x11.XAutoRepeatOn(x11Display);
           } else {
               x11.XAutoRepeatOff(x11Display);
           }
        }

        /**
         * Gibt das zum Tastennamen gehörende Tastensymbol zurück.
         *
         * @param keyName Name der Taste
         * @return Tastensymbol
         */
        public X11.KeySym getKeySym(String keyName) {
            return x11.XStringToKeysym(keyName);
        }

        /**
         * Gibt das zum Tastencode gehörende Tastensymbol zurück.
         *
         * @param keyCode Tastencode
         * @param index Element des Tastencode-Vektors
         * @return Tastensymbol
         */
        public X11.KeySym getKeySym(byte keyCode, int index) {
            return x11.XKeycodeToKeysym(x11Display, keyCode, index);
        }

        /**
         * Gibt den zum Tastensymbol gehörenden Tastencode zurück.
         *
         * @param keySym Tastensymbol
         * @return Tastencode
         */
        public byte getKeyCode(X11.KeySym keySym) {
            return x11.XKeysymToKeycode(x11Display,  keySym);
        }

        /**
         * Gibt den zum Tastennamen gehörenden Tastencode zurück.
         *
         * @param keyName Name der Taste
         * @return Tastencode
         */
        public byte getKeyCode(String keyName) {
            return x11.XKeysymToKeycode(x11Display,  getKeySym(keyName));
        }

        /**
         * Gibt den zum Tastensymbol gehörenden Tastennamen zurück.
         *
         * @param keySym Tastensymbol
         * @return Name der Taste
         */
        public String getKeyName(X11.KeySym keySym) {
            return x11.XKeysymToString(keySym);
        }

        /**
         * Gibt den zum Tastencode und zum Index im Tastencode-Vektor gehörenden Tastennamen zurück.
         *
         * @param keyCode Tastencode
         * @param index Index im Tastencode-Vektor
         * @return Name der Taste
         */
        public String getKeyName(byte keyCode, int index) {
            return getKeyName(getKeySym(keyCode, index));
        }

        /**
         * Gibt die Belegung der Modifikatortasten zurück.
         *
         * @return Belegung der Modifikatortasten
         */
        public ModifierKeymap getModifierKeymap() {
            X11.XModifierKeymapRef xModifierKeymapRef = x11.XGetModifierMapping(x11Display);
            ModifierKeymap modifierKeymap = new ModifierKeymap(xModifierKeymapRef);
            x11.XFreeModifiermap(xModifierKeymapRef);
            return modifierKeymap;
        }

        /**
         * Legt die Belegung der Modifikatortasten fest.
         *
         * @param modifierKeymap Belegung der Modifikatortasten
         */
        public void setModifierKeymap(ModifierKeymap modifierKeymap) {
            X11.XModifierKeymapRef xModifierKeymapRef = modifierKeymap.toXModifierKeyamp();
            x11.XSetModifierMapping(x11Display,xModifierKeymapRef);
        }
    }


    /**
     * Modifikatortastenbelegung. Die Listen shift, lock, control, mod1, mod2, mod3, mod4, mod5
     * enthalten die Tastencodes als Byte-Objekte. Die Listen können direkt verwendet werden, um
     * Tastencodes dieser Modifikatoren zu lesen, zu ersetzen, zu entfernen oder einzufügen.
     * Zum Anwenden einer neuen Modifikatortastenbelegung
     * {@link X.Display#setModifierKeymap(ModifierKeymap)}.
     */
    public static class ModifierKeymap {
        /** Shift-Modifikator als ArrayList&lt;Byte&gt;. */
        public ArrayList<Byte> shift = new ArrayList<Byte>(4);
        /** Lock-Modifikator als ArrayList&lt;Byte&gt;. */
        public ArrayList<Byte> lock = new ArrayList<Byte>(4);
        /** Control-Modifikator als ArrayList&lt;Byte&gt;. */
        public ArrayList<Byte> control = new ArrayList<Byte>(4);
        /** Mod1-Modifikator als ArrayList&lt;Byte&gt;. */
        public ArrayList<Byte> mod1 = new ArrayList<Byte>(4);
        /** Mod2-Modifikator als ArrayList&lt;Byte&gt;. */
        public ArrayList<Byte> mod2 = new ArrayList<Byte>(4);
        /** Mod3-Modifikator als ArrayList&lt;Byte&gt;. */
        public ArrayList<Byte> mod3 = new ArrayList<Byte>(4);
        /** Mod4-Modifikator als ArrayList&lt;Byte&gt;. */
        public ArrayList<Byte> mod4 = new ArrayList<Byte>(4);
        /** Mod5-Modifikator als ArrayList&lt;Byte&gt;. */
        public ArrayList<Byte> mod5 = new ArrayList<Byte>(4);

        /**
         * Erstellt eine leere Modifikatortastenbelegung.
         */
        public ModifierKeymap() {
        }

        /**
         * Erstellt eine Modifikatortastenbelegung und liest die Modifikatoren aus der XModifierKeymap.
         *
         * @param xModifierKeymapRef XModifierKeymap
         */
        public ModifierKeymap(X11.XModifierKeymapRef xModifierKeymapRef) {
            fromXModifierKeymap(xModifierKeymapRef);
        }

        /**
         * Liest alle Modifikatoren aus der XModifierKeymap.
         *
         * @param xModifierKeymapRef XModifierKeymap
         */
        public void fromXModifierKeymap(X11.XModifierKeymapRef xModifierKeymapRef) {
            int count = xModifierKeymapRef.max_keypermod;
            byte[] keys = xModifierKeymapRef.modifiermap.getByteArray(0, 8*count);

            ArrayList<Byte>[] allModifiers = getAllModifiers();

            for (int modNr = 0; modNr < 8; modNr++) {
                ArrayList<Byte> modifier = allModifiers[modNr];
                modifier.clear();

                for (int keyNr = 0; keyNr < count; keyNr++) {
                    byte key = keys[modNr*count + keyNr];
                    if (key != 0) {
                        modifier.add(new Byte(key));
                    }
                }
            }
        }

        /**
         * Gibt eine zu diesem Objekt passende XModifierKeymap zurück.
         *
         * @return XModifierKeymap
         */
        public X11.XModifierKeymapRef toXModifierKeyamp() {
            ArrayList<Byte>[] allModifiers = getAllModifiers();

            // Maximale Listengröße ermitteln
            int count = 0;
            for (int i = 0; i < allModifiers.length; i++) {
                count = Math.max(count, allModifiers[i].size());
            }

            byte[] keys = new byte[8*count];
            for (int modNr = 0; modNr < 8; modNr++) {
                ArrayList<Byte> modifier = allModifiers[modNr];

                for (int keyNr = 0; keyNr < modifier.size(); keyNr++) {
                    keys[modNr*count + keyNr] = modifier.get(keyNr).byteValue();
                }
            }

            X11.XModifierKeymapRef xModifierKeymapRef = new X11.XModifierKeymapRef();
            xModifierKeymapRef.max_keypermod = count;
            xModifierKeymapRef.modifiermap = new Memory(keys.length);
            xModifierKeymapRef.modifiermap.write(0, keys, 0, keys.length);

            return xModifierKeymapRef;
        }

        /**
         * Gibt alle Modifikatoren als Array zurück.
         *
         * @return Array der Modifikatorlisten
         */
        public ArrayList<Byte>[] getAllModifiers() {        	
            return new ArrayList[] {
                    shift, lock, control, mod1, mod2, mod3, mod4, mod5
            };
        }
    }

    /**
     * X-Arbeitsfläche.
     */
    public static class Desktop {
        public X.Display display;
        public int number;
        public String name;

        public Desktop(Display display, int number, String name) {
            this.display = display;
            this.number = number;
            this.name = name;
        }
    }


    /**
     * X-Fenster.
     */
    public static class Window {
        private X.Display display;
        private X11.Window x11Window;

        /**
         * Gibt das X11-Fensterobjekt zurück.
         *
         * @return X11-Fenster
         */
        public X11.Window getX11Window() {
            return x11Window;
        }

        /**
         * Gibt die ID des Fensters zurück.
         *
         * @return Fenster-ID
         */
        public int getID() {
            return x11Window.intValue();
        }

        /**
         * Erstellt das Fenster.
         *
         * @param display Anzeige, der das Fenster zugeordnet ist
         * @param x11Window X11-Fenster
         */
        public Window(X.Display display, X11.Window x11Window) {
            this.display = display;
            this.x11Window = x11Window;
        }

        /**
         * Gibt den Fenstertitel zurück.
         *
         * @return Fenstertitel
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public String getTitle() throws X11Exception {
            try {
                return getUtf8Property(display.getAtom("UTF8_STRING"), "_NET_WM_NAME");
            } catch (X11Exception e) {
                return getUtf8Property(X11.XA_STRING, X11.XA_WM_NAME);
            }

        }
        		
		public int getState() throws X11Exception {
			try {
				return getIntProperty(display.getAtom("ATOM"), "_NET_WM_STATE");
			} catch (Exception e) {
				return 0;
			}
		}

		/**
		 * Gibt den Fenstertyp zurück.
		 * 
		 * @return Atome des Fenstertyps
		 * @throws X11Exception thrown if X11 window errors occured
		 *
		 * 
		 */
		public int getType() throws X11Exception {
			try {
				return getIntProperty(display.getAtom("ATOM"), "_NET_WM_WINDOW_TYPE");
			} catch (Exception e) {
				return 0;
			}
		}

        /**
         * Gibt die Fensterklasse zurück.
         *
         * @return Fensterklasse
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public String getWindowClass() throws X11Exception {
            return getUtf8Property(X11.XA_STRING, X11.XA_WM_CLASS);
        }

        /**
         * Gibt die Prozess-ID des Fensters zurück.
         *
         * @return Prozess-ID des Fensters
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public int getPID() throws X11Exception {
            return getIntProperty(X11.XA_CARDINAL, "_NET_WM_PID");
        }

        /**
         * Gibt die Arbeitsflächen-ID des Fensters zurück.
         *
         * @return Arbeitsflächen-ID des Fensters
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public int getDesktop() throws X11Exception {
            try {
                return getIntProperty(X11.XA_CARDINAL, "_NET_WM_DESKTOP");
            } catch (X11Exception e) {
                return getIntProperty(X11.XA_CARDINAL, "_WIN_WORKSPACE");
            }
        }

        /**
         * Gibt den Rechnernamen des Fenster-Clients zurück.
         *
         * @return Rechnername des Fenster-Clients
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public String getMachine() throws X11Exception {
            return getStringProperty(X11.XA_STRING, "WM_CLIENT_MACHINE");
        }
        
        /**
         * Gibt die XWindowAttributes des Fensters zurück.
         *
         * @return XWindowAttributes des Fensters
         */
        public X11.XWindowAttributes getXWindowAttributes() {
            X11.XWindowAttributes xwa = new X11.XWindowAttributes();
            x11.XGetWindowAttributes(display.x11Display, x11Window, xwa);

            return xwa;
        }

        /**
         * Gibt die Geometrie des Fensters zurück.
         *
         * @return Geometrie des Fensters
         */
        public Geometry getGeometry() {
            X11.WindowByReference junkRoot = new X11.WindowByReference();
            IntByReference junkX = new IntByReference();
            IntByReference junkY = new IntByReference();
            IntByReference x = new IntByReference();
            IntByReference y = new IntByReference();
            IntByReference width = new IntByReference();
            IntByReference height = new IntByReference();
            IntByReference borderWidth = new IntByReference();
            IntByReference depth = new IntByReference();

            x11.XGetGeometry(display.x11Display, x11Window, junkRoot, junkX, junkY, width, height, borderWidth, depth);

            x11.XTranslateCoordinates(display.x11Display, x11Window, junkRoot.getValue(), junkX.getValue(),
                junkY.getValue(), x, y, junkRoot);

            return new Geometry(x.getValue(), y.getValue(), width.getValue(), height.getValue(),
                borderWidth.getValue(), depth.getValue());
        }

        /**
         * Gibt den Begrenzungsrahmen des Fensters zurück.
         *
         * @return Begrenzungsrahmen des Fensters
         */
        public Rectangle getBounds() {
            X11.WindowByReference junkRoot = new X11.WindowByReference();
            IntByReference junkX = new IntByReference();
            IntByReference junkY = new IntByReference();
            IntByReference x = new IntByReference();
            IntByReference y = new IntByReference();
            IntByReference width = new IntByReference();
            IntByReference height = new IntByReference();
            IntByReference border_width = new IntByReference();
            IntByReference depth = new IntByReference();

            x11.XGetGeometry(display.x11Display, x11Window, junkRoot, junkX, junkY, width, height, border_width, depth);

            x11.XTranslateCoordinates(display.x11Display, x11Window, junkRoot.getValue(), junkX.getValue(),
                junkY.getValue(), x, y, junkRoot);

            int xVal = x.getValue();
            int yVal = y.getValue();

            int left = 0;
            int right = 0;
            int top = 0;
            int bottom = 0;

            try {
                byte[] extents = getProperty(X11.XA_CARDINAL, "_NET_FRAME_EXTENTS");
                if (extents != null && extents.length >= 16) {
                    left = bytesToInt(extents, 0);
                    right = bytesToInt(extents, 4);
                    top = bytesToInt(extents, 8);
                    bottom = bytesToInt(extents, 12);
                }
            } catch (X11Exception e) {
                // Auf die Client-Geometrie zurückgreifen, wenn der Fenstermanager keine Rahmenmaße liefert.
            }

            return new Rectangle(
                xVal - left,
                yVal - top,
                width.getValue() + left + right,
                height.getValue() + top + bottom
            );
        }

        /**
         * Aktiviert das Fenster.
         *
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public void activate() throws X11Exception {
            clientMsg("_NET_ACTIVE_WINDOW", 0, 0, 0, 0, 0);
            x11.XMapRaised(display.x11Display, x11Window);
        }

        /**
         * Verschiebt das Fenster auf die angegebene Arbeitsfläche.
         *
         * @param desktopNr Arbeitsfläche
         * @return X11.SUCCESS, wenn das Schließen erfolgreich war
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public int moveToDesktop(int desktopNr) throws X11Exception {
            return clientMsg("_NET_WM_DESKTOP", desktopNr, 0, 0, 0, 0);
        }

        /**
         * Wählt die zu überwachenden Eingabeereignisse aus.
         *
         * @param eventMask Ereignismaske für die zu überwachenden Ereignisse
         */
        public void selectInput(int eventMask) {
            x11.XSelectInput(display.x11Display, x11Window, new NativeLong(eventMask));
        }

        public int nextEvent(X11.XEvent event) {
            return x11.XNextEvent(display.x11Display, event);
        }

        public void sendEvent(int eventMask, X11.XEvent event) {
            x11.XSendEvent(display.x11Display, x11Window, 1, new NativeLong(eventMask), event);
        }

        /**
         * Schließt das Fenster ordnungsgemäß.
         *
         * @return X11.SUCCESS, wenn das Schließen erfolgreich war
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public int close() throws X11Exception {
            return clientMsg("_NET_CLOSE_WINDOW", 0, 0, 0, 0, 0);
        }

        /**
         * Gibt den Eigenschaftswert als Ganzzahl zurück.
         *
         * @param xa_prop_type Typ der Eigenschaft
         * @param xa_prop_name Name der Eigenschaft
         * @return Eigenschaftswert als Ganzzahl
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public int getIntProperty(X11.Atom xa_prop_type, X11.Atom xa_prop_name) throws X11Exception {
            return bytesToInt(getProperty(xa_prop_type, xa_prop_name));
        }

        /**
         * Gibt den Eigenschaftswert als Ganzzahl zurück.
         *
         * @param xa_prop_type Typ der Eigenschaft
         * @param xa_prop_name Name der Eigenschaft
         * @return Eigenschaftswert als Ganzzahl
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public int getIntProperty(X11.Atom xa_prop_type, String xa_prop_name) throws X11Exception {
            return getIntProperty(xa_prop_type, display.getAtom(xa_prop_name));
        }

        /**
         * Gibt den Eigenschaftswert als Fenster zurück.
         *
         * @param xa_prop_type Typ der Eigenschaft
         * @param xa_prop_name Name der Eigenschaft
         * @return Eigenschaftswert als Fenster
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public Window getWindowProperty(X11.Atom xa_prop_type, X11.Atom xa_prop_name) throws X11Exception {
            int windowId = getIntProperty(xa_prop_type, xa_prop_name);
            X11.Window x11Window = new X11.Window(windowId);
            return new Window(display, x11Window);
        }

        /**
         * Gibt den Eigenschaftswert als Fenster zurück.
         *
         * @param xa_prop_type Typ der Eigenschaft
         * @param xa_prop_name Name der Eigenschaft
         * @return Eigenschaftswert als Fenster
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public Window getWindowProperty(X11.Atom xa_prop_type, String xa_prop_name) throws X11Exception {
            return getWindowProperty(xa_prop_type, display.getAtom(xa_prop_name));
        }

        /**
         * Gibt den Eigenschaftswert als nullterminiertes Byte-Array zurück.
         *
         * @param xa_prop_type Typ der Eigenschaft
         * @param xa_prop_name Name der Eigenschaft
         * @return Eigenschaftswert als nullterminiertes Byte-Array
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public byte[] getNullTerminatedProperty(X11.Atom xa_prop_type, X11.Atom xa_prop_name) throws X11Exception {
            byte[] bytesOrig = getProperty(xa_prop_type, xa_prop_name);
            byte[] bytesDest;

            // Nach '\0' suchen
            int i;
            for (i = 0; i < bytesOrig.length; i++) {
                if (bytesOrig[i] == '\0') break;
            }

            if (i < bytesOrig.length - 1) {
                bytesDest = new byte[i + 1];
                System.arraycopy(bytesOrig, 0, bytesDest, 0, i + 1);
            } else {
                bytesDest = bytesOrig;
            }

            return bytesDest;
        }

        /**
         * Gibt den Eigenschaftswert als nullterminiertes Byte-Array zurück.
         *
         * @param xa_prop_type Typ der Eigenschaft
         * @param xa_prop_name Name der Eigenschaft
         * @return Eigenschaftswert als nullterminiertes Byte-Array
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public byte[] getNullTerminatedProperty(X11.Atom xa_prop_type, String xa_prop_name) throws X11Exception {
            return getNullTerminatedProperty(xa_prop_type, display.getAtom(xa_prop_name));
        }

        /**
         * Gibt den Eigenschaftswert als Byte-Array zurück, in dem jedes '\0' durch '.' ersetzt wird.
         *
         * @param xa_prop_type Typ der Eigenschaft
         * @param xa_prop_name Name der Eigenschaft
         * @return Eigenschaftswert als Byte-Array, in dem jedes '\0' durch '.' ersetzt wird
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public byte[] getNullReplacedStringProperty(X11.Atom xa_prop_type, X11.Atom xa_prop_name) throws X11Exception {
            byte[] bytes = getProperty(xa_prop_type, xa_prop_name);

            // Nach '\0' suchen
            int i;
            for (i = 0; i < bytes.length; i++) {
                if (bytes[i] == '\0') {
                    bytes[i] = '.';
                }
            }

            return bytes;
        }

        /**
         * Gibt den Eigenschaftswert als Byte-Array zurück, in dem jedes '\0' durch '.' ersetzt wird.
         *
         * @param xa_prop_type Typ der Eigenschaft
         * @param xa_prop_name Name der Eigenschaft
         * @return Eigenschaftswert als Byte-Array, in dem jedes '\0' durch '.' ersetzt wird
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public byte[] getNullReplacedStringProperty(X11.Atom xa_prop_type, String xa_prop_name) throws X11Exception {
            return getNullReplacedStringProperty(xa_prop_type, display.getAtom(xa_prop_name));
        }

        /**
         * Gibt den Eigenschaftswert als Zeichenkette zurück, in der jedes '\0' durch '.' ersetzt wird.
         *
         * @param xa_prop_type Typ der Eigenschaft
         * @param xa_prop_name Name der Eigenschaft
         * @return Eigenschaftswert als Zeichenkette, in der jedes '\0' durch '.' ersetzt wird
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public String getStringProperty(X11.Atom xa_prop_type, X11.Atom xa_prop_name) throws X11Exception {
            return new String(getNullReplacedStringProperty(xa_prop_type, xa_prop_name));
        }

        /**
         * Gibt den Eigenschaftswert als Zeichenkette zurück, in der jedes '\0' durch '.' ersetzt wird.
         *
         * @param xa_prop_type Typ der Eigenschaft
         * @param xa_prop_name Name der Eigenschaft
         * @return Eigenschaftswert als Zeichenkette, in der jedes '\0' durch '.' ersetzt wird
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public String getStringProperty(X11.Atom xa_prop_type, String xa_prop_name) throws X11Exception {
            return new String(getNullReplacedStringProperty(xa_prop_type, xa_prop_name));
        }

        /**
         * Gibt den Eigenschaftswert als Zeichenkettenliste zurück.
         *
         * @param xa_prop_type Typ der Eigenschaft
         * @param xa_prop_name Name der Eigenschaft
         * @return Eigenschaftswert als Zeichenkettenliste
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public String[] getStringListProperty(X11.Atom xa_prop_type, X11.Atom xa_prop_name) throws X11Exception {
            return new String(getProperty(xa_prop_type, xa_prop_name)).split("\0");
        }

        /**
         * Gibt den Eigenschaftswert als Zeichenkettenliste zurück.
         *
         * @param xa_prop_type Typ der Eigenschaft
         * @param xa_prop_name Name der Eigenschaft
         * @return Eigenschaftswert als Zeichenkettenliste
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public String[] getStringListProperty(X11.Atom xa_prop_type, String xa_prop_name) throws X11Exception {
            return new String(getProperty(xa_prop_type, xa_prop_name)).split("\0");
        }

        /**
         * Gibt den Eigenschaftswert als UTF-8-Zeichenkette zurück, in der jedes '\0' durch '.' ersetzt wird.
         *
         * @param xa_prop_type Typ der Eigenschaft
         * @param xa_prop_name Name der Eigenschaft
         * @return Eigenschaftswert als UTF-8-Zeichenkette, in der jedes '\0' durch '.' ersetzt wird
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public String getUtf8Property(X11.Atom xa_prop_type, X11.Atom xa_prop_name) throws X11Exception {
            try {
                return new String(getNullReplacedStringProperty(xa_prop_type, xa_prop_name), "UTF8");
            } catch (UnsupportedEncodingException e) {
                throw new X11Exception(e);
            }
        }

        /**
         * Gibt den Eigenschaftswert als UTF-8-Zeichenkette zurück, in der jedes '\0' durch '.' ersetzt wird.
         *
         * @param xa_prop_type Typ der Eigenschaft
         * @param xa_prop_name Name der Eigenschaft
         * @return Eigenschaftswert als UTF-8-Zeichenkette, in der jedes '\0' durch '.' ersetzt wird
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public String getUtf8Property(X11.Atom xa_prop_type, String xa_prop_name) throws X11Exception {
            return getUtf8Property(xa_prop_type, display.getAtom(xa_prop_name));
        }

        /**
         * Gibt den Eigenschaftswert als UTF-8-Zeichenkettenliste zurück.
         *
         * @param xa_prop_type Typ der Eigenschaft
         * @param xa_prop_name Name der Eigenschaft
         * @return Eigenschaftswert als UTF-8-Zeichenkettenliste
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public String[] getUtf8ListProperty(X11.Atom xa_prop_type, X11.Atom xa_prop_name) throws X11Exception {
            try {
                return new String(getProperty(xa_prop_type, xa_prop_name), "UTF8").split("\0");
            } catch (UnsupportedEncodingException e) {
                throw new X11Exception(e);
            }
        }

        /**
         * Gibt den Eigenschaftswert als UTF-8-Zeichenkettenliste zurück.
         *
         * @param xa_prop_type Typ der Eigenschaft
         * @param xa_prop_name Name der Eigenschaft
         * @return Eigenschaftswert als UTF-8-Zeichenkettenliste
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public String[] getUtf8ListProperty(X11.Atom xa_prop_type, String xa_prop_name) throws X11Exception {
            return getUtf8ListProperty(xa_prop_type, display.getAtom(xa_prop_name));
        }

        /**
         * Gibt den Eigenschaftswert als Byte-Array zurück.
         *
         * @param xa_prop_type Typ der Eigenschaft
         * @param xa_prop_name Name der Eigenschaft
         * @return Eigenschaftswert als Byte-Array
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public byte[] getProperty(X11.Atom xa_prop_type, X11.Atom xa_prop_name) throws X11Exception {
            X11.AtomByReference xa_ret_type_ref = new X11.AtomByReference();
            IntByReference ret_format_ref = new IntByReference();
            NativeLongByReference ret_nitems_ref = new NativeLongByReference();
            NativeLongByReference ret_bytes_after_ref = new NativeLongByReference();
            PointerByReference ret_prop_ref = new PointerByReference();

            NativeLong long_offset = new NativeLong(0);
            NativeLong long_length = new NativeLong(MAX_PROPERTY_VALUE_LEN / 4);

            /* Erläuterung zu MAX_PROPERTY_VALUE_LEN / 4 aus der Handbuchseite zu XGetWindowProperty:
                                    *
                                    * long_length = Gibt die Länge der in Vielfachen von 32 Bit
                                    *               abzurufenden Daten an.
                                    */
            if (x11.XGetWindowProperty(display.x11Display, x11Window, xa_prop_name, long_offset, long_length, false,
                xa_prop_type, xa_ret_type_ref, ret_format_ref,
                ret_nitems_ref, ret_bytes_after_ref, ret_prop_ref) != X11.Success) {
                String prop_name = x11.XGetAtomName(display.x11Display, xa_prop_name);
                throw new X11Exception("Abrufen fehlgeschlagen: " + prop_name + " (Eigenschaft).");
            }

            X11.Atom xa_ret_type = xa_ret_type_ref.getValue();
            Pointer ret_prop = ret_prop_ref.getValue();

            if (xa_ret_type == null || xa_prop_type == null ||
                !xa_ret_type.toNative().equals(xa_prop_type.toNative())) {
                x11.XFree(ret_prop);
                String prop_name = x11.XGetAtomName(display.x11Display, xa_prop_name);
                throw new X11Exception("Ungültiger Typ für " + prop_name + " (Eigenschaft)");
            }

            int ret_format = ret_format_ref.getValue();
            long ret_nitems = ret_nitems_ref.getValue().longValue();

            // Ergebnis nullterminieren, um die Verarbeitung als Zeichenkette zu erleichtern
            int nbytes;
            if (ret_format == 32)
                nbytes = Native.LONG_SIZE;
            else if (ret_format == 16)
                nbytes = Native.LONG_SIZE / 2;
            else if (ret_format == 8)
                nbytes = 1;
            else if (ret_format == 0)
                nbytes = 0;
            else
                throw new X11Exception("Ungültiges Rückgabeformat");
            int length = Math.min((int) ret_nitems * nbytes, MAX_PROPERTY_VALUE_LEN);

            byte[] ret = ret_prop.getByteArray(0, length);

            x11.XFree(ret_prop);
            return ret;
        }

        /**
         * Gibt den Eigenschaftswert als Byte-Array zurück.
         *
         * @param xa_prop_type Typ der Eigenschaft
         * @param xa_prop_name Name der Eigenschaft
         * @return Eigenschaftswert als Byte-Array
         * @throws X11Exception thrown if X11 window errors occurred
         */
        public byte[] getProperty(X11.Atom xa_prop_type, String xa_prop_name) throws X11Exception {
            return getProperty(xa_prop_type, display.getAtom(xa_prop_name));
        }

        public int clientMsg(String msg, int data0, int data1, int data2, int data3, int data4) throws X11Exception {
            return clientMsg(
                msg,
                new NativeLong(data0),
                new NativeLong(data1),
                new NativeLong(data2),
                new NativeLong(data3),
                new NativeLong(data4)
            );
        }

        public int clientMsg(String msg, NativeLong data0, NativeLong data1, NativeLong data2, NativeLong data3, NativeLong data4) throws X11Exception {
            X11.XClientMessageEvent event;
            NativeLong mask = new NativeLong(X11.SubstructureRedirectMask | X11.SubstructureNotifyMask);

            event = new X11.XClientMessageEvent();
            event.type = X11.ClientMessage;
            event.serial = new NativeLong(0);
            event.send_event = 1;
            event.message_type = display.getAtom(msg);
            event.window = x11Window;
            event.format = 32;
            event.data.setType(NativeLong[].class);
            event.data.l[0] = data0;
            event.data.l[1] = data1;
            event.data.l[2] = data2;
            event.data.l[3] = data3;
            event.data.l[4] = data4;

            X11.XEvent e = new X11.XEvent();
            e.setTypedValue(event);

            if (x11.XSendEvent(display.x11Display, display.getRootWindow().x11Window, 0, mask, e) != 0) {
                return X11.Success;
            } else {
                throw new X11Exception("Senden fehlgeschlagen: " + msg + " (Ereignis).");
            }
        }

        public String toString() {
            return x11Window.toString();
        }

        public static class Geometry {
            public int x, y, width, height, borderWidth, depth;

            public Geometry(int x, int y, int width, int height, int border_width, int depth) {
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;
                this.borderWidth = border_width;
                this.depth = depth;
            }
        }
    }

    /**
     * Allgemeine Ausnahme bei einem X11-Fensterfehler.
     */
    public static class X11Exception extends Exception {
		private static final long serialVersionUID = 1L;
        public X11Exception() {
        }

        public X11Exception(String message) {
            super(message);
        }

        public X11Exception(String message, Throwable cause) {
            super(message, cause);
        }

        public X11Exception(Throwable cause) {
            super(cause);
        }
    }
}
