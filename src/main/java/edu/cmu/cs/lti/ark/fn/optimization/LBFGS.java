package edu.cmu.cs.lti.ark.fn.optimization;

import edu.cmu.cs.lti.ark.fn.constants.FNConstants;

/**
 * Thin wrapper around riso.numerical.LBFGS. Calls with reasonable defaults
 *
 * @author sthomson@cs.cmu.edu
 */
public class LBFGS {
	public static int lbfgs(double[] params,
							 double f,
							 double[] gradient)
			throws riso.numerical.LBFGS.ExceptionWithIflag {
		// minimum verbosity
		int[] iprint = new int[] {
				FNConstants.m_debug ? 1 : -1,
				0
		};
		// lbfgs sets this flag instead of throwing errors. cool.
		int[] iflag = new int[] { 0 };
		int modelSize = params.length;
		riso.numerical.LBFGS.lbfgs(modelSize,
				FNConstants.m_num_corrections,
				params,
				f,
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
}
