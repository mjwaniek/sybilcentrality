package sybilcentrality.strategy.sybil;

import java.util.List;

import anansi.core.Coalition;
import anansi.core.graph.Graph;

public abstract class SybilHidingStrategy {
	
	public abstract String getName();
	
	public abstract boolean connectSybils(Graph g, Coalition sybils, Coalition alters);
	
	public void formSybilRing(Graph g, Coalition sybils) {
		List<Integer> sybilList = sybils.asList();
		for (int i = 0; i < sybilList.size(); ++i)
			g.addEdge(sybilList.get(i), sybilList.get((i + 1) % sybilList.size()));
	}
}
