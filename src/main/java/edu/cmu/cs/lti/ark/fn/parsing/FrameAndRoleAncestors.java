package edu.cmu.cs.lti.ark.fn.parsing;

import com.google.common.base.Charsets;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author mkshirsa@cs.cmu.edu
 */
public class FrameAndRoleAncestors {
	private static final String DEFAULT_ANCESTORS_FILE = "ancestors.csv";
	private static final String DEFAULT_PARENTS_FILE = "frame_parents.csv";
	private static final String DEFAULT_ROLES_FILE = "frame_parent_rolemappings.csv";
	public static final int PARENT = 1, ANCESTOR = 2; // mk: ancestors

	private static InputSupplier<InputStream> DEFAULT_ANCESTOR_SUPPLIER = new InputSupplier<InputStream>() {
		@Override public InputStream getInput() throws IOException {
			return getClass().getClassLoader().getResourceAsStream(DEFAULT_ANCESTORS_FILE);
		} };

	private static InputSupplier<InputStream> DEFAULT_PARENT_SUPPLIER = new InputSupplier<InputStream>() {
		@Override public InputStream getInput() throws IOException {
			return getClass().getClassLoader().getResourceAsStream(DEFAULT_PARENTS_FILE);
		} };

	private static InputSupplier<InputStream> DEFAULT_ROLE_SUPPLIER = new InputSupplier<InputStream>() {
		@Override public InputStream getInput() throws IOException {
			return getClass().getClassLoader().getResourceAsStream(DEFAULT_ROLES_FILE);
		} };

	private final Multimap<String, String> ancestors;
	private final Multimap<String, String> childToAncestorRoleMap;

	public FrameAndRoleAncestors(Multimap<String, String> ancestors) {
		this.ancestors = ancestors;
		this.childToAncestorRoleMap = HashMultimap.create();
	}

	public FrameAndRoleAncestors(Multimap<String, String> ancestors, Multimap<String, String> rolemap) {
		this.ancestors = ancestors;
		this.childToAncestorRoleMap = rolemap;
	}

	public static Multimap<String, String> loadAncestors(int type) throws IOException {
		if(type == PARENT)
			return readCsv(CharStreams.newReaderSupplier(DEFAULT_PARENT_SUPPLIER, Charsets.UTF_8));
		else
			return readCsv(CharStreams.newReaderSupplier(DEFAULT_ANCESTOR_SUPPLIER, Charsets.UTF_8));
	}

	public static FrameAndRoleAncestors load(InputSupplier<InputStreamReader> input) throws IOException {
		return new FrameAndRoleAncestors(readCsv(input));
	}

	private static Multimap<String, String> readCsv(InputSupplier<InputStreamReader> input) throws IOException {
		final Multimap<String, String> ancestors = HashMultimap.create();
		final List<String> lines = CharStreams.readLines(input);
		for (String line : lines) {
			final String[] frames = line.split(",", 2);
			if (frames.length > 1) {
				ancestors.putAll(frames[0], Lists.newArrayList(frames[1].split(",")));
			}
		}
		return ancestors;
	}

	public static FrameAndRoleAncestors loadAncestorsAndRoles(int type) throws IOException {
		Multimap<String, String> anc = loadAncestors(type);
		Multimap<String, String> roles = loadRoles();
		FrameAndRoleAncestors obj = new FrameAndRoleAncestors(anc, roles);
		return obj;
	}

	public static Multimap<String, String> loadRoles() throws IOException {
		return loadRolesfile(CharStreams.newReaderSupplier(DEFAULT_ROLE_SUPPLIER, Charsets.UTF_8));
	}

	private static Multimap<String, String> loadRolesfile(InputSupplier<InputStreamReader> input) throws IOException {
		Multimap<String, String> rolemap = HashMultimap.create();
		final List<String> lines = CharStreams.readLines(input);
        for (String line : lines) {
            final String[] mapping = line.split(",",2);
			if (mapping.length > 1) {
				rolemap.putAll(mapping[0], Lists.newArrayList(mapping[1].split(",")));
			}
		}
		return rolemap;
	}

	public Collection<String> getAncestors(String frame) {
		return ancestors.get(frame);
	}

	public Collection<String> getAncestorRoles(String frameAndRole) {
		return childToAncestorRoleMap.get(frameAndRole);
	}

	public Map<String, Collection<String>> getAllAncestors() {
		return ancestors.asMap();
	}

	public Collection<String> getAllParents() {
		return ancestors.values();
	}

}
