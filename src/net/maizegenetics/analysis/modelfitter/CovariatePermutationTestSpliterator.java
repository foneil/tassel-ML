package net.maizegenetics.analysis.modelfitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.math3.distribution.FDistribution;

import net.maizegenetics.matrixalgebra.Matrix.DoubleMatrix;
import net.maizegenetics.matrixalgebra.Matrix.DoubleMatrixFactory;
import net.maizegenetics.stats.linearmodels.CovariateModelEffect;
import net.maizegenetics.stats.linearmodels.ModelEffect;
import net.maizegenetics.stats.linearmodels.SweepFastLinearModel;

public class CovariatePermutationTestSpliterator implements Spliterator<double[]> {
    private List<double[]> myPermutedData;
    private List<AdditiveSite> mySites;
    private List<ModelEffect> myBaseModel;
    private DoubleMatrix baseX;
    private int origin;
    private final int end;

    public CovariatePermutationTestSpliterator(List<double[]> permutedData,
            List<AdditiveSite> siteList, List<ModelEffect> baseModel) {
        myPermutedData = permutedData;
        mySites = siteList;
        myBaseModel = baseModel;
        origin = 0;
        end = siteList.size();
        int numberOfEffects = baseModel.size();
        DoubleMatrix[][] components = new DoubleMatrix[1][numberOfEffects];
        for (int i = 0; i < numberOfEffects; i++) {
            components[0][i] = myBaseModel.get(i).getX();
        }
        baseX = DoubleMatrixFactory.DEFAULT.compose(components);

    }

    @Override
    public boolean tryAdvance(Consumer<? super double[]> action) {
        if (origin == end)
            return false;
        AdditiveSite as = mySites.get(origin);
        List<ModelEffect> myModel = new ArrayList<ModelEffect>(myBaseModel);
        ModelEffect me;
        CovariateModelEffect cme = new CovariateModelEffect(as.getCovariate());
        myModel.add(cme);

        SweepFastLinearModel sflm = new SweepFastLinearModel(myModel, myPermutedData.get(0));
        double dfError = sflm.getResidualSSdf()[1];
        DoubleMatrix G = sflm.getInverseOfXtX();
        DoubleMatrix X = baseX.concatenate(cme.getX(), false);
        FDistribution fdist = new FDistribution(1, dfError);
        double[] pvals =
                myPermutedData.stream().map(d -> DoubleMatrixFactory.DEFAULT.make(d.length, 1, d))
                        .mapToDouble(y -> {
                            DoubleMatrix Xty = X.crossproduct(y);
                            DoubleMatrix beta = G.mult(Xty);
                            double ssTotal = y.crossproduct().get(0, 0);
                            double ssModel = Xty.crossproduct(beta).get(0, 0);
                            double ssError = ssTotal - ssModel;
                            double kb =
                                    beta.get(beta.numberOfRows() - 1, beta.numberOfColumns() - 1);
                            double kgk = G.get(G.numberOfRows() - 1, G.numberOfColumns() - 1);
                            double F = kb * kb / kgk / ssError * dfError;
                            double p = 1 - fdist.cumulativeProbability(F);
                            return p;
                        }).toArray();

        action.accept(pvals);
        origin++;
        return true;
    }

    @Override
    public Spliterator<double[]> trySplit() {
        int numberRemaining = end - origin;
        if (numberRemaining < 50)
            return null;
        int mid = origin + numberRemaining / 2;
        List<AdditiveSite> splitSublist = mySites.subList(origin, mid);
        origin = mid;
        List<double[]> permutedDataCopy =
                myPermutedData.stream().map(d -> Arrays.copyOf(d, d.length)).collect(Collectors.toList());

        return new CovariatePermutationTestSpliterator(permutedDataCopy, splitSublist, myBaseModel);
    }

    @Override
    public long estimateSize() {
        return end - origin;
    }

    @Override
    public int characteristics() {
        return Spliterator.IMMUTABLE + Spliterator.NONNULL + Spliterator.SIZED
                + Spliterator.SUBSIZED;
    }
}
