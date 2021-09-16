import edu.cmu.tetrad.algcomparison.independence.FisherZ;
import edu.cmu.tetrad.algcomparison.statistic.*;
import edu.cmu.tetrad.data.ContinuousVariable;
import edu.cmu.tetrad.data.CovarianceMatrix;
import edu.cmu.tetrad.graph.EdgeListGraph;
import edu.cmu.tetrad.graph.Graph;
import edu.cmu.tetrad.graph.GraphUtils;
import edu.cmu.tetrad.graph.Node;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

public class TestAnneAnalysis {


    public static void main(String... args) {
        new TestAnneAnalysis().run1();
    }

    private void run1() {
        System.out.println("V PD N AFP AFN AP AR AHP AHR SHD");

        for (int v : new int[]{5, 10, 20}) {

            File fileadj = new File("/Users/josephramsey/Downloads/dataforjoe/dataforjoe/adjmats_p"
                    + v + "_b1K.txt");

            File filecor = new File("/Users/josephramsey/Downloads/dataforjoe/dataforjoe/cormats_p"
                    + v + "_b1K.txt");


//            for (double pd : new double[]{1}) {//, 2, 3, 4, 5}) {
            for (double alpha : new double[]{1e-8, 1e-4, 0.001, 0.01, 0.05, 0.1, 0.2, 0.5, 0.8}) {
                try {
                    BufferedReader inadj = new BufferedReader(new FileReader(fileadj));
                    BufferedReader incor = new BufferedReader(new FileReader(filecor));

                    String lineadj, linecor;

                    List<Node> vars = new ArrayList<>();
                    for (int i = 0; i < v; i++) vars.add(new ContinuousVariable("x" + (i + 1)));

                    for (int n : new int[]{50, 100, 500, 1000, 5000, 10000, 50000}) {
                        double sumFp = 0;
                        double sumFn = 0;
                        double sumAp = 0;
                        double sumAr = 0;
                        double sumAhp = 0;
                        double sumAhr = 0;
                        double sumShd = 0;

                        double countFp = 0;
                        double countFn = 0;
                        double countAp = 0;
                        double countAr = 0;
                        double countAhp = 0;
                        double countAhr = 0;
                        double countShd = 0;

                        for (int i = 0; i < 1000; i++) {
                            Graph trueCpdag;
                            CovarianceMatrix cov;

                            {
                                lineadj = inadj.readLine();
                                linecor = incor.readLine();

                                trueCpdag = getTrueG1(lineadj, vars);
                                cov = getCov1(linecor, vars, n);
                            }

                            Graph estCpdag;

                            {
                                {
                                    edu.cmu.tetrad.algcomparison.algorithm.oracle.cpdag.Pc pc
                                            = new edu.cmu.tetrad.algcomparison.algorithm.oracle.cpdag.Pc(new FisherZ());
                                    Parameters parameters = new Parameters();
                                    parameters.set(Params.STABLE_FAS, true);
                                    parameters.set(Params.ALPHA, alpha);
                                    parameters.set(Params.VERBOSE, false);
                                    estCpdag = pc.search(cov, parameters, trueCpdag);
                                }

                                estCpdag = GraphUtils.replaceNodes(estCpdag, trueCpdag.getNodes());

                                double afp = new AdjacencyFP().getValue(trueCpdag, estCpdag, null);
                                double afn = new AdjacencyFN().getValue(trueCpdag, estCpdag, null);
                                double ap = new AdjacencyPrecision().getValue(trueCpdag, estCpdag, null);
                                double ar = new AdjacencyRecall().getValue(trueCpdag, estCpdag, null);
                                double ahp = new ArrowheadPrecision().getValue(trueCpdag, estCpdag, null);
                                double ahr = new ArrowheadRecall().getValue(trueCpdag, estCpdag, null);
                                double shd = new SHD().getValue(trueCpdag, estCpdag, null);

                                if (!Double.isNaN(afp)) {
                                    sumFp += afp;
                                    countFp += 1;
                                }

                                if (!Double.isNaN(afn)) {
                                    sumFn += afn;
                                    countFn += 1;
                                }

                                if (!Double.isNaN(ap)) {
                                    sumAp += ap;
                                    countAp += 1;
                                }

                                if (!Double.isNaN(ar)) {
                                    sumAr += ar;
                                    countAr += 1;
                                }

                                if (!Double.isNaN(ahp)) {
                                    sumAhp += ahp;
                                    countAhp += 1;
                                }

                                if (!Double.isNaN(ahr)) {
                                    sumAhr += ahr;
                                    countAhr += 1;
                                }

                                if (!Double.isNaN(shd)) {
                                    sumShd += shd;
                                    countShd += 1;
                                }
                            }
                        }

                        double eFp = sumFp / countFp;
                        double eFn = sumFn / countFn;
                        double eAp = sumAp / countAp;
                        double eAr = sumAr / countAr;
                        double eAhp = sumAhp / countAhp;
                        double eAhr = sumAhr / countAhr;
                        double eShd = sumShd / countShd;

                        NumberFormat nf = new DecimalFormat("0.00");

                        System.out.println(v + " " + nf.format(alpha) + " " + n + " " + nf.format(eFp) + " " + nf.format(eFn)
                                + " " + nf.format(eAp) + " " + nf.format(eAr) + " " + nf.format(eAhp) + " " + nf.format(eAhr)
                                + " " + nf.format(eShd));

                    }

                    inadj.close();
                    incor.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @NotNull
    private CovarianceMatrix getCov1(String linecor, List<Node> vars, int n) {
        int v = vars.size();
        double[][] matrix = new double[v][v];
        String[] tokenscor = linecor.split(" ");
        int colcor = 2;

        for (int j = 0; j < v; j++) {
            for (int k = 0; k < v; k++) {
                matrix[j][k] = Double.parseDouble(tokenscor[colcor++]);
            }
        }

        return new CovarianceMatrix(vars, matrix, n);
    }

    @NotNull
    private Graph getTrueG1(String lineadj, List<Node> vars) {
        int v = vars.size();
        Graph trueG = new EdgeListGraph(vars);
        int[][] adjmat = new int[v][v];

        String[] tokensadj = lineadj.split(" ");
        int coladj = 2;

        for (int j = 0; j < v; j++) {
            for (int k = 0; k < v; k++) {
                adjmat[j][k] = Integer.parseInt(tokensadj[coladj++]);
            }
        }

        for (int j = 0; j < v; j++) {
            for (int k = 0; k < v; k++) {
                if (adjmat[j][k] == 1) {
                    trueG.addUndirectedEdge(vars.get(j), vars.get(k));
                }
            }
        }

        return trueG;
    }
}
