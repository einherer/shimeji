package com.group_finity.mascot.action;

import java.util.logging.Logger;

import com.group_finity.mascot.script.VariableMap;

/**
 * Führt aus mehreren Aktionen genau eine aus, deren Bedingung erfüllt ist.
 * @author Yuki Yamada
 */
public class Select extends ComplexAction {

	private static final Logger log = Logger.getLogger(Select.class.getName());

	public Select(final VariableMap params, final Action... actions) {
		super(params, actions);
	}

}
