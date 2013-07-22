package edu.cmu.cs.lti.ark.fn.optimization;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.io.OutputSupplier;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import riso.numerical.LBFGS;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import static com.google.common.io.Files.newWriterSupplier;
import static org.apache.commons.io.IOUtils.closeQuietly;

/**
 * Convenience functions for riso.numerical.LBFGS. Calls with reasonable defaults.
 *
 * @author sthomson@cs.cmu.edu
 */
public class Lbfgs {
	private static final Logger logger = Logger.getLogger(Lbfgs.class.getCanonicalName());
	static { logger.addHandler(new ConsoleHandler()); }
	/*
	 * LBFGS constants
	 */
	public static int MAX_ITERATIONS = 2000;
	// we've converged when ||gradient step|| <= STOPPING_THRESHOLD * max(||parameters||, 1)
    public static double STOPPING_THRESHOLD = 1.0e-4;
	public static double XTOL = calculateMachineEpsilon(); //estimate of machine precision.  ~= 2.220446049250313E-16
	// number of corrections, between 3 and 7
    // a higher number means more computation per iteration, but possibly less iterations until convergence
    public static int NUM_CORRECTIONS = 6;
	public static boolean DEBUG = true;
	public static int SAVE_EVERY_K = 10;

	public static double[] trainAndSaveModel(double[] startingParams,
											 Function<double[], Pair<Double, double[]>> valueAndGradientProvider,
											 String modelFilePrefix) throws Exception {
		int iteration = 0;
		// parameters needed for lbfgs
		double[] currentParams = startingParams.clone();
		int modelSize = currentParams.length;
		// Output every iteration:
		// iteration count, number of function evaluations, function value, norm of the gradient, and steplength
		int[] iprint = new int[] {DEBUG ? 1 : -1, 0};
		// lbfgs sets this flag to zero when it has converged
		final int[] iflag = { 0 };
		// unused
		final double[] diag = new double[modelSize];
		final boolean diagco = false;
		try {
			do {
				Pair<Double, double[]> valueAndGradient =
						valueAndGradientProvider.apply(currentParams);
				double value = valueAndGradient.first;
				double[] gradients = valueAndGradient.second;
					final long startTime = System.currentTimeMillis();
					riso.numerical.LBFGS.lbfgs(modelSize,
							NUM_CORRECTIONS,
							currentParams,
							value,
							gradients,
							diagco,
							diag,
							iprint,
							STOPPING_THRESHOLD,
							XTOL,
							iflag
					);
					final long endTime = System.currentTimeMillis();
					logger.info(String.format("Finished LBFGS step. Took %s seconds.", (endTime - startTime) / 1000.0));
				iteration++;
				if (iteration % SAVE_EVERY_K == 0) {
					final String modelFilename = String.format("%s_%05d", modelFilePrefix, iteration);
					saveModel(riso.numerical.LBFGS.solution_cache, newWriterSupplier(new File(modelFilename), Charsets.UTF_8));
				}
			} while (iteration <= MAX_ITERATIONS && iflag[0] != 0);
		} catch (LBFGS.ExceptionWithIflag e) {
			// these exceptions happen sometimes even though the training was successful
			// we still want to save the most recent model
			// TODO: separate out the ok exceptions from the bad exceptions
			e.printStackTrace();
		}
		final String modelFilename = String.format("%s_%05d", modelFilePrefix, iteration);
		saveModel(riso.numerical.LBFGS.solution_cache, newWriterSupplier(new File(modelFilename), Charsets.UTF_8));
		return currentParams;
	}

	/**
	 * Writes parameters, one param per line.
	 *
	 * @param outputSupplier the output to write to
	 */
	private static void saveModel(double[] params, OutputSupplier<OutputStreamWriter> outputSupplier)
			throws IOException {
		final OutputStreamWriter output = outputSupplier.getOutput();
		try {
			for (double param : params) output.write(param + "\n");
		} finally {
			closeQuietly(output);
		}
	}

	public static double calculateMachineEpsilon() {
		double machineEpsilon = 1.0;
		do {
			machineEpsilon /= 2.0;
		} while (1.0 + (machineEpsilon / 2.0) != 1.0);
		return machineEpsilon;
	}
}
