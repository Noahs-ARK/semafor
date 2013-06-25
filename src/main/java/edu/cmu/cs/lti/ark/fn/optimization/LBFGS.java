package edu.cmu.cs.lti.ark.fn.optimization;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.io.OutputSupplier;
import edu.cmu.cs.lti.ark.fn.constants.FNConstants;
import edu.cmu.cs.lti.ark.util.ds.Pair;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
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

	public static int singleStep(double[] currentParams, double objectiveValue, double[] gradient)
			throws riso.numerical.LBFGS.ExceptionWithIflag {
		// minimum verbosity
		int[] iprint = new int[] {FNConstants.m_debug ? 1 : -1, 0};
		// lbfgs sets this flag instead of throwing errors. cool.
		int[] iflag = new int[] { 0 };
		int modelSize = currentParams.length;
		riso.numerical.LBFGS.lbfgs(modelSize,
				FNConstants.m_num_corrections,
				currentParams,
				objectiveValue,
				gradient,
				false,
				new double[modelSize],
				iprint,
				FNConstants.m_eps,
				FNConstants.xtol,
				iflag
		);
		return iflag[0];
	}

	public static double[] trainAndSaveModel(double[] startingParams,
											 Function<double[], Pair<Double, double[]>> valueAndGradientProvider,
											 String modelFilePrefix) throws Exception {
		double[] currentParams = startingParams.clone();
		int iflag;  // lbfgs sets this flag to 0 when it's done
		int iteration = 0;
		do {
			logger.info("Starting iteration:" + iteration);
			Pair<Double, double[]> valueAndGradient =
					valueAndGradientProvider.apply(currentParams);
			assert valueAndGradient != null;
			double value = valueAndGradient.getFirst();
			double[] gradients = valueAndGradient.getSecond();
			logger.info("Function value:" + value);
			// updates currentParams as a side-effect
			iflag = LBFGS.singleStep(currentParams, value, gradients);
			logger.info("Finished iteration:" + iteration);
			iteration++;
			if (iteration % FNConstants.save_every_k == 0) {
				final String modelFilename = String.format("%s_%05d", modelFilePrefix, iteration);
				saveModel(currentParams, newWriterSupplier(new File(modelFilename), Charsets.UTF_8));
			}
		} while (iteration <= FNConstants.m_max_its && iflag != 0);
		saveModel(currentParams, newWriterSupplier(new File(modelFilePrefix), Charsets.UTF_8));
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
