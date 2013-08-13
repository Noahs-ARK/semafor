//package edu.cmu.cs.lti.ark.fn.parsing;
//
//import javax.servlet.ServletException;
//import javax.servlet.http.HttpServlet;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;
//import java.io.IOException;
//import java.io.PrintWriter;
//import java.io.StringReader;
//
///**
//* @author sthomson@cs.cmu.edu
//*/
//public class SemaforServlet extends HttpServlet {
//	private final Semafor parser;
//
//	public SemaforServlet(Semafor parser) {
//		this.parser = parser;
//	}
//
//	public void doGet(HttpServletRequest request,
//					  HttpServletResponse response)
//			throws ServletException, IOException {
//		final String conllInput = request.getParameter("conll");
//		response.setContentType("application/json");
//		final PrintWriter out = response.getWriter();
//		final StringReader input = new StringReader(conllInput);
//		try {
//			parser.runParser(input, out);
//		} catch (Exception e) {
//			throw new ServletException(e);
//		}
//	}
//}
