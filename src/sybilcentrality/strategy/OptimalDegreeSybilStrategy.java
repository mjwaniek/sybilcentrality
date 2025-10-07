package sybilcentrality.strategy;

import java.util.Comparator;
import java.util.List;

import anansi.core.Coalition;
import anansi.core.graph.Graph;

public class OptimalDegreeSybilStrategy extends SybilHidingStrategy {

	private int interSybilDegree;
	
	public OptimalDegreeSybilStrategy(int interSybilDegree) {
		this.interSybilDegree = interSybilDegree;
	}
	
	public OptimalDegreeSybilStrategy() {
		this(0);
	}
	
	@Override
	public String getName() {
		return "optimal-degree";
	}

	@Override
	public boolean connectSybils(Graph g, Coalition sybils, Coalition alters) {
		Graph gg = new Graph(g);
		gg.startRecordingHistory();
		Integer bestKappa = null; //The final degree of the sybils
		Long bestRanking = null;
		int minKappa = (int)Math.ceil((double)alters.size()/sybils.size()) + interSybilDegree;
		for (int kappa = alters.size() + interSybilDegree; kappa >= minKappa; --kappa){
			connectSybilDegree(gg, sybils, alters, kappa);
			int fkappa = kappa;
			long rank = gg.nodesStream().filter(i -> !sybils.contains(i) && gg.getDegree(i) > fkappa).count();	
			if (bestRanking == null || rank > bestRanking) {
				bestKappa = kappa;
				bestRanking = rank;
			}
			gg.resetGraph();
		}
		if (bestKappa != null)
			connectSybilDegree(g, sybils, alters, bestKappa);
		return true;
	}
	
	private void connectSybilDegree(Graph g, Coalition sybilsC, Coalition altersC, int kappa) {
		List<Integer> sybils = sybilsC.asList();
		int sybilInd = 0;
		List<Integer> alters = altersC.asList();
		alters.sort(Comparator.comparingInt(i -> -g.getDegree(i)));
		int extraEdges = (kappa - interSybilDegree) * sybils.size() - alters.size();
		for (int alter : alters){
			g.addEdge(alter, sybils.get(sybilInd));
			sybilInd = (sybilInd + 1) % sybils.size();
			int diff = kappa + 1 - g.getDegree(alter);
			if (diff > 0 && diff < sybils.size() && diff <= extraEdges)
				for (int i = 0; i < diff; ++i) {
					g.addEdge(alter, sybils.get(sybilInd));
					sybilInd = (sybilInd + 1) % sybils.size();
					--extraEdges;
				}
		}
	}
}
