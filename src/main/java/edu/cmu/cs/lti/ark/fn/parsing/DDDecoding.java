/*******************************************************************************
 * Copyright (c) 2012 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * CommandLineOptions.java is part of SEMAFOR 2.1.
 * 
 * SEMAFOR 2.1 is free software: you can redistribute it and/or modify  it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * SEMAFOR 2.1 is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. 
 * 
 * You should have received a copy of the GNU General Public License along
 * with SEMAFOR 2.1.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package edu.cmu.cs.lti.ark.fn.parsing;

import edu.cmu.cs.lti.ark.fn.utils.BitOps;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import gnu.trove.THashMap;
import gnu.trove.TIntHashSet;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class DDDecoding implements JDecoding {
	private Map<String, Set<Pair<String, String>>> excludesMap;
	private Map<String, Set<Pair<String, String>>> requiresMap;
	public static final double TAU = 1.5;
	public static final double RHO_START = 0.03;
	public static final double MAX_RHO = 66.67;
	public static final double MIN_RHO = 0.00067;
	public static final int TOTAL_AD3_ITERATIONS = 1000;
	private String mFactorFile;
	private boolean WRITE_FACTORS_TO_FILE = false;
	private BufferedWriter bWriter = null;
	public static final int STATUS_OPTIMAL_INTEGER = 1;
	public static final int STATUS_OPTIMAL_FRACTIONAL = 2;
	public static final int STATUS_INFEASIBLE = 3;
	public static final int STATUS_UNSOLVED = 4;
	public static final double RESIDUAL_THRESH = 0.0000001;
	public static final int NUM_ITERATIONS_COMPUTE_DUAL = 50;
	public static boolean mExact = false;
	
	public DDDecoding(boolean exact) {
		mExact = exact;
	}
	
	public void setMaps(Map<String, Set<Pair<String, String>>> excludesMap, 
						Map<String, Set<Pair<String, String>>> requiresMap) {
		this.excludesMap = excludesMap;
		this.requiresMap = requiresMap;
	}
	
	public Map<String, Pair<String, Double>> decode(Map<String, Pair<int[], Double>[]> scoreMap, 
									  String frame,
									  boolean costAugmented,
									  FrameFeatures goldFF) {
		if (WRITE_FACTORS_TO_FILE) {
			try {
				if (bWriter == null) {
					bWriter = new BufferedWriter(new FileWriter(mFactorFile));
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.exit(-1);
			}
		}
		Map<String, Pair<String, Double>> res = new THashMap<String, Pair<String, Double>>();
		if (scoreMap.size() == 0) {
			return res;
		}
		String[] keys = new String[scoreMap.size()];
		scoreMap.keySet().toArray(keys);
		Arrays.sort(keys);	
		int totalCount = 0;
		int max = -Integer.MAX_VALUE;
		
		// counting the total number of z variables needed
		// also mapping the role and span indices to a variable index
		int[][] mappedIndices = new int[keys.length][];
		int count = 0;
		for (int i = 0; i < keys.length; i++) {
			Pair<int[], Double>[] arr = scoreMap.get(keys[i]);
			totalCount += arr.length;
			mappedIndices[i] = new int[arr.length];
			for (int j = 0; j < arr.length; j++) {
				int start = arr[j].first[0];
				int end = arr[j].first[1];
				if (start != -1) {
					if (start > max) {
						max = start;
					}
				}
				if (end != -1) {
					if (end > max) {
						max = end;
					}
				}
				mappedIndices[i][j] = count;
				count++;
			}
		}		
		System.out.println("Max index:" + max);
		TIntHashSet[] overlapArray = new TIntHashSet[max+1];
		for (int i = 0; i < max+1; i++) {
			overlapArray[i] = new TIntHashSet();
		}
		
		// counting number of required slaves
		// doing this to add slack variables for required slaves
		ArrayList<int[]> requiredSets = new ArrayList<int[]>();
		if (requiresMap.containsKey(frame)) {
			Set<Pair<String, String>> set = requiresMap.get(frame);
			for (Pair<String, String> p: set) {
				String one = p.first;
				String two = p.second;
				int oneIndex = Arrays.binarySearch(keys, one);
				if (oneIndex < 0) {
					continue;
				}
				int twoIndex = Arrays.binarySearch(keys, two);
				if (twoIndex < 0) {
					continue;
				}
				System.out.println("Found two FEs with a requires relationship: " + one + "\t" + two);
				
				int nullIndex1 = -1;
				int nullIndex2 = -1;
				Pair<int[], Double>[] arr1 = scoreMap.get(one);
				Pair<int[], Double>[] arr2 = scoreMap.get(two);
				for (int j = 0; j < scoreMap.get(one).length; j++) {
					if (arr1[j].first[0] == -1 && arr1[j].first[1] == -1) {
						nullIndex1 = mappedIndices[oneIndex][j];
						break;
					}
				}				
				for (int j = 0; j < scoreMap.get(two).length; j++) {
					if (arr2[j].first[0] == -1 && arr2[j].first[1] == -1) {
						nullIndex2 = mappedIndices[twoIndex][j];
						break;
					}
				}
				int[] a1 = new int[2];
				a1[0] = nullIndex1;
				a1[1] = nullIndex2;
				requiredSets.add(a1);
			}
		}
		
		double[] objVals = new double[totalCount];
		double[] costs = new double[totalCount];
		
		// adding costs to the objVals for cost augmented decoding
		if (costAugmented) {
			List<String> fes = goldFF.fElements;
			for (int i = 0; i < fes.size(); i++) {
				String fe = fes.get(i);
				int index = Arrays.binarySearch(keys, fe);
				if (index < 0) {
					System.out.println("Problem. Fe: " + fe + " not found in array. Exiting.");
					System.exit(-1);
				}
				Pair<int[], Double>[] arr = scoreMap.get(keys[index]);
				int[] goldSpan = goldFF.fElementSpansAndFeatures.get(i)[goldFF.goldSpanIdxs.get(i)].span;
				for (int j = 0; j < arr.length; j++) {
					if (arr[j].first[0] == goldSpan[0] &&
					    arr[j].first[1] == goldSpan[1]) {
						costs[mappedIndices[index][j]] = 0.0;
					} else {
						costs[mappedIndices[index][j]] = 1.0;
					}
				}
			}
		}		
		count = 0;
		for (int i = 0; i < keys.length; i++) {
			Pair<int[], Double>[] arr = scoreMap.get(keys[i]);
			for (int j = 0; j < arr.length; j++) {
				objVals[count] = arr[j].second;
				if (costAugmented) {
					objVals[count] += costs[count];
				}
				int start = arr[j].first[0];
				int end = arr[j].first[1];
				if (start != -1 && end != -1) {
					for (int k = start; k <= end; k++) {
						overlapArray[k].add(count);
					}
				}
				count++;
			}
		}
		// fixing the rest of the variables to zero
		for (int i = count; i < objVals.length; i++) {
			objVals[i] = 0.0;
		}	
		
		// counting number of exclusion slaves
		ArrayList<int[]> exclusionSets = new ArrayList<int[]>();
		if (excludesMap.containsKey(frame)) {
			Set<Pair<String, String>> set = excludesMap.get(frame);	
			for (Pair<String, String> p: set) {
				String one = p.first;
				String two = p.second;
				int oneIndex = Arrays.binarySearch(keys, one);
				if (oneIndex < 0) {
					continue;
				}
				int twoIndex = Arrays.binarySearch(keys, two);
				if (twoIndex < 0) {
					continue;
				}
				System.out.println("Found two mutually exclusive FEs: " + one + "\t" + two);
				int nullIndex1 = -1;
				int nullIndex2 = -1;
				count = 0;
				Pair<int[], Double>[] arr1 = scoreMap.get(one);
				Pair<int[], Double>[] arr2 = scoreMap.get(two);
				for (int j = 0; j < scoreMap.get(one).length; j++) {
					if (arr1[j].first[0] == -1 && arr1[j].first[1] == -1) {
						nullIndex1 = mappedIndices[oneIndex][j];
						break;
					}
				}
				for (int j = 0; j < scoreMap.get(two).length; j++) {
					if (arr2[j].first[0] == -1 && arr2[j].first[1] == -1) {
						nullIndex2 = mappedIndices[twoIndex][j];
						break;
					}
					count++;
				}
				int[] arr = new int[2];
				arr[0] = nullIndex1;
				arr[1] = nullIndex2;
				exclusionSets.add(arr);
			}
		}
		
		int len = objVals.length;
		int[] deltaarray = new int[len];
		int numExclusionSlaves = exclusionSets.size();
		int numRequiredSlaves = requiredSets.size();
		int slavelen = keys.length + max + 1 + numExclusionSlaves + numRequiredSlaves;
		int[][] slaveparts = new int[slavelen][];
		int[][] partslaves = new int[len][];
		Arrays.fill(deltaarray, 0);
		
		if (WRITE_FACTORS_TO_FILE) {
			try {
				bWriter.write(objVals.length + "\n");
				bWriter.write(slavelen + "\n");
				for (int i = 0; i < objVals.length; i++) {
					bWriter.write(objVals[i] + "\n");
				}
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Could not write length of variables");
				System.exit(-1);
			}
		}
		
		// creating deltaarray
		// for every unique span slave
		for (int i = 0; i < keys.length; i++) {
			for (int j = 0; j < mappedIndices[i].length; j++) {
				deltaarray[mappedIndices[i][j]] += 1;
			}
		}		
		// for the overlap slaves
		for (int i = keys.length; i < keys.length + max + 1; i++) {
			int[] vars = overlapArray[i-keys.length].toArray();
			for (int v: vars) {
				deltaarray[v] += 1;
			}
		}
		// for the exclusion slaves
		for (int[] vars: exclusionSets) {
			for (int v: vars) {
				deltaarray[v] += 1;
			}
		}
		// for the required slaves
		for (int[] vars: requiredSets) {
			for (int v: vars) {
				deltaarray[v] += 1;
			}
		}		
		// end of creation of deltaarray
				
		double[] thetas = new double[objVals.length];
		for (int i = 0; i < len; i++) {
			thetas[i] = objVals[i] / (double) deltaarray[i];
		}
		
		// creating slaves
		Slave[] slaves = new Slave[slavelen];
		for (int i = 0; i < keys.length; i++) {
			slaves[i] = new UniqueSpanSlave(thetas, 
					   						mappedIndices[i][0], 
					   						mappedIndices[i][mappedIndices[i].length-1] + 1);
			slaveparts[i] = new int[mappedIndices[i].length];
			for (int j = 0; j < mappedIndices[i].length; j++) {
				slaveparts[i][j] = mappedIndices[i][j];
			}
			if (WRITE_FACTORS_TO_FILE) {
				try {
					String line = "XOR ";
					int numVars = mappedIndices[i][mappedIndices[i].length-1] + 1 - 
								  mappedIndices[i][0];
					line += numVars + " ";
					for (int j = mappedIndices[i][0]; 
							 j < mappedIndices[i][mappedIndices[i].length-1] + 1; 
							 j++) {
						line += (j+1) + " ";
					}
					line = line.trim();
					bWriter.write(line + "\n");
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("Could not XOR factors");
					System.exit(-1);
				}
			}
		}
		
		for (int i = keys.length; i < keys.length + max + 1; i++) {
			int[] vars = overlapArray[i-keys.length].toArray();
			slaves[i] = new OverlapSlave(thetas, vars);
			slaveparts[i] = Arrays.copyOf(vars, vars.length);
			if (WRITE_FACTORS_TO_FILE) {
				try {
					String line = "XOR1 " + vars.length + " ";
					for (int var: vars) {
						line += (var+1) + " ";
					}
					line = line.trim();
					bWriter.write(line + "\n");
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("Could not write overlap factors");
					System.exit(-1);
				}
			}
		}
		
		for (int i = keys.length + max + 1; 
				 i < keys.length + max + 1 + numExclusionSlaves; 
				 i++) {
			int[] vars = exclusionSets.get(i - (keys.length + max + 1));
			slaves[i] = new ExclusionSlave(thetas, vars);
			slaveparts[i] = Arrays.copyOf(vars, vars.length);
			if (WRITE_FACTORS_TO_FILE) {
				try {
					String line = "OR " + vars.length + " ";
					for (int var: vars) {
						line += (var+1) + " ";
					}
					line = line.trim();
					bWriter.write(line + "\n");
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("Could not write exclusion set factors");
					System.exit(-1);
				}
			}
		}		
		
		for (int i = keys.length + max + 1 + numExclusionSlaves;
			     i < keys.length + max + 1 + numExclusionSlaves + numRequiredSlaves;
			     i++) {
			int[] vars = requiredSets.get(i - (keys.length + max + 1 + numExclusionSlaves));
			slaves[i] = new RequiredSlave(thetas, vars);
			slaveparts[i] = Arrays.copyOf(vars, vars.length);
			if (WRITE_FACTORS_TO_FILE) {
				try {
					String line = "XOR " + vars.length + " ";
					if (vars.length != 2) {
						System.out.println("Problem. Required set's size is more than 2. Exiting");
						System.exit(-1);
					}
					line += (vars[0]+1) + " -" + (vars[1]+1);
					line = line.trim();
					bWriter.write(line + "\n");
				} catch (IOException e) {
					e.printStackTrace();
					System.out.println("Could not write required factors");
					System.exit(-1);
				}
			}
		}
		
		if (WRITE_FACTORS_TO_FILE) {
			try {
				bWriter.write("\n");
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Could not write required factors");
				System.exit(-1);
			}
		}
		
		for (int s = 0; s < slaveparts.length; s++) {
			Arrays.sort(slaveparts[s]);
		}
		
		double totalDelta = 0.0;
		TIntHashSet[] partslavessets = new TIntHashSet[len];
		for (int i = 0; i < len; i++) {
			totalDelta += deltaarray[i];
			partslavessets[i] = new TIntHashSet();			
			for (int s = 0; s < slavelen; s++) {
				if (Arrays.binarySearch(slaveparts[s], i) >= 0) {
					partslavessets[i].add(s);
				}
			}
			partslaves[i] = partslavessets[i].toArray();
			Arrays.sort(partslaves[i]);
		}		
		double[] u = new double[len];
		Arrays.fill(u, 0.5);
				
		double[] lowerBound0 = new double[1];
		lowerBound0[0] = -Double.MAX_VALUE;
		double[] upperBound0 = new double[1];
		double[] value0 = new double[1];
		
		if (!mExact) {
			runAD3(len, slavelen, u, slaves, totalDelta, 
			   slaveparts, partslaves, deltaarray, TOTAL_AD3_ITERATIONS,
			   objVals,
			   thetas,
			   lowerBound0,
			   value0,
			   upperBound0);
		} else {			
			runAD3ILP(len, slavelen, u, slaves, totalDelta, 
				   slaveparts, partslaves, deltaarray, TOTAL_AD3_ITERATIONS,
				   objVals, thetas,
				   upperBound0,
				   value0,
				   lowerBound0[0]);
		}	
		
		count = 0;
		double totalScore = 0.0;
		for (int i = 0; i < keys.length; i++) {
			Pair<int[], Double>[] arr = scoreMap.get(keys[i]);
			double maxVal = -Double.MAX_VALUE;
			int maxIndex = -1;
			// System.out.println(keys[i]);
			double score = 0.0;
			for (int j = 0; j < arr.length; j++) {
				// String span = arr[j].getFirst()[0] + "_" + arr[j].getFirst()[1];
				// System.out.println(span + " " + u[count]);
				if (u[count] > maxVal) {
					maxVal = u[count];
					maxIndex = j;
					score = objVals[count];
				}
				count++;
			}
			// System.out.println();
			if (maxIndex != -1 && maxVal > 0) {
				totalScore += score;
				Pair<String, Double> p =
					new Pair<String, Double>(arr[maxIndex].first[0] + "_" + arr[maxIndex].first[1], score);
				res.put(keys[i], p);
			}  			
		}
		System.out.println("Solution value: " + totalScore);
		return res;
	}
	
	public int runAD3ILP(int len, int slavelen, double[] mU,
			  Slave[] slaves, double totalDelta,
			  int[][] slaveparts, int[][] partslaves,
			  int[] deltaarray,
			  int niters,
			  double[] objVals,
			  double[] thetas,
			  double[] upperBound0,
			  double[] value0,
			  double lowerBound) {
		double[] bestLowerBound0 = new double[1];
		bestLowerBound0[0] = lowerBound;
		boolean[] branchedVariables = new boolean[mU.length];
		Arrays.fill(branchedVariables, false);
		int status = runBranchAndBound(len, slavelen, mU, slaves, totalDelta, slaveparts,
				                       partslaves, deltaarray, niters, objVals, thetas,
				                       upperBound0, value0, bestLowerBound0, branchedVariables,
				                       0.0);
		System.out.println("Solution value of AD3 ILP: " + value0[0]);
		return status;
	}
	
	public int runBranchAndBound(int len, int slavelen, double[] mU,
			  					 Slave[] slaves, double totalDelta,
			                     int[][] slaveparts, int[][] partslaves,
			                     int[] deltaarray,
			                     int niters,
			                     double[] objVals,
			                     double[] thetas,
			                     double[] bestUpperBound0,
			                     double[] value0,
			                     double[] bestLowerBound0,
			                     boolean[] branchedVariables,
			                     double cumulativeValue) {
		bestLowerBound0[0] += cumulativeValue;
		// solve the LP relaxation
		int status = runAD3(len, slavelen, mU, slaves, totalDelta, slaveparts, partslaves,
							deltaarray, niters, objVals, thetas, bestLowerBound0, value0,
							bestUpperBound0);
		value0[0] -= cumulativeValue;
		bestUpperBound0[0] -= cumulativeValue;
		if (status == STATUS_OPTIMAL_INTEGER) {
			if (value0[0] > bestLowerBound0[0]) {
				bestLowerBound0[0] = value0[0];
			}
			return status;
		} else if (status == STATUS_INFEASIBLE) {
			value0[0] = -Double.MAX_VALUE;
			bestUpperBound0[0] = -Double.MAX_VALUE;
			return status;
		}
		
		// look for the most fractional component
		int variableToBranch = -1;
		double mostFractionalValue = 1.0;
		for (int i = 0; i < mU.length; i++) {
			if (branchedVariables[i]) continue;
			double diff = mU[i] - 0.5;
			diff *= diff;
			if (variableToBranch < 0 || diff < mostFractionalValue) {
				variableToBranch = i;
				mostFractionalValue = diff;
			}
		}
		if (variableToBranch < 0) {
			System.out.println("Branched all variables.");
			return STATUS_UNSOLVED;
		}
		branchedVariables[variableToBranch] = true;
		System.out.println("Branching on variable " + variableToBranch);
		System.out.println("Value: " + mU[variableToBranch]);
		
		double infinitePotential = 1000.0 * deltaarray[variableToBranch];
		double originalPotential = objVals[variableToBranch];
		objVals[variableToBranch] -= infinitePotential;
		thetas[variableToBranch] -= 1000.0;		
		
		double[] value00 = new double[1];
		double[] posteriors0 = new double[mU.length];
		// zero branch
		int status0 = runBranchAndBound(len, slavelen, posteriors0, slaves, totalDelta, slaveparts,
				          partslaves, deltaarray, niters, objVals, thetas,
				          bestUpperBound0, value00, bestLowerBound0, branchedVariables,
				          cumulativeValue);
		objVals[variableToBranch] = originalPotential;
		thetas[variableToBranch] = originalPotential / (double) deltaarray[variableToBranch];
		if (status0 != STATUS_OPTIMAL_INTEGER && status0 != STATUS_INFEASIBLE) {
			return STATUS_UNSOLVED;
		}
		
		// one branch
		double[] posteriors1 = new double[mU.length];
		double[] value01 = new double[1];
		objVals[variableToBranch] += infinitePotential;
		thetas[variableToBranch] += 1000.0;
		int status1 = runBranchAndBound(len, slavelen, posteriors1, slaves, totalDelta, slaveparts,
									partslaves, deltaarray, niters, objVals, thetas,
									bestUpperBound0, value01, bestLowerBound0, branchedVariables,
									cumulativeValue + infinitePotential);
		objVals[variableToBranch] = originalPotential;
		thetas[variableToBranch] = originalPotential / (double) deltaarray[variableToBranch];
		if (status1 != STATUS_OPTIMAL_INTEGER && status1 != STATUS_INFEASIBLE) {
			return STATUS_UNSOLVED;
		}
		
		
		if (status0 == STATUS_INFEASIBLE && status1 == STATUS_INFEASIBLE) {
			value0[0] = -Double.MAX_VALUE;
			return STATUS_INFEASIBLE;
		}
		
		if (value00[0] >= value01[0]) {
			value0[0] = value00[0];
			for (int i = 0; i < mU.length; i++) {
				mU[i] = posteriors0[i];
			}
		} else {
			value0[0] = value01[0];
			for (int i = 0; i < mU.length; i++) {
				mU[i] = posteriors1[i];
			}
		}
		
		return STATUS_OPTIMAL_INTEGER;
	}
	
	
	public int runAD3(int len, int slavelen, double[] mU,
						  Slave[] slaves, double totalDelta,
						  int[][] slaveparts, int[][] partslaves,
						  int[] deltaarray,
						  int niters,
						  double[] objVals,
						  double[] thetas,
						  double[] lowerBound0,
						  double[] value0,
						  double[] upperBound0) {
		boolean optimal = false;
		boolean reachedLowerBound = false;
		double dualObjBest = Double.MAX_VALUE;
		double primalRelObjBest = -Double.MAX_VALUE;
				
		double[][] zs = new double[slavelen][len];
		double[][] lambdas = new double[slavelen][len];
		double[] u = new double[len];
		Arrays.fill(u, 0.5);
		for (int i = 0; i < slavelen; i++) {
			lambdas[i] = new double[len];
			Arrays.fill(lambdas[i], 0.0);
			zs[i] = new double[len];
			Arrays.fill(zs[i], 0.0);
		}			
		double rho = RHO_START;
		for (int itr = 0; itr < niters; itr++) {
			// System.out.println("Rho: " + rho);
			double eta = TAU * rho;
			// System.out.println("Eta: " + eta);
			// making z-update
			for (int s = 0; s < slavelen; s++) {
				zs[s] = slaves[s].makeZUpdate(thetas, rho, u, lambdas[s], zs[s]);
			}
			// making u update
			double[] oldus = Arrays.copyOf(u, u.length);
			for (int i = 0; i < len; i++) {
				double sum = 0.0;
				for (int j = 0; j < partslaves[i].length; j++) {
					int s = partslaves[i][j];
					sum += zs[s][i];
				}
				u[i] = sum / deltaarray[i];
			}
			
			// making lambda update
			for (int s = 0; s < slavelen; s++) {
				for (int r = 0; r < slaveparts[s].length; r++) {
					int i = slaveparts[s][r];
					lambdas[s][i] = lambdas[s][i] - eta * (zs[s][i] - u[i]);
				}
			}
			
			// computing the primal residual
			double pr = 0.0;
			for (int s = 0; s < slavelen; s++) {
				for (int p = 0; p < slaveparts[s].length; p++) {
					pr += (zs[s][slaveparts[s][p]] - u[slaveparts[s][p]]) *
						   (zs[s][slaveparts[s][p]] - u[slaveparts[s][p]]);
				}
			}
			pr /= totalDelta;
			
			// computing the dual residual
			double dr = 0.0;
			for (int i = 0; i < len; i++) {
				dr += deltaarray[i] * (u[i] - oldus[i]) * (u[i] - oldus[i]); 
			}
			dr /= totalDelta;
			
			// System.out.println(itr + ": Primal residual: " + pr);
			// System.out.println(itr + ": Dual residual: " + dr);
			if (pr > dr) {
				double rat;
				if (dr == 0.0) {
					rat = 20.0;
				} else {
					rat = pr / dr;
				}
				if (rat > 10.0 && rho < MAX_RHO) {
					rho = rho * 2.0;
				}
			} else {
				double rat = 0;
				if (pr == 0) {
					if (dr != 0) {
						rat = 20.0;
					}
				} else {
					rat = dr / pr;
				}
				if (rat > 10.0 && rho > MIN_RHO) {
					rho = rho / 2.0;
				}
			}
			
			// If primal residual is low enough or enough iterations                                                                                                 
		    // have passed, compute the dual.                                                                                                                        
		    boolean computeDual = false;
		    boolean computePrimalRel = false;                                                                                                          
		    if (pr < RESIDUAL_THRESH) {
		    	computeDual = true;
		    	computePrimalRel = true;
		    } else if (itr > 0 && 0 == (itr % NUM_ITERATIONS_COMPUTE_DUAL)) {
		    	computeDual = true;
		    }
		    
		    // computing dual objective
		    double dualObjective = Double.MAX_VALUE;
		    if (computeDual) {
		    	dualObjective = 0.0;
		    	for (int j = 0; j < slaves.length; ++j) {
		    		dualObjective += slaves[j].computeDual(thetas, rho, u, lambdas[j], zs[j]);
		      }
		    }			
			
		    // computing relaxed primal objective
		    double primalRelObjective = -Double.MAX_VALUE;
		    if (computePrimalRel) {
		    	primalRelObjective = 0.0;
		    	for (int j = 0; j < u.length; j++) {
		    		primalRelObjective += u[j] * objVals[j];
		    	}
		    	// no higher order potentials, hence summing up only over unary factors
		    }
		    
		    if (dualObjBest > dualObjective) {
		    	dualObjBest = dualObjective;
		    	for (int j = 0; j < u.length; j++) {
		    		mU[j] = u[j];
		    	}
		    	if (dualObjBest < lowerBound0[0]) {
		    		reachedLowerBound = true;
		    		break;
		    	}
		    }
		    
		    if (primalRelObjBest < primalRelObjective) {
		    	primalRelObjBest = primalRelObjective;
		    }		    
		    
			if (pr < RESIDUAL_THRESH && dr < RESIDUAL_THRESH) {
				System.out.println("Optimization converged: " + pr + " " + dr);
				for (int j = 0; j < u.length; j++) {
		    		mU[j] = u[j];
		    	}
				optimal = true;
				break;
			}
		}
		boolean fractional = false;
		value0[0] = 0.0;
		for (int i = 0; i < u.length; i++) {
			if (!BitOps.nearlyBinary(u[i], FNModelOptions.TOL)) fractional = true;
			value0[0] += u[i] * objVals[i]; 
		}
		upperBound0[0] = dualObjBest;
		
		if (optimal) {
		    if (!fractional) {
		      System.out.println("Solution is integer.");
		      return STATUS_OPTIMAL_INTEGER;
		    } else {
		      System.out.println("Solution is fractional.");
		      return STATUS_OPTIMAL_FRACTIONAL;
		    }
		  } else {
		    if (reachedLowerBound) {
		      System.out.println("Reached lower bound: " + lowerBound0[0]);
		      return STATUS_INFEASIBLE;
		    } else {
		      System.out.println("Solution is only approximate.");
		      return STATUS_UNSOLVED;
		    }
		  }		
	}
	
	@Override
	public void end() {
		// TODO Auto-generated method stub
		if (bWriter != null) {
			try { 
				bWriter.close();
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Could not close file.");
				System.exit(-1);
			}
		}
	}

	@Override
	public void setFactorFile(String factorFile) {
		// TODO Auto-generated method stub
		if (factorFile != null && !factorFile.equals("null")) {
			WRITE_FACTORS_TO_FILE = true;
			mFactorFile = factorFile;
		} else {
			mFactorFile = null;
		}
	}

	@Override
	public void setFlag(String flag) {
		// TODO Auto-generated method stub
		
	}
}