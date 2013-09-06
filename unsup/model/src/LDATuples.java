import com.google.common.base.Function;
import com.google.common.collect.*;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;

import util.Arr;
import util.BasicFileIO;
import util.FastRandom;
import util.JsonUtil;
import util.MCMC;
import util.U;
import util.Util;
import util.Vocabulary;


public class LDATuples {

	double linkyConc = .01;
	double wordConc = 100;
	double frameConc = .1;
	
	static class Opts {
		static String dataFilename = "data/crime.tuple";
		static String outputDir = "out";
		static int maxIter = 10000 + 1;
		static int saveEvery = 1000;
		static int concResampleEvery = 100;
	}
	
	int numFrames = 4;
	int numRolesPerFrame = 4;   // totally lame
	
	int numWordTypes = -1;
	int numPathTypes = -1;

	Vocabulary wordVocab;
	Vocabulary pathVocab;
	
	ArrayList<Tuple> tuples;
	
	// CGS token count tables
	int[] nFrame;
	int[][][] nWordFrameRole;
	int[][] nFrameRole;
	int[][][] nPathFrameRole;
	int[][] nPathFrame;
	int[][] nHeadFrame;  // triggerwords ("headwords").. um which term to use?
	
	static class Tuple {
		int headid = -1;  // DATA
		List<Arg> args;  // mostly DATA
		int frameid = -1;  // LATENT
		
		public Tuple() { args = new ArrayList<>(); } 
	}
	static class Arg {
		int pathid = -1;  // DATA
		int wordid = -1;  // DATA
		int roleid = -1;  // LATENT
	}
	
	FastRandom rand = new FastRandom();
	
	public LDATuples() {
		wordVocab = new Vocabulary();
		pathVocab = new Vocabulary();
		tuples = Lists.newArrayList();
	}
	
	void readData() throws JsonProcessingException, IOException {
		U.pf("reading %s\n", Opts.dataFilename);
		for (String line : BasicFileIO.openFileLines(Opts.dataFilename)) {
			JsonNode jt = JsonUtil.readJson(line);
			Tuple tuple = new Tuple();
			String trigger = jt.get(0).asText();
			tuple.headid = wordVocab.num(trigger);
			for (JsonNode ja : jt.get(1)) {
				String path = ja.get(0).asText();
				String word = ja.get(1).asText();
				
				Arg arg = new Arg();
				arg.pathid = pathVocab.num(path);
				arg.wordid = wordVocab.num(word);
				tuple.args.add(arg);
			}
			tuples.add(tuple);
		}
		numWordTypes = wordVocab.size();
		numPathTypes = pathVocab.size();
		U.pf("%d paths, %d wordtypes, %d tuples\n", pathVocab.size(), wordVocab.size(), tuples.size());
	}

	void saveInitial() {
		try {
			pathVocab.dump(U.sf("%s/path.vocab", Opts.outputDir));
			wordVocab.dump(U.sf("%s/word.vocab", Opts.outputDir));
		} catch (IOException e) {
		}
	}
	
	void setupModel() {
		nFrame = new int[numFrames];
		nWordFrameRole = new int[numWordTypes][numFrames][numRolesPerFrame];
		nFrameRole = new int[numFrames][numRolesPerFrame];
		nPathFrameRole = new int[numPathTypes][numFrames][numRolesPerFrame];
		nPathFrame = new int[numPathTypes][numFrames];
		nHeadFrame = new int[numWordTypes][numFrames];
	}
	
	/** delta=+1 for increment.  delta=-1 for decrement. */
	void updateCounts_Frame(Tuple tuple, int delta) {
		nFrame[tuple.frameid] += delta;
		nHeadFrame[tuple.headid][tuple.frameid] += delta;
		for (Arg arg : tuple.args) {
			nPathFrameRole[arg.pathid][tuple.frameid][arg.roleid] += delta;
			nPathFrame[arg.pathid][tuple.frameid] += delta;
			nWordFrameRole[arg.wordid][tuple.frameid][arg.roleid] += delta;
			nFrameRole[tuple.frameid][arg.roleid] += delta;
		}
	}
	void updateCounts_Role(Tuple tuple, Arg arg, int delta) {
		nPathFrameRole[arg.pathid][tuple.frameid][arg.roleid] += delta;
		nWordFrameRole[arg.wordid][tuple.frameid][arg.roleid] += delta;
		nFrameRole[tuple.frameid][arg.roleid] += delta;
	}

	/** this should NOT look at arg.frameid */
	double calcFrameField(double[] field, Tuple tuple, boolean skipArgs) {
		assert field.length == numFrames;
		double psum = 0;
		for (int f=0; f<numFrames; f++) {
			field[f] = nFrame[f] + frameConc/numFrames;
			double a = nHeadFrame[tuple.headid][f] + wordConc/numWordTypes;
			double b = nFrame[f] + wordConc;
			field[f] *= a/b;
			if (!skipArgs) {
				for (Arg arg : tuple.args) {
					double c = nPathFrameRole[arg.pathid][f][arg.roleid] + linkyConc/numRolesPerFrame;
					double d = nPathFrame[arg.pathid][f] + linkyConc;
					field[f] *= c/d;
				}
			}
			psum += field[f];
		}
		return psum;
	}
	/** this should NOT look at arg.roleid */
	double calcRoleField(double[] field, Tuple tuple, Arg arg) {
		assert field.length == numRolesPerFrame;
		double psum = 0;
		double b = nPathFrame[arg.pathid][tuple.frameid] + linkyConc;
		for (int r=0; r<numRolesPerFrame; r++) {
			double a = nPathFrameRole[arg.pathid][tuple.frameid][r] + linkyConc/numRolesPerFrame;
			double c = nWordFrameRole[arg.wordid][tuple.frameid][r] + wordConc/numWordTypes;
			double d = nFrameRole[tuple.frameid][r] + wordConc;
			field[r] = a/b * c/d;
			psum += field[r];
		}
		return psum;
	}

	void sampleIteration() {
		double pseudoLL = 0;
		
		double[] frameField = new double[numFrames];
		double[] roleField = new double[numRolesPerFrame];
		for (Tuple tuple : tuples) {
			updateCounts_Frame(tuple, -1);
			double s1 = calcFrameField(frameField, tuple, false);
			int f = rand.nextDiscrete(frameField, s1);
			tuple.frameid = f;
			updateCounts_Frame(tuple, +1);
			
			pseudoLL += Math.log(frameField[f] / s1);
			
			for (Arg arg : tuple.args) {
				updateCounts_Role(tuple, arg, -1);
				double s2 = calcRoleField(roleField, tuple, arg);
				int r = rand.nextDiscrete(roleField, s2);
				arg.roleid = r;
				updateCounts_Role(tuple, arg, +1);
				
				pseudoLL += Math.log(roleField[r] / s2);
			}
		}
		U.pf("pseudoLL %.1f\n", pseudoLL);
		U.pf("frames "); U.p(nFrame);
		U.p("framerole"); U.p(nFrameRole);
		U.p("pathframe"); U.p(nPathFrame);
	}

	/** don't have roles assigned yet when doing frame sampling decision. */
	void initializeSamples() {
		double[] frameField = new double[numFrames];
		double[] roleField = new double[numRolesPerFrame];
		for (Tuple tuple : tuples) {
			double s1 = calcFrameField(frameField, tuple, true);
			int f = rand.nextDiscrete(frameField, s1);
			tuple.frameid = f;
			nFrame[f]++;
			nHeadFrame[tuple.headid][f]++;
			for (Arg arg : tuple.args) {
				double s2 = calcRoleField(roleField, tuple, arg);
				int r = rand.nextDiscrete(roleField, s2);
				arg.roleid = r;
				nPathFrameRole[arg.pathid][tuple.frameid][arg.roleid] += 1;
				nPathFrame[arg.pathid][tuple.frameid] += 1;
				nWordFrameRole[arg.wordid][tuple.frameid][arg.roleid] += 1;
				nFrameRole[tuple.frameid][arg.roleid] += 1;
			}
		}
		
	}

	
//	/** this does NOT use model's concentration; instead the passed-in value. **/
//	double calcClassLL(double _docConc) {
//		double ll = 0;
//		for (int docid=0; docid < pathVocab.size(); docid++) {
//			ll += Util.dirmultSymmLogprob(nDocTopic[docid], nDoc[docid], _docConc/numFrames);
//		}
//		return ll;
//	}
//	/** this does NOT use model's concentration; instead the passed-in value. **/
//	double calcWordLL(double _wordConc) {
//		double ll = 0;
//		for (int k=0; k < numFrames; k++) {
//			double vec[] = Arr.getCol(nWordFrameRole, k);
//			ll += Util.dirmultSymmLogprob(vec, nFrame[k], _wordConc/numWordTypes);
//		}
//		return ll;
//	}
//
//	double calcWordLL() {
//		return calcWordLL(wordConc);
//	}
//	double calcClassLL() {
//		return calcClassLL(docConc);
//	}

//	void resampleConcs() {
//		Function <double[],Double> zLL = new Function<double[],Double>() {
//			@Override
//			public Double apply(double[] input) {
//				return calcClassLL(Math.exp(input[0]));
//			}
//		};
//		Function <double[],Double> wLL = new Function<double[],Double>() {
//			@Override
//			public Double apply(double[] input) {
//				return calcWordLL(Math.exp(input[0]));
//			}
//		};
//		List<double[]> history;
//
//		history = MCMC.slice_sample(wLL, new double[]{Math.log(wordConc)}, new double[]{1}, 30);
//		this.wordConc  = Math.exp(history.get(history.size()-1)[0]);
//		U.pf("wordConc %.6g\n", wordConc);
//		
//		history = MCMC.slice_sample(zLL, new double[]{Math.log(docConc)}, new double[]{1}, 30);
//		this.docConc  = Math.exp(history.get(history.size()-1)[0]);
//		U.pf("docConc %.6g\n", docConc);
//	}
	
	/** output:  (i,j,k, value)  */
	static double[][] toSparseCoordinates(double[][][] X) {
		int n1 = X.length;
		int n2 = X[0].length;
		int n3 = X[0][0].length;
		
		List<double[]> outrows = new ArrayList<>();
		for (int i1=0; i1<n1; i1++) {
			for (int i2=0; i2<n2; i2++) {
				for (int i3=0; i3<n3; i3++) {
					double value = X[i1][i2][i3];
					if (value==0) continue;
					outrows.add(new double[]{ i1, i2, i3, value });
				}
			}
		}
		double[][] ret = new double[outrows.size()][4];
		for (int i=0; i<outrows.size(); i++)
			ret[i] = outrows.get(i);
		return ret;
	}
	
	/** output:  (i,j,k, value)  */
	static int[][] toSparseCoordinates(int[][][] X) {
		int n1 = X.length;
		int n2 = X[0].length;
		int n3 = X[0][0].length;
		
		List<int[]> outrows = new ArrayList<>();
		for (int i1=0; i1<n1; i1++) {
			for (int i2=0; i2<n2; i2++) {
				for (int i3=0; i3<n3; i3++) {
					int value = X[i1][i2][i3];
					if (value==0) continue;
					outrows.add(new int[]{ i1, i2, i3, value });
				}
			}
		}
		int[][] ret = new int[outrows.size()][4];
		for (int i=0; i<outrows.size(); i++)
			ret[i] = outrows.get(i);
		return ret;
	}
	
	void saveModel(int iter) {
		String prefix = U.sf("%s/model.%d", Opts.outputDir, iter);
		Arr.write(nHeadFrame, prefix + ".nHeadFrame");
		Arr.write(toSparseCoordinates(nWordFrameRole), prefix + ".nWordFrameRole.sparse");
		Arr.write(toSparseCoordinates(nPathFrameRole), prefix + ".nPathFrameRole.sparse");
	}

	void train() {
		initializeSamples();
		for (int iter=1; iter<Opts.maxIter; iter++) {
			U.pf("ITER %d\n", iter);
			sampleIteration();
			
//			if (iter<=100 || iter % 20 == 0) {
//				double wordLL = calcWordLL();
//				double classLL= calcClassLL();
//				U.pf("totalLL %f\n", wordLL+classLL);
//			}
//			
//			if (iter % Opts.concResampleEvery == 0 && Opts.concResampleEvery>=0) {
//				resampleConcs();
//			}
			if (iter % Opts.saveEvery == 0) {
				U.p("saving");
				saveModel(iter);
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
		LDATuples m = new LDATuples();
		
//		m.numFrames = Integer.valueOf(args[0]);
		LDATuples.Opts.dataFilename = args[0];
//		LDATuples.Opts.outputDir = args[2];
		
		Files.createParentDirs(new File(LDATuples.Opts.outputDir + "/bla"));		
		m.readData();
		m.setupModel();
		m.saveInitial();
		m.train();
	}
}
