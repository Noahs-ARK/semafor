package edu.cmu.cs.lti.ark.fn.optimization;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.io.OutputSupplier;
import edu.cmu.cs.lti.ark.fn.constants.FNConstants;
import edu.cmu.cs.lti.ark.util.ds.Pair;

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
public class LBFGS {
	private static final Logger logger = Logger.getLogger(LBFGS.class.getCanonicalName());
	static { logger.addHandler(new ConsoleHandler()); }

	public static double[] trainAndSaveModel(double[] startingParams,
											 Function<double[], Pair<Double, double[]>> valueAndGradientProvider,
											 String modelFilePrefix) throws Exception {
		int iteration = 0;
		// parameters needed for lbfgs
		double[] currentParams = startingParams.clone();
		int modelSize = currentParams.length;
		// Output every iteration:
		// iteration count, number of function evaluations, function value, norm of the gradient, and steplength
		int[] iprint = new int[] {FNConstants.m_debug ? 1 : -1, 0};
		// lbfgs sets this flag to zero when it has converged
		final int[] iflag = { 0 };
		// unused
		final double[] diag = new double[modelSize];
		final boolean diagco = false;
		do {
			logger.info("Starting iteration:" + iteration);
			Pair<Double, double[]> valueAndGradient =
					valueAndGradientProvider.apply(currentParams);
			double value = valueAndGradient.getFirst();
			double[] gradients = valueAndGradient.getSecond();
			logger.info("Function value:" + value);

			riso.numerical.LBFGS.lbfgs(modelSize,
					FNConstants.m_num_corrections,
					currentParams,
					value,
					gradients,
					diagco,
					diag,
					iprint,
					FNConstants.m_eps,
					FNConstants.xtol,
					iflag
			);
			logger.info("Finished iteration:" + iteration);
			iteration++;
			if (iteration % FNConstants.save_every_k == 0) {
				final String modelFilename = String.format("%s_%05d", modelFilePrefix, iteration);
				saveModel(riso.numerical.LBFGS.solution_cache, newWriterSupplier(new File(modelFilename), Charsets.UTF_8));
			}
		} while (iteration <= FNConstants.m_max_its && iflag[0] != 0);
		saveModel(riso.numerical.LBFGS.solution_cache, newWriterSupplier(new File(modelFilePrefix), Charsets.UTF_8));
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

}
