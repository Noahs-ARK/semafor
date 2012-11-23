/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * FeatureExtractor.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.identification;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import edu.cmu.cs.lti.ark.fn.wordnet.WordNetRelations;
import edu.cmu.cs.lti.ark.util.IFeatureExtractor;
import edu.cmu.cs.lti.ark.util.ds.map.IntCounter;
import edu.cmu.cs.lti.ark.util.nlp.parse.DependencyParse;
import gnu.trove.THashMap;
import gnu.trove.THashSet;

public class FeatureExtractor implements IFeatureExtractor
{
	public Lock r = null;
	public Lock w = null;
	
	
	public FeatureExtractor() {
		ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
		r = lock.readLock();
		w = lock.writeLock();
	}
	
	
	public String getLowerCaseLemma(THashMap<String,String> lemmaCache, String word, String POS, WordNetRelations wnr)
	{
		String pair = word+"_"+POS;
		if(lemmaCache==null)
		{
			return wnr.getLemmaForWord(word, POS).toLowerCase();
		}
		else if(!lemmaCache.contains(pair))
		{	
			lemmaCache.put(pair, wnr.getLemmaForWord(word, POS).toLowerCase());
			return wnr.getLemmaForWord(word, POS).toLowerCase();
		}
		else
		{
			return lemmaCache.get(pair);
		}		
	}	
	
	public String getLowerCaseLemma(Map<String,String> lemmaCache, String word, String POS)
	{
		return lemmaCache.get(word + "_" + POS);		
	}
	
	public String getLowerCaseLemma(int index, String[][] data)
	{
		return data[5][index];
	}	

	public Set<String> getWNRelations(THashMap<String, THashSet<String>> wnCacheMap,String sWord, String tWord, WordNetRelations wnr)
	{
		String pair = sWord.toLowerCase()+"\t"+tWord.toLowerCase();
		if(wnCacheMap==null)
		{
			return wnr.getRelations(sWord.toLowerCase(), tWord.toLowerCase());
		}
		else if(!wnCacheMap.contains(pair))
		{
			Set<String> relations = wnr.getRelations(sWord.toLowerCase(), tWord.toLowerCase());
			if(relations.contains(WordNetRelations.NO_RELATION))
				return relations;
			else
			{
				THashSet<String> nR = new THashSet<String>();
				for(String string:relations)
					nR.add(string);
				wnCacheMap.put(pair, nR);
				return relations;
			}
		}
		else
		{
			return wnCacheMap.get(pair);
		}
	}

	public String getClusterMembership(String token, THashMap<String,THashSet<String>> clusterMap, int K)
	{
		THashSet<String> set  = clusterMap.get(token.toLowerCase());
		String k = ""+K;
		String tag=null;
		if(set!=null)
		{	for(String string:set)
		{
			String[] arr = string.split("_");
			if(arr[1].equals(k))
				tag="C"+arr[2];
		}
		}
		return tag;
	}

	public IntCounter<String> extractFeaturesWithClusters(String mFrameName, 
			int[] tokenNums, 
			String hiddenWord, 
			String[][] parseData, 
			WordNetRelations wnr, 
			String trainOrTest, 
			THashMap<String,THashSet<String>> wnCacheMap,
			THashMap<String,String> lemmaCache,
			DependencyParse parse,
			THashMap<String,THashSet<String>> clusterMap,
			int K)
			{
		int[] mTokenNums = tokenNums;
		Arrays.sort(mTokenNums);
		IntCounter<String> featureMap = new IntCounter<String>();

		String hiddenUnitTokens = "";
		String hiddenUnitLemmas = "";
		String hiddenLemmaAndFPOS = "";

		String actualTokens = "";
		String actualLemmas = "";
		String actualLemmaAndFPOS="";

		String hiddenPOSSeq = "";
		String hiddenFinePOSSeq = "";

		String actualPOSSeq = "";
		String actualFinePOSSeq = "";

		String[] hiddenToks = hiddenWord.split(" ");
		for(int i = 0; i < hiddenToks.length; i ++)
		{
			String[] arr = hiddenToks[i].split("_");
			hiddenUnitTokens+=arr[0]+" ";
			hiddenPOSSeq+=arr[1]+" ";
			hiddenFinePOSSeq+=arr[1].substring(0,1)+" ";
			w.lock();
			hiddenUnitLemmas+=getLowerCaseLemma(lemmaCache,arr[0], arr[1],wnr)+" ";
			hiddenLemmaAndFPOS+=getLowerCaseLemma(lemmaCache,arr[0], arr[1],wnr)+"_"+arr[1].substring(0,1)+" ";
			w.unlock();
		}
		hiddenUnitTokens=hiddenUnitTokens.trim();
		hiddenUnitLemmas=hiddenUnitLemmas.trim();
		hiddenPOSSeq=hiddenPOSSeq.trim();
		hiddenFinePOSSeq=hiddenFinePOSSeq.trim();
		hiddenLemmaAndFPOS=hiddenLemmaAndFPOS.trim();


		for(int i = 0; i < mTokenNums.length; i ++)
		{
			String lexUnit = parseData[0][mTokenNums[i]];
			String pos = parseData[1][mTokenNums[i]];	
			actualTokens+=lexUnit+" ";
			w.lock();
			actualLemmas+=getLowerCaseLemma(lemmaCache,lexUnit, pos,wnr)+" ";
			actualPOSSeq+=pos+" ";
			actualFinePOSSeq+=pos.substring(0,1)+" ";
			actualLemmaAndFPOS+=getLowerCaseLemma(lemmaCache,lexUnit, pos,wnr)+"_"+pos.substring(0,1)+" ";
			w.unlock();
		}
		actualTokens=actualTokens.trim();
		actualLemmas=actualLemmas.trim();
		actualPOSSeq=actualPOSSeq.trim();
		actualFinePOSSeq=actualFinePOSSeq.trim();
		actualLemmaAndFPOS=actualLemmaAndFPOS.trim();

		String hTag = getClusterMembership(hiddenUnitTokens,clusterMap,K);
		String aTag = getClusterMembership(actualTokens,clusterMap,K);		
		String aLTag = getClusterMembership(actualLemmas,clusterMap,K);	

		if(hTag!=null)
		{
			String feature = "hTag:"+hTag+"_f:"+mFrameName;
			featureMap.increment(feature);
		}
		if(aTag!=null)
		{
			String feature = "aTag:"+aTag+"_f:"+mFrameName;
			featureMap.increment(feature);
		}
		if(aLTag!=null)
		{
			String feature = "aLTag:"+aLTag+"_f:"+mFrameName;
			featureMap.increment(feature);
		}	

		/*
		 * token relationships
		 */
		Set<String> relations = null;
		if(wnCacheMap!=null)
		{
			String pair = hiddenUnitTokens.toLowerCase()+"\t"+actualTokens.toLowerCase();
			if(!wnCacheMap.contains(pair))
			{
				relations = new THashSet<String>();
				relations.add(WordNetRelations.NO_RELATION);
			}
			else
			{
				relations = wnCacheMap.get(pair);
			}
		}
		else
		{
			w.lock();
			relations = getWNRelations(wnCacheMap,hiddenUnitTokens, actualTokens,wnr);
			w.unlock();
		}
		for(String relation: relations)
		{
			String feature = "tRLn:"+relation+"_f:"+mFrameName;
			featureMap.increment(feature);
			feature = "tRLn:"+relation+"_hU:"+hiddenUnitTokens.replaceAll(" ","_")+"_f:"+mFrameName;
			featureMap.increment(feature);
			if(relation.equals(WordNetRelations.NO_RELATION))
				continue;
			feature = "tRLn:"+relation+"_hU:"+hiddenUnitTokens.replaceAll(" ","_")+"_hP:"+hiddenFinePOSSeq.replaceAll(" ", "_")+"_aP:"+actualFinePOSSeq.replaceAll(" ", "_")+"_f:"+mFrameName;
			featureMap.increment(feature);
		}

		if(hTag!=null&&aTag!=null)
		{
			if(hTag.equals(aTag))
			{
				String feature = "haSameTag_f:"+mFrameName;
				featureMap.increment(feature);
				feature = "haSameTag_t:_"+aTag+"f:"+mFrameName;
				featureMap.increment(feature);
			}
		}		

		if(hTag!=null&&aLTag!=null)
		{
			if(hTag.equals(aLTag))
			{
				String feature = "haLSameTag_f:"+mFrameName;
				featureMap.increment(feature);
				feature = "haLSameTag_t:_"+aLTag+"f:"+mFrameName;
				featureMap.increment(feature);
			}
		}		

		/*
		 * features
		 */
		String feature = "hTs:"+hiddenUnitTokens.replaceAll(" ","_")+"_f:"+mFrameName;
		featureMap.increment(feature);
		feature = "hLs:"+hiddenUnitLemmas.replaceAll(" ","_")+"_f:"+mFrameName;
		featureMap.increment(feature);
		feature = "hLFPOSs:"+hiddenLemmaAndFPOS.replaceAll(" ", "_")+"_f:"+mFrameName;
		featureMap.increment(feature);
		if(hiddenUnitTokens.equals(actualTokens))
		{
			feature = "sTs_f:"+mFrameName;
			featureMap.increment(feature);
			feature = "sTs_hLs:"+hiddenUnitTokens.replaceAll(" ","_")+"_f:"+mFrameName;
			featureMap.increment(feature);
			feature = "sTs_pSeqs_A:"+actualFinePOSSeq.replaceAll(" ","_")+"_H:"+hiddenFinePOSSeq.replaceAll(" ", "_")+"_f:"+mFrameName;
			featureMap.increment(feature);
			feature = "sTs_pLSeqs_A:"+actualLemmaAndFPOS.replaceAll(" ","_")+"_H:"+hiddenLemmaAndFPOS.replaceAll(" ", "_")+"_f:"+mFrameName;
			featureMap.increment(feature);
		}
		if(hiddenUnitLemmas.equals(actualLemmas))
		{
			feature = "sLs_f:"+mFrameName;
			featureMap.increment(feature);
			feature = "sLs_hLs:"+hiddenUnitTokens.replaceAll(" ","_")+"_f:"+mFrameName;
			featureMap.increment(feature);
			feature = "sLs_pSeqs_A:"+actualFinePOSSeq.replaceAll(" ","_")+"_H:"+hiddenFinePOSSeq.replaceAll(" ", "_")+"_f:"+mFrameName;
			featureMap.increment(feature);
			feature = "sLs_pLSeqs_A:"+actualLemmaAndFPOS.replaceAll(" ","_")+"_H:"+hiddenLemmaAndFPOS.replaceAll(" ", "_")+"_f:"+mFrameName;
			featureMap.increment(feature);
		}

		/*
		 * syntactic features
		 */
		DependencyParse[] sortedNodes = DependencyParse.getIndexSortedListOfNodes(parse);

		DependencyParse node = DependencyParse.getHeuristicHead(sortedNodes, mTokenNums);

		String headWord = node.getWord();
		String hwC = getClusterMembership(headWord,clusterMap,K);
		if(hwC!=null)
		{
			feature = "hwc:"+hwC+"f:"+mFrameName;
			featureMap.increment(feature);
		}

		String nodePOS = node.getPOS().substring(0,1);
		List<DependencyParse> children = node.getChildren();
		String subcat = "";
		String clusterSubcat = "";
		String dependencies = "";
		String clusterDependencies = "";

		THashSet<String> deps =  new THashSet<String>();
		THashSet<String> hwDeps = new THashSet<String>();



		for(DependencyParse child : children)
		{
			String lab = child.getLabelType();
			deps.add(lab);
			String childC = getClusterMembership(child.getWord(),clusterMap,K);
			if(childC!=null)
				hwDeps.add(childC);
			if(nodePOS.equals("V"))
			{
				if(!lab.equals("SUB")&&!lab.equals("P")&&!lab.equals("CC"))
				{
					subcat+=lab+"_";
					if(childC!=null)
						clusterSubcat+=childC+"_";
				}
			}
		}
		for(String dep: deps)
		{
			dependencies+=dep+"_";
		}		

		for(String dep:hwDeps)
		{
			clusterDependencies+=dep+"_";
		}

		feature = "d:"+dependencies+"f:"+mFrameName;
		featureMap.increment(feature);

		feature = "cD:"+clusterDependencies+"f:"+mFrameName;
		featureMap.increment(feature);


		if(nodePOS.equals("V"))
		{
			feature = "sC:"+subcat+"f:"+mFrameName;
			featureMap.increment(feature);


			feature = "sCC:"+clusterSubcat+"f:"+mFrameName;
			featureMap.increment(feature);
		}
		DependencyParse dp = node.getParent();
		String parPOS = null;
		String parLab = null;
		String parCluster = null;
		if(dp==null)
		{
			parPOS = "NULL";
			parLab = "NULL";
			parCluster = "NULL";
		}
		else
		{
			parPOS = dp.getPOS();
			parLab = dp.getLabelType();
			parCluster = getClusterMembership(dp.getWord(), clusterMap, K);
		}
		feature = "pP:"+parPOS+"_f:"+mFrameName;
		featureMap.increment(feature);
		feature = "pL:"+parLab+"_f:"+mFrameName;
		featureMap.increment(feature);
		if(parCluster!=null)
		{
			feature = "pC:"+parCluster+"_f:"+mFrameName;
			featureMap.increment(feature);	
		}		
		return featureMap;
			}


	public IntCounter<String> extractFeatures(String mFrameName, 
			int[] tokenNums, 
			String hiddenWord, 
			String[][] parseData, 
			WordNetRelations wnr, 
			String trainOrTest, 
			THashMap<String,THashSet<String>> wnCacheMap,
			THashMap<String,String> lemmaCache,
			DependencyParse parse)
			{
		int[] mTokenNums = tokenNums;
		Arrays.sort(mTokenNums);
		IntCounter<String> featureMap = new IntCounter<String>();

		String hiddenUnitTokens = "";
		String hiddenUnitLemmas = "";
		String hiddenLemmaAndFPOS = "";

		String actualTokens = "";
		String actualLemmas = "";
		String actualLemmaAndFPOS="";

		String hiddenPOSSeq = "";
		String hiddenFinePOSSeq = "";

		String actualPOSSeq = "";
		String actualFinePOSSeq = "";

		String[] hiddenToks = hiddenWord.split(" ");
		for(int i = 0; i < hiddenToks.length; i ++)
		{
			String[] arr = hiddenToks[i].split("_");
			hiddenUnitTokens+=arr[0]+" ";
			hiddenPOSSeq+=arr[1]+" ";
			hiddenFinePOSSeq+=arr[1].substring(0,1)+" ";
			w.lock();
			hiddenUnitLemmas+=getLowerCaseLemma(lemmaCache,arr[0], arr[1],wnr)+" ";
			hiddenLemmaAndFPOS+=getLowerCaseLemma(lemmaCache,arr[0], arr[1],wnr)+"_"+arr[1].substring(0,1)+" ";
			w.unlock();
		}
		hiddenUnitTokens=hiddenUnitTokens.trim();
		hiddenUnitLemmas=hiddenUnitLemmas.trim();
		hiddenPOSSeq=hiddenPOSSeq.trim();
		hiddenFinePOSSeq=hiddenFinePOSSeq.trim();
		hiddenLemmaAndFPOS=hiddenLemmaAndFPOS.trim();


		for(int i = 0; i < mTokenNums.length; i ++)
		{
			String lexUnit = parseData[0][mTokenNums[i]];
			String pos = parseData[1][mTokenNums[i]];	
			actualTokens+=lexUnit+" ";
			w.lock();
			actualLemmas+=getLowerCaseLemma(lemmaCache,lexUnit, pos,wnr)+" ";
			actualPOSSeq+=pos+" ";
			actualFinePOSSeq+=pos.substring(0,1)+" ";
			actualLemmaAndFPOS+=getLowerCaseLemma(lemmaCache,lexUnit, pos,wnr)+"_"+pos.substring(0,1)+" ";
			w.unlock();
		}
		actualTokens=actualTokens.trim();
		actualLemmas=actualLemmas.trim();
		actualPOSSeq=actualPOSSeq.trim();
		actualFinePOSSeq=actualFinePOSSeq.trim();
		actualLemmaAndFPOS=actualLemmaAndFPOS.trim();

		/*
		 * token relationships
		 */
		Set<String> relations = null;
		if(wnCacheMap!=null)
		{
			String pair = hiddenUnitTokens.toLowerCase()+"\t"+actualTokens.toLowerCase();
			if(!wnCacheMap.contains(pair))
			{
				relations = new THashSet<String>();
				relations.add(WordNetRelations.NO_RELATION);
			}
			else
			{
				relations = wnCacheMap.get(pair);
			}
		}
		else
		{
			w.lock();
			relations = getWNRelations(wnCacheMap,hiddenUnitTokens, actualTokens,wnr);
			w.unlock();
		}
		for(String relation: relations)
		{
			String feature = "tRLn:"+relation+"_f:"+mFrameName;
			featureMap.increment(feature);
			feature = "tRLn:"+relation+"_hU:"+hiddenUnitTokens.replaceAll(" ","_")+"_f:"+mFrameName;
			featureMap.increment(feature);
			if(relation.equals(WordNetRelations.NO_RELATION))
				continue;
			feature = "tRLn:"+relation+"_hU:"+hiddenUnitTokens.replaceAll(" ","_")+"_hP:"+hiddenFinePOSSeq.replaceAll(" ", "_")+"_aP:"+actualFinePOSSeq.replaceAll(" ", "_")+"_f:"+mFrameName;
			featureMap.increment(feature);
		}

		/*
		 * features
		 */
		String feature = "hTs:"+hiddenUnitTokens.replaceAll(" ","_")+"_f:"+mFrameName;
		featureMap.increment(feature);
		feature = "hLs:"+hiddenUnitLemmas.replaceAll(" ","_")+"_f:"+mFrameName;
		featureMap.increment(feature);
		feature = "hLFPOSs:"+hiddenLemmaAndFPOS.replaceAll(" ", "_")+"_f:"+mFrameName;
		featureMap.increment(feature);
		if(hiddenUnitTokens.equals(actualTokens))
		{
			feature = "sTs_f:"+mFrameName;
			featureMap.increment(feature);
			feature = "sTs_hLs:"+hiddenUnitTokens.replaceAll(" ","_")+"_f:"+mFrameName;
			featureMap.increment(feature);
			feature = "sTs_pSeqs_A:"+actualFinePOSSeq.replaceAll(" ","_")+"_H:"+hiddenFinePOSSeq.replaceAll(" ", "_")+"_f:"+mFrameName;
			featureMap.increment(feature);
			feature = "sTs_pLSeqs_A:"+actualLemmaAndFPOS.replaceAll(" ","_")+"_H:"+hiddenLemmaAndFPOS.replaceAll(" ", "_")+"_f:"+mFrameName;
			featureMap.increment(feature);
		}
		if(hiddenUnitLemmas.equals(actualLemmas))
		{
			feature = "sLs_f:"+mFrameName;
			featureMap.increment(feature);
			feature = "sLs_hLs:"+hiddenUnitTokens.replaceAll(" ","_")+"_f:"+mFrameName;
			featureMap.increment(feature);
			feature = "sLs_pSeqs_A:"+actualFinePOSSeq.replaceAll(" ","_")+"_H:"+hiddenFinePOSSeq.replaceAll(" ", "_")+"_f:"+mFrameName;
			featureMap.increment(feature);
			feature = "sLs_pLSeqs_A:"+actualLemmaAndFPOS.replaceAll(" ","_")+"_H:"+hiddenLemmaAndFPOS.replaceAll(" ", "_")+"_f:"+mFrameName;
			featureMap.increment(feature);
		}

		/*
		 * syntactic features
		 */
		DependencyParse[] sortedNodes = DependencyParse.getIndexSortedListOfNodes(parse);

		DependencyParse node = DependencyParse.getHeuristicHead(sortedNodes, mTokenNums);

		String nodePOS = node.getPOS().substring(0,1);
		List<DependencyParse> children = node.getChildren();
		String subcat = "";
		String dependencies = "";
		THashSet<String> deps =  new THashSet<String>();
		for(DependencyParse child : children)
		{
			String lab = child.getLabelType();
			deps.add(lab);
			if(nodePOS.equals("V"))
			{
				if(!lab.equals("SUB")&&!lab.equals("P")&&!lab.equals("CC"))
				{
					subcat+=lab+"_";
				}
			}
		}
		for(String dep: deps)
		{
			dependencies+=dep+"_";
		}		
		feature = "d:"+dependencies+"f:"+mFrameName;
		featureMap.increment(feature);
		if(nodePOS.equals("V"))
		{
			feature = "sC:"+subcat+"f:"+mFrameName;
			featureMap.increment(feature);
		}
		DependencyParse dp = node.getParent();
		String parPOS = null;
		String parLab = null;
		if(dp==null)
		{
			parPOS = "NULL";
			parLab = "NULL";
		}
		else
		{
			parPOS = dp.getPOS();
			parLab = dp.getLabelType();
		}
		feature = "pP:"+parPOS+"_f:"+mFrameName;
		featureMap.increment(feature);
		feature = "pL:"+parLab+"_f:"+mFrameName;
		featureMap.increment(feature);

		return featureMap;
	}
	
	public IntCounter<String> extractFeaturesLessMemory(
			String mFrameName, 
			int[] tokenNums, 
			String hiddenWord, 
			String[][] parseData,
			String trainOrTest, 
			Map<String, Set<String>> relatedWordsForWord,
			Map<String, Map<String, Set<String>>> revisedRelationsMap,
			Map<String, String> mHVLemmas,
			DependencyParse parse)
	{
		int[] mTokenNums = tokenNums;
		Arrays.sort(mTokenNums);
		IntCounter<String> featureMap = new IntCounter<String>();

		String hiddenUnitTokens = "";
		String hiddenUnitLemmas = "";
		String hiddenLemmaAndFPOS = "";

		String actualTokens = "";
		String actualLemmas = "";
		String actualLemmaAndFPOS="";

		String hiddenPOSSeq = "";
		String hiddenFinePOSSeq = "";

		String actualPOSSeq = "";
		String actualFinePOSSeq = "";

		String[] hiddenToks = hiddenWord.split(" ");
		for(int i = 0; i < hiddenToks.length; i ++)
		{
			String[] arr = hiddenToks[i].split("_");
			hiddenUnitTokens+=arr[0]+" ";
			hiddenPOSSeq+=arr[1]+" ";
			hiddenFinePOSSeq+=arr[1].substring(0,1)+" ";
			hiddenUnitLemmas+=getLowerCaseLemma(mHVLemmas, arr[0], arr[1]) + " ";
			hiddenLemmaAndFPOS+=getLowerCaseLemma(mHVLemmas, arr[0], arr[1]) + "_" + arr[1].substring(0,1)+" ";
		}
		hiddenUnitTokens=hiddenUnitTokens.trim();
		hiddenUnitLemmas=hiddenUnitLemmas.trim();
		hiddenPOSSeq=hiddenPOSSeq.trim();
		hiddenFinePOSSeq=hiddenFinePOSSeq.trim();
		hiddenLemmaAndFPOS=hiddenLemmaAndFPOS.trim();
		
		for(int i = 0; i < mTokenNums.length; i ++)
		{
			String lexUnit = parseData[0][mTokenNums[i]];
			String pos = parseData[1][mTokenNums[i]];	
			actualTokens+=lexUnit+" ";
			actualLemmas+=getLowerCaseLemma(mTokenNums[i], parseData)+" ";
			actualPOSSeq+=pos+" ";
			actualFinePOSSeq+=pos.substring(0,1)+" ";
			actualLemmaAndFPOS+=getLowerCaseLemma(mTokenNums[i], parseData)+"_"+pos.substring(0,1)+" ";
		}
		actualTokens=actualTokens.trim();
		actualLemmas=actualLemmas.trim();
		actualPOSSeq=actualPOSSeq.trim();
		actualFinePOSSeq=actualFinePOSSeq.trim();
		actualLemmaAndFPOS=actualLemmaAndFPOS.trim();

		/*
		 * finding relationships without the WordNetRelations object
		 */
		Set<String> relations = null;
		if(!relatedWordsForWord.containsKey(hiddenUnitTokens.toLowerCase())) {
			System.out.println("Problem with hidden word:" + hiddenUnitTokens.toLowerCase() + 
					". Not contained in cache. Exiting:");
			System.exit(-1);
		} 
		Set<String> relatedWords = 
			relatedWordsForWord.get(hiddenUnitTokens.toLowerCase());
		if (!relatedWords.contains(actualTokens.toLowerCase())) {
			relations = new THashSet<String>(); 
			relations.add(WordNetRelations.NO_RELATION);
		} else {
			relations = 
				revisedRelationsMap.get(hiddenUnitTokens.toLowerCase()).get(actualTokens.toLowerCase());
		}
		
		for(String relation: relations)
		{
			String feature = "tRLn:"+relation+"_f:"+mFrameName;
			featureMap.increment(feature);
			feature = "tRLn:"+relation+"_hU:"+hiddenUnitTokens.replaceAll(" ","_")+"_f:"+mFrameName;
			featureMap.increment(feature);
			if(relation.equals(WordNetRelations.NO_RELATION))
				continue;
			feature = "tRLn:"+relation+"_hU:"+hiddenUnitTokens.replaceAll(" ","_")+"_hP:"+hiddenFinePOSSeq.replaceAll(" ", "_")+"_aP:"+actualFinePOSSeq.replaceAll(" ", "_")+"_f:"+mFrameName;
			featureMap.increment(feature);
		}

		/*
		 * features
		 */
		String feature = "hTs:"+hiddenUnitTokens.replaceAll(" ","_")+"_f:"+mFrameName;
		featureMap.increment(feature);
		feature = "hLs:"+hiddenUnitLemmas.replaceAll(" ","_")+"_f:"+mFrameName;
		featureMap.increment(feature);
		feature = "hLFPOSs:"+hiddenLemmaAndFPOS.replaceAll(" ", "_")+"_f:"+mFrameName;
		featureMap.increment(feature);
		if(hiddenUnitTokens.equals(actualTokens))
		{
			feature = "sTs_f:"+mFrameName;
			featureMap.increment(feature);
			feature = "sTs_hLs:"+hiddenUnitTokens.replaceAll(" ","_")+"_f:"+mFrameName;
			featureMap.increment(feature);
			feature = "sTs_pSeqs_A:"+actualFinePOSSeq.replaceAll(" ","_")+"_H:"+hiddenFinePOSSeq.replaceAll(" ", "_")+"_f:"+mFrameName;
			featureMap.increment(feature);
			feature = "sTs_pLSeqs_A:"+actualLemmaAndFPOS.replaceAll(" ","_")+"_H:"+hiddenLemmaAndFPOS.replaceAll(" ", "_")+"_f:"+mFrameName;
			featureMap.increment(feature);
		}
		if(hiddenUnitLemmas.equals(actualLemmas))
		{
			feature = "sLs_f:"+mFrameName;
			featureMap.increment(feature);
			feature = "sLs_hLs:"+hiddenUnitTokens.replaceAll(" ","_")+"_f:"+mFrameName;
			featureMap.increment(feature);
			feature = "sLs_pSeqs_A:"+actualFinePOSSeq.replaceAll(" ","_")+"_H:"+hiddenFinePOSSeq.replaceAll(" ", "_")+"_f:"+mFrameName;
			featureMap.increment(feature);
			feature = "sLs_pLSeqs_A:"+actualLemmaAndFPOS.replaceAll(" ","_")+"_H:"+hiddenLemmaAndFPOS.replaceAll(" ", "_")+"_f:"+mFrameName;
			featureMap.increment(feature);
		}

		/*
		 * syntactic features
		 */
		DependencyParse[] sortedNodes = DependencyParse.getIndexSortedListOfNodes(parse);

		DependencyParse node = DependencyParse.getHeuristicHead(sortedNodes, mTokenNums);

		String nodePOS = node.getPOS().substring(0,1);
		List<DependencyParse> children = node.getChildren();
		String subcat = "";
		String dependencies = "";
		THashSet<String> deps =  new THashSet<String>();
		for(DependencyParse child : children)
		{
			String lab = child.getLabelType();
			deps.add(lab);
			if(nodePOS.equals("V"))
			{
				if(!lab.equals("SUB")&&!lab.equals("P")&&!lab.equals("CC"))
				{
					subcat+=lab+"_";
				}
			}
		}
		for(String dep: deps)
		{
			dependencies+=dep+"_";
		}		
		feature = "d:"+dependencies+"f:"+mFrameName;
		featureMap.increment(feature);
		if(nodePOS.equals("V"))
		{
			feature = "sC:"+subcat+"f:"+mFrameName;
			featureMap.increment(feature);
		}
		DependencyParse dp = node.getParent();
		String parPOS = null;
		String parLab = null;
		if(dp==null)
		{
			parPOS = "NULL";
			parLab = "NULL";
		}
		else
		{
			parPOS = dp.getPOS();
			parLab = dp.getLabelType();
		}
		feature = "pP:"+parPOS+"_f:"+mFrameName;
		featureMap.increment(feature);
		feature = "pL:"+parLab+"_f:"+mFrameName;
		featureMap.increment(feature);

		return featureMap;
	}
}
