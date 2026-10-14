package com.group_finity.mascot.action;

import java.awt.Point;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

/**
 * Aktion zum Bewegen.
 * @author Yuki Yamada
 */
public class Move extends BorderedAction {

	private static final Logger log = Logger.getLogger(Move.class.getName());

	private static final String PARAMETER_TARGETX = "目的地X";

	private static final int DEFAULT_TARGETX = Integer.MAX_VALUE;

	private static final String PARAMETER_TARGETY = "目的地Y";

	private static final int DEFAULT_TARGETY = Integer.MAX_VALUE;

	public Move(final List<Animation> animations, final VariableMap params) {
		super(animations, params);
	}

	@Override
	public boolean hasNext() throws VariableException {

		final int targetX = getTargetX();
		final int targetY = getTargetY();

		boolean noMoveX = false;
		boolean noMoveY = false;

		if (targetX != Integer.MIN_VALUE) {
			// Ist eine Bewegung in X-Richtung erforderlich?
			if (getMascot().getAnchor().x == targetX) {
				noMoveX = true;
			}
		}

		if (targetY != Integer.MIN_VALUE) {
			// Ist eine Bewegung in Y-Richtung erforderlich?
			if (getMascot().getAnchor().y == targetY) {
				noMoveY = true;
			}
		}

		return super.hasNext() && !noMoveX && !noMoveY;
	}

	@Override
	protected void tick() throws LostGroundException, VariableException {

		super.tick();

		if ((getBorder() != null) && !getBorder().isOn(getMascot().getAnchor())) {
			// Wandkontakt verloren
			log.log(Level.INFO, "Randkontakt verloren ({0},{1})", new Object[] { getMascot(), this });
			throw new LostGroundException();
		}

		final int targetX = getTargetX();
		final int targetY = getTargetY();

		boolean down = false;

		if (targetX != DEFAULT_TARGETX) {
			if (getMascot().getAnchor().x != targetX) {
				getMascot().setLookRight(getMascot().getAnchor().x < targetX);
			}
		}
		if (targetY != DEFAULT_TARGETY) {
			down = getMascot().getAnchor().y < targetY;
		}

		// Animation ausführen
		getAnimation().next(getMascot(), getTime());

		if (targetX != DEFAULT_TARGETX) {
			// überschüssige Bewegung zurücknehmen
			if ((getMascot().isLookRight() && (getMascot().getAnchor().x >= targetX))
					|| (!getMascot().isLookRight() && (getMascot().getAnchor().x <= targetX))) {
				getMascot().setAnchor(new Point(targetX, getMascot().getAnchor().y));
			}
		}
		if (targetY != DEFAULT_TARGETY) {
			// überschüssige Bewegung zurücknehmen
			if ((down && (getMascot().getAnchor().y >= targetY)) ||
					(!down && (getMascot().getAnchor().y <= targetY))) {
				getMascot().setAnchor(new Point(getMascot().getAnchor().x, targetY));
			}
		}

	}

	private int getTargetY() throws VariableException {
		return eval(PARAMETER_TARGETY, Number.class, DEFAULT_TARGETY).intValue();
	}

	private int getTargetX() throws VariableException {
		return eval(PARAMETER_TARGETX, Number.class, DEFAULT_TARGETX).intValue();
	}

}
