/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * SelectBestGraph.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.identification.training;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import edu.cmu.cs.lti.ark.fn.data.prep.ParsePreparation;

public class SelectBestGraph {
	public static void main(String[] args) throws IOException {
		String dir = "/mal2/dipanjan/experiments/FramenetParsing/fndata-1.5/ACLSplits";
		String[] mu = {"0.01", "0.1", "0.3", "0.5", "1.0"};
		//String[] a = {"0.2", "0.5", "0.8"};
		String[] a = {"0.0"};
		String[] t = {"2", "3", "5", "10"};
		String maxfile = null;
		double maxacc = -Double.MAX_VALUE;
		for (int al = 0; al < a.length; al++) {
			for (int k = 5; k <= 20; k = k + 5) {
				for (int m = 0; m < mu.length; m++) {
					for (int tl = 0; tl < t.length; tl++) {
						String resultfile = 
							"smoothed.graph.a."+a[al]+".k."+k+".mu."+mu[m]+".nu.0.000001.t."+t[tl]+".jobj.gz_results";
						System.out.println("Result file:"+resultfile);
						boolean found = true;
						for (int cv = 0; cv <= 4; cv++)	{
							String file = dir + "/" + cv + "/results/" + resultfile;
							File f = new File(file);
							if (!f.exists()) {
								found = false;
								break;
							}
						}
						if (!found) {
							continue;
						}
						double total = 0.0;
						double correct = 0.0;
						found = true;
						ArrayList<String> indresults = new ArrayList<String>();
						for (int cv = 0; cv <= 4; cv++)	{
							String file = dir + "/" + cv + "/results/" + resultfile;
							List<String> sents =
								ParsePreparation.readLines(file);
							int size = sents.size()-1;
							double tot = 0.0;
							double corr = 0.0;
							for (int l = 0; l <= size; l++) {
								if (sents.get(l).contains("Fscore")) {
									String line = sents.get(l).trim();
									String[] toks1 = line.split("\\(");
									String last = ""+toks1[toks1.length-1];
									toks1 = last.split("\\)");
									String first = ""+toks1[0];
									toks1 = first.split("/");
									corr = new Double(toks1[0]);
									tot = new Double(toks1[1]);
									break;
								}
							}
							if (corr == 0.0) {
								System.out.println("Problem with:"+resultfile+" split="+cv);
								found = false;
								break;
							}
							indresults.add(corr + " / " + tot);
							correct += corr;
							total += tot;
						}
						if (!found) {
							continue;
						}
						double avg = correct / total;
						if (avg>maxacc) {
							maxacc = avg;
							maxfile = resultfile;
						}
						System.out.println("Done with:"+resultfile + " Avg:"+avg);
						for (int i = 0; i < 5; i++) {
							System.out.println(indresults.get(i));
						}
					}
					
 				}
			}
		}
		System.out.println("Maxfile:"+maxfile);
		System.out.println("Maxacc:"+maxacc);
	}	
}
