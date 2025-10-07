package sybilcentrality;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import anansi.centrality.BetweennessCentrality;
import anansi.centrality.Centrality;
import anansi.centrality.ClosenessCentrality;
import anansi.centrality.DegreeCentrality;
import anansi.centrality.EigenvectorCentrality;
import anansi.community.IgraphCommunityDetector;
import anansi.core.graph.Graph;
import anansi.experiment.ExperimentAggregator;
import anansi.experiment.ExperimentResult;
import anansi.experiment.ExperimentRunner;
import anansi.experiment.Row;
import sybilcentrality.strategy.CommunitySybilStrategy;
import sybilcentrality.strategy.OptimalDegreeSybilStrategy;
import sybilcentrality.strategy.RandomSybilStrategy;
import sybilcentrality.strategy.SybilHidingStrategy;
import sybilcentrality.RunSybilHiding;
import anansi.utils.RChartPrinter;
import anansi.utils.RChartPrinter.RChartBuilder;
import anansi.utils.Utils;

public class RunSybilHiding extends ExperimentRunner {
	
	protected static final Map<String, String> COMMUNITY_DETECTION = Map.of(
			IgraphCommunityDetector.ALG_EDGE_BETWEENNESS, "Girvan-Newman",
			IgraphCommunityDetector.ALG_LEADING_EIGEN, "Leading eigenvector",
			IgraphCommunityDetector.ALG_WALKTRAP, "Walktrap",
			IgraphCommunityDetector.ALG_INFOMAP, "Infomap",
			IgraphCommunityDetector.ALG_LOUVAIN, "Louvain",
			IgraphCommunityDetector.ALG_SPINGLASS, "Spinglass");

	public static void main(String[] args) {
		int times = args.length > 0 ? Integer.parseInt(args[0]) : 2; //20;
		int n = args.length > 1 ? Integer.parseInt(args[1]) : 200; //1000;
		int samples = args.length > 2 ? Integer.parseInt(args[2]) : 10;
		int hidingBudget = 5;
		int avgDegree = 10;
		boolean sybilRing = true; 
		
		RunSybilHiding r = new RunSybilHiding();

		r.runErdosRenyi(n, avgDegree, times, hidingBudget, samples, sybilRing);
		r.runSmallWorld(n, avgDegree, .25, times, hidingBudget, samples, sybilRing);
		r.runBarabasiAlbert(n, avgDegree, times, hidingBudget, samples, sybilRing);
		
		r.aggregateAll();
		r.printCharts();
	}
	
	@Override
	public String getDirectoryName() {
		return "sybil-basic";
	}
	
	protected List<Centrality> getCentralities(Graph g){
		return Utils.aList(new DegreeCentrality(), new ClosenessCentrality(), new BetweennessCentrality(),
				new EigenvectorCentrality(SybilHidingExperiment.EPSILON));
	}
	
	protected List<SybilHidingStrategy> getStrategies(Graph g, boolean sybilRing){
		List<SybilHidingStrategy> strats = Utils.aList(new RandomSybilStrategy(false),
				new OptimalDegreeSybilStrategy(sybilRing ? 2 : 0));
		for (String algName : COMMUNITY_DETECTION.keySet())
			strats.add(new CommunitySybilStrategy(IgraphCommunityDetector.getAlgorithm(algName)));
		return strats;
	}

	@Override
	public void runSingle(Object... params) {
		Graph g = (Graph)params[0];
		int budget = (int)params[1];
		int samples = (int)params[2];
		boolean sybilRing = (boolean)params[3];
		new SybilHidingExperiment(getDataPath(g), g, budget, samples, getCentralities(g),
				getStrategies(g, sybilRing), sybilRing).perform();
	}

	@Override
	public List<ExperimentAggregator> getAggregators() {
		return Utils.aList(new InitialScatterAggregator(), new InitialLineAggregator());
	}
	
	private class InitialScatterAggregator extends ExperimentAggregator {

		@Override
		public String getName() {
			return "initial-scatter";
		}
		
		@Override
		protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) {
			return expand(rows, header, experimentDir).stream();
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			res.addColumn("file", r -> r.concatVals("graph", "budget", "centrality"));
			res.addColumn("strategyRead", r -> strategyRead(r.get("strategy")));
			return res;
		}
		
		protected static final String LEGEND_ORDER = "Random,Optimal degree,"
				+ COMMUNITY_DETECTION.get(IgraphCommunityDetector.ALG_EDGE_BETWEENNESS) + ","
				+ COMMUNITY_DETECTION.get(IgraphCommunityDetector.ALG_LEADING_EIGEN) + ","
				+ COMMUNITY_DETECTION.get(IgraphCommunityDetector.ALG_WALKTRAP) + ","
				+ COMMUNITY_DETECTION.get(IgraphCommunityDetector.ALG_INFOMAP) + ","
				+ COMMUNITY_DETECTION.get(IgraphCommunityDetector.ALG_LOUVAIN) + ","
				+ COMMUNITY_DETECTION.get(IgraphCommunityDetector.ALG_SPINGLASS);
		protected static final String POINT_SHAPES = "16,15," + // Standard strategies
												"21,24,25,22,23,9"; // Community detection
		
		protected RChartBuilder initialChartBuilder(ExperimentResult res) {
			return new RChartBuilder(res, "init.pdf", "rankBefore", "rankAfter", "strategyRead")
					.multipleFilesColumn("file").fileWidth(8.).fileHeight(5.).textSize(22).colorBrewer("Dark2")
					.ablineIntercept(0.).ablineSlope(1.).ablineSize(.5).ablineLinetype("dashed")
					.legendPosition("none").legendSeparate().legendSingle().legendDirection("horizontal")
					.legendOrder(LEGEND_ORDER).legendNRows(1).legendTextSize(12).legendKeySize(4);
		}
		
		@Override
		public void printCharts(ExperimentResult res) {
			RChartPrinter.printLinePlot(initialChartBuilder(res)
					.xTitle("Initial ranking").yTitle("Ranking after attack")
					.lineHide().pointShapeList(POINT_SHAPES));
		}
	}
	
	private class InitialLineAggregator extends InitialScatterAggregator {

		@Override
		public String getName() {
			return "initial-line";
		}
		
		@Override
		public void printCharts(ExperimentResult res) {
			RChartPrinter.printLinePlot(initialChartBuilder(res)
					.pointHide(true).smoothLine().xLabelsHide().yLabelsHide());
		}
	}
	
	protected String strategyRead(String name) {
		if (name.startsWith("community"))
			return COMMUNITY_DETECTION.get(name.replace("community-", ""));
		return Utils.capitalize(name).replace("-", " ");
	}

	protected ExperimentResult expand(Stream<Row> rows, List<String> header, File experimentDir) {
		ExperimentResult res = new ExperimentResult(experimentDir, header, rows);
		res.expand("graph", SybilHidingExperiment.HD_GRAPH);
		res.expand("budget", SybilHidingExperiment.HD_BUDGET);
		res.expand("connectRing", SybilHidingExperiment.HD_SYBIL_RING);
		return res;
	}
}
