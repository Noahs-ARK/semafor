package edu.cmu.cs.lti.ark.fn.data;

import com.google.common.collect.ImmutableMap;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;

import java.util.Map;

/**
 * Information about how the training data was split into train, dev, and test sets.
 */
public class Splits {
	/* Document descriptor: SubcorpusName/DocumentName
	 * Note that sentence indices are only unique within a particular data set

	train (18 documents, 1663 sentences)
	ANC/EntrepreneurAsMadonna	0	33
	ANC/HistoryOfJerusalem		171	292
	NTI/BWTutorial_chapter1		292     393
	NTI/Iran_Chemical			393     536
	NTI/Iran_Introduction		536     598
	NTI/Iran_Missile			598     778
	NTI/Iran_Nuclear			778     913
	NTI/Kazakhstan				913     942
	NTI/LibyaCountry1			942     983
	NTI/NorthKorea_ChemicalOverview		983     1055
	NTI/NorthKorea_NuclearCapabilities	1055    1085
	NTI/NorthKorea_NuclearOverview		1085    1206
	NTI/Russia_Introduction		1206    1247
	NTI/SouthAfrica_Introduction		1247    1300
	NTI/Syria_NuclearOverview			1300    1356
	NTI/Taiwan_Introduction		1356    1392
	NTI/WMDNews_062606			1392    1476
	PropBank/PropBankCorpus		1476    1801

	dev (4 documents, 251 sentences)
	ANC/StephanopoulosCrimes	146     178
	NTI/Iran_Biological			178     280
	NTI/NorthKorea_Introduction	280     329
	NTI/WMDNews_042106			329     397

	test (3 documents, 120 sentences)
	ANC/IntroOfDublin			0       67
	NTI/chinaOverview			67      106
	NTI/workAdvances			106     120
	*/
	public static final String FN13_LEXICON_EXEMPLARS = "exemplars";
	public static final String SEMEVAL07_TRAIN_SET = "train";
	public static final String SEMEVAL07_DEV_SET = "dev";
	public static final String SEMEVAL07_TEST_SET = "test";
	/** Sentence index ranges for documents in the train, dev, and test portions of the SemEval'07 data */
	public static final Map<String, ? extends Map<String, Range0Based>> DOCUMENT_SENTENCE_RANGES =
			ImmutableMap.of(
					FN13_LEXICON_EXEMPLARS, ImmutableMap.of(
							"*", new Range0Based(0, 139439, false)),
					SEMEVAL07_TRAIN_SET, ImmutableMap.<String, Range0Based>builder()
							.put("ANC/EntrepreneurAsMadonna", new Range0Based(0, 33, false))
							.put("ANC/HistoryOfJerusalem", new Range0Based(171, 292, false))
							.put("NTI/BWTutorial_chapter1", new Range0Based(292, 393, false))
							.put("NTI/Iran_Chemical", new Range0Based(393, 536, false))
							.put("NTI/Iran_Introduction", new Range0Based(536, 598, false))
							.put("NTI/Iran_Missile", new Range0Based(598, 778, false))
							.put("NTI/Iran_Nuclear", new Range0Based(778, 913, false))
							.put("NTI/Kazakhstan", new Range0Based(913, 942, false))
							.put("NTI/LibyaCountry1", new Range0Based(942, 983, false))
							.put("NTI/NorthKorea_ChemicalOverview", new Range0Based(983, 1055, false))
							.put("NTI/NorthKorea_NuclearCapabilities", new Range0Based(1055, 1085, false))
							.put("NTI/NorthKorea_NuclearOverview", new Range0Based(1085, 1206, false))
							.put("NTI/Russia_Introduction", new Range0Based(1206, 1247, false))
							.put("NTI/SouthAfrica_Introduction", new Range0Based(1247, 1300, false))
							.put("NTI/Syria_NuclearOverview", new Range0Based(1300, 1356, false))
							.put("NTI/Taiwan_Introduction", new Range0Based(1356, 1392, false))
							.put("NTI/WMDNews_062606", new Range0Based(1392, 1476, false))
							.put("PropBank/PropBankCorpus", new Range0Based(1476, 1801, false))
							.build(),
					SEMEVAL07_DEV_SET, ImmutableMap.of(
							"ANC/StephanopoulosCrimes", new Range0Based(146, 178, false),
							"NTI/Iran_Biological", new Range0Based(178, 280, false),
							"NTI/NorthKorea_Introduction", new Range0Based(280, 329, false),
							"NTI/WMDNews_042106", new Range0Based(329, 397, false)),
					SEMEVAL07_TEST_SET, ImmutableMap.of(
							"ANC/IntroOfDublin", new Range0Based(0, 67, false),
							"NTI/chinaOverview", new Range0Based(67, 106, false),
							"NTI/workAdvances", new Range0Based(106, 120, false)));
}
