package sybilcentrality.strategy.edge;

import java.util.List;

import anansi.core.Ranking;
import anansi.core.graph.Graph;

/**
 * Hiding evader from centrality measures by cutting off one of their neighbors and attaching them to other neighbors
 * 
 * @author Marcin Waniek
 */
public class ROAM extends EdgeHeuristic {

	protected int edgesToAdd;
	protected boolean cutMax;
	protected boolean connectMax;

	public ROAM(int edgesToAdd){
		this.edgesToAdd = edgesToAdd;
		this.cutMax = true;
		this.connectMax = false;
	}

	public ROAM(int edgesToAdd, boolean cutMax, boolean connectMax){
		this.edgesToAdd = edgesToAdd;
		this.cutMax = cutMax;
		this.connectMax = connectMax;
	}

	@Override
	public Double decreaseCentrality(Graph g, Integer source) {
		if (g.getDegree(source) <= 1)
			return 0.;
		
		List<Integer> toRem = new Ranking<Integer>(g.getNeighs(source), v -> remScore(g, v)).getList();
		for (int i = 0; i < toRem.size(); ++i){
			Integer v0 = toRem.get(i);
			List<Integer> toAdd = genToAdd(v0, source, g);
			if (toAdd.size() >= edgesToAdd)
				return perform(source, v0, g, toAdd);
		}
		int v0 = toRem.get(0);
		return perform(source, v0, g, genToAdd(v0, source, g));
	}
	
	private List<Integer> genToAdd(Integer v0, Integer source, Graph g){
		List<Integer> toAdd = new Ranking<Integer>(g.getNeighs(source), v -> addScore(g, v)).getList();
		toAdd.remove(v0);
		toAdd.removeAll(g.getNeighs(v0).getNodes());
		return toAdd;
	}
	
	private double perform(Integer source, Integer v0, Graph g, List<Integer> toAdd) {
		g.removeEdge(source, v0);
		g.removeEdge(v0, source);
		int added = 0;
		for (int j = 0; j < Math.min(edgesToAdd, toAdd.size()); ++j){
			Integer v1 = toAdd.remove(0);
			g.addEdge(v1, v0);
			g.addEdge(v0, v1);
			++added;
		}
		return 1. + added;
	}
	
	protected double remScore(Graph g, int v){
		if (cutMax)
			return (double)g.getDegree(v);
		else
			return -(double)g.getDegree(v);
	}
	
	protected double addScore(Graph g, int v){
		if (connectMax)
			return (double)g.getDegree(v);
		else
			return -(double)g.getDegree(v);
	}

	@Override
	public String getName() {
		String res;
//		res = "ROAM-";
//		if (cutMax)
//			res += "max";
//		else
//			res += "min";
//		res += "-";
//		if (connectMax)
//			res += "max";
//		else
//			res += "min";
//		res += "(" + (edgesToAdd+1) + ")";
		res = "ROAM(" + (edgesToAdd + 1) + ")";
		return res;
	}

	@Override
	public Double getMaxCost() {
		return edgesToAdd + 1.;
	}
}
