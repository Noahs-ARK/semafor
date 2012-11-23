/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * Rerank.java is part of SEMAFOR 2.0.
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

import java.io.BufferedInputStream; //import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet; //import java.util.List;
import java.util.Scanner;

import edu.cmu.cs.lti.ark.util.FileUtil;

public class Rerank {
	public static BufferedInputStream bis;
	public String eventsfilename;
	public static PrintStream ps;
	public static int numFeatures;
	public double[] w;
	
	/**@brief maximum total number of frame element combinations*/
	public static final int K = 100;

	public static boolean train = true;

	public static int start_idx[]={146,178,280,329};
	
	public static void main(String[] args) {
		FEFileName.alphafilename=FEFileName.tmpDirname+"parser.conf";
		FEFileName.modelFilename=FEFileName.tmpDirname+"model.out";
		Rerank rrk = new Rerank();

		for(int i=0;i<start_idx.length;i++){
			FEFileName.rrkfefilename=FEFileName.tmpDirname+ "rerank.frame.elements"+start_idx[i];
			FEFileName.listfilename =FEFileName.tmpDirname+ "dev.list"+start_idx[i];
			rrk.eventsfilename =FEFileName.tmpDirname+ "bin"+start_idx[i]; 
			rrk.genList();
			rrk.writeFEFile(FEFileName.tmpDirname+"span"+start_idx[i],
					"lrdata/dev.frame.elements"+start_idx[i], FEFileName.rrkfefilename);
		}
		
		FEFileName.rrkfefilename = FEFileName.tmpDirname+ "rerank.train.frame.elements";
		FEFileName.listfilename  =FEFileName.tmpDirname+  "train.list";
		rrk.eventsfilename = FEFileName.tmpDirname+ "parser.events.bin";
	//	rrk.genList();
	//	rrk.writeFEFile(
	//			FEFileName.tmpDirname+ "train.span",
	//			"lrdata/semeval.fulltrain.sentences.frame.elements",
	//			FEFileName.rrkfefilename);

	}

	public Rerank() {
		readModel();
	}
	/**@brief generate a sorted list of probabilities of
		candidate spans given model, feature indices.
		
	*/
	public void genList() {
		bis = new BufferedInputStream(FileUtil.openInputStream(eventsfilename));
		ps = FileUtil.openOutFile(FEFileName.listfilename );
		int[] line = readALine(bis);
		int count = 0;
		ArrayList<int[]> temp = new ArrayList<int[]>();
		while (line.length > 0) {
			while (line.length > 0) {
				temp.add(line);
				line = readALine(bis);
			}
			double weiFeatSum[] = new double[temp.size()];
			ArrayList<Score> scoreList = new ArrayList<Score>();
			double sumScore = 0;
			double exp =0;
			for (int i = 0; i <temp.size() ; i++) {
				weiFeatSum[0] = w[0];
				for (int j = 0; j < temp.get(i).length; j++) {
					if (temp.get(i)[j] != 0)
						weiFeatSum[i] += w[temp.get(i)[j]];
				}
				exp = Math.exp(weiFeatSum[i]);
				sumScore += exp;
				scoreList.add(new Score(i, exp));
			}
			//ugly hack
			if(scoreList.size()<FrameElement.NUM_CANDIDATES){
				Score last=scoreList.get(scoreList.size()-1);
				for(int i=scoreList.size();i<FrameElement.NUM_CANDIDATES;i++){
					scoreList.add(last);
				}
			}
			Collections.sort(scoreList);
			for (int i = 0; i < FrameElement.NUM_CANDIDATES; i++) {
				ps.print(scoreList.get(i).i + "\t"
						+ Math.log(scoreList.get(i).s / sumScore) + "\t");
			}
			ps.println(scoreList.size() - 1);
			temp = new ArrayList<int[]>();
			line = readALine(bis);
			count++;
		}
		ps.close();
	}

	public static int[] readALine(InputStream fis) {
		ArrayList<Integer> temp = new ArrayList<Integer>();
		int[] ret;
		int n = readAnInt(fis);
		while (n != -1) {
			temp.add(n);
			n = readAnInt(fis);
		}
		ret = new int[temp.size()];
		for (int i = 0; i < temp.size(); i++) {
			ret[i] = temp.get(i);
		}
		return ret;
	}

	public static int readAnInt(InputStream fis) {
		byte[] b = new byte[4];
		try {
			fis.read(b);
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());
		}
		int ret = 0;
		ret += ((int) b[0] & 0xff) << 24;
		ret += ((int) b[1] & 0xff) << 16;
		ret += ((int) b[2] & 0xff) << 8;
		ret += ((int) b[3] & 0xff);
		return ret;
	}

	public void readModel() {
		Scanner localsc = FileUtil.openInFile(FEFileName.alphafilename);
		Scanner paramsc = FileUtil.openInFile(FEFileName.modelFilename);
		numFeatures = localsc.nextInt() + 1;
		localsc.close();
		w = new double[numFeatures];
		for (int i = 0; i < numFeatures; i++) {
			double val = Double.parseDouble(paramsc.nextLine());
			w[i] = val;
		}
	}

	public class Score implements Comparable<Score> {
		public int i;
		public double s;

		public int compareTo(Score sa) {
			if (sa.s > s)
				return 1;
			if (sa.s < s)
				return -1;
			return 0;
		}

		public Score(int index, double score) {
			i = index;
			s = score;
		}
	}

	public void writeFEFile(String spanFilename, String feFilename,
			String outFilename) {
		Scanner psc, spansc, fsc;
		
		psc = FileUtil.openInFile(FEFileName.listfilename );
		spansc = FileUtil.openInFile(spanFilename);
		fsc = FileUtil.openInFile(feFilename);
		PrintStream localps = FileUtil.openOutFile(outFilename);
		String spantoks[] = spansc.nextLine().split("\t");
		int max = 0;
		int count = 0;
		int findex = 0;
		int spanindex = 0;
		while (fsc.hasNextLine()) {
			String fline = fsc.nextLine().trim();
			if (count % 10 == 0)
				System.out.print(".");
			if (count % 100 == 0)
				System.out.println(count);
			count++;
			String ftoks[] = fline.split("\t");
			if (ftoks.length < 3)
				break;
			ArrayList<FrameElement> felist = new ArrayList<FrameElement>();
			spanindex = Integer.parseInt(spantoks[5]);
			while (spanindex == findex) {

				String span[] = spansc.nextLine().split("\t");
				ArrayList<Integer> canstart, canend;
				canstart = new ArrayList<Integer>();
				canend = new ArrayList<Integer>();
				while (span.length > 1) {
					canstart.add(Integer.parseInt(span[0]));
					canend.add(Integer.parseInt(span[1]));
					span = spansc.nextLine().split("\t");
				}
				int prediction;
				// predicted start and end index of frame element
				int pstart[] = new int[FrameElement.NUM_CANDIDATES];
				int pend[] = new int[FrameElement.NUM_CANDIDATES];
				double logProb[] = new double[FrameElement.NUM_CANDIDATES];
				for (int i = 0; i < pstart.length; i++) {
					prediction = psc.nextInt();
					pstart[i] = canstart.get(prediction);
					pend[i] = canend.get(prediction);
					logProb[i] = psc.nextDouble();
				}
				psc.nextInt();
				felist
						.add(new FrameElement(spantoks[1], pstart, pend,
								logProb));
				if (!spansc.hasNextLine())
					break;
				spantoks = spansc.nextLine().split("\t");
				spanindex = Integer.parseInt(spantoks[5]);
			}
			if (felist.size() >= 15) {
				System.out.println("warning");
			}
			max = Math.max(max, felist.size());
			localps.print("0\t" + (felist.size() + 1));
			for (int i = 1; i < ftoks.length; i++) {
				localps.print("\t" + ftoks[i]);
			}
			localps.println();
			// print gold standard if generating file for training
			if (train) {
				localps.print("0");
				double lprob = 0;
				for (int i = 6; i < ftoks.length; i += 2) {
					localps.print("\t" + ftoks[i] + "\t" + ftoks[i + 1]);
					String fespantoks[] = ftoks[i + 1].split(":");
					int start = Integer.parseInt(fespantoks[0]);
					int end = start;
					if (fespantoks.length > 1)
						end = Integer.parseInt(fespantoks[1]);
					FrameElement goldfe = new FrameElement(ftoks[i], start, end);
					for (FrameElement fe : felist) {
						if (fe.n.equals(goldfe.n)) {
							fe.choice = 0;
							double localscore = 0;
							lprob += fe.getProb();
							if (fe.getStart() == goldfe.getStart()
									&& fe.getEnd() == goldfe.getEnd()) {

								localscore += fe.getProb();
								break;
							}
							fe.choice = 1;
							lprob += fe.getProb();
							if (fe.getStart() == goldfe.getStart()
									&& fe.getEnd() == goldfe.getEnd()) {

								localscore += fe.getProb();
								break;
							}
							lprob += Math.log((1 - Math.exp(lprob)) / 10);
						}
					}
				}
				localps.print("\t" + lprob);
				localps.println();
			}
			ArrayList<Combination> comblist = new ArrayList<Combination>();
			for (int i = 0; i <= (Math.pow(FrameElement.NUM_CANDIDATES,felist.size())) - 1; i++) {
				for (int j = 0; j < felist.size(); j++) {
					int choice = get_bit(i, j,FrameElement.NUM_CANDIDATES);
					FrameElement fe = felist.get(j);
					fe.choice = choice;
				}
				boolean same = true;
				if (train) {
					// dont add gold standard to candidate list if training
					HashSet<FrameElement> feset = new HashSet<FrameElement>();
					for (int j = 6; j < ftoks.length; j += 2) {
						String fespantoks[] = ftoks[j + 1].split(":");
						int start = Integer.parseInt(fespantoks[0]);
						int end = start;
						if (fespantoks.length > 1)
							end = Integer.parseInt(fespantoks[1]);
						{
							FrameElement fe = new FrameElement(ftoks[j], start,
									end);
							feset.add(fe);
							// System.out.println(fe.hashCode()+"\t"+fe);
						}
					}

					for (FrameElement fe : felist) {
						if (!fe.present())
							continue;
						// System.out.println(fe.hashCode()+"\t"+fe);
						if (!feset.remove(fe)) {
							same = false;
						}
					}
					if (!feset.isEmpty()) {
						same = false;
					}
				}
				if (!same) {
					Combination comb=new Combination(felist,i);
					if(!hasDup(comb,felist))
						comblist.add(comb);
				} else {
					// System.out.println("same as gold");
				}
			}
			Collections.sort(comblist);

			int cannum = 0;
			for (int i = 0; i < comblist.size(); i++) {

				Combination comb = comblist.get(i);
				comb.setChoice();
				
				localps.print(cannum);

				for (int j = 0; j < comb.felist.size(); j++) {
					FrameElement fe = comb.felist.get(j);
					if (fe.present()) {
						localps.print("\t" + fe.toString());
					}
				}
				localps.print("\t" + comb.lprob);

				localps.println();
				cannum++;
				if (cannum >= K)
					break;
			}
			localps.println(K);
			findex++;
		}
		localps.close();
		psc.close();
		spansc.close();
		fsc.close();
		System.out.println(max);
		// System.out.println(numdupfe);
	}

	public boolean isSameFrame(String[] spantoks, String[] tokenNums,
			String frameName) {
		return spantoks[2].equals(frameName)
				&& spantoks[3].equals(tokenNums[0])
				&& spantoks[4].equals(tokenNums[tokenNums.length - 1]);
	}
	/**
	 * 
	 * @param x
	 * @param i
	 * @param carry carry how many choices per digit
	 * @return
	 */
	public static int get_bit(int x, int i,int carry) {
		int t=x%(int)Math.pow(carry, i+1);
		t=t/(int)Math.pow(carry, i);
		return t;
	}

	public static boolean isOverlap(int s1[], int s2[]) {
		if (s1[0] >= s2[0] && s2[1] >= s1[0]) {
			return true;
		}
		if (s1[0] <= s2[0] && s2[0] <= s1[1]) {
			return true;
		}
		return false;
	}
	public static boolean hasDup(Combination comb,ArrayList<FrameElement >felist){
		boolean hasDup = false;
		for (int j = 0; j < comb.felist.size(); j++) {
			if (!felist.get(j).present())
				continue;
			int[] s1 = new int[2];
			s1[0] = felist.get(j).getStart();
			s1[1] = felist.get(j).getEnd();
			for (int k = j + 1; k < comb.felist.size(); k++) {
				if (!felist.get(k).present())
					continue;
				int[] s2 = new int[2];
				s2[0] = felist.get(k).getStart();
				s2[1] = felist.get(k).getEnd();
				if (isOverlap(s1, s2)) {
					hasDup = true;
					break;
				}
			}
			if (hasDup)
				break;
		}
		return hasDup;
	}
}
