package edu.cmu.cs.lti.ark.fn.parsing;

import com.google.common.base.Charsets;
import com.google.common.io.InputSupplier;
import com.google.common.io.OutputSupplier;
import edu.cmu.cs.lti.ark.fn.utils.FNModelOptions;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;

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
		final Semafor server = Semafor.getSemaforInstance(modelDirectory);
		// Set up socket server
		final ServerSocket serverSocket = new ServerSocket(port);
		System.err.println("Listening on port: " + port);
		while (true) {
			try {
				final Socket clientSocket = serverSocket.accept();
				final InputSupplier<InputStreamReader> inputSupplier = new InputSupplier<InputStreamReader>() {
					@Override
					public InputStreamReader getInput() throws IOException {
						return new InputStreamReader(clientSocket.getInputStream(), Charsets.UTF_8);
					}
				};
				final OutputSupplier<OutputStreamWriter> outputSupplier = new OutputSupplier<OutputStreamWriter>() {
					@Override
					public OutputStreamWriter getOutput() throws IOException {
						return new OutputStreamWriter(clientSocket.getOutputStream(), Charsets.UTF_8);
					}
				};
				server.runParser(inputSupplier, outputSupplier, 1);
			} catch (Exception e) {
				e.printStackTrace(System.err);
			}
		}
	}
}