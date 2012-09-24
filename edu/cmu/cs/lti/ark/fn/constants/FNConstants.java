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
	public static final String FRAME_ID_ATTR = "ID";
	public static final String FRAME_NAME_ATTR = "name";
	
	public static final String LE_PATH = "/lexical-entry";
	public static final String LE_ID_ATTR = "ID";
	public static final String LE_NAME_ATTR = "name";
	public static final String LE_FRAME_ATTR = "frame";
	public static final String LE_POS_ATTR = "pos";
	
	public static final String LU_PATH = "/lexunit-annotation";
	public static final String LU_ID_ATTR = "ID";
	public static final String LU_NAME_ATTR = "name";
	public static final String LU_FRAME_ATTR = "frame";
	public static final String LU_POS_ATTR = "pos";
	public static final String LEXEMES = "lexemes";
	public static final String SUB_CORPUS = "subcorpus";
	public static final String ANNOTATION_SET = "annotationSet";
	public static final String SENTENCE = "sentence";
	
	
public static final String[] NECESSARY_TAGS = {"PERSON", "ORGANIZATION", "LOCATION", "DATE", "TIME"};
	
	public static final String[] NECESSARY_TAGS_TWO = {
		"ANIMAL-B",
		"ANIMAL-I",
		"CARDINAL-B",
		"CARDINAL-I",
		"CONTACT-INFO-B",
		"CONTACT-INFO-I",
		"DATE-B",
		"DATE-I",
		"DISEASE-B",
		"DISEASE-I",
		"EVENT-B",
		"EVENT-I",
		"FAC-B",
		"FAC-DESC-B",
		"FAC-DESC-I",
		"FAC-I",
		"GAME-B",
		"GAME-I",
		"GPE-B",
		"GPE-DESC-B",
		"GPE-DESC-I",
		"GPE-I",
		"LANGUAGE-B",
		"LANGUAGE-I",
		"LAW-B",
		"LAW-I",
		"LOCATION-B",
		"LOCATION-I",
		"MONEY-B",
		"MONEY-I",
		"NATIONALITY-B",
		"NATIONALITY-I",
		"ORDINAL-B",
		"ORDINAL-I",
		"ORG-DESC-B",
		"ORG-DESC-I",
		"ORGANIZATION-B",
		"ORGANIZATION-I",
		"PER-DESC-B",
		"PER-DESC-I",
		"PERCENT-B",
		"PERCENT-I",
		"PERSON-B",
		"PERSON-I",
		"PLANT-B",
		"PLANT-I",
		"PRODUCT-B",
		"PRODUCT-DESC-B",
		"PRODUCT-DESC-I",
		"PRODUCT-I",
		"QUANTITY-B",
		"QUANTITY-I",
		"SUBSTANCE-B",
		"SUBSTANCE-I",
		"TIME-B",
		"TIME-I",
		"WORK-OF-ART-B",
		"WORK-OF-ART-I",
		};


	
	public static final String[] UNNECESSARY_TAGS = {"ANIMAL",
													"CONTACT_INFO",
													"DISEASE",
													"EVENT",
													"FAC",
													"FAC_DESC",
													"GAME",
													"GPE",
													"GPE_DESC",
													"LANGUAGE",
													"LAW",
													"NATIONALITY",
													"ORG_DESC",
													"PER_DESC",
													"PLANT",
													"PRODUCT",
													"PRODUCT_DESC",
													"SUBSTANCE",
													"WORK_OF_ART"};	
	
	public static final String DUMMY_ROOT_LABEL="$$";
	
	public static final String NULL_WORD="null_word";
	
	public static final String NULL_LEMMA="null_lemma";
	
	public static final String NULL_NULL_WORD="null_null_word";
	
	public static final boolean PRUNE_AWAY_PARSES=false;
	
	public static final int MAX_SOURCE_LEN=45;
	
	public static final boolean PRUNE_TILL_BASE_NPS=false;
	
	public static final boolean PRINT_SPECIFIC_ALIGNMENTS = true;
	
	public static final String NULL_TAG = "NULL-B";
	public static final String NULL_NULL_TAG = "NULL_NULL-B";
	
}
