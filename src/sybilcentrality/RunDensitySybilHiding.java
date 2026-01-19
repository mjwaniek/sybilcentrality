package sybilcentrality;

import java.io.File;
import java.text.DecimalFormat;
import java.util.List;
import java.util.stream.Stream;

import anansi.core.graph.Graph;
import anansi.experiment.ExperimentAggregator;
import anansi.experiment.ExperimentResult;
import anansi.experiment.Row;
import anansi.utils.RChartPrinter;
import anansi.utils.Utils;
import anansi.utils.RChartPrinter.RChartBuilder;
import sybilcentrality.strategy.sybil.DensitySybilStrategy;
import sybilcentrality.strategy.sybil.SybilHidingStrategy;

public class RunDensitySybilHiding extends RunSybilHiding {

	public static void main(String[] args) {
		int times = args.length > 0 ? Integer.parseInt(args[0]) : 20;
		int n = args.length > 1 ? Integer.parseInt(args[1]) : 1000;
		int samples = args.length > 2 ? Integer.parseInt(args[2]) : 10;
		int hidingBudget = 10;
		int avgDegree = 10;
		
		RunDensitySybilHiding r = new RunDensitySybilHiding();

		r.runSmallWorld(n, avgDegree, .25, times, hidingBudget, samples, false);
		r.runErdosRenyi(n, avgDegree, times, hidingBudget, samples, false);
		r.runBarabasiAlbert(n, avgDegree, times, hidingBudget, samples, false);
		
		r.aggregateAll();
		r.printCharts();
	}
	
	@Override
	public String getDirectoryName() {
		return "sybil-density";
	}
	
	protected List<SybilHidingStrategy> getStrategies(Graph g, boolean sybilRing){
		List<SybilHidingStrategy> strategies = Utils.aList();
		for (int sybilDensity = 0; sybilDensity <= 100; sybilDensity += 10)
			for (int alterDensity = 0; alterDensity <= 100; alterDensity += 10)
				strategies.add(new DensitySybilStrategy(sybilDensity, alterDensity));
		return strategies;
	}

	@Override
	public List<ExperimentAggregator> getAggregators() {
		return Utils.aList(new DensityAggregator());
	}
	
	private class DensityAggregator extends ExperimentAggregator {

		@Override
		public String getName() {
			return "density";
		}

		@Override
		protected Stream<Row> processEvery(Stream<Row> rows, List<String> header, File experimentDir) {
			ExperimentResult res = expand(rows, header, experimentDir);
			res.addIntColumn("rankDiff", r -> r.getInt("rankAfter") - r.getInt("rankBefore"));
			return res.stream();
		}

		@Override
		protected Stream<Row> processGroup(Stream<Row> rows, File groupDir) {
			return aggregate(rows, Utils.aList("graph", "budget", "strategy", "centrality"),
					Utils.aList("rankBefore", "rankAfter", "rankDiff"));
		}
		
		@Override
		protected ExperimentResult postprocessMerged(ExperimentResult res) {
			DecimalFormat df = new DecimalFormat("#.0");
			res.addColumn("file", r -> r.concatVals("graph", "budget", "centrality"));
			res.addColumn("legend", r -> r.get("graph").split("-")[1]);
			res.addColumn("sybilDensity", r -> r.get("strategy").split("-")[1]);
			res.addColumn("alterDensity", r -> r.get("strategy").split("-")[2]);
			res.addColumn("rankDiffRead", r -> df.format(r.getDouble("rankDiffMean")));
			return res;
		}
		
		@Override
		public void printCharts(ExperimentResult res) {
			RChartPrinter.printHeatMap(
							new RChartBuilder(res, "diff.pdf", "sybilDensity", "alterDensity", "rankDiffMean")
					.multipleFilesColumn("file").fileWidth(6.).fileHeight(4.5).textSize(20)
					.xTitle("Sybil density").yTitle("Alter density")
					.sortXAscending().sortXColumn("sybilDensity").sortYAscending().sortYColumn("alterDensity")
					.heatScaleTwoEnd().heatHighColor("dodgerblue4").heatLowColor("firebrick4")
					.labelsColumn("rankDiffRead").labelsSize(4).labelsHjust(.5).labelsVjust(.5)
					.legendPosition("none").legendSeparate().legendColumn("legend")
					.legendKeyWidth(5).legendKeyHeight(45));
		}
	}
}
