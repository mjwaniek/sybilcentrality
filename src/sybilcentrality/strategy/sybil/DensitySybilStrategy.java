package sybilcentrality.strategy.sybil;

import java.util.List;
import java.util.stream.Collectors;

import anansi.core.Coalition;
import anansi.core.Edge;
import anansi.core.graph.Graph;
import anansi.utils.Utils;

public class DensitySybilStrategy extends SybilHidingStrategy {

	private int sybilDensity;
	private int alterDensity;
	
	public DensitySybilStrategy(int sybilDensity, int alterDensity) {
		this.sybilDensity = sybilDensity;
		this.alterDensity = alterDensity;
	}

	@Override
	public String getName() {
		return "density-" + sybilDensity + "-" + alterDensity;
	}

	@Override
	public boolean connectSybils(Graph g, Coalition sybils, Coalition alters) {
		List<Edge> potentialSybilEdges = Utils.aList();
		for (int i : sybils)
			for (int j : sybils)
				if (i < j)
					potentialSybilEdges.add(g.e(i, j));
		int sybilEdges = 0;
		while ((sybilEdges++ * 100 * 2 < sybilDensity * sybils.size() * (sybils.size() - 1))
				&& !potentialSybilEdges.isEmpty())
			g.addEdge(Utils.removeRandom(potentialSybilEdges));
		List<Edge> potentialAlterEdges = Utils.aList();
		for (int a : alters) {
			List<Edge> portion = sybils.stream().mapToObj(i -> g.e(a, i)).collect(Collectors.toList());
			g.addEdge(Utils.removeRandom(portion));
			potentialAlterEdges.addAll(portion);
		}
		int alterEdges = alters.size();
		while ((alterEdges++ * 100 * 2 < alterDensity * sybils.size() * (sybils.size() - 1))
				&& !potentialAlterEdges.isEmpty())
			g.addEdge(Utils.removeRandom(potentialAlterEdges));
		return true;
	}

}
