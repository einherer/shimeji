package com.group_finity.mascot.config;

import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.group_finity.mascot.animation.Animation;
import com.group_finity.mascot.animation.Pose;
import com.group_finity.mascot.exception.AnimationInstantiationException;
import com.group_finity.mascot.exception.VariableException;
import com.group_finity.mascot.image.ImagePair;
import com.group_finity.mascot.image.ImagePairLoader;
import com.group_finity.mascot.script.Variable;
import java.lang.Math;

public class AnimationBuilder {

	private static final Logger log = Logger.getLogger(AnimationBuilder.class.getName());

	private final String condition;

	private final List<Pose> poses = new ArrayList<Pose>();
	
	private int cnt = 0;
	private int tot = 0;

	public AnimationBuilder(final Entry animationNode) throws IOException {
		this.condition = animationNode.getAttribute("条件") == null ? "true" : animationNode.getAttribute("条件");

		log.log(Level.INFO, "Animation wird geladen");

		for (final Entry frameNode : animationNode.getChildren()) {

			this.getPoses().add(loadPose(frameNode));
		}

		log.log(Level.INFO, "Animation wurde geladen");
	}

	private Pose loadPose(final Entry frameNode) throws IOException {

		final String imageText = frameNode.getAttribute("画像");
		final String anchorText = frameNode.getAttribute("基準座標");
		final String moveText = frameNode.getAttribute("移動速度");
		final String durationText = frameNode.getAttribute("長さ");

		final String[] anchorCoordinates = anchorText.split(",");
		final Point anchor = new Point(Integer.parseInt(anchorCoordinates[0]), Integer.parseInt(anchorCoordinates[1]));

		final ImagePair image = ImagePairLoader.load(imageText, anchor);

		final String[] moveCoordinates = moveText.split(",");
		Point move = new Point(Integer.parseInt(moveCoordinates[0]), Integer.parseInt(moveCoordinates[1]));
		
		/**
		 * Für Standarddateien vom Typ Actions.xml muss jede Geschwindigkeit halbiert und
		 * muss jede Frame-Dauer verdoppelt werden. Ursache ist das halbierte Tick-Intervall gegenüber
		 * dem ursprünglichen Programm. Geschwindigkeit 1 darf nicht auf 0 halbiert werden. Stattdessen wird
		 * die Anzahl der Frames für diese Animationen verdoppeln und mit der ursprünglichen Geschwindigkeit von
		 * 1 in jedem zweiten Frame.
		 */
		int newx = 0;
		int newy = 0;
		newx = (int)move.getX();
		newy = (int)move.getY();
		boolean oneX = false;
		boolean oneY = false;
		
	// Prüfen, ob eine der Geschwindigkeiten 1 beträgt
		if (Math.abs(newx) != 1) {
			newx = newx/2;
		} else {
			oneX=true;
		}
		
		if (Math.abs(newy) != 1) {
			newy = newy/2;
		} else {
			oneY=true;
		}
		
		int duration = Integer.parseInt(durationText);
	// Bei Geschwindigkeit 1 rekursiv aufrufen, um die zusätzlichen
	// Frames zu erzeugen und zwischen den Geschwindigkeiten 1 und 0 zu wechseln. Eine globale
	// Zählvariable verfolgt den Ablauf.
		if (oneX || oneY) {
			if (tot == 0) tot = duration*2; 
			if (cnt < tot) {
				if (oneX && ((cnt % 2) == 1)) newx=0;
				if (oneY && ((cnt % 2) == 1)) newy=0;
				cnt++;
				this.getPoses().add(loadPose(frameNode));
			} else {
				cnt = 0;
				tot = 0;
			}
			duration = duration/4;
		} else {
			duration = duration*2;
		} 
		
		
		move = new Point(newx,newy); 
		final Pose pose = new Pose(image, move.x, move.y, duration);

		log.log(Level.INFO, "Pose wird geladen ({0})", pose);

		return pose;

	}

	public Animation buildAnimation() throws AnimationInstantiationException {
		try {
			return new Animation(Variable.parse(this.getCondition()), this.getPoses().toArray(new Pose[0]));
		} catch (final VariableException e) {
			throw new AnimationInstantiationException("Bedingung konnte nicht ausgewertet werden", e);
		}
	}

	private List<Pose> getPoses() {
		return this.poses;
	}

	private String getCondition() {
		return this.condition;
	}
}
