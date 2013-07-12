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
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.google.common.io.InputSupplier;
import gnu.trove.TIntObjectHashMap;

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
	private ArrayList<FrameFeatures> frameFeaturesList;
	private List<String> frameLines;

	public LocalFeatureReading(String eventsFilename, String spansFilename, List<String> frameLines) {
		this.eventsFilename = eventsFilename;
		this.spansFilename = spansFilename;
		this.frameLines = frameLines;
		frameFeaturesList = Lists.newArrayList();
	}

	public LocalFeatureReading(String eventsFile, String spanFile, String frameFile) throws IOException {
		this(eventsFile, spanFile, Files.readLines(new File(frameFile), Charsets.UTF_8));
	}

	public ArrayList<FrameFeatures> readLocalFeatures() throws IOException {
		readSpansFile(spansFilename);
		readLocalEventsFile(Files.newInputStreamSupplier(new File(eventsFilename)));
		return frameFeaturesList;
	}
	
	private void readLocalEventsFile(InputSupplier<? extends InputStream> eventsInputSupplier) throws IOException {
		final InputStream input = eventsInputSupplier.getInput();
		try {
			readLocalEventsFile(input);
		} finally { closeQuietly(input); }
	}

	private void readLocalEventsFile(InputStream bis) {
		int currentFrameFeaturesIndex = 0;
		int currentFEIndex = 0;
		int[] line = readALine(bis);
		ArrayList<int[]> temp = new ArrayList<int[]>();
		boolean skip = false;
		while (line.length > 0||skip) {
			if(!skip) {
				while (line.length > 0) {
					temp.add(line);
					line = readALine(bis);
				}
			} else {
				skip = false;
			}
			int size = temp.size();
			FrameFeatures f = frameFeaturesList.get(currentFrameFeaturesIndex);
			if(f.fElements.size()==0) {
				System.out.println(f.frameName + ". No frame elements for the frame.");
				currentFrameFeaturesIndex++;
				if(currentFrameFeaturesIndex== frameFeaturesList.size()) {
					break;
				}
				currentFEIndex = 0;
				System.out.println("temp.size()=" + temp.size());
				skip = true;
				continue;
			}
			SpanAndCorrespondingFeatures[] spans = f.fElementSpansAndFeatures.get(currentFEIndex);
			System.out.println(f.frameName+" "+f.fElements.get(currentFEIndex)+" "+spans.length);
			if(size!=spans.length) {
				System.out.println("Problem. Exiting. currentFrameFeaturesIndex:"+currentFrameFeaturesIndex+" temp.size()="+size+" spans.length:"+spans.length);
				System.exit(0);
			}
			for(int k = 0; k < size; k ++) {
				spans[k].features = temp.get(k);
			}
			SpanAndCorrespondingFeatures gold = new SpanAndCorrespondingFeatures();
			gold.span = new int[2];
			gold.features = new int[spans[0].features.length];
			gold.span[0] = spans[0].span[0];
			gold.span[1] = spans[0].span[1];
			System.arraycopy(spans[0].features, 0, gold.features, 0, gold.features.length);
			SpanAndCorrespondingFeatures.sort(spans);
			int ind = SpanAndCorrespondingFeatures.search(spans, gold);
			f.fGoldSpans.add(ind);
			if(currentFEIndex == f.fElements.size() - 1) {
				currentFrameFeaturesIndex++;
				currentFEIndex = 0;
			} else {
				currentFEIndex++;
			}
			temp = new ArrayList<int[]>();
			line = readALine(bis);
		}
	}

	private void addIntSpanArray(ArrayList<SpanAndCorrespondingFeatures[]> list, int[][] arr) {
		int len=arr.length;
		SpanAndCorrespondingFeatures[] stringSpans = new SpanAndCorrespondingFeatures[len];
		for(int i = 0; i < len; i ++) {
			stringSpans[i] = new SpanAndCorrespondingFeatures();
			stringSpans[i].span = new int[2];
			stringSpans[i].span[0] = arr[i][0];
			stringSpans[i].span[1] = arr[i][1];
		}		
		list.add(stringSpans);
	}
	
	public void readSpansFile(String mSpansFile) throws IOException {
		final TIntObjectHashMap<ArrayList<Integer>> frameIndexMap = new TIntObjectHashMap<ArrayList<Integer>>();
		for(int i = 0; i < frameLines.size(); i ++) {
			frameIndexMap.put(i, new ArrayList<Integer>());
		}
		final List<String> lines = readLines(mSpansFile);
		int i;
		int lineSize = lines.size();
		ArrayList<String> feLines = Lists.newArrayList();
		ArrayList<SpanAndCorrespondingFeatures[]> spansList = Lists.newArrayList();
		ArrayList<int[]> spans = Lists.newArrayList();
		if (lineSize != 0) {
			String feLine = lines.get(0);
			feLines.add(feLine);
			i = 1;
			//System.out.println("LineSize:" + lineSize);
			while(i < lineSize) {
				String[] toks = lines.get(i).split("\t");
				if(toks.length==6) {
					feLines.add(lines.get(i));
					int spanSize = spans.size();
					int[][] spansArr = new int[spanSize][];
					spans.toArray(spansArr);
					addIntSpanArray(spansList, spansArr);
					spans = new ArrayList<int[]>();
				}
				else if(toks.length==2) {
					int[] intSpan = new int[2];
					intSpan[0] = new Integer(toks[0]);
					intSpan[1] = new Integer(toks[1]);
					spans.add(intSpan);
				}
				else {
					System.out.println("Problem with line:"+lines.get(i));
				}
				i++;
			}
		}
		int spanSize = spans.size();
		int[][] spansArr = new int[spanSize][];
		spans.toArray(spansArr);
		addIntSpanArray(spansList, spansArr);	
		System.out.println("FE Lines Size:"+feLines.size());
		System.out.println("Spans List Size:" + spansList.size());
		for(i = 0; i < feLines.size(); i ++) {
			String[] toks = feLines.get(i).split("\t");
			int sentNum = Integer.parseInt(toks[toks.length - 1]);
			ArrayList<Integer> list = frameIndexMap.get(sentNum);
			if(list==null) {
				list = new ArrayList<Integer>();
				list.add(i);
				frameIndexMap.put(sentNum, list);
			}
			else {
				list.add(i);
			}
		}	
		int[] keys = frameIndexMap.keys();
		for(i = 0; i < keys.length; i ++) {
			if(frameIndexMap.get(keys[i]).size()==0) {
				System.out.println("There are no spans listed for line:"+keys[i]);
			}
		}	
		System.out.println("Frame index map size:"+frameIndexMap.size());
		frameFeaturesList = new ArrayList<FrameFeatures>();
		for(i = 0; i < frameLines.size(); i ++) {
			String[] toks = frameLines.get(i).split("\t");
			// throw away the first two fields (rank and score)
			// hack around the fact that parsing this goddamn file format is hardcoded in like 20 different places
			List<String> tokens = Arrays.asList(toks).subList(2, toks.length);

			String frame = tokens.get(1).trim();
			String[] span = tokens.get(3).split("_");
			int start = parseInt(span[0]);
			int end = parseInt(span[span.length-1]);
			ArrayList<Integer> feLineNums = frameIndexMap.get(i);
			if(feLineNums.size() > 0) {
				String assocFeLine = feLines.get(feLineNums.get(0));
				String[] aFLToks = assocFeLine.split("\t");
				int aStart = parseInt(aFLToks[3]);
				int aEnd = parseInt(aFLToks[4]);
				if (start != aStart || end != aEnd) {
					throw new RuntimeException("Problem with frameline:" + frameLines.get(i));
				}
			}
			FrameFeatures f = new FrameFeatures(frame, start, end);
			for (Integer feLineNum1 : feLineNums) {
				final String feLine = feLines.get(feLineNum1);
				final String[] feLine1Toks = feLine.split("\t");
				f.fElements.add(feLine1Toks[1]);
				f.fElementSpansAndFeatures.add(spansList.get(feLineNum1));
			}
			frameFeaturesList.add(f);
		}	
		System.out.println("Checked all frame lines.");
	}

	/** Read lines from a file ignoring empty lines */
	private List<String> readLines(String filename) throws IOException {
		ArrayList<String> lines = Lists.newArrayList();
		BufferedReader bReader = null;
		try {
			bReader = new BufferedReader(new FileReader(filename));
			String line;
			while((line = bReader.readLine()) != null) {
				line = line.trim();
				if(line.equals("")) continue;
				lines.add(line);
			}
			return lines;
		} finally { closeQuietly(bReader); }
	}
}
