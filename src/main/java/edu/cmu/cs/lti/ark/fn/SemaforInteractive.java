package edu.cmu.cs.lti.ark.fn;

import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;

import java.io.*;
import com.fasterxml.jackson.databind.ObjectMapper;

import static edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec.ConllCodec;

import java.net.URISyntaxException;

public class SemaforInteractive {

	/*
	 * Interactive runner for semafor
	 * Provide output as CONLL style parsed sentences, one token per line
	 * Separate sentences with blank lines
	 * Output (in json format) will be provided after every sentence
	 */

	private static final ObjectMapper jsonMapper = new ObjectMapper();

	public static void main(String[] args) throws Exception {
		final FNModelOptions options = new FNModelOptions(args);
		final String modelDirectory = options.modelDirectory.get();
		runInteractive(modelDirectory);
	}

	public static void runInteractive(String modelDirectory)
	    throws IOException, ClassNotFoundException, URISyntaxException {
		final Semafor semafor = Semafor.getSemaforInstance(modelDirectory);

		final SentenceCodec.SentenceIterator sentences =
		    ConllCodec.readInput(new InputStreamReader(System.in));
		final PrintWriter output =
		    new PrintWriter(System.out);

		System.err.println("[READY] Semafor running in interactive mode. "
				   +"Please provide dependency-parsed sentences in CONLL format "
				   +"separated by blank lines");
		output.println(">>>");
		output.flush();
		while (sentences.hasNext()) {
		    SemaforSocketServer.processSentence(semafor, sentences.next(), output);
		    output.println(">>>");
		    output.flush();
		}
	}
}
