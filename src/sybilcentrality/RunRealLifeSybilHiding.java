package sybilcentrality;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import anansi.core.graph.Graph;
import anansi.experiment.ExperimentAggregator;
import anansi.experiment.ExperimentResult;
import anansi.experiment.Row;
import anansi.utils.RChartPrinter;
import anansi.utils.RChartPrinter.RChartBuilder;
import anansi.utils.anet.ANETResource;
import anansi.utils.anet.GraphImporter;
import sybilcentrality.strategy.edge.EdgeHeuristic;
import sybilcentrality.strategy.edge.ROAM;
import anansi.utils.Utils;

public class RunRealLifeSybilHiding extends RunSybilHiding {

	public static void main(String[] args) {
		int samples = args.length > 2 ? Integer.parseInt(args[2]) : 10;
		int hidingBudget = 2;
		boolean sybilRing = false; 
		
		RunRealLifeSybilHiding r = new RunRealLifeSybilHiding();

		r.runSingle(getArtisHamburgWTC2001(), hidingBudget, samples, sybilRing);
		r.runSingle(getArtisBali2002(), hidingBudget, samples, sybilRing);
		r.runSingle(getArtisMadrid2004(), hidingBudget, samples, sybilRing);
		r.runSingle(getArtisNovember17Greece(), hidingBudget, samples, sybilRing);
		r.runSingle(getArtisAustralianEmbassy2004(), hidingBudget, samples, sybilRing);
		r.runSingle(getArtisChristmasEve2000(), hidingBudget, samples, sybilRing);
		
		r.aggregateAll();
		r.printCharts();
	}
	
	@Override
	public String getDirectoryName() {
		return "sybil-reallife";
	}

	protected List<EdgeHeuristic> getEdgeHeuristics(Graph g){
		return Utils.aList(new ROAM(1), new ROAM(2), new ROAM(3));
	}

	@Override
	public void runSingle(Object... params) {
		Graph g = (Graph)params[0];
		int budget = (int)params[1];
		int samples = (int)params[2];
		boolean sybilRing = (boolean)params[3];
		new RealLifeSybilHidingExperiment(getDataPath(g), g, budget, samples, getCentralities(g),
				getStrategies(g, sybilRing), sybilRing, getEdgeHeuristics(g)).perform();
	}

	@Override
	public List<ExperimentAggregator> getAggregators() {
		return Utils.aList(new RealLifeAggregator());
	}
	
	protected class RealLifeAggregator extends ExperimentAggregator {

		@Override
		public String getName() {
			return "reallife";
		}
		
		@Override
		protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) {
			ExperimentResult res = expand(rows, header, experimentDir);
			res.addIntColumn("rankDiff", r -> r.getInt("rankAfter") - r.getInt("rankBefore"));
			return keepOnlyBestCommunity(res).stream();
		}

		@Override
		protected Stream<Row> processGroup(Stream<Row> rows, File groupDir) {
			return aggregate(rows, Utils.aList("graph", "budget", "strategy", "centrality"), "rankDiff");
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			DecimalFormat df = new DecimalFormat("#.0");
			res.addColumn("file", r -> r.concatVals("graph", "budget", "centrality"));
			res.addColumn("rankDiffRead", r -> df.format(r.getDouble("rankDiffMean")));
			res.addColumn("strategyRead", r -> strategyRead(r.get("strategy")));
			return res;
		}
		
		@Override
		public void printCharts(ExperimentResult res) {
			RChartPrinter.printBarPlot(new RChartBuilder(res, "diff.pdf", "strategyRead", "rankDiffMean", "strategyRead")
					.multipleFilesColumn("file").fileWidth(7.).fileHeight(5.5).textSize(28)
					.yTitle("Evader's ranking change").xTitle("Hiding strategy").xLabelsHide().xAxisHide()
					.errbarColumn("rankDiffC95").errbarWidth(0.).errbarSize(1.)
					.unifyScale("centrality").colorBrewer("Dark2").barAlpha(.75).barBorderSize(1.)
					.legendPosition("none").legendSeparate().legendSingle().legendNRows(1)
					.legendTextSize(12).legendKeySize(4));
		}
	}

	@Override
	protected String strategyRead(String name) {
		return super.strategyRead(name);
	}
	
	protected ExperimentResult keepOnlyBestCommunity(ExperimentResult res) {
		Set<Row> bestCommunityRows = res.groupByKey("evader", "centrality").values().stream()
				.map(rs -> Utils.argmax(
						rs.stream().filter(r -> r.get("strategy").startsWith("community-")),
						r -> r.getInt("rankAfter")))
				.collect(Collectors.toSet());
		res.filter(r -> !r.get("strategy").startsWith("community-") || bestCommunityRows.contains(r));			
		res.stream().filter(r -> r.get("strategy").startsWith("community-"))
				.forEach(r -> r.set("strategy", "best-community"));
		return res;
	}


	public static Graph getArtisAustralianEmbassy2004(){
		return GraphImporter.importGraph(new ANETResource("terrorist/artis/artis-australian-embassy.anet"));
	}
	
	public static Graph getArtisBali2002(){
		return GraphImporter.importGraph(new ANETResource("terrorist/artis/artis-bali-2002.anet"));
	}
	
	public static Graph getArtisChristmasEve2000(){
		return GraphImporter.importGraph(new ANETResource("terrorist/artis/artis-christmas-eve.anet"));
	}
	
	public static Graph getArtisHamburgWTC2001(){
		return GraphImporter.importGraph(new ANETResource("terrorist/artis/artis-hamburg-wtc.anet"));
	}
	
	public static Graph getArtisMadrid2004(){
		return GraphImporter.importGraph(new ANETResource("terrorist/artis/artis-madrid-train.anet"));
	}
	
	public static Graph getArtisNovember17Greece(){
		return GraphImporter.importGraph(new ANETResource("terrorist/artis/artis-november-17-greece.anet"));
	}
}
