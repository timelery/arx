/*
 * ARX: Powerful Data Anonymization
 * Copyright 2012 - 2015 Florian Kohlmayer, Fabian Prasser
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.deidentifier.arx.risk;

import com.carrotsearch.hppc.IntIntOpenHashMap;

/**
 * This class implements the SNBModel, for details see Chen, 1998
 * 
 * @author Michael Schneider
 * @version 1.09
 */
class ModelSNB extends AbstractModelUniqueness {

    /**
     * number of equivalence classes of size one
     */
    protected int    c1;

    /**
     * number of non-empty classes equivalence classes in the population
     */
    protected double numberOfNonEmptyClasses;

    /**
     * sampling fraction
     */
    protected double samplingFraction;

    /**
     * Shlosser estimator for variable K, giving number of non zero classes in
     * the population
     * 
     * @param pi
     *            sampling fraction
     * @param eqClasses
     *            Map containing the equivalence class sizes (as keys) of the
     *            data set and the corresponding frequency (as values) e.g. if
     *            the key 2 has value 3 then there are 3 equivalence classes of
     *            size two.
     */
    ModelSNB(final double pi, final IntIntOpenHashMap eqClasses) {
        super(pi, eqClasses);
        samplingFraction = pi;
        numberOfNonEmptyClasses = estimateNonEmptyEquivalenceClasses();
        c1 = eqClasses.get(1);
    }

    /**
     * @return Shlosser estimator for variable K, giving number of non zero
     *         classes in the population estimated according to Haas, 1998 and
     *         Shlosser
     * 
     */
    private double estimateNonEmptyEquivalenceClasses() {
        double var1 = 0, var2 = 0, var3 = 0, var4 = 0;

        final int[] keys = eqClasses.keys;
        final int[] values = eqClasses.values;
        final boolean[] states = eqClasses.allocated;
        for (int i = 0; i < states.length; i++) {
            if (states[i]) {
                int key = keys[i];
                int value = values[i];
                var1 += key * samplingFraction * samplingFraction * Math.pow((1 - (samplingFraction * samplingFraction)), key - 1) * value;
                var2 += Math.pow((1 - samplingFraction), key) * (Math.pow((1 + samplingFraction), key) - 1) * value;
                var3 += Math.pow((1 - samplingFraction), key) * value;
                var4 += key * samplingFraction * Math.pow((1 - samplingFraction), (key - 1)) * value;
            }
        }

        final double K = numberOfEquivalenceClasses + (c1 * (var1 / var2) * ((var3 / var4) * (var3 / var4)));
        return K;
    }

    @Override
    protected double getRisk() {
        return (getPopulationUniques() / populationSize);
    }

    @Override
    protected double getPopulationUniques() {
        double result = Double.NaN;
        double alpha = 0, beta = 0;

        final AlgorithmNewtonSNB snbModel = new AlgorithmNewtonSNB(numberOfNonEmptyClasses,
                                                 samplingFraction,
                                                 eqClasses);

        // start values are initialized randomly
        alpha = Math.random();
        beta = Math.random();
        final double[] initialGuess = { alpha, beta };

        // use Newton Raphson Algorithm to compute solution for the nonlinear
        // multivariate equations
        final double[] output = snbModel.getSolution(initialGuess);

        result = numberOfNonEmptyClasses * Math.pow(output[1], output[0]);
        return result;
    }

}
