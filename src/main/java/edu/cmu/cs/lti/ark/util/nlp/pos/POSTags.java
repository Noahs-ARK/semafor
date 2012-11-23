/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * POSTags.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.util.nlp.pos;

/**
 * Methods for dealing with Penn Treebank part-of-speech tags for English.
 * 
 * The full list of tags is reproduced below. 
 * Source: M. Marcus, Beatrice Santorini, and M.A. Marcinkiewicz (1993), <i>Building a large annotated corpus of English: The Penn Treebank</i>. 
 * In Computational Linguistics, volume 19, number 2, pp. 313-330
 * <a href="http://acl.ldc.upenn.edu/J/J93/J93-2004.pdf">http://acl.ldc.upenn.edu/J/J93/J93-2004.pdf</a>
 * 
 * <dl>
 * <dt>	1. CC	</dt><dd>	Coordinating conjunction	</dd>
 * <dt>	2. CD	</dt><dd>	Cardinal number	</dd>
 * <dt>	3. DT	</dt><dd>	Determiner	</dd>
 * <dt>	4. EX	</dt><dd>	Existential there	</dd>
 * <dt>	5. FW	</dt><dd>	Foreign word	</dd>
 * <dt>	6. IN	</dt><dd>	Preposition/subord. conjunction	</dd>
 * <dt>	7. JJ	</dt><dd>	Adjective	</dd>
 * <dt>	8. JJR	</dt><dd>	Adjective, comparative	</dd>
 * <dt>	9. JJS	</dt><dd>	Adjective, superlative	</dd>
 * <dt>	10. LS	</dt><dd>	List item marker	</dd>
 * <dt>	11. MD	</dt><dd>	Modal	</dd>
 * <dt>	12. NN	</dt><dd>	Noun, singular or mass	</dd>
 * <dt>	13. NNS	</dt><dd>	Noun, plural	</dd>
 * <dt>	14. NNP	</dt><dd>	Proper noun, singular	</dd>
 * <dt>	15. NNPS	</dt><dd>	Proper noun, plural	</dd>
 * <dt>	16. PDT	</dt><dd>	Predeterminer	</dd>
 * <dt>	17. POS	</dt><dd>	Possessive ending	</dd>
 * <dt>	18. PRP	</dt><dd>	Personal pronoun	</dd>
 * <dt>	19. PP$	</dt><dd>	Possessive pronoun	</dd>
 * <dt>	20. RB	</dt><dd>	Adverb	</dd>
 * <dt>	21. RBR	</dt><dd>	Adverb, comparative	</dd>
 * <dt>	22. RBS	</dt><dd>	Adverb, superlative	</dd>
 * <dt>	23. RP	</dt><dd>	Particle	</dd>
 * <dt>	24. SYM	</dt><dd>	Symbol (mathematical or scientific)	</dd>
 * <dt>	25. TO	</dt><dd>	<i>to</i>	</dd>
 * <dt>	26. UH	</dt><dd>	Interjection	</dd>
 * <dt>	27. VB	</dt><dd>	Verb, base form	</dd>
 * <dt>	28. VBD	</dt><dd>	Verb, past tense	</dd>
 * <dt>	29. VBG	</dt><dd>	Verb, gerund/present participle	</dd>
 * <dt>	30. VBN	</dt><dd>	Verb, past participle	</dd>
 * <dt>	31. VBP	</dt><dd>	Verb, non-3rd ps. sing. present	</dd>
 * <dt>	32. VBZ	</dt><dd>	Verb, 3rd ps. sing. present	</dd>
 * <dt>	33. WDT	</dt><dd>	wh-determiner	</dd>
 * <dt>	34. WP	</dt><dd>	wh-pronoun	</dd>
 * <dt>	35. WP$	</dt><dd>	Possessive wh-pronoun	</dd>
 * <dt>	36. WRB	</dt><dd>	wh-adverb	</dd>
 * <dt>	37. #	</dt><dd>	Pound sign	</dd>
 * <dt>	38. $	</dt><dd>	Dollar sign	</dd>
 * <dt>	39. .	</dt><dd>	Sentence-final punctuation	</dd>
 * <dt>	40. ,	</dt><dd>	Comma	</dd>
 * <dt>	41. :	</dt><dd>	Colon, semi-colon	</dd>
 * <dt>	42. ( or -LRB-	</dt><dd>	Left bracket character	</dd>
 * <dt>	43. ) or -RRB-	</dt><dd>	Right bracket character	</dd>
 * <dt>	44. "	</dt><dd>	Straight double quote	</dd>
 * <dt>	45. ‘	</dt><dd>	Left open single quote	</dd>
 * <dt>	46. "	</dt><dd>	Left open double quote	</dd>
 * <dt>	47. ’	</dt><dd>	Right close single quote	</dd>
 * <dt>	48. "	</dt><dd>	Right close double quote	</dd>
 * </dl>
 * 
 * @author Nathan Schneider (nschneid)
 * @since 2009-12-07
 *
 */
public class POSTags {

	/** Given a Penn Treebank POS tag, returns a corresponding coarse tag */
	public static String getCoarsePOS(String finePOS) {
		if (finePOS.isEmpty()) return "";
		String c0 = finePOS.substring(0,1);
		if (c0.equals("J") || c0.equals("N") || c0.equals("V") || c0.equals("W"))	// adjective, noun, verb, WH-word
			return c0;
		
		if (finePOS.length()>1) {
			String c01 = finePOS.substring(0,2);
			if (c01.equals("PR") || c01.equals("RB"))	// pronoun, adverb
				return c01;
		}
		
		return finePOS;
	}

	public static boolean isContentPOS(String pos) {
		return (pos.startsWith("N") || pos.startsWith("V")
				|| pos.startsWith("J") || pos.startsWith("R"));	// TODO: Fixed bug - this has "A" instead of "J" for adjectives
	}

	public static boolean isNumber(String pos) {
		return (pos.startsWith("CD"));
	}

	public static boolean isClosedClass(String pos) {
		return (!isContentPOS(pos) && !isNumber(pos));
	}
	
	public static boolean isPunctuationOrSymbol(String pos) {
		return (!Character.isLetter(pos.charAt(0)) || pos.equals("SYM"));
	}

	public static boolean isPronoun(String pos) {
		return (pos.startsWith("WP") || pos.startsWith("PR"));
	}
	
	public static boolean isDeterminer(String pos) {
		return (pos.endsWith("DT"));
	}
}
