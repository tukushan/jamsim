package org.jamsim.ascape.weights;

import java.util.Map;
import java.util.Observer;

import org.jamsim.io.ParameterSet;

/**
 * An Observable {@link ParameterSet}s that calculates a weight when provided
 * with a variable map.
 * 
 * @author Oliver Mannion
 * @version $Revision$
 */
public interface WeightCalculator extends ParameterSet {

	/**
	 * Return the appropriate factor weight based on the values in {@code vars}.
	 * 
	 * @param vars
	 *            map of variable names and values
	 * @return weight
	 */
	double getWeight(Map<String, Double> vars);

	/**
	 * Adds an observer to the set of observers for this object, provided that
	 * it is not the same as some observer already in the set.
	 */
	void addObserver(Observer o);

}