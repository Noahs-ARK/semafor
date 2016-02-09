/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * SignificanceTests.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.evaluation;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;
import gnu.trove.THashSet;
import gnu.trove.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;


/**
 * Performs significance testing for the outputs of various stages of two semantic parsing systems.
 * Employs the testing procedure used by Dan Bikel for parses, described below 
 * (from <a href="http://www.cis.upenn.edu/~dbikel/software.html#comparator">http://www.cis.upenn.edu/~dbikel/software.html#comparator</a>):
 * 
 * <blockquote cite="http://www.cis.upenn.edu/~dbikel/software.html#comparator">
 * <p>The test employed is a type of “stratified shuffling” (which in turn is a type of 
 * “compute-intensive randomized test”). In this testing method, the null hypothesis is that 
 * the two models that produced the observed results are the same, such that for each test 
 * instance (sentence that was parsed), the two observed scores are equally likely. This null 
 * hypothesis is tested by randomly shuffling individual sentences’ scores between the two 
 * models and then re-computing the evaluation metrics (precision and recall, in this case). 
 * If the difference in a particular metric after a shuffling is equal to or greater than the 
 * original observed difference in that metric, then a counter for that metric is incremented. 
 * Ideally, one would perform all 2<i>n</i> shuffles, where <i>n</i> is the number of test cases (sentences), 
 * but given that this is often prohibitively expensive, the default number of iterations is 
 * 10,000. After all iterations, the likelihood of incorrectly rejecting the null is simply 
 * (<i>nc</i> + 1)/(<i>nt</i> + 1), where <i>nc</i> is the number of random differences greater than the original 
 * observed difference, and <i>nt</i> is the total number of iterations.</p>
 * 
 * <p>Caveat: This type of testing method assumes independence between test instances 
 * (sentences). This is not a bad assumption for parsing results, but is not correct, either.</p>
 * </blockquote>
 * 
 * @author dipanjan
 *
 */
public class SignificanceTests
{
	/** Number of samples to extract by randomly swapping sentences from the two system outputs (a.k.a. <i>nt</i>) */
	private static final int TOTAL_TIMES = 10000;
	
	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.err.println("Usage: SignificanceTests <file1> <file2>");
			System.exit(1);
		}
		String[] filenames = convertToNiceFormat(args[0], args[1]);
		frameIdenSigTests(2, filenames[0], filenames[1]);
	}

	/**
	 * Perform significance testing for a pair of frame identification results.
	 * @param flag Metric being compared: {@code 0} for precision, {@code 1} for recall, or {@code 2} for F1 score
	 */
	public static void frameIdenSigTests(int flag, String system1File, String system2File) throws IOException {
		Random r = new Random(new Date().getTime());
		List<String> system1Lines = ParsePreparation.readLines(system1File);
		List<String> system2Lines = ParsePreparation.readLines(system2File);
		double sys1Metric=getNumber(system1Lines,flag);
		double sys2Metric=getNumber(system2Lines,flag);
		double actualDiff = sys1Metric-sys2Metric;
		System.out.println("sys1Metric="+sys1Metric+" sys2Metric="+sys2Metric+" Difference:"+actualDiff);
		int size = system1Lines.size();
		
		int nc = 0;
		for(int j = 0; j < TOTAL_TIMES; j ++)
		{
			ArrayList<String> sample1Lines = new ArrayList<String>();
			ArrayList<String> sample2Lines = new ArrayList<String>();
			
			for(int i = 0; i < size; i ++)
			{
				double rN = r.nextDouble();
				if(rN>=0.5)
				{
					sample1Lines.add(system1Lines.get(i));
					sample2Lines.add(system2Lines.get(i));
				}
				else
				{
					sample2Lines.add(system1Lines.get(i));
					sample1Lines.add(system2Lines.get(i));
				}
			}	
			double sample1Metric=getNumber(sample1Lines,flag);
			double sample2Metric=getNumber(sample2Lines,flag);
			double diff = sample1Metric-sample2Metric;
			//System.out.println("sys1Metric="+sample1Metric+" sys2Metric="+sample2Metric+" Difference:"+diff);
			if(diff>=actualDiff)
				nc++;
		}
		double p = (double)(nc+1)/(TOTAL_TIMES+1);
		System.out.println("p-value:"+p);
	}
	
	/**
	 * Perform significance testing for a pair of full frame parsing results.
	 * @param flag Metric being compared: {@code 0} for precision, {@code 1} for recall, or {@code 2} for F1 score
	 */
	public static void fullSigTests(int flag, String system1File, String system2File) throws IOException {
		Random r = new Random(new Date().getTime());
		List<String> system1Lines = ParsePreparation.readLines(system1File);
		List<String> system2Lines = ParsePreparation.readLines(system2File);
		double sys1Metric=getNumber(system1Lines,flag);
		double sys2Metric=getNumber(system2Lines,flag);
		double actualDiff = sys1Metric-sys2Metric;
		System.out.println("sys1Metric="+sys1Metric+" sys2Metric="+sys2Metric+" Difference:"+actualDiff);
		int size = system1Lines.size();
		
		int nc = 0;
		for(int j = 0; j < TOTAL_TIMES; j ++)
		{
			ArrayList<String> sample1Lines = new ArrayList<String>();
			ArrayList<String> sample2Lines = new ArrayList<String>();
			
			for(int i = 0; i < size; i ++)
			{
				double rN = r.nextDouble();
				if(rN>=0.5)
				{
					sample1Lines.add(system1Lines.get(i));
					sample2Lines.add(system2Lines.get(i));
				}
				else
				{
					sample2Lines.add(system1Lines.get(i));
					sample1Lines.add(system2Lines.get(i));
				}
			}	
			double sample1Metric=getNumber(sample1Lines,flag);
			double sample2Metric=getNumber(sample2Lines,flag);
			double diff = sample1Metric-sample2Metric;
			System.out.println("sys1Metric="+sample1Metric+" sys2Metric="+sample2Metric+" Difference:"+diff);
			if(diff>=actualDiff)
				nc++;
		}
		double p = (double)(nc+1)/(TOTAL_TIMES+1);
		System.out.println("p-value:"+p);
	}
	
	/**
	 * Perform significance testing for a pair of target identification (a.k.a. segmentation) results.
	 * @param flag Metric being compared: {@code 0} for precision, {@code 1} for recall, or {@code 2} for F1 score
	 */
	public static void segmentationSigTests(int flag, String system1File, String system2File) throws IOException {
		Random r = new Random(new Date().getTime());
		List<String> system1Lines = ParsePreparation.readLines(system1File);
		List<String> system2Lines = ParsePreparation.readLines(system2File);
		double sys1Metric=getNumber(system1Lines,flag);
		double sys2Metric=getNumber(system2Lines,flag);
		double actualDiff = sys1Metric-sys2Metric;
		System.out.println("sys1Metric="+sys1Metric+" sys2Metric="+sys2Metric+" Difference:"+actualDiff);
		int size = system1Lines.size();
		
		int nc = 0;
		for(int j = 0; j < TOTAL_TIMES; j ++)
		{
			ArrayList<String> sample1Lines = new ArrayList<String>();
			ArrayList<String> sample2Lines = new ArrayList<String>();
			
			for(int i = 0; i < size; i ++)
			{
				double rN = r.nextDouble();
				if(rN>=0.5)
				{
					sample1Lines.add(system1Lines.get(i));
					sample2Lines.add(system2Lines.get(i));
				}
				else
				{
					sample2Lines.add(system1Lines.get(i));
					sample1Lines.add(system2Lines.get(i));
				}
			}	
			double sample1Metric=getNumber(sample1Lines,flag);
			double sample2Metric=getNumber(sample2Lines,flag);
			double diff = sample1Metric-sample2Metric;
			System.out.println("sys1Metric="+sample1Metric+" sys2Metric="+sample2Metric+" Difference:"+diff);
			if(diff>=actualDiff)
				nc++;
		}
		System.out.println("NC="+nc);
		double p = (double)(nc+1)/(TOTAL_TIMES+1);
		System.out.println("p-value:"+p);
	}
	
	
	public static double getNumber(List<String> resLines, int flag)
	{
		double totalMatched = 0.0;
		double totalGold = 0.0;
		double totalFound = 0.0;
		for(String line:resLines)
		{
			String[] toks = line.split("/");
			double m = new Double(toks[0].trim());
			double f = new Double(toks[1].trim());
			double g = new Double(toks[2].trim());
			totalMatched+=m;
			totalFound+=f;
			totalGold+=g;
		}
		double prec = totalMatched/totalFound;
		double recall = totalMatched/totalGold;
		double f = 2*prec*recall/(prec+recall);
		if(flag==0)
			return prec;
		else if(flag==1)
			return recall;
		else
			return f;
	}

	public static void convertToNiceFormatSegmentation(String filename1, String filename2,
													   String segmfile1, String segmfile2,
													   String goldfile) throws IOException {
		getSegmentationResultsForASystem(filename1, segmfile1, goldfile);
		getSegmentationResultsForASystem(filename2, segmfile2, goldfile);
	}
	
	
	public static void getSegmentationResultsForASystem(String file, String outFile, String goldFile) throws IOException {
		List<String> goldStuff = ParsePreparation.readLines(goldFile);

		TIntObjectHashMap<THashSet<String>> goldSpans = new TIntObjectHashMap<THashSet<String>>();
		for(String gold:goldStuff)
		{
			String[] toks = gold.split("\t");
			int sentNum = Integer.parseInt(toks[5]);
			THashSet<String> set = goldSpans.get(sentNum);
			if(set==null)
			{
				set=new THashSet<String>();
				set.add(toks[3]);
				goldSpans.put(sentNum, set);
			}
			else
			{
				set.add(toks[3]);
				goldSpans.put(sentNum, set);
			}
		}	
		TIntObjectHashMap<THashSet<String>> modelSpans = new TIntObjectHashMap<THashSet<String>>();
		List<String> modelStuff = ParsePreparation.readLines(file);
		for(String line:modelStuff)
		{
			String[] toks = PaperEvaluation.getTokens(line);
			int sentNum = Integer.parseInt(toks[toks.length-1]);
			String span = toks[toks.length-3];
			THashSet<String> set = modelSpans.get(sentNum);
			if(set==null)
			{
				set=new THashSet<String>();
				set.add(span);
				modelSpans.put(sentNum, set);
			}
			else
			{
				set.add(span);
				modelSpans.put(sentNum, set);
			}
			
		}
		THashSet<Integer> keySet = new THashSet<Integer>();
		for(int i = 0; i <= 119; i ++)
		{
			keySet.add(i);
		}
		Integer[] keys = new Integer[keySet.size()];
		keySet.toArray(keys);
		Arrays.sort(keys);
		
		ArrayList<String> outLines = new ArrayList<String>();
		for(Integer key:keys)
		{
			THashSet<String> goldSet = goldSpans.get(key);
			THashSet<String> modelSet = modelSpans.get(key);
			int goldSize = 0;
			int modelSize = 0;
			int matches = 0;
			if(goldSet!=null)
			{
				goldSize = goldSet.size();
				if(modelSet!=null)
				{
					modelSize=modelSet.size();
					for(String span:goldSet)
					{
						if(modelSet.contains(span))
							matches++;
					}
				}
			}
			outLines.add(matches+" / "+modelSize+" / "+goldSize);
			System.out.println(matches+" / "+modelSize+" / "+goldSize);
		}
		ParsePreparation.writeSentencesToFile(outFile, outLines);
	}
	
	
	public static String[] convertToNiceFormat(String filename1, String filename2)
	{
		String[] files = {filename1, filename2};
		String[] formattedFiles = {files[0]+"_formatted", files[1]+"_formatted"};

		for(int i = 0; i < 2; i ++)
		{
			ArrayList<String> resLines = new ArrayList<String>();
			try
			{
				BufferedReader bReader = new BufferedReader(new FileReader(files[i]));
				String line = null;
				while((line=bReader.readLine())!=null)
				{
					line=line.trim();
					if(line.startsWith("Total:"))
					{
						line=line.substring(7).trim();
						resLines.add(line);
					}
				}
				bReader.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
			double totalMatched = 0.0;
			double totalGold = 0.0;
			double totalFound = 0.0;
			for(String line:resLines)
			{
				String[] toks = line.split("/");
				double m = new Double(toks[0].trim());
				double f = new Double(toks[1].trim());
				double g = new Double(toks[2].trim());
				totalMatched+=m;
				totalFound+=f;
				totalGold+=g;
			}
			double prec = totalMatched/totalFound;
			double recall = totalMatched/totalGold;
			double f = 2*prec*recall/(prec+recall);
			System.out.print("Precision:"+prec+" ");
			System.out.print("Recall:"+recall+" ");
			System.out.print("F1 score:"+f+"\n");
			ParsePreparation.writeSentencesToFile(formattedFiles[i], resLines);
		}
		return formattedFiles;
	}
	
}
