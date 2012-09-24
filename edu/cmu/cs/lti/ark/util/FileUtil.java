/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * FileUtil.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.util;
import java.util.*;
import java.io.*;
public class FileUtil {
	public static Scanner openInFile(String filename){
		Scanner localsc=null;
		try
		{
			localsc=new Scanner (new FileInputStream(filename));

		}catch(IOException ioe){
			System.out.println(ioe.getMessage());
		}
		return localsc;
	}
	public static PrintStream  openOutFile(String filename){
		PrintStream localps=null;
		try
		{
			localps=new PrintStream (new FileOutputStream(filename));

		}catch(IOException ioe){
			System.out.println(ioe.getMessage());
		}
		return localps;
	}
	public static FileInputStream openInputStream(String infilename){
		FileInputStream fis=null;
		try {
			fis =(new FileInputStream(infilename));
			
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());
		}
		return fis;
	}
	
	public static String[] listContents(File dir) {
		return listContents(dir,null);
	}
	
	/**
	 * List files and directories within a given directory.
	 * Does not recurse on subdirectories.
	 * @param dir Containing directory
	 * @param regex Regular expression to match against the file/directory name, 
	 * or {@code null} to match all contents.
	 * @return Names of matching contents
	 */
	public static String[] listContents(File dir, final String regex) {
		assert dir.isDirectory();
		return dir.list(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.matches(regex);
			}
		});
	}
	
	public static File[] listFiles(File dir) {
		return listFiles(dir,null);
	}
	
	/**
	 * List files (but not directories) within a given directory. 
	 * Does not recurse on subdirectories.
	 * @param dir Containing directory
	 * @param regex Regular expression to match against the file name, 
	 * or {@code null} to match all files.
	 * @return Matching files
	 */
	public static File[] listFiles(File dir, final String regex) {
		assert dir.isDirectory();
		return dir.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return name.matches(regex);
			}
		});
	}
}
