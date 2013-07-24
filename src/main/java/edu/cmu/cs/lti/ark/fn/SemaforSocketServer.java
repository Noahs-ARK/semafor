package edu.cmu.cs.lti.ark.fn;

import com.google.common.base.Charsets;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.Sentence;
import edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;

import static com.google.common.io.Closeables.closeQuietly;
import static edu.cmu.cs.lti.ark.fn.data.prep.formats.SentenceCodec.ConllCodec;

public class SemaforSocketServer {
	/**
	 * required flags:
	 * model-dir
	 * port
	 */
	public static void main(String[] args) throws Exception {
		final FNModelOptions options = new FNModelOptions(args);
		final String modelDirectory = options.modelDirectory.get();
		final int port = options.port.get();
		runSocketServer(modelDirectory, port);
	}

	public static void runSocketServer(String modelDirectory, int port)
			throws URISyntaxException, IOException, ClassNotFoundException {
		final Semafor semafor = Semafor.getSemaforInstance(modelDirectory);
		// Set up socket server
		final ServerSocket serverSocket = new ServerSocket(port);
		System.err.println("Listening on port: " + port);
		while (true) {
			try {
				final Socket clientSocket = serverSocket.accept();
				final SentenceCodec.SentenceIterator sentences =
						ConllCodec.readInput(new InputStreamReader(clientSocket.getInputStream(), Charsets.UTF_8));
				final PrintWriter output =
						new PrintWriter(new OutputStreamWriter(clientSocket.getOutputStream(), Charsets.UTF_8));
				while (sentences.hasNext()) {
					final Sentence sentence = sentences.next();
					final long start = System.currentTimeMillis();
					output.println(semafor.parseSentence(sentence).toJson());
					output.flush();
					final long end = System.currentTimeMillis();
					System.err.printf("parsed sentence with %d tokens in %d millis.%n", sentence.size(), end - start);
				}
				closeQuietly(sentences);
				closeQuietly(output);
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}
}
