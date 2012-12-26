package edu.cmu.cs.lti.ark.fn.data.prep.formats;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Scanner;

import static edu.cmu.cs.lti.ark.util.IntRanges.xrange;

/**
 * Utilities for reading from and writing to files in the all.lemma.tags format
 * @author sthomson@cs.cmu.edu
 */
public class AllLemmaTags {
	// description of the parse array format
	// TODO: would be better to just make a class for parses
	public static final int NUM_PARSE_ROWS = 6;
	public static final int PARSE_TOKEN_ROW = 0;
	public static final int PARSE_POS_ROW = 1;
	public static final int PARSE_DEPREL_ROW = 2;
	public static final int PARSE_HEAD_ROW = 3;
	public static final int PARSE_NE_ROW = 4;
	public static final int PARSE_LEMMA_ROW = 5;

	public static String[][] readLine(String line) {
		final Scanner fields = new Scanner(line.trim()).useDelimiter("\t");
		final int numTokens = Integer.parseInt(fields.next());

		final String[][] parseData = new String[NUM_PARSE_ROWS][numTokens];
		for(int k : xrange(NUM_PARSE_ROWS)) {
			for(int j : xrange(numTokens)) {
				parseData[k][j] = fields.next().trim();
			}
		}
		return parseData;
	}

	public static String makeLine(String[][] parseData) {
		final int numTokens = parseData[0].length;
		final List<String> flattened = Lists.newArrayList();
		for(int k : xrange(NUM_PARSE_ROWS)) {
			for(int j : xrange(numTokens)) {
				flattened.add(parseData[k][j]);
			}
		}
		final Joiner tab = Joiner.on("\t");
		return tab.join(numTokens, tab.join(flattened));
	}
}
