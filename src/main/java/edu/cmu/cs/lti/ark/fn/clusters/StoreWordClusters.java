/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * StoreWordClusters.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.clusters;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Set;

import edu.cmu.cs.lti.ark.util.SerializedObjects;
import gnu.trove.*;

public class StoreWordClusters
{
	public static void main(String[] args)
	{		
		String rootDir="/usr2/dipanjan/experiments/FramenetParsing/KMeansWordClustering/scripts/kmeans";
		String[] dirs = {"clusters_64", "clusters_128", "clusters_256", "clusters_incomplete/clusters_512", "clusters_1024"};
		int[] Ks = {64,128,256,512,1024};
		FilenameFilter filter = new FilenameFilter()
		{
			public boolean accept(File arg0, String arg1)
			{
				return arg1.startsWith("clusters_");
			}
		};
		THashMap<String,THashSet<String>> clusterMap = new THashMap<String,THashSet<String>>();
		for(int i = 0; i < dirs.length; i ++)
		{
			String dir=rootDir+"/"+dirs[i];
			File dirFile = new File(dir);
			String[] clusters = dirFile.list(filter);
			long maxTime = Long.MIN_VALUE;
			String maxFile = null;
			for(String file:clusters)
			{
				String fileName=dir+"/"+file;
				File f = new File(fileName);
				if(f.lastModified()>maxTime)
				{
					maxTime=f.lastModified();
					maxFile=file;
				}
			}
			if(Ks[i]==512)
				maxFile="clusters_21";
			System.out.println("Max file:"+maxFile);
			int count = 0;
			try
			{
				BufferedReader bReader = new BufferedReader(new FileReader(dirFile.getAbsolutePath()+"/"+maxFile));
				String line = null;
				while((line=bReader.readLine())!=null)
				{
					line=line.trim();
					String[] toks = line.split("\\s");
					for(String tok:toks)
					{
						THashSet<String> set = clusterMap.get(tok.toLowerCase());
						if(set==null)
						{
							set=new THashSet<String>();
							set.add("c_"+Ks[i]+"_"+count);
							clusterMap.put(tok,set);
						}
						else
						{
							set.add("c_"+Ks[i]+"_"+count);
							clusterMap.put(tok,set);
						}
					}
					count++;
					System.out.println("Line number:"+count);
				}
				bReader.close();
			}
			catch(Exception e)
			{
				e.printStackTrace();
				System.exit(0);
			}
		}
		String outFile = rootDir+"/pos_clusters.jobj";
		SerializedObjects.writeSerializedObject(clusterMap, outFile);
		Set<String> keys = clusterMap.keySet();
		String[] keyArr = new String[keys.size()];
		keys.toArray(keyArr);
		Arrays.sort(keyArr);
		for(String key:keyArr)
		{
			System.out.print(key);
			THashSet<String> val = clusterMap.get(key);
			for(String string:val)
				System.out.print(" "+string);
			System.out.println();
		}
	}
	
	
}
