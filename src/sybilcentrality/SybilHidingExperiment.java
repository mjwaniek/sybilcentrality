package sybilcentrality;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import anansi.centrality.Centrality;
import anansi.core.Coalition;
import anansi.core.Edge;
import anansi.core.Ranking;
import anansi.core.graph.Graph;
import anansi.core.graph.LWGraph;
import anansi.experiment.Experiment;
import anansi.experiment.ExperimentResult;
import anansi.utils.Utils;
import sybilcentrality.strategy.sybil.SybilHidingStrategy;

public class SybilHidingExperiment extends Experiment {

	public static final int EVADER_MIN_DEGREE = 5;
	public static final double EPSILON = .000001;
	
	public static final int HD_GRAPH = 1;
	public static final int HD_BUDGET = 2;
	public static final int HD_SYBIL_RING = 3;

	protected Graph g;
	protected Integer hidingBudget;
	protected Integer samples;
	protected List<Centrality> centralities;
	protected List<SybilHidingStrategy> strategies;
	protected Boolean sybilRing;

	public SybilHidingExperiment(String resultsDirPath, Graph g, int hidingBudget, int samples,
			List<Centrality> centralities, List<SybilHidingStrategy> strategies, boolean sybilRing) {
		super(resultsDirPath);
		this.g = g;
		this.hidingBudget = hidingBudget;
		this.samples = samples;
		this.centralities = centralities;
		this.strategies = strategies;
		this.sybilRing = sybilRing;
	}
	
	@Override
	public String getName() {
		return "sybil-" + g.getName();
	}
	
	@Override
	protected List<String> getHeader() {
		return Utils.aList(getName(), g.getName(), hidingBudget.toString(), sybilRing.toString());
	}

	@Override
	protected List<String> getColumnNames() {
		return Utils.aList("evader", "strategy", "centrality", "rankBefore", "rankAfter");
	}

	@Override
	protected void perform(ExperimentResult res) {
		Map<Centrality, Ranking<Integer>> beforeRankings = centralities.stream().collect(Collectors.toMap(
				c -> c, c -> c.getRanking(g)));
		for (Integer evader : selectPotentialEvaders(g).getRandom(samples))
			performSybilHiding(evader, res, beforeRankings);
	}

	protected Coalition selectPotentialEvaders(Graph g) {
		return g.nodesStream().boxed().filter(i -> g.getDegree(i) >= EVADER_MIN_DEGREE).collect(Coalition.getCollector());
	}
	
	protected void performSybilHiding(int evader, ExperimentResult res, Map<Centrality, Ranking<Integer>> beforeRankings) {
		LWGraph<Integer, Void> lwg = removeEvaderAddBots(evader);
		Coalition sybils = lwg.getLabels().stream().filter(l -> l < 0).map(l -> lwg.findNode(l)).collect(Coalition.getCollector());
		Coalition alters = g.getNeighsStream(evader).map(i -> lwg.findNode(i)).boxed().collect(Coalition.getCollector());
		lwg.startRecordingHistory();
		for (SybilHidingStrategy strat: strategies){
			if (strat.connectSybils(lwg, sybils, alters)) {
				if (sybilRing)
					strat.formSybilRing(lwg, sybils);
				if (!validSolution(lwg, sybils, alters))
					System.err.println("Invalid solution for " + strat.getName());
				for (Centrality c: centralities){
					Ranking<Integer> afterRank = c.getRanking(lwg);
					res.addRow(evader, strat.getName(), c.getName(),
							beforeRankings.get(c).getExAequoPosition(evader, EPSILON),
							sybils.stream().map(bot -> afterRank.getExAequoPosition(bot, EPSILON)).min().getAsInt());
				}
			}
			lwg.resetGraph();
		}
	}
	
	protected LWGraph<Integer, Void> removeEvaderAddBots(int evader){
		List<Integer> labels = g.nodesStream().filter(i -> i != evader).boxed().collect(Collectors.toList());
		IntStream.rangeClosed(1, hidingBudget).forEach(i -> labels.add(-i));
		LWGraph<Integer, Void> res = new LWGraph<Integer, Void>(g.getName(), labels);
		for (Edge e: g.edges())
			if (!e.contains(evader))
				res.addLWEdge(e.i(), e.j());
		return res;		
	}
	
	protected boolean validSolution(Graph g, Coalition sybils, Coalition alters) {
		for (int alter : alters)
			if (g.getNeighs(alter).inplaceIntersect(sybils).findAny().isEmpty())
				return false;
		return true;
	}
}
