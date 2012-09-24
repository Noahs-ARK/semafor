/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * SerializedObjects.java is part of SEMAFOR 2.0.
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



import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class SerializedObjects
{
	public static void writeSerializedObject(Object object, String outFile)
	{
		ObjectOutput output = null;
		try {
			OutputStream file = null;
			if(outFile.endsWith(".gz")) {
				file = new GZIPOutputStream(new FileOutputStream(outFile));
			} else {
				file = new FileOutputStream(outFile);
			}
			OutputStream buffer = new BufferedOutputStream( file );
			output = new ObjectOutputStream( buffer );
			output.writeObject(object);
			buffer.close();
			file.close();
		}
		catch(IOException ex){
			ex.printStackTrace();
		}
		finally{
			try {
				if (output != null) {
					//flush and close "output" and its underlying streams
					output.close();
				}
			}
			catch (IOException ex ){
				ex.printStackTrace();
			}
		}
	}

	public static Object readSerializedObject(String inputFile)
	{
		ObjectInput input = null;
		Object recoveredObject=null;
		try{
			//use buffering
			InputStream file = null;
			if (inputFile.endsWith(".gz")) {
				file = new GZIPInputStream(new FileInputStream(inputFile));
			} else {
				file = new FileInputStream(inputFile);
			}
			InputStream buffer = new BufferedInputStream(file);
			input = new ObjectInputStream(buffer);
			//deserialize the List
			recoveredObject = input.readObject();
		}
		catch(IOException ex){
			ex.printStackTrace();
		}
		catch (ClassNotFoundException ex){
			ex.printStackTrace();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
		finally{
			try {
				if ( input != null ) {
					//close "input" and its underlying streams
					input.close();
				}
			}
			catch (IOException ex){
				ex.printStackTrace();
			}
		}
		return recoveredObject;
	}

}
