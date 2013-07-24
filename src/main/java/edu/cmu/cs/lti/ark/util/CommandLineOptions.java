/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * CommandLineOptions.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import edu.cmu.cs.lti.ark.util.ds.Range;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;
import edu.cmu.cs.lti.ark.util.ds.Range1Based;
import edu.cmu.cs.lti.ark.util.ds.map.SingleAssignmentHashMap;

/**
 * Processes command-line arguments and stores a variety of configuration parameters as option-value pairs. 
 * Options should be formatted as optname:value (or simply the optname if boolean).
 * See the code for details.
 * 
 * @author Nathan Schneider (nschneid)
 * @since 2009-04-24
 * 2009-09-25: Made abstract, with application-specific options in {@link edu.cmu.cs.lti.ark.util.FNModelOptions}
 */
public abstract class CommandLineOptions {
	public class InvalidOptionsException extends Exception {
		private static final long serialVersionUID = -4353285681883730567L;
		public InvalidOptionsException(String s) {
			super(s);
		}
	}
	
	public class MissingOptionsException extends InvalidOptionsException {
		private static final long serialVersionUID = 3740404390768087830L;

		public MissingOptionsException(Option requiredOption) {
			super(String.format("Missing required argument: %s", requiredOption.name));
		}

		public MissingOptionsException(List<Option> missingOptions) {
			super("Missing required arguments: " + Joiner.on(", ").join(optionNames(missingOptions)));
		}
	}
	
	public abstract class Option {
		public final String name;
		abstract void set(String value) throws InvalidOptionsException;
		public Option(String name) { this.name = name; opts.put(name, this); }
		public boolean present() { return args.containsKey(name); }
		public boolean absent() { return !present(); }
		public abstract String toString();	// should call get(); necessary for string concatenation
		public String make() { return (present()) ? make(toString()) : ""; }
		public String make(String v) { return name + ":" + v; }
	}
	
	public class StringOption extends Option {
		public String get() { return (String)args.get(name); }
		public StringOption(String name) { super(name); }
		public void set(String value) { args.put(name, value); }
		public String toString() { return toString(get()); }
		public String toString(String v) { return v; }
	}
	
	/** Option which is a path in the local file system */
	public class PathOption extends Option {
		public boolean isDirectory = false;
		public String get() { return toString(); }
		public File getFile() { return (File)args.get(name); }
		public PathOption(String name) { super(name); }
		public void set(String path) throws InvalidOptionsException { set(new File(path)); }
		public void set(File path) { args.put(name, path); }
		public String toString() { return toString(getFile()); }
		public String toString(File v) { return v.getAbsolutePath(); }
		public boolean exists() { return getFile().exists(); }
		public boolean exists(String path) { return new File(path).exists(); }
		public boolean parentDirectoryExists() { return getFile().getParentFile().exists(); }
		public boolean parentDirectoryExists(String path) {
			final File parentFile = new File(path).getParentFile();
			return parentFile != null && parentFile.exists();
		}
	}
	/** Option which is a path that should already exist in the local file system */
	public class ExistingPathOption extends PathOption {
		public ExistingPathOption(String name) { super(name); }
		public void set(String path) throws InvalidOptionsException { 
			if (!exists(path))
				throw new InvalidOptionsException("Path value of '" + name + "' option does not exist: " + path);
			else 
				super.set(path); 
		}
	}
	/** Option which is a path that should not yet exist in the local file system */
	public class NewPathOption extends PathOption {
		public NewPathOption(String name) { super(name); }
		public void set(String path) throws InvalidOptionsException { 
			if (exists(path))
				throw new InvalidOptionsException("Path value of '" + name + "' option already exists: " + path);
			else 
				super.set(path); 
		}
	}
	/** Option which is a path to a file that should not exist yet, but whose parent directory should already exist in the local file system */
	public class NewFilePathOption extends NewPathOption {
		public NewFilePathOption(String name) { super(name); }
		public void set(String path) throws InvalidOptionsException { 
			if (!parentDirectoryExists(path))
				throw new InvalidOptionsException("Parent directory of the value of '" + name + "' option does not exist: " + path);
			else 
				super.set(path); 
		}
	}
	
	/** Option for a file which is to be created as output during one phase of the program and later used as input. */
	public class IntermediateFilePathOption extends PathOption {
		protected int status = 0;	// 1 if it should exist already (input); -1 if it should not exist yet (output)
		public IntermediateFilePathOption(String name) { super(name); }
		public void set(String path) throws InvalidOptionsException { 
			if (!parentDirectoryExists(path))
				throw new InvalidOptionsException("Parent directory of the value of '" + name + "' option does not exist: " + path);
			else 
				super.set(path); 
		}
		public IntermediateFilePathOption asInput() throws InvalidOptionsException {
			String path = toString();
			if (!exists(path))
				throw new InvalidOptionsException("Path value of '" + name + "' option does not exist: " + path);
			status = 1;
			return this;
		}
		public IntermediateFilePathOption asOutput() throws InvalidOptionsException {
			if (status>0)
				throw new InvalidOptionsException("Cannot change from input file to output file");
			String path = toString();
			if (exists(path))
				throw new InvalidOptionsException("Path value of '" + name + "' option already exists: " + path);
			status = -1;
			return this;
		}
		/** @return {@literal 1} if serving as an input file, {@literal -1} if serving as an output file, 
		 * and {@literal 0} if the status is unknown */
		public int getStatus() {
			return status;
		}
		public String get() {
			if (status==0)
				System.err.println("WARNING: IntermediateFilePathOption unspecified as input or output file.");
			return super.get();
		}
		public File getFile() {
			if (status==0)
				System.err.println("WARNING: IntermediateFilePathOption unspecified as input or output file.");
			return super.getFile();
		}
	}
	
	public class IntOption extends Option {
		protected Range range;
		public int get() {
			return (Integer)args.get(name);
		}
		public IntOption(String name) { this(name,null); }
		public IntOption(String name, Range validRange) {
			super(name);
			range = validRange;
		}
		public void set(String value) throws InvalidOptionsException {
			int v = Integer.parseInt(value);
			if (range!=null && !range.contains(v))
				throw new InvalidOptionsException("Integer value " + v + " for option " + this.name + " falls outside the legal range: " + range.toString());
			args.put(name, v);
		}
		public String toString() { return toString(get()); }
		public String toString(int v) { return Integer.toString(v); }
		public String make(int v) { return make(toString(v)); }
	}
	public class NonnegativeIntOption extends IntOption {
		public NonnegativeIntOption(String name) { super(name, new Range0Based(0, Integer.MAX_VALUE - 1, true)); }
	}
	public class PositiveIntOption extends IntOption {
		public PositiveIntOption(String name) { super(name, new Range0Based(1, Integer.MAX_VALUE - 1, true)); }
	}
	public class DoubleOption extends Option {
		public double get() { return (Double)args.get(name); }
		public DoubleOption(String name) { super(name); }
		public void set(String value) { args.put(name, new Double(value)); }
		public String toString() { return toString(get()); }
		public String toString(double v) { return Double.toString(v); }
		public String make(double v) { return make(toString(v)); }
	}
	public class BoolOption extends Option {
		public final boolean DEFAULT_VALUE = false;
		public boolean get() { return (Boolean)args.get(name); }
		public BoolOption(String name) { super(name); args.put(name, DEFAULT_VALUE); }
		public void set(String val) { assert(get()==false); args.reset(name); args.put(name, true); }
		public boolean present() { return get()==true; }
		public String toString() { return toString(get()); }
		public String toString(boolean v) { return Boolean.toString(v); }
		public String make() { return make(present()); }
		public String make(boolean v) { return (v) ? name : ""; }
	}
	
	protected SingleAssignmentHashMap<String,Object> args = new SingleAssignmentHashMap<String,Object>();
	protected Map<String,Option> opts = new SingleAssignmentHashMap<String,Option>();

	/**
	 * Given an array of command-line arguments, loads the values for various member variables 
	 * corresponding to options. Also prints the options and their values to standard output.
	 * Arguments should be formatted as optname:value (or simply the optname if boolean).
	 * Unknown options trigger an error and exit by default (but see other constructor).
	 */
	public CommandLineOptions(String[] args)
	{
		this(args, false);
	}
	
	
	/**
	 * Given an array of command-line arguments, loads the values for various member variables 
	 * corresponding to options. Also prints the options and their arguments to standard output. 
	 * Arguments should be formatted as optname:value (or simply the optname if boolean).
	 * If 'ignoreUnknownOptions' is true, the presence of unknown option flags in 'args' 
	 * will cause the constructor to print an error and exit.
	 */
	public CommandLineOptions(String[] args, boolean ignoreUnknownOptions)
	{
		init(args,ignoreUnknownOptions);
	}
	
	protected CommandLineOptions() { }
	
	protected void init(String[] args, boolean ignoreOptions) {
		boolean ok = true;
		for(int i = 0; i < args.length; i ++)
		{
			System.out.println(args[i]);
			String[] pair = args[i].split(":");
			
			String optName = pair[0];
			Option opt = getOptionByName(optName);
			if (opt==null) {
				System.err.println("Invalid option name: " + optName);
				ok = false;
			}
			else {
				String val = (pair.length>1) ? pair[1].trim() : null;
				try {
					opt.set(val);
				} catch (InvalidOptionsException ex) {
					ex.printStackTrace();
					ok = false;
				}
			}
		}
		if (!ok) {
			System.err.println("Exiting (invalid set of options)");
			System.exit(1);
		}
	}
	
	public Option getOptionByName(String optName) {
		return opts.get(optName);
	}

	public Object getArgumentByOptionName(String optName) {
		return args.get(optName);
	}

	public boolean isPresent(String optName) {
		return args.containsKey(optName);
	}

	public void ensurePresence(Option requiredOption) throws MissingOptionsException {
		if (requiredOption.absent()) {
			throw new MissingOptionsException(requiredOption);
		}
	}

	public void ensurePresence(Option[] requiredOptions) throws MissingOptionsException {
		List<Option> missingOptions = new ArrayList<Option>();
		for (Option opt : requiredOptions) {
			if (opt.absent())
				missingOptions.add(opt);
		}
		if (!missingOptions.isEmpty())
			throw new MissingOptionsException(missingOptions);
	}

	public void ensurePresenceOrQuit(Option[] requiredOptions) {
		try {
			ensurePresence(requiredOptions);
		}
		catch (MissingOptionsException ex) {
			ex.printStackTrace();
			System.err.println("Exiting due to missing command line arguments");
			System.exit(1);
		}
	}

	/**
	 * @return An iterator over ALL options (including ones without values)
	 */
	public Iterator<Option> getIterator() {
		return opts.values().iterator();
	}

	/**
	 * @return Generated argument string specifying all stored option-value pairs, in no particular order
	 */
	public String make() {
		String s = "";
		for (Iterator<Option> iter = getIterator(); iter.hasNext();) {
			Option opt = iter.next();
			s += opt.make() + " ";
		}
		return s.trim();
	}

	/**
	 * Generates a string specifying option-value pairs for particular options
	 * @param optionsToInclude The options, in the order they are to be listed in the string
	 * @return Generated argument string
	 */
	public static String make(Option[] optionsToInclude) {
		String s = "";
		for (Option opt : optionsToInclude) {
			s += opt.make() + " ";
		}
		return s.trim();
	}

	protected static List<String> optionNames(List<Option> opts) {
		List<String> names = new ArrayList<String>();
		for (Option opt : opts)
			names.add(opt.name);
		return names;
	}

}
