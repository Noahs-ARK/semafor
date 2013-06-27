package edu.cmu.cs.lti.ark.fn.identification;

import com.google.common.base.Charsets;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;

/**
 * @author sthomson@cs.cmu.edu
 */
public class FrameCosts {
	public static final String DEFAULT_COSTS_FILE = "frame_costs.csv";
	private static InputSupplier<InputStream> DEFAULT_INPUT_SUPPLIER = new InputSupplier<InputStream>() {
		@Override public InputStream getInput() throws IOException {
			return getClass().getClassLoader().getResourceAsStream(DEFAULT_COSTS_FILE);
		} };
	public static final float DEFAULT_COST = 1.0f;
	private static final int EXPECTED_NUM_FRAMES = 1000;

	private final Table<String, String, Float> costs;

	public FrameCosts(Table<String, String, Float> costs) {
		this.costs = costs;
	}

	public static FrameCosts load() throws IOException {
		return fromCsv(CharStreams.newReaderSupplier(DEFAULT_INPUT_SUPPLIER, Charsets.UTF_8));
	}

	public static FrameCosts fromCsv(InputSupplier<InputStreamReader> input) throws IOException {
		return new FrameCosts(readCsv(input));
	}

	private static Table<String, String, Float> readCsv(InputSupplier<InputStreamReader> input) throws IOException {
		final Table<String, String, Float> costs = HashBasedTable.create(EXPECTED_NUM_FRAMES, EXPECTED_NUM_FRAMES);
		final List<String> lines = CharStreams.readLines(input);
		final String header = lines.get(0);
		// header line starts with a comma
		final List<String> allFrames = Lists.newArrayList(header.substring(1, header.length()).split(","));

		for (String line : lines.subList(1, lines.size())) {
			// name of frame followed by costs
			final String[] fields = line.split(",", 2);
			final String fromFrame = fields[0];
			final String[] costStrs = fields[1].split(",");
			for (int i : xrange(costStrs.length)) {
				final String toFrame = allFrames.get(i);
				float cost = Float.parseFloat(costStrs[i]);
				costs.put(fromFrame, toFrame, cost);
			}
		}
		return costs;
	}

	public float getCost(String goldFrame, String predictedFrame) {
		if (!costs.contains(goldFrame, predictedFrame)) return DEFAULT_COST;
		return costs.get(goldFrame, predictedFrame);
	}
}
