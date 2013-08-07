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
package edu.cmu.cs.lti.ark.fn;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import edu.cmu.cs.lti.ark.fn.parsing.SemaforParseResult;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;
import edu.jhu.hlt.concrete.Concrete;

import java.io.File;
import java.util.List;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Lists.transform;
import static edu.cmu.cs.lti.ark.fn.Semafor.getSemaforInstance;

/**
 * Converts semafor output to Concrete protobuf objects
 */
public class SemaforConcrete {
	protected final Semafor semafor;

	/**
	 * required flags:
	 * model-dir
	 * input-file
	 * output-file
	 */
	public static void main(String[] args) throws Exception {
		final FNModelOptions options = new FNModelOptions(args);
		final File inputFile = new File(options.inputFile.get());
		final File outputFile = new File(options.outputFile.get());
		final String modelDirectory = options.modelDirectory.get();
		final int numThreads = options.numThreads.present() ? options.numThreads.get() : 1;
		final SemaforConcrete semafor = new SemaforConcrete(getSemaforInstance(modelDirectory));
//		semafor.runParser(
//				Files.newReaderSupplier(inputFile, Charsets.UTF_8),
//				Files.newWriterSupplier(outputFile, Charsets.UTF_8),
//				numThreads);
	}

	public SemaforConcrete(Semafor semafor) {
		this.semafor = semafor;
	}

	/**
	 * Converts a SemaforParseResult to an equivalent Concrete version
	 * @param parseResult a Semafor-parsed sentence
	 * @return a List of SituationMentions, each one representing a frame and its arguments.
	 */
	public List<Concrete.SituationMention> semaforParseToConcrete(SemaforParseResult parseResult, Concrete.Sentence sentence) {
		final Concrete.UUID sentenceUuid = sentence.getUuid();
		final Concrete.Tokenization tokenization = sentence.getTokenization(0);
		final List<Concrete.Token> tokens = tokenization.getTokenList();
		final Concrete.Token token = tokens.get(0);
		final Concrete.UUID tokenizationUuid = tokenization.getUuid();
		final List<Concrete.TokenTagging> lemmas = tokenization.getLemmasList();
		final List<Concrete.TokenTagging> posTags = tokenization.getPosTagsList();
		final Concrete.DependencyParse dependencyParse = tokenization.getDependencyParse(0);

		return copyOf(concat(transform(parseResult.frames,
				new Function<SemaforParseResult.Frame, List<Concrete.SituationMention>>() {
					@Override public List<Concrete.SituationMention> apply(SemaforParseResult.Frame input) {
						return frameParseToConcrete(input);
					} })));
	}

	List<Concrete.SituationMention> frameParseToConcrete(SemaforParseResult.Frame frameParse) {
		final List<Concrete.SituationMention> situationMentions = Lists.newArrayList();
		final Concrete.TokenRefSequence targetTokens = spansToConcrete(frameParse.target.spans);
		for (SemaforParseResult.Frame.ScoredRoleAssignment scoredRoleAssignment : frameParse.annotationSets) {
			final Concrete.SituationMention.Builder predicateBuilder = Concrete.SituationMention.newBuilder();
			predicateBuilder.setTokens(targetTokens);
			predicateBuilder.setConfidence((float) scoredRoleAssignment.score);
			final String frameName = frameParse.target.name;
			predicateBuilder.setSituationKindLemma("FrameNet:" + frameName);
			for (SemaforParseResult.Frame.NamedSpanSet frameElement : scoredRoleAssignment.frameElements) {
				final Concrete.SituationMention.Builder roleSituationBuilder = Concrete.SituationMention.newBuilder();
				roleSituationBuilder.setTokens(spansToConcrete(frameElement.spans));
				final Concrete.SituationMention roleSituation = roleSituationBuilder.build();
				situationMentions.add(roleSituation);
				final Concrete.SituationMention.Argument.Builder argumentBuilder =
						Concrete.SituationMention.Argument.newBuilder();
				argumentBuilder.setRoleLabel("FrameNet:" + frameName + ":" + frameElement.name);
				argumentBuilder.setSituationMentionId(roleSituation.getUuid());
				predicateBuilder.addArgument(argumentBuilder.build());
			}
			situationMentions.add(predicateBuilder.build());
		}
		return situationMentions;
	}

	protected Concrete.TokenRefSequence spansToConcrete(Iterable<SemaforParseResult.Frame.Span> spans) {
		final Concrete.TokenRefSequence.Builder targetTokensBuilder = Concrete.TokenRefSequence.newBuilder();
		for (SemaforParseResult.Frame.Span span : spans) {
			targetTokensBuilder.addAllTokenIndex(new Range0Based(span.start, span.end));
		}
		return targetTokensBuilder.build();
	}
}
