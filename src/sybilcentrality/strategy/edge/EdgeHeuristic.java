package sybilcentrality.strategy.edge;

import anansi.core.graph.Graph;

/**
 * Heuristic algorithm hiding evader from centrality measures.
 * 
 * @author Marcin Waniek
 */
public abstract class EdgeHeuristic {
	
	public abstract Double decreaseCentrality(Graph g, Integer source);
	public abstract Double getMaxCost();
	public abstract String getName();
}
