package sybilcentrality.strategy;

import java.util.List;

import anansi.community.CommunityDetectionAlgorithm;
import anansi.community.CommunityStructure;
import anansi.core.Coalition;
import anansi.core.graph.Graph;
import anansi.core.graph.LWGraph;

public class CommunitySybilStrategy extends SybilHidingStrategy {

	private CommunityDetectionAlgorithm commDetectAlg;
	
	public CommunitySybilStrategy(CommunityDetectionAlgorithm commDetectAlg) {
		this.commDetectAlg = commDetectAlg;
	}

	@Override
	public String getName() {
		return "community-" + commDetectAlg.getName();
	}

	@Override
	public boolean connectSybils(Graph g, Coalition sybils, Coalition alters) {
		LWGraph<Integer,Void> ng = g.getInducedGraph(alters);
		CommunityStructure cs = commDetectAlg.detectCommunities(ng, Math.min(sybils.size(), alters.size()));
		if (cs == null)
			return false;
		List<Integer> sybilList = sybils.asList();
		int sybilIndex = 0;
		for (Coalition c : cs) {
			for (int alter : c)
				g.addEdge(sybilList.get(sybilIndex), ng.l(alter));
			sybilIndex = (sybilIndex + 1) % sybilList.size();
		}
		return true;
	}

}
