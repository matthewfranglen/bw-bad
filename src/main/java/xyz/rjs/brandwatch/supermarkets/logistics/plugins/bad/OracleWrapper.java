package xyz.rjs.brandwatch.supermarkets.logistics.plugins.bad;

import java.text.DecimalFormat;
import java.util.Random;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import xyz.rjs.brandwatch.supermarkets.logistics.plugins.AbstractPlugin;


/**
 * @author matthew
 *
 */
public class OracleWrapper extends AbstractPlugin {

	private static final Logger logger = LoggerFactory.getLogger(OracleWrapper.class);
	private static final DecimalFormat formatter = new DecimalFormat("#,###");

	private final Oracle oracle;

	private final int bound;

	public OracleWrapper(int bound) {
		oracle = new Oracle();
		this.bound = bound;
	}

	public boolean isFixed() {
		return oracle.size() == 1;
	}

	public Random getRandom() {
		return oracle.getRandom();
	}

	private void preLogging(long originalSize, long predictedSize) {
		if (originalSize > Oracle.SIZE_TRANSITION_LIMIT && predictedSize < Oracle.SIZE_TRANSITION_LIMIT) {
			logger.warn("The Oracle is about to start.\nThis will use all of your cores and all of your memory and probably fail anyway.\nYou can try altering the parameters in SeedGenerator.\nGood luck");
		}
	}
	private void postLogging(long originalSize, long predictedSize, long originalTime) {
		final long size = oracle.size();
		if (originalSize > Oracle.SIZE_TRANSITION_LIMIT && predictedSize < Oracle.SIZE_TRANSITION_LIMIT) {
			logger.info(String.format(
					"The Oracle has completed the calculation!\n%s ms for %s seeds",
					formatter.format(originalTime - System.currentTimeMillis()),
					formatter.format(SeedGenerator.size())
				));
		}
		else if (originalSize > 1 && size == 1) {
			logger.info("Oracle has fixated");
		}
		else if (size > 1) {
			logger.info(String.format(
					"Oracle has contracted from %s to %s",
					formatter.format(originalSize),
					formatter.format(size)
				));
		}
	}

	protected void calledNextInt(Function<Random, Boolean> call) {
		final long originalSize = oracle.size(), predictedSize = originalSize / bound;
		final long originalTime = System.currentTimeMillis();

		preLogging(originalSize, predictedSize);
		oracle.calledNextInt(call, bound);
		postLogging(originalSize, predictedSize, originalTime);
	}

	protected void calledNextInt(int value) {
		final long originalSize = oracle.size(), predictedSize = originalSize / bound;
		final long originalTime = System.currentTimeMillis();

		preLogging(originalSize, predictedSize);
		oracle.calledNextInt(value, bound);
		postLogging(originalSize, predictedSize, originalTime);
	}
}
