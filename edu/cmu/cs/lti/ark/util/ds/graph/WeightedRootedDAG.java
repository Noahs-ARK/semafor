/*******************************************************************************
 * Copyright (c) 2011 Dipanjan Das 
 * Language Technologies Institute, 
 * Carnegie Mellon University, 
 * All Rights Reserved.
 * 
 * WeightedRootedDAG.java is part of SEMAFOR 2.0.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

//import edu.cmu.cs.lti.aqmar.morph.FeatureModels.ModelException;
import edu.cmu.cs.lti.ark.util.ds.map.CounterMap;
import edu.cmu.cs.lti.ark.util.ds.Pair;
import edu.cmu.cs.lti.ark.util.ds.Range0Based;
import edu.cmu.cs.lti.ark.util.Semirings;
import gnu.trove.THashMap;
import gnu.trove.THashSet;

public class WeightedRootedDAG<R extends RootedDAGNode<R> & HasWeightedEdges<R,W>,W> extends RootedDAG<R> {
	/** @deprecated */
	protected CounterMap<R,R> _weights;
	/** @deprecated For testing purposes only; normally edge weights should be accessible via the source node */
	public WeightedRootedDAG(R root, CounterMap<R,R> weights) {
		super(root);
		_weights = weights;
	}
	public WeightedRootedDAG(R root) {
		super(root);
	}
	
	/**
	 * Simple version:
	 * arrive(j) (+)= start(j)
	 * arrive(j) (+)= arrive(I) (*) arc(I, j)
	 *   goal(j) (+)= arrive(j) (*) end(j)
	 * 
	 * With history:
	 * arrive(*, j) (+)= start(j)
	 * arrive(i, j) (+)= arrive(H, i) (*) arc(i, j)
	 *   goal(i, j) (+)= arrive(i, j) (*) end(j)
	 * 
	 * @param semr
	 * @return
	 * @throws GraphException 
	 */
	@SuppressWarnings("unchecked")
	public <V> Object computePath(Semirings.Semiring<V> semr) throws GraphException {
		R r = (R)_root;
		List<R> order = RootedDAGNode.topologicalSort(r);
		V[] vals = (V[])new Object[order.size()];
		vals[0] = semr.oone();	// source
		for (int i=1; i<vals.length; i++)
			vals[i] = semr.ozero();	// initialize
		arrive(order, vals, semr, 0);
		
		return vals[vals.length-1];	// sink
	}
	public <V> void arrive(List<R> order, V[] vals, Semirings.Semiring<V> semr, int i) {
		R node = order.get(i);
		List<R> children = node.getChildren();
		for (R c : children) {
			int j = order.indexOf(c);
			vals[j] = semr.plus(vals[j], semr.times(vals[i], getEdgeValue(node,c,semr)));
			arrive(order, vals, semr, j);	// TODO: should this be moved into a separate, subsequent for loop?
		}
	}
	private <V> V getEdgeValue(R source, R target, Semirings.Semiring<V> semr) {
		if (semr instanceof Semirings.PathSemiring<?,?>) {
			List<R> l = new ArrayList<R>();
			l.add(target);
			return (V)(new Pair<W,List<R>>(getWeight(source,target), l));
		}
		return (V)getWeight(source,target);
	}
	private W getWeight(R source, R target) {
		//return _weights.getCount(source, target);
		return source.getWeight(target);
	}
	/*	// TODO
	public W getBestPathScore() throws GraphException {
		return (W)computePath(new Semirings.MaxPlus());
	}
	
	public W getSumOfPaths() throws GraphException {
		return (W)computePath(new Semirings.PlusTimes());
	}*/
	
	
	public static void main(String[] args) {
		String sentenceS = "foxes are";
		List<String> sentence = Arrays.asList(sentenceS.split(" "));
		List<Set<Range0Based>> morphemeSpans = new ArrayList<Set<Range0Based>>();
		morphemeSpans.add(new THashSet<Range0Based>());
		morphemeSpans.add(new THashSet<Range0Based>());
		morphemeSpans.add(new THashSet<Range0Based>());
		morphemeSpans.add(new THashSet<Range0Based>());
		
		// first word
		morphemeSpans.get(0).add(new Range0Based(0,3,false));
		morphemeSpans.get(0).add(new Range0Based(0,1,false));
		morphemeSpans.get(0).add(new Range0Based(1,3,false));
		morphemeSpans.get(0).add(new Range0Based(1,5,false));
		morphemeSpans.get(0).add(new Range0Based(3,5,false));
		
		// second word ...
		morphemeSpans.get(1).add(new Range0Based(0,3,false));
		
		/*
		morphemeSpans.get(2).add(new Range0Based(0,4,false));
		morphemeSpans.get(2).add(new Range0Based(3,7,false));
		morphemeSpans.get(2).add(new Range0Based(4,7,false));
		morphemeSpans.get(2).add(new Range0Based(0,3,false));
		
		morphemeSpans.get(3).add(new Range0Based(0,5,false));
		morphemeSpans.get(3).add(new Range0Based(5,7,false));
		morphemeSpans.get(3).add(new Range0Based(0,5,false));	// Duplicate range--should not result in an extra node
		*/
		
		/*try {
			MorphLatticeNode lat = new MorphLatticeNode(sentence, morphemeSpans);
			System.out.println(lat.toString(true));
			
			THashMap<String,MorphLatticeNode> allNodes = new THashMap<String,MorphLatticeNode>();
			for (MorphLatticeNode n : lat.getDescendants(true))
				allNodes.put(n.getPosition(), n);
			MorphLatticeNode n00 = allNodes.get("0,0");
			MorphLatticeNode n01 = allNodes.get("0,1");
			MorphLatticeNode n03 = allNodes.get("0,3");
			MorphLatticeNode n10 = allNodes.get("1,0");
			MorphLatticeNode n20 = allNodes.get("2,0");
			
			CounterMap<MorphLatticeNode,MorphLatticeNode> weights = new CounterMap<MorphLatticeNode,MorphLatticeNode>();
			weights.get(n00).put(n01, 0.1);
			weights.get(n00).put(n03, 0.2);
			weights.get(n01).put(n03, 0.3);
			weights.get(n01).put(n10, 0.1);
			weights.get(n03).put(n10, 0.1);
			weights.get(n10).put(n20, 1.0);
			
			WeightedRootedDAG<MorphLatticeNode> wtdlat = new WeightedRootedDAG<MorphLatticeNode>(lat, weights);
			System.out.println(wtdlat.getBestPathScore());
			System.out.println(wtdlat.getSumOfPaths());
			System.out.println(wtdlat.computePath(new Semirings.MaxTimes()));
		} catch (GraphException e) {
			e.printStackTrace();
		} catch (ModelException e) {
			e.printStackTrace();
		}*/
	}
}
