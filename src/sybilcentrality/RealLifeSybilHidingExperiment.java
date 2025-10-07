package sybilcentrality;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import anansi.centrality.Centrality;
import anansi.core.Coalition;
import anansi.core.Ranking;
import anansi.core.graph.Graph;
import anansi.experiment.ExperimentResult;
import sybilcentrality.strategy.EdgeRewiringStrategy;
import sybilcentrality.strategy.SybilHidingStrategy;

public class RealLifeSybilHidingExperiment extends SybilHidingExperiment {
	
	protected List<EdgeRewiringStrategy> edgeHeuristics;
	
	public RealLifeSybilHidingExperiment(String resultsDirPath, Graph g, int hidingBudget, int samples,
			List<Centrality> centralities, List<SybilHidingStrategy> strategies, boolean sybilRing,
			List<EdgeRewiringStrategy> edgeHeuristics) {
		super(resultsDirPath, g, hidingBudget, samples, centralities, strategies, sybilRing);
		this.edgeHeuristics = edgeHeuristics;
	}
	
	@Override
	public String getName() {
		return "reallifesybil-" + g.getName();
	}

	@Override
	protected void perform(ExperimentResult res) {
		Map<Centrality, Ranking<Integer>> beforeRankings = centralities.stream().collect(Collectors.toMap(
				c -> c, c -> c.getRanking(g)));
		for (Integer evader : selectPotentialEvaders(g).getRandom(samples)){
			performSybilHiding(evader, res, beforeRankings);
			for (EdgeRewiringStrategy h : edgeHeuristics){
				g.startRecordingHistory();
				for (int step = 1; step <= hidingBudget; ++step)
					h.decreaseCentrality(g, evader);
				for (Centrality c: centralities){
					Ranking<Integer> afterRank = c.getRanking(g);
					res.addRow(evader, h.getName(), c.getName(), 
							beforeRankings.get(c).getExAequoPosition(evader, EPSILON),
							afterRank.getExAequoPosition(evader, EPSILON));
				}
				g.resetGraph();
			}
		}
	}

	protected Coalition selectPotentialEvaders(Graph g) {
		return g.nodesStream().boxed().sorted((i, j) -> Integer.compare(g.getDegree(j), g.getDegree(i)))
				.filter(i -> g.getDegree(i) >= EVADER_MIN_DEGREE).limit(samples).collect(Coalition.getCollector());
	}
}
