/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * WeightedRootedHyperDAG.java is part of SEMAFOR 2.0.
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
package edu.cmu.cs.lti.ark.util.ds.graph;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.cmu.cs.lti.ark.util.ds.map.CounterMap;
import edu.cmu.cs.lti.ark.util.ds.map.FactoryDefaultMap;
import edu.cmu.cs.lti.ark.util.ds.map.FactoryDefaultMap.DefaultValueFactory;
import edu.cmu.cs.lti.ark.util.ds.path.IndexedLabeledPath;
import edu.cmu.cs.lti.ark.util.ds.path.IndexedPath;
import edu.cmu.cs.lti.ark.util.ds.path.LabeledPath;
import edu.cmu.cs.lti.ark.util.ds.path.Path;
import edu.cmu.cs.lti.ark.util.ds.ArrayUtil;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.optimization.LDouble;
import edu.cmu.cs.lti.ark.util.optimization.LogFormula;
import edu.cmu.cs.lti.ark.util.optimization.LDouble.IdentityElement;
import edu.cmu.cs.lti.ark.util.optimization.LogFormula.Op;
import edu.cmu.cs.lti.ark.util.Indexer;
import edu.cmu.cs.lti.ark.util.Semirings;
import edu.cmu.cs.lti.ark.util.Subroutine;
import gnu.trove.THashSet;

/**
 * DAG class with a general-purpose implementation of Dijkstra's algorithm 
 * wherein multiple edges in a path may be considered at once in scoring 
 * (i.e. allowing for an arbitrarily large Markov order).
 * <br/><br/>
 * Note: other type parameters used in methods of this class include:
 * <dl><dt>{@code <L>}</dt><dd>Type of the label associated with each edge, if any</dd>
 * <dt>{@code <V>}</dt><dd>Semiring value associated with a path</dd>
 * <dt>{@code <I>}</dt><dd>Indexer, i.e. a path instance where nodes are indicated with integers</dd>
 * <dt>{@code <T>},{@code <U>}</dt><dd>Type of {@link Path} elements: if the path is labeled, this will 
 * be a (node, label) pair, i.e. {@link LabeledPath}{@code<N,L> extends }{@link Path}{@code<Pair<N,L>>}; 
 * otherwise it will be a node, i.e. {@link Path}{@code<N>}</dd></dl>
 * 
 * @author Nathan Schneider (nschneid)
 * 
 * @param <N> Graph node type
 * @param <W> Edge weight type
 */
public class WeightedRootedHyperDAG<N extends RootedDAGNode<N> & HasWeightedHyperEdges<N,W>,W> extends RootedDAG<N> {
	/** @deprecated */
	protected CounterMap<N,N> _weights;
	
	public void initLogger(FileWriter f) {
		_logger = new DebugLogger(f);
	}
	
	protected class DebugLogger {
		FileWriter _f;
		public DebugLogger(FileWriter f) {
			_f = f;
		}
		public void log(int[] ii, int[] jj, int chartSize) {
			writeString(String.format("%s\t%s\t%d\n", Arrays.toString(ii), Arrays.toString(jj), chartSize));
		}
		public void close() {
			try {
				_f.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		public void popped(Object poppedItem, int newAgendaSize) {
			writeString(String.format("%s\t%d\n", poppedItem.toString(), newAgendaSize));
		}
		public void height(int nodeHeight) {
			writeString(String.format("%d", nodeHeight));
		}
		protected void writeString(String s) {
			try {
				_f.write(s);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	protected DebugLogger _logger;
	
	/** @deprecated For testing purposes only; normally edge weights should be accessible via the source node */
	public WeightedRootedHyperDAG(N root, CounterMap<N,N> weights) {
		super(root);
		_weights = weights;
	}
	public WeightedRootedHyperDAG(N root) {
		super(root);
	}
	
	/**
	 * Simple version:
	 * arrive(j) (+)= start(j)
	 * arrive(j) (+)= arrive(H) (*) arc(H, j)
	 *   goal(j) (+)= arrive(j) (*) end(j)
	 * 

	 * @param semr
	 * @return
	 * @throws GraphException 
	 */
	public <V> Object computePath(Semirings.Semiring<V> semr) throws GraphException {
		return computeUnlabeledHyperpath(semr,1);
	}
	
	public <V> V computeUnlabeledHyperpath(Semirings.Semiring<V> semr, int order) throws GraphException {
		return computeUnlabeledHyperpath(semr,order,null);
	}
	
	public <V> V computeUnlabeledHyperpath(int order, Subroutine edgeOperation) throws GraphException {
		return (V)computeULHyperpath(null,order,null,edgeOperation);
	}
	
	public <V> V computeUnlabeledHyperpath(Semirings.Semiring<V> semr, int order, Path<N> relevantPath) throws GraphException {
		return computeHyperpath(semr,order,relevantPath,null);
	}
	public <V> V computeLabeledHyperpath(Semirings.Semiring<V> semr, int order) throws GraphException {
		return computeLabeledHyperpath(semr,order,null);
	}
	public <V,L> V computeLabeledHyperpath(Semirings.Semiring<V> semr, int order, LabeledPath<N,L> relevantPath) throws GraphException {
		return computeHyperpath(semr,order,relevantPath,null);
	}
	
	public <V> V computeLabeledHyperpath(int order, Subroutine edgeOperation) throws GraphException {
		return (V)computeLHyperpath(null,order,null,edgeOperation);
	}
	
	/** Initialize the chart and agenda for decoding in the unlabeled case. */
	public <V> Pair<Map<IndexedPath<N>,V>, SortedSet<IndexedPath<N>>> initUnlabeledChartAndAgenda(Semirings.Semiring<V> semr, int order) {
		Map<IndexedPath<N>,V> chart = null;
		if (semr!=null)
			chart = new FactoryDefaultMap<IndexedPath<N>,V>(new SemiringZeroFactory<V>(semr));
		
		int[] ii = new int[order+1];
		for (int k=0; k<order; k++) {
			ii[k] = 0;	// START symbol
		}
		ii[order] = 1;	// index of head node of the path
		
		
		IndexedPath<N> initialItem = new IndexedPath<N>(ArrayUtil.toArrayList(ii),true);
		
		// initialize chart
		if (chart!=null)
			chart.put(initialItem, semr.oone());
		
		SortedSet<IndexedPath<N>> agenda = new TreeSet<IndexedPath<N>>(new Comparator<IndexedPath<N>>() {
			@Override
			public int compare(IndexedPath<N> o1, IndexedPath<N> o2) {	// to determine priority, sort by the last element of the int[], 
				// i.e. the index of the node to arrive at; ensures nodes are arrived at from left to right, starting with the root
				// For comparing lists of the form [a, b, c], where c is the arrival node, primary sort order is ascending by c, secondary sort order 
				// is DESCENDING by b, ternary sort order is descending by a, etc.
				assert o1.size()==o2.size();
				boolean latest = true;
				for (int i=o1.size()-1; i>=0; i--) {
					if (latest) {
						if (o1.get(i)<o2.get(i)) return -1;
						if (o1.get(i)>o2.get(i)) return 1;
					}
					else {
						if (o1.get(i)<o2.get(i)) return 1;
						if (o1.get(i)>o2.get(i)) return -1;
					}
				}
				return 0;
			}
		});
		
		agenda.add(initialItem);
		
		return new Pair<Map<IndexedPath<N>,V>, SortedSet<IndexedPath<N>>>(chart, agenda);
	}
	
	/** Initialize the chart and agenda for decoding in the unlabeled case. */
	public <V,L> Pair<Map<IndexedLabeledPath<N,L>,V>, SortedSet<IndexedLabeledPath<N,L>>> initLabeledChartAndAgenda(Semirings.Semiring<V> semr, int order, L dummy) {
		Map<IndexedLabeledPath<N,L>,V> chart = null;
		if (semr!=null)
			chart = new FactoryDefaultMap<IndexedLabeledPath<N,L>,V>(new SemiringZeroFactory<V>(semr));
		
		
		int[] ii = new int[order+1];
		List<L> lbls = new ArrayList<L>();
		for (int k=0; k<order; k++) {
			ii[k] = 0;	// START symbol
			lbls.add(null);
		}
		ii[order] = 1;	// index of head node of the path
		
		
		IndexedLabeledPath<N,L> initialItem = new IndexedLabeledPath<N,L>(ii,true,lbls,true);
		
		// initialize chart
		if (chart!=null)
			chart.put(initialItem, semr.oone());
		
		SortedSet<IndexedLabeledPath<N,L>> agenda = new TreeSet<IndexedLabeledPath<N,L>>();
		agenda.add(initialItem);
		
		return new Pair<Map<IndexedLabeledPath<N,L>,V>, SortedSet<IndexedLabeledPath<N,L>>>(chart, agenda);
	}
	
	public <V,L> V computeHyperpath(Semirings.Semiring<V> semr, int order, Path<N> path, Subroutine edgeOperation) throws GraphException {
		return computeULHyperpath(semr,order,path,edgeOperation);
	}
	public <V,L> V computeULHyperpath(Semirings.Semiring<V> semr, int order, Path<N> path, Subroutine edgeOperation) throws GraphException {
		N r = (N)_root;
		List<N> sortedNodes = RootedDAGNode.topologicalSort(r);
		sortedNodes.add(0,null);	// a START symbol
		
		if (path!=null && path.isSourceIncluded() && path.getSource()!=sortedNodes.get(1))
			throw new GraphException("path must start with the root node of the graph");
		
		return _startAgenda(false, this.initUnlabeledChartAndAgenda(semr, order), 
				sortedNodes, semr, order, path, edgeOperation);
	}
	public <V,L> V computeHyperpath(Semirings.Semiring<V> semr, int order, LabeledPath<N,L> path, Subroutine edgeOperation) throws GraphException {
		return computeLHyperpath(semr,order,path,edgeOperation);
	}
	public <V,L> V computeLHyperpath(Semirings.Semiring<V> semr, int order, LabeledPath<N,L> path, Subroutine edgeOperation) throws GraphException {
		N r = (N)_root;
		List<N> sortedNodes = RootedDAGNode.topologicalSort(r);
		sortedNodes.add(0,null);	// a START symbol
		
		if (path!=null && path.isSourceIncluded() && path.getSource()!=sortedNodes.get(1))
			throw new GraphException("path must start with the root node of the graph");
		
		return _startAgenda(true, this.initLabeledChartAndAgenda(semr, order, (path==null) ? null : path.getLabels().get(0)), 
				sortedNodes, semr, order, path, edgeOperation);
	}
	
	protected <T,U,V,I extends Path<T> & Indexer<List<Integer>,List<N>,? extends Path<U>>,L> V _startAgenda(boolean isLabeled, Pair<Map<I,V>, SortedSet<I>> chartAndAgenda, List<N> sortedNodes, 
			Semirings.Semiring<V> semr, int order, Path<U> relevantPath, Subroutine edgeOperation) {
		Map<I,V> chart = chartAndAgenda.getFirst();
		SortedSet<I> agenda = chartAndAgenda.getSecond();
		
		while (!agenda.isEmpty()) {
			I nextItem = agenda.first();
			agenda.remove(nextItem);	// pop
			if (_logger!=null)
				_logger.popped(nextItem, agenda.size());
			for (I item : arrive(isLabeled, sortedNodes, chart, semr, nextItem, relevantPath, edgeOperation)) {
				if (!agenda.contains(item)) {
					agenda.add(item);	// arrive at node jj[-1] given history jj[:-1]
				}
			}
			if (relevantPath!=null)
				relevantPath = relevantPath.getTail();
		}
		
//if (_logger!=null && relevantNodes==null)
//		_logger.close();
		
		if (semr!=null)	// sum of all histories going into the last node
			return arrivalsTo(sortedNodes, chart, semr, sortedNodes.size()-1, order);
		return null;
	}
	
	public static class SemiringZeroFactory<V> implements DefaultValueFactory<V> {
		private Semirings.Semiring<V> _semiring;
		public <W> SemiringZeroFactory(Semirings.Semiring<V> semiring) {
			_semiring = semiring;
		}
		public V newDefaultValue() { return _semiring.ozero(); }
	}
	
	/** chart lookup */
	protected static <V,T,U,N,I extends Path<T> & Indexer<List<Integer>,List<N>,? extends Path<U>>> V $(Map<I,V> chart, I path) {
		return chart.get(path);
	}
	/** chart assignment */
	protected static <V,T,U,N,I extends Path<T> & Indexer<List<Integer>,List<N>,? extends Path<U>>> V $(Map<I,V> chart, I path, V newvalue) {
		return chart.put(path, newvalue);
	}
	
	private <T,U,L> Iterable<T> _continue(boolean isLabeled, N node, List<N> sortedNodes, Path<U> relevantPath, T dummy) {
		Set<T> continuations = new THashSet<T>();
		if (isLabeled) {
			for (N c : node.getChildren()) {
				int j = sortedNodes.indexOf(c);
				
				if (relevantPath!=null /*&& i>0*/ && relevantPath.getNode(1)!=c)
					continue;
				for (L l : ((HasLabeledEdges<N,L>)node).getLabels(c)) {
					if (relevantPath!=null) {
						L rL = ((Pair<N,L>)relevantPath.get(1)).getSecond();
						if ((rL==null)!=(l==null) || !rL.equals(l))
							continue;
					}
					continuations.add((T)new Pair<Integer,L>(j,l));
				}
			}
		}
		else {
			for (N c : node.getChildren()) {
				if (relevantPath!=null /*&& i>0*/ && relevantPath.getNode(1)!=c)
					continue;
				Integer j = sortedNodes.indexOf(c);
				continuations.add((T)j);
			}
		}
		return continuations;
	}
	
	/**
	 * 
	 * With history (hyperpath):
	 * arrive(*, j) (+)= start(j)
	 * arrive(i, j) (+)= arrive(H, i) (*) arc(i, j)
	 *   goal(j) (+)= arrive(I, j) (*) end(j)
	 * 
	 * 
	 * arrive(*, *, j) (+)= start(j)
	 * arrive(ii, i, j) (+)= arrive(H, ii, i) (*) arc(i, j)
	 *   goal(j) (+)= arrive(II, I, j) (*) end(j)
	 * 
	 * etc.
	 *  
	 * @param <V> Semiring value type
	 * @param semr
	 * @param order Markov order (1 reduces to a path problem)
	 * @return 
	 * @throws GraphException
	 */
	private <V,T,U,I extends Path<T> & Indexer<List<Integer>,List<N>,? extends Path<U>>,L> List<I> arrive(boolean isLabeled, List<N> sortedNodes, Map<I,V> chart, Semirings.Semiring<V> semr, I item, 
			Path<U> relevantPath, Subroutine edgeOperation) {
		// ii: indices of nodes such that we are computing the arrival at node ii[-1] given history ii[:-1]
		List<Integer> ii = item.indices();
		int i = ii.get(ii.size()-1);
		N node = sortedNodes.get(i);
		assert (relevantPath==null || i==0 || relevantPath.getNode(0)==node);
		
		// sourceNodesSuffix = nodes indexed by ii[1:]
		//Path<U> path = ((Indexer<List<Integer>,List<N>,Path<U>>)item.getTail()).apply(sortedNodes);
		
		List<I> newItems = new ArrayList<I>();
		for (T e : _continue(isLabeled, node, sortedNodes, relevantPath, item.getEnd())) {
			I newItem = (I)item.shift(e);	// = ii[1:] + [j]
			
			Path<U> newItemFull = newItem.apply(sortedNodes);
			
			if (chart!=null) {
				// arrive(ii[1:],j) (+)= arrive(ii[:-1],ii[-1]) (*) featureScore(ii[1:],j)
				V edgeValue = (V)getEdgeValue(newItemFull, semr);
				assert edgeValue!=null;
				
				V lookup1 = $(chart,item);
				V prod = semr.times(lookup1, edgeValue);
				V lookup2 = $(chart,newItem);
				V sum = semr.plus(lookup2, prod);
				$(chart, newItem, sum);
			}
			else {	// Execute an arbitrary function on the edge
				edgeOperation.$(newItemFull);
			}
			
			newItems.add(newItem);	// add to agenda
		}
		
		return newItems;
	}
		
	protected <V,T,U,I extends Path<T> & Indexer<List<Integer>,List<N>,? extends Path<U>>> V arrivalsTo(List<N> sortedNodes, Map<I,V> subchart, Semirings.Semiring<V> semr, int iTarget, int markovOrder) {
		V v = semr.ozero();
		for (I item : subchart.keySet()) {
			if ((Integer)item.getTarget()==iTarget)
				v = semr.plus(v, subchart.get(item));
		}
		return v;
	}
	
	protected <V> Object getEdgeValue(Path<?> path, Semirings.Semiring<V> semr) {
		if (semr instanceof Semirings.PathSemiring<?,?>) {
			Set<Pair<W,? extends Path<N>>> s = new HashSet<Pair<W,? extends Path<N>>>();
			s.add(new Pair<W,Path<N>>(getWeight(path), (Path<N>)path));
			return s;
		}
		return getWeight(path);
	}
	protected W getWeight(Path<?> path) {
		N target = (N)path.getTarget();
		W result = target.getWeight(path);
		assert result!=null;
		return result;
	}
	
	private static class TestNode extends RootedDAGNode<TestNode> implements HasWeightedHyperEdges<TestNode,Double> {
		protected Map<Path<TestNode>,Double> _weights;
		
		public TestNode(String lbl) {
			this(lbl, new HashMap<Path<TestNode>,Double>());
		}
		public TestNode(String lbl, Map<Path<TestNode>,Double> incomingEdges) {
			_weights = incomingEdges;
			this.setLabelType(lbl);
		}
		
		@Override
		public Double getWeight(Path<?> path) {
			Double w = _weights.get(path);
			return w;
		}
		
		@Override
		public String toString() {
			return getLabelType();
		}
				
		public static <W> void link(TestNode a, TestNode b, Double wt) throws GraphException {
			link(a, b);
			b._weights.put(new Path<TestNode>(Arrays.asList(new TestNode[]{a,b})), wt);
		}
	}
	
	private static class TestNodeLF extends RootedDAGNode<TestNodeLF> implements HasWeightedHyperEdges<TestNodeLF,LogFormula> {
		protected Map<Path<TestNodeLF>,LogFormula> _weights;
		
		public TestNodeLF(String lbl) {
			this(lbl, new HashMap<Path<TestNodeLF>,LogFormula>());
		}
		public TestNodeLF(String lbl, Map<Path<TestNodeLF>,LogFormula> incomingEdges) {
			_weights = incomingEdges;
			this.setLabelType(lbl);
		}
		
		@Override
		public LogFormula getWeight(Path<?> path) {
			LogFormula w = _weights.get(path);
			return w;
		}
		
		@Override
		public String toString() {
			return getLabelType();
		}
				
		public static void link(TestNodeLF a, TestNodeLF b, LogFormula wt) throws GraphException {
			link(a, b);
			b._weights.put(new Path<TestNodeLF>(Arrays.asList(new TestNodeLF[]{a,b})), wt);
		}
	}

	
	public static void main(String[] args) {
		
		try {	// Unit testing for path computations with various semirings
			/*
			 *    R --[0.5]--> C --[0.6]--> D --[1.0]-->\
			 *    |\            \                        F --[1.0]-->\
			 *    | \            \-[0.2]--> E --[1.0]-->/             \
			 *    |  \                       \                        S
			 *    |   \                       \--[1.0]-->\           /|
			 *     \   \--[0.35]------------------------> B -[1.0]->/ |
			 *      \                                                 /
			 *       \---[0.15]--> A --[1.0]------------------------>/
			 *       
			 *       Semiring       Correct value
			 *       ----------------------------
			 *       Max-Times      max { .3, .1, .1, .35, .15 } = .35
			 *       Plus-Times     sum { .3, .1, .1, .35, .15 } = 1.0 (edge weights can be interpreted as probabilities)
			 *       Max-Plus       max { 3.1, 2.7, 2.7, 1.35, 1.15 } = 3.1
			 *       
			 */
			
			WeightedRootedHyperDAG<TestNode,Double> g = new WeightedRootedHyperDAG<TestNode, Double>(new TestNode("R"));
			/*try {
				g.initLogger(new FileWriter(new File("chartlog.csv")));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}*/
			g._root.parents = new ArrayList<TestNode>();	// .getParents() was returning null
			TestNode sink = new TestNode("S");
			sink.children = new ArrayList<TestNode>();	// .getChildren() was returning null
			TestNode.link(g._root, new TestNode("C"), 0.5);	// R-->C
			TestNode b = new TestNode("B", new HashMap<Path<TestNode>,Double>());
			TestNode.link(g._root, b, 0.35);	// R-->B
			TestNode.link(g._root, new TestNode("A"), 0.15);	// R-->A
			TestNode.link(g._root.children.get(0), new TestNode("D"), 0.6);	// C-->D
			TestNode.link(g._root.children.get(0), new TestNode("E"), 0.2);	// C-->E
			TestNode f = new TestNode("F");
			TestNode.link(g._root.children.get(0).children.get(0), f, 1.0);	// D-->F
			TestNode.link(g._root.children.get(0).children.get(1), f, 1.0);	// E-->F
			TestNode.link(g._root.children.get(0).children.get(1), b, 1.0);	// E-->B
			TestNode.link(f,sink,1.0);	// F-->S
			TestNode.link(g._root.children.get(1), sink, 1.0);	// B-->S
			TestNode.link(g._root.children.get(2), sink, 1.0);	// A-->S
			
			System.out.println(g.computePath(new Semirings.MaxTimes()));
			System.out.println(g.computePath(new Semirings.PlusTimes()));
			System.out.println(g.computePath(new Semirings.MaxPlus()));
			
			System.out.println(g.computePath(new Semirings.MaxTimesPath<TestNode>(new Semirings.MaxTimes())));
			System.out.println(g.computePath(new Semirings.PlusTimesPath<TestNode>(new Semirings.PlusTimes())));
			System.out.println(g.computePath(new Semirings.MaxPlusPath<TestNode>(new Semirings.MaxPlus())));
			
			
			// Now reconstruct the above graph, but with log equivalents for weights
			/*
			 *       Semiring       Correct value (log space)    Path yielding this value
			 *       --------------------------------------------------------------------
			 *       Log-Max-Times  log(.35) = -1.05             R --> B --> S
			 *       Log-Plus-Times log(1.0) = 0.0               all paths
			 *       Log-Max-Plus   log(3.1) = 1.13              R --> C --> D --> F --> S
			 */
			
			g = new WeightedRootedHyperDAG<TestNode, Double>(new TestNode("R"));
			g._root.parents = new ArrayList<TestNode>();	// .getParents() was returning null
			sink = new TestNode("S");
			sink.children = new ArrayList<TestNode>();	// .getChildren() was returning null
			TestNode.link(g._root, new TestNode("C", new HashMap<Path<TestNode>,Double>()), Math.log(0.5));	// R-->C
			b = new TestNode("B", new HashMap<Path<TestNode>,Double>());
			TestNode.link(g._root, b, Math.log(0.35));	// R-->B
			TestNode.link(g._root, new TestNode("A", new HashMap<Path<TestNode>,Double>()), Math.log(0.15));	// R-->A
			TestNode.link(g._root.children.get(0), new TestNode("D", new HashMap<Path<TestNode>,Double>()), Math.log(0.6));	// C-->D
			TestNode.link(g._root.children.get(0), new TestNode("E", new HashMap<Path<TestNode>,Double>()), Math.log(0.2));	// C-->E
			f = new TestNode("F");
			TestNode.link(g._root.children.get(0).children.get(0), f, 0.0);	// D-->F
			TestNode.link(g._root.children.get(0).children.get(1), f, 0.0);	// E-->F
			TestNode.link(g._root.children.get(0).children.get(1), b, 0.0);	// E-->B
			TestNode.link(f,sink, 0.0);	// F-->S
			TestNode.link(g._root.children.get(1), sink, 0.0);	// B-->S
			TestNode.link(g._root.children.get(2), sink, 0.0);	// A-->S
			
			System.out.println(g.computePath(new Semirings.LogMaxTimes()));
			System.out.println(g.computePath(new Semirings.LogPlusTimes()));
			System.out.println(g.computePath(new Semirings.LogMaxPlus()));
			
			System.out.println(g.computePath(new Semirings.MaxTimesPath<TestNode>(new Semirings.LogMaxTimes())));
			System.out.println(g.computePath(new Semirings.PlusTimesPath<TestNode>(new Semirings.LogPlusTimes())));
			System.out.println(g.computePath(new Semirings.MaxPlusPath<TestNode>(new Semirings.LogMaxPlus())));
			
			// LogFormula version
			
			WeightedRootedHyperDAG<TestNodeLF,LogFormula> g2 = new WeightedRootedHyperDAG<TestNodeLF, LogFormula>(new TestNodeLF("R"));
			g2._root.parents = new ArrayList<TestNodeLF>();	// .getParents() was returning null
			TestNodeLF sink2 = new TestNodeLF("S");
			sink2.children = new ArrayList<TestNodeLF>();	// .getChildren() was returning null
			TestNodeLF.link(g2._root, new TestNodeLF("C", new HashMap<Path<TestNodeLF>,LogFormula>()), new LogFormula(LDouble.convertToLogDomain(0.5)));	// R-->C
			TestNodeLF b2 = new TestNodeLF("B", new HashMap<Path<TestNodeLF>,LogFormula>());
			TestNodeLF.link(g2._root, b2, new LogFormula(LDouble.convertToLogDomain(0.35)));	// R-->B
			TestNodeLF.link(g2._root, new TestNodeLF("A", new HashMap<Path<TestNodeLF>,LogFormula>()), new LogFormula(LDouble.convertToLogDomain(0.15)));	// R-->A
			TestNodeLF.link(g2._root.children.get(0), new TestNodeLF("D", new HashMap<Path<TestNodeLF>,LogFormula>()), new LogFormula(LDouble.convertToLogDomain(0.6)));	// C-->D
			TestNodeLF.link(g2._root.children.get(0), new TestNodeLF("E", new HashMap<Path<TestNodeLF>,LogFormula>()), new LogFormula(LDouble.convertToLogDomain(0.2)));	// C-->E
			TestNodeLF f2 = new TestNodeLF("F");
			TestNodeLF.link(g2._root.children.get(0).children.get(0), f2, new LogFormula(LDouble.convertToLogDomain(1.0)));	// D-->F
			TestNodeLF.link(g2._root.children.get(0).children.get(1), f2, new LogFormula(LDouble.convertToLogDomain(1.0)));	// E-->F
			TestNodeLF.link(g2._root.children.get(0).children.get(1), b2, new LogFormula(LDouble.convertToLogDomain(1.0)));	// E-->B
			TestNodeLF.link(f2,sink2, new LogFormula(LDouble.convertToLogDomain(1.0)));	// F-->S
			TestNodeLF.link(g2._root.children.get(1), sink2, new LogFormula(LDouble.convertToLogDomain(1.0)));	// B-->S
			TestNodeLF.link(g2._root.children.get(2), sink2, new LogFormula(LDouble.convertToLogDomain(1.0)));	// A-->S
			
			Semirings.LogFormulaBuilder lfb = new Semirings.LogFormulaBuilder() {
				@Override
				public LogFormula recruitFormula(IdentityElement ie) {
					return new LogFormula(ie);
				}
				
				@Override
				public LogFormula recruitFormula(Op op) {
					return new LogFormula(op);
				}
			};	// this is the Log-Plus-Times semiring, only encoding the computations as an arithmetic circuit
			
			LogFormula result = (LogFormula)g2.computePath(lfb);
			System.out.println(result.evaluate(null));	// should be 0
			
			Path<TestNodeLF> nodesInPath = new Path<TestNodeLF>();	// path R-->C-->D-->F-->S
			nodesInPath.add(g2._root);	// R
			nodesInPath.add(g2._root.children.get(0));	// C
			nodesInPath.add(g2._root.children.get(0).children.get(0));	// D
			nodesInPath.add(f2);	// F
			nodesInPath.add(sink2);	// S
			result = (LogFormula)g2.computeUnlabeledHyperpath(lfb, 1, nodesInPath);
			System.out.println(result.evaluate(null));	// should be log(0.3) = -1.20
			
		} catch (GraphException e1) {
			e1.printStackTrace();
		}
	}
}
