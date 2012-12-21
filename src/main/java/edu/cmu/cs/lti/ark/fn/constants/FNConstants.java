/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * FNConstants.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.fn.constants;


public class FNConstants
{
	
	/*
	 * LBFGS constants
	 */
	public static int m_max_its = 2000;
    //not sure how this parameter comes into play
    public static double m_eps = 1.0e-4;
    public static double xtol = 1.0e-10; //estimate of machine precision.  get this right
    //number of corrections, between 3 and 7
    //a higher number means more computation and time, but more accuracy, i guess
    public static int m_num_corrections = 3; 
    public static boolean m_debug = true;   
    public static int save_every_k = 10;
    
	
	/*
	 * xpaths
	 */
	public static final String FRAME_PATH = "/frames/frame";
	public static final String FRAME_NAME_ATTR = "name";

	public static final String LU_PATH = "/lexunit-annotation";
	public static final String LU_NAME_ATTR = "name";
	public static final String LU_FRAME_ATTR = "frame";
	public static final String SENTENCE = "sentence";
}
