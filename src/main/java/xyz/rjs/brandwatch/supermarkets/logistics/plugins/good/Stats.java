package xyz.rjs.brandwatch.supermarkets.logistics.plugins.good;

import static com.google.common.base.Preconditions.checkPositionIndex;
import static java.lang.Math.sqrt;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Calculates the confidence level for a set of values over a period of time.
 *
 * This assumes that at most one value can be generated per time unit.
 * 
 * @author matthew
 *
 */
public class Stats {

	private final Collection<Double> values;

	public Stats() {
		values = new ArrayList<Double>();
	}

	public void add(double value) {
		values.add(value);
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}

	/**
	 * Calculate the mean of all of the values.
	 * 
	 * @param count - the number of time units that have passed
	 * @return
	 */
	public double average(int count) {
		checkPositionIndex(values.size(), count, "This assumes that there cannot be more values than there are counts");

		return values.stream().mapToDouble(Number::doubleValue).sum() / count;
	}

	/**
	 * Calculate the mean of all of the values.
	 * 
	 * @return
	 */
	public double average() {
		return average(values.size());
	}

	/**
	 * Calculates the 2 standard deviation confidence level for the values.
	 * 
	 * @param count - the number of time units that have passed
	 * @return
	 */
	public double confidence(int count) {
		checkPositionIndex(values.size(), count, "This assumes that there cannot be more values than there are counts");

		final double a = average(count), sd = standardDeviation(count);
		return a + (2 * sd);
	}

	/**
	 * Calculates the 2 standard deviation confidence level for the values.
	 * 
	 * @return
	 */
	public double confidence() {
		return confidence(values.size());
	}

	/**
	 * Calculates the standard deviation of the values.
	 * 
	 * @param count - the number of time units that have passed
	 * @return
	 */
	public double standardDeviation(int count) {
		checkPositionIndex(values.size(), count, "This assumes that there cannot be more values than there are counts");

		final double a = average(count);

		// There can be one value for any count, so the number of unrecorded
		// 'zero' values is the count minus the number of values. These zero
		// values have to be included in the standard deviation calculation, as
		// they can differ from the average.
		final double zeroSum = a * a * (count - values.size());

		// The standard deviation is:
		// The square root of the sum of the square of the difference between the value and the average.
		// This calculates the square of the differences for the recorded values.
		final double valueSum = values.stream().mapToDouble(Number::doubleValue).map(v -> v - a).map(v -> v * v).sum();

		return sqrt((zeroSum + valueSum) / count);
	}


	/**
	 * Calculates the standard deviation of the values.
	 * 
	 * @return
	 */
	public double standardDeviation() {
		return standardDeviation(values.size());
	}
}
