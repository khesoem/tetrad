package edu.cmu.tetrad.algcomparison.independence;

import edu.cmu.tetrad.annotation.TestOfIndependence;
import edu.cmu.tetrad.data.*;
import edu.cmu.tetrad.search.*;
import edu.cmu.tetrad.util.Parameters;
import edu.cmu.tetrad.util.Params;

import java.util.*;

/**
 * Wrapper for Fisher Z test.
 *
 * @author jdramsey
 */
@TestOfIndependence(
        name = "SEM BIC Test",
        command = "sem-bic-test",
        dataType = {DataType.Continuous, DataType.Covariance}
)
//@Experimental
public class SemBicTest implements IndependenceWrapper {

    static final long serialVersionUID = 23L;

    @Override
    public IndependenceTest getTest(DataModel dataSet, Parameters parameters) {
        SemBicScore semBicScore;

        if (dataSet instanceof ICovarianceMatrix) {
            semBicScore = new SemBicScore((ICovarianceMatrix) dataSet);
        } else {
            semBicScore = new SemBicScore((DataSet) dataSet);
        }

        semBicScore.setPenaltyDiscount(parameters.getDouble(Params.PENALTY_DISCOUNT));
//        score.setStructurePrior(parameters.getDouble(Params.SEM_BIC_STRUCTURE_PRIOR));

        switch (parameters.getInt(Params.SEM_BIC_RULE)) {
            case 1:
                semBicScore.setRuleType(edu.cmu.tetrad.search.SemBicScore.RuleType.BIC);
                break;
            case 2:
                semBicScore.setRuleType(edu.cmu.tetrad.search.SemBicScore.RuleType.GIC4);
                break;
            case 3:
                semBicScore.setRuleType(edu.cmu.tetrad.search.SemBicScore.RuleType.GIC5);
                break;
            case 4:
                semBicScore.setRuleType(edu.cmu.tetrad.search.SemBicScore.RuleType.GIC6);
                break;
            default:
                throw new IllegalStateException("Expecting 1, 2, 3 or 4: " + parameters.getInt(Params.SEM_BIC_RULE));
        }

        return new IndTestScore(semBicScore, dataSet);
    }

    @Override
    public String getDescription() {
        return "SEM BIC Test";
    }

    @Override
    public DataType getDataType() {
        return DataType.Continuous;
    }

    @Override
    public List<String> getParameters() {
        List<String> params = new ArrayList<>();
        params.add(Params.PENALTY_DISCOUNT);
//        params.add(Params.SEM_BIC_STRUCTURE_PRIOR);
        params.add(Params.SEM_BIC_RULE);
        return params;
    }
}
