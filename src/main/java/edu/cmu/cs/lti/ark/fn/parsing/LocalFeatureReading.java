/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 *
 * LocalFeatureReading.java is part of SEMAFOR 2.0.
 *
 * SEMAFOR 2.0 is free software: you can redistribute it and/or modify  it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 *
 * SEMAFOR 2.0 is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. 
 *
 * You should have received a copy of the GNU General Public License along
 * with SEMAFOR 2.0.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package edu.cmu.cs.lti.ark.fn.parsing;

import com.google.common.base.Charsets;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import edu.cmu.cs.lti.ark.util.ds.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static edu.cmu.cs.lti.ark.fn.utils.BitOps.readALine;
import static java.lang.Integer.parseInt;
import static org.apache.commons.io.IOUtils.closeQuietly;

public class LocalFeatureReading {
	private String eventsFilename;
	private String spansFilename;
	private List<String> frameLines;

	public LocalFeatureReading(String eventsFilename, String spansFilename, List<String> frameLines) {
		this.eventsFilename = eventsFilename;
		this.spansFilename = spansFilename;
		this.frameLines = frameLines;
	}

	public List<FrameFeatures> readLocalFeatures() throws IOException {
		final List<String> spanLines = Files.readLines(new File(spansFilename), Charsets.UTF_8);
		final List<FrameFeatures> frameFeaturesList = parseSpanLines(spanLines, frameLines);
		return readLocalEventsFile(Files.newInputStreamSupplier(new File(eventsFilename)), frameFeaturesList);
	}

	private List<FrameFeatures> readLocalEventsFile(InputSupplier<? extends InputStream> eventsInputSupplier,
													List<FrameFeatures> frameFeaturesList) throws IOException {
		final InputStream input = eventsInputSupplier.getInput();
		try {
			return readLocalEventsFile(input, frameFeaturesList);
		} finally {
			closeQuietly(input);
		}
	}

	private List<FrameFeatures> readLocalEventsFile(InputStream inputStream, List<FrameFeatures> frameFeaturesList) {
		int currentFrameFeaturesIndex = 0;
		int currentFEIndex = 0;
		int[] line = readALine(inputStream);
		boolean skip = false;
		while (line.length > 0 || skip) {
			final ArrayList<int[]> temp = Lists.newArrayList();
			if (!skip) {
				while (line.length > 0) {
					temp.add(line);
					line = readALine(inputStream);
				}
			} else {
				skip = false;
			}
			final FrameFeatures f = frameFeaturesList.get(currentFrameFeaturesIndex);
			if (f.fElements.size() == 0) {
				System.out.println(f.frameName + ". No frame elements for the frame.");
				currentFrameFeaturesIndex++;
				if (currentFrameFeaturesIndex == frameFeaturesList.size()) {
					break;
				}
				currentFEIndex = 0;
				skip = true;
				continue;
			}
			final SpanAndCorrespondingFeatures[] spans = f.fElementSpansAndFeatures.get(currentFEIndex);
			for (int k = 0; k < temp.size(); k++) {
				spans[k].features = temp.get(k);
			}
			final SpanAndCorrespondingFeatures goldSpan = spans[0];
			Arrays.sort(spans);
			f.goldSpanIdxs.add(Arrays.binarySearch(spans, goldSpan));
			if (currentFEIndex == f.fElements.size() - 1) {
				currentFrameFeaturesIndex++;
				currentFEIndex = 0;
			} else {
				currentFEIndex++;
			}
			line = readALine(inputStream);
		}
		return frameFeaturesList;
	}

	private List<FrameFeatures> parseSpanLines(List<String> spanLines, List<String> frameLines) {
		final Pair<List<String>, List<List<SpanAndCorrespondingFeatures>>> feLinesAndSpanLines =
				readFeLinesAndSpans(spanLines);
		final List<String> feLines = feLinesAndSpanLines.first;
		final List<List<SpanAndCorrespondingFeatures>> spansList = feLinesAndSpanLines.second;

		// group by sentence idx
		// map from sentence idx to list of feLines/parseLines idxs for that sentence
		final ListMultimap<Integer, Integer> frameIndexMap = ArrayListMultimap.create();
		for (int i = 0; i < feLines.size(); i++) {
			final String[] fields = feLines.get(i).split("\t");
			final int sentenceIdx = Integer.parseInt(fields[fields.length - 1]);
			frameIndexMap.put(sentenceIdx, i);
		}
		// zip up into list of FrameFeatures'
		final List<FrameFeatures> frameFeaturesList = Lists.newArrayList();
		for (int i = 0; i < frameLines.size(); i++) {
			final String[] fields = frameLines.get(i).split("\t");
			// throw away the first two fields (rank and score)
			// hack around the fact that parsing this goddamn file format is hardcoded in 20 different places
			final List<String> tokens = Arrays.asList(fields).subList(2, fields.length);
			final String frame = tokens.get(1).trim();
			final String[] span = tokens.get(3).split("_");
			int targetStartTokenIdx = parseInt(span[0]);
			int targetEndTokenIdx = parseInt(span[span.length - 1]);
			final List<Integer> feLineIdxs = frameIndexMap.get(i);
			final List<String> fElements = Lists.newArrayList();
			final List<SpanAndCorrespondingFeatures[]> fElementSpansAndFeatures = Lists.newArrayList();
			for (int feLineIdx : feLineIdxs) {
				fElements.add(feLines.get(feLineIdx).split("\t")[1]);
				final List<SpanAndCorrespondingFeatures> spans = spansList.get(feLineIdx);
				fElementSpansAndFeatures.add(spans.toArray(new SpanAndCorrespondingFeatures[spans.size()]));
			}
			final FrameFeatures frameFeatures =
					new FrameFeatures(frame, targetStartTokenIdx, targetEndTokenIdx, fElements, fElementSpansAndFeatures);
			frameFeaturesList.add(frameFeatures);
		}
		return frameFeaturesList;
	}

	private Pair<List<String>, List<List<SpanAndCorrespondingFeatures>>> readFeLinesAndSpans(List<String> spanLines) {
		final List<String> feLines = Lists.newArrayList();
		final List<List<SpanAndCorrespondingFeatures>> spansList = Lists.newArrayList();

		List<SpanAndCorrespondingFeatures> spans = Lists.newArrayList();
		for (final String spanLine : spanLines) {
			final String[] fields = spanLine.split("\t");
			if (fields.length == 6) {
				feLines.add(spanLine);
			} else if (fields.length == 2) {
				spans.add(new SpanAndCorrespondingFeatures(
						new int[] {Integer.parseInt(fields[0]), Integer.parseInt(fields[1])}));
			} else if (spanLine.isEmpty()) {
				// blank line marks the end of a block
				spansList.add(spans);
				spans = Lists.newArrayList();
			}
		}
		return Pair.of(feLines, spansList);
	}
}
