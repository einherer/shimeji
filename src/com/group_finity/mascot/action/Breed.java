package com.group_finity.mascot.action;

import java.awt.Point;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.group_finity.mascot.Main;
import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.BehaviorInstantiationException;
import com.group_finity.mascot.exception.CantBeAliveException;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

/**
 * Aktion zum Vermehren.
 * 
 * @author Yuki Yamada
 */
public class Breed extends Animate {

	private static final Logger log = Logger.getLogger(Breed.class.getName());

	public static final String PARAMETER_BORNX = "生まれる場所X";

	private static final int DEFAULT_BORNX = 0;

	public static final String PARAMETER_BORNY = "生まれる場所Y";

	private static final int DEFAULT_BORNY = 0;

	public static final String PARAMETER_BORNBEHAVIOR = "生まれた時の行動";

	private static final String DEFAULT_BORNBEHAVIOR = "";

	public Breed(final List<Animation> animations, final VariableMap params) {
		super(animations, params);
	}

	@Override
	protected void tick() throws LostGroundException, VariableException {

		super.tick();

		if (getTime() == getAnimation().getDuration() - 1) {
			// Vermehren
			breed();
		}
	}

	private void breed() throws VariableException {
		if (!Main.getInstance().isSelfCloningEnabled()) {
			log.log(Level.INFO, "Selbstvermehrung deaktiviert; Vermehrung von {0} wird übersprungen", getMascot());
			return;
		}

		// Ein Maskottchen erstellen
		final Mascot mascot = new Mascot();

		log.log(Level.INFO, "Vermehrung ({0},{1},{2})", new Object[] { getMascot(), this, mascot });

		// Außerhalb des sichtbaren Bereichs beginnen
		if (getMascot().isLookRight()) {
			mascot.setAnchor(new Point(getMascot().getAnchor().x - getBornX(), getMascot().getAnchor().y
					+ getBornY().intValue()));
		} else {
			mascot.setAnchor(new Point(getMascot().getAnchor().x + getBornX(), getMascot().getAnchor().y
					+ getBornY().intValue()));
		}
		mascot.setLookRight(getMascot().isLookRight());

		try {
			mascot.setBehavior(Main.getInstance().getConfiguration().buildBehavior(getBornBehavior()));

			getMascot().getManager().add(mascot);
		
		} catch (final BehaviorInstantiationException e) {
			log.log(Level.SEVERE, "Verhalten nach der Vermehrung konnte nicht initialisiert werden", e);
			mascot.dispose();
		} catch (final CantBeAliveException e) {
			log.log(Level.SEVERE, "Maskottchen kann in dieser Situation nicht weiterlaufen", e);
			mascot.dispose();
		}
	}

	private Number getBornY() throws VariableException {
		return eval(PARAMETER_BORNY, Number.class, DEFAULT_BORNY);
	}

	private int getBornX() throws VariableException {
		return eval(PARAMETER_BORNX, Number.class, DEFAULT_BORNX).intValue();
	}

	private String getBornBehavior() throws VariableException {
		return eval(PARAMETER_BORNBEHAVIOR, String.class, DEFAULT_BORNBEHAVIOR);
	}

}
