package sybilcentrality.strategy;

import anansi.core.Coalition;
import anansi.core.graph.Graph;

public class RandomSybilStrategy extends SybilHidingStrategy {

	private boolean interSybilConnections;
		
	public RandomSybilStrategy(boolean interSybilConnections) {
		this.interSybilConnections = interSybilConnections;
	}

	@Override
	public String getName() {
		return "random";
	}

	@Override
	public boolean connectSybils(Graph g, Coalition sybils, Coalition alters) {
		for (int alter : alters)
			g.addEdge(alter, sybils.getRandom());
		if (interSybilConnections)
			for (int sybil : sybils) {
				Coalition otherSybils = Coalition.diff(sybils, g.getNeighs(sybil));
				otherSybils.remove(sybil);
				otherSybils.getRandom(3).forEach(other -> g.addEdge(sybil, other));
			}
		return true;
	}
}
