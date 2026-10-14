package com.group_finity.mascot.action;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.script.VariableMap;

/**
 * Aktion, die ausschließlich eine Animation ausführt.
 * @author Yuki Yamada
 */
public class Animate extends BorderedAction {

	private static final Logger log = Logger.getLogger(Animate.class.getName());

	public Animate(final List<Animation> animations, final VariableMap params) {
		super(animations, params);

	}

	@Override
	protected void tick() throws LostGroundException, VariableException {

		super.tick();

		if ((getBorder() != null) && !getBorder().isOn(getMascot().getAnchor())) {
			// Bodenkontakt verloren
			log.log(Level.INFO, "Randkontakt verloren ({0},{1})", new Object[] { getMascot(), this });
			throw new LostGroundException();
		}

		// Animation ausführen
		getAnimation().next(getMascot(), getTime());

	}

	@Override
	public boolean hasNext() throws VariableException {

		final boolean intime = getTime() < getAnimation().getDuration();

		return super.hasNext() && intime;
	}

}
