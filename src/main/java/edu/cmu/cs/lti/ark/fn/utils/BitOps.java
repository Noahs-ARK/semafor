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
package edu.cmu.cs.lti.ark.fn.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class BitOps {
	public static boolean nearlyEquals(double[] a1, double[] a2, double tol) {
		if (a1.length != a2.length) {
			return false;
		}
		for (int i = 0; i < a1.length; i++) {
			if ((a1[i] - a2[i])*(a1[i] - a2[i]) >= tol) {
				return false;
			}
		}
		return true;
	}
	
	public static boolean nearlyBinary(double a1, double tol) {
		if (a1*a1 < tol) {
			return true;
		} else if ((a1 - 1.0)*(a1 - 1.0) < tol) {
			return true;
		} else {
			return false;
		}
	}
	
	public static void writeInt(int a, OutputStream ps) {
		byte byteout[] = { (byte) ((a >> 24) & 0xff),
				(byte) ((a >> 16) & 0xff), (byte) ((a >> 8) & 0xff),
				(byte) (a & 0xff) };
		try {
			ps.write(byteout);
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());
		}
	}
	
	public static int readAnInt(InputStream fis) {
		byte[] b = new byte[4];
		try {
			fis.read(b);
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());
			return -1;
		}
		int ret = 0;
		ret += ((int) b[0] & 0xff) << 24;
		ret += ((int) b[1] & 0xff) << 16;
		ret += ((int) b[2] & 0xff) << 8;
		ret += ((int) b[3] & 0xff);
		return ret;
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

}
