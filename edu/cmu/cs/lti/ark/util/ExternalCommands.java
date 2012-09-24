/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * ExternalCommands.java is part of SEMAFOR 2.0.
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;


public class ExternalCommands
{
	public static void runExternalCommand(String command, String printFile)
	{
		String s = null;
		try {
			Process p = Runtime.getRuntime().exec(command);
			PrintStream errStream=System.err;
			System.setErr(System.out);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(printFile));
			// read the output from the command
			System.out.println("Here is the standard output of the command:");
			while ((s = stdInput.readLine()) != null) {
				bWriter.write(s.trim()+"\n");
			}
			bWriter.close();
			System.out.println("Here is the standard error of the command (if any):");
			while ((s = stdError.readLine()) != null) {
				System.out.println(s);
			}
			p.destroy();
			System.setErr(errStream);
		}
		catch (IOException e) {
			System.out.println("exception happened - here's what I know: ");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	public static void runExternalCommandAndStoreinFile(String command, String file)
	{
		String s = null;
		try {
			Process p = Runtime.getRuntime().exec(command);
			PrintStream errStream=System.err;
			System.setErr(System.out);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			BufferedWriter bWriter = new BufferedWriter(new FileWriter(file));
			System.out.println("Here is the standard output of the command:");
			while ((s = stdInput.readLine()) != null) {
				bWriter.write(s.trim()+"\n");
			}
			bWriter.close();
			System.out.println("Here is the standard error of the command (if any):");
			while ((s = stdError.readLine()) != null) {
				System.out.println(s);
			}
			p.destroy();
			System.setErr(errStream);
		}
		catch (IOException e) {
			System.out.println("exception happened - here's what I know: ");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	
	public static void runExternalCommand(String command)
	{
		String s = null;
		try {
			Process p = Runtime.getRuntime().exec(command);
			PrintStream errStream=System.err;
			System.setErr(System.out);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			// read the output from the command
			while ((s = stdInput.readLine()) != null) {
				System.out.println(s);
			}
			while ((s = stdError.readLine()) != null) {
				System.out.println(s);
			}
			p.destroy();
			System.setErr(errStream);
		}
		catch (IOException e) {
			System.out.println("exception happened - here's what I know: ");
			e.printStackTrace();
			System.exit(-1);
		}
	}
}
