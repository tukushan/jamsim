package org.jamsim.ascape.weights;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JOptionPane;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.jamsim.ascape.r.ScapeRInterface;
import org.jamsim.io.MutableNumeratorTableModel;
import org.jamsim.math.MathUtil;
import org.jamsim.math.MutableNumerator;
import org.jamsim.shared.InvalidDataException;
import org.omancode.r.RInterfaceException;
import org.omancode.r.RUtil;
import org.rosuda.REngine.REXP;
import org.rosuda.REngine.REXPDouble;
import org.rosuda.REngine.REXPMismatchException;

/**
 * Calculates weights for each factor level of a single variable.
 * 
 * @author Oliver Mannion
 * @version $Revision$
 */
public class SingleVarWeightCalc extends Observable implements
		WeightCalculator {

	/**
	 * The name of the R variable, eg: {@code sol1}.
	 */
	private final String variableName;

	/**
	 * Variable description. Used for display purposes.
	 */
	private final String variableDesc;

	/**
	 * Variable factor levels and their weighting.
	 */
	private final Map<Double, MutableNumerator> factorLevelWeights;

	/**
	 * Weights at each factor level. An array version of
	 * {@link #factorLevelWeights}.
	 */
	private final MutableNumerator[] weights;

	/**
	 * A {@link TableModel} wrapped around {@link #weights}.
	 */
	private final AbstractTableModel tableModel;

	/**
	 * Construct a set of weightings at each factor level of {@code
	 * variableName}.
	 * 
	 * @param scapeR
	 *            scape R interface
	 * @param rVariable
	 *            the R variable that will be used as the basis for weighting,
	 *            eg: {@code children$sol1}
	 * @param variableName
	 *            the name of the R variable, eg: {@code sol1}
	 * @param variableDesc
	 *            description of the R variable. Used for display purposes.
	 * @throws RInterfaceException
	 *             if problem reading category proportions of {@code variable}
	 *             from R
	 * @throws InvalidDataException
	 *             if weights do not sum to 1. See {@link #validate()}.
	 */
	public SingleVarWeightCalc(ScapeRInterface scapeR, String rVariable,
			String variableName, String variableDesc)
			throws RInterfaceException, InvalidDataException {

		this.variableName = variableName;
		this.variableDesc = variableDesc;
		this.factorLevelWeights = getFactorLevelsWithProp(rVariable, scapeR);
		this.weights =
				factorLevelWeights.values().toArray(
						new MutableNumerator[factorLevelWeights.size()]);
		this.tableModel = new MutableNumeratorTableModel(weights);

		validate();

	}

	/**
	 * Gets a map of factor levels for {@code variable} and calculates the
	 * proportion (ie: factor level count / total counts) at each factor level.
	 * 
	 * @param variable
	 *            variable name
	 * @param scapeR
	 *            scape R interface
	 * @return an map of {@link MutableNumerator} for each factor level with the
	 *         denominator and numerator set to the proportion of counts at each
	 *         factor level. The map key represents the factor level value.
	 * @throws RInterfaceException
	 */
	private Map<Double, MutableNumerator> getFactorLevelsWithProp(
			String variable, ScapeRInterface scapeR)
			throws RInterfaceException {
		String cmd = "prop.table(table(" + variable + "))";

		REXP rexp = scapeR.parseEvalTry(cmd);

		// r command must return a REXPDouble
		if (!(rexp instanceof REXPDouble)) {
			throw new RInterfaceException(cmd + " returned "
					+ rexp.getClass().getCanonicalName());
		}

		// get names. these are the factors.
		String[] valueNames = RUtil.getNamesAttribute(rexp);
		if (valueNames == null) {
			throw new RInterfaceException("Result of " + cmd
					+ " does not supply names attribute.");
		}

		// get values
		double[] values;
		try {
			values = rexp.asDoubles();
		} catch (REXPMismatchException e) {
			throw new RInterfaceException(e);
		}

		// create MutableNumerators with the denominator and numerator
		// equal to the count proportion
		Map<Double, MutableNumerator> adjFactors =
				new LinkedHashMap<Double, MutableNumerator>(values.length);
		for (int i = 0; i < values.length; i++) {
			MutableNumerator num =
					new MutableNumerator(valueNames[i], values[i], values[i]);
			adjFactors.put(Double.valueOf(valueNames[i]), num);
		}

		return adjFactors;
	}

	/**
	 * Lookup the variable name and return the appropriate factor weight based
	 * on the value of the variable in {@code vars}.
	 * 
	 * @param vars
	 *            map of variable names and values
	 * @return weight for value of variable in {@code vars}.
	 */
	public double getWeight(Map<String, Double> vars) {
		Double var = vars.get(variableName);

		MutableNumerator factorReweight = factorLevelWeights.get(var);

		if (factorReweight == null) {
			throw new IllegalStateException(
					"Cannot find reweighting value for " + variableName
							+ " with value = " + var);
		}

		double weight = factorReweight.getFraction();

		return weight;
	}

	@Override
	public String getName() {
		return "Weightings - " + variableDesc;
	}

	/**
	 * Process a change to the underlying weights. Validates them and displays a
	 * prompt if there is a problem. Then notifies all {@link Observer}s that
	 * there has been a change.
	 */
	@Override
	public void update() {

		if (validateWithPrompt()) {
			// notify all observers
			setChanged();
			notifyObservers();
		}

	}

	/**
	 * Validate weightings, ie: make sure they sum to 1.
	 * 
	 * @throws InvalidDataException
	 *             if weightings cannot be validated
	 */
	public final void validate() throws InvalidDataException {
		double total = 0;

		for (MutableNumerator adj : weights) {
			total += adj.doubleValue();
		}

		if (!MathUtil.equals(total, 1)) {
			throw new InvalidDataException("sol reweights (" + total
					+ ") must add to 1");
		}

	}

	/**
	 * Validate, and show a message box if validation fails.
	 * 
	 * @return {@code true} if validation succeeds
	 */
	public boolean validateWithPrompt() {
		boolean result = true;

		try {
			validate();
		} catch (InvalidDataException e) {
			result = false;

			// display message box
			JOptionPane.showMessageDialog(null, e.getMessage());

		}

		return result;
	}

	@Override
	public TableModel getTableModel() {
		return tableModel;
	}

	@Override
	public void resetDefaults() {

		for (MutableNumerator num : weights) {
			num.setNumerator(num.getDenominator());
		}

		tableModel.fireTableDataChanged();
		update();
	}

}
