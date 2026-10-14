package com.group_finity.mascot.action;

import com.group_finity.mascot.Mascot;
import com.group_finity.mascot.exception.LostGroundException;
import com.group_finity.mascot.exception.VariableException;

/**
 * Objekt, das die Animation eines Maskottchens beschreibt.
 * 
 * {@link #next(Mascot)} wird in regelmäßigen Abständen aufgerufen
 */
public interface Action {

	/**
	 * Wird beim Start einer Aktion aufgerufen.
	 * @param mascot Zugeordnetes Maskottchen.
	 */
	public void init(Mascot mascot) throws VariableException;

	/**
	 * Prüfen, ob ein weiterer Frame vorhanden ist.
	 * @return Ob ein weiterer Frame vorhanden ist.
	 */
	public boolean hasNext() throws VariableException;
	
	/**
	 * Maskottchen zum nächsten Frame weiterschalten.
	 * @throws LostGroundException Wenn kein Boden vorhanden ist.
	 */
	public void next() throws LostGroundException, VariableException;
	
}
