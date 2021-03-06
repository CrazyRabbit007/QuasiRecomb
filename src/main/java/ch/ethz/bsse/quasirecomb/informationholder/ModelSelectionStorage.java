/**
 * Copyright (c) 2011-2013 Armin Töpfer
 *
 * This file is part of QuasiRecomb.
 *
 * QuasiRecomb is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or any later version.
 *
 * QuasiRecomb is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * QuasiRecomb. If not, see <http://www.gnu.org/licenses/>.
 */
package ch.ethz.bsse.quasirecomb.informationholder;

import ch.ethz.bsse.quasirecomb.model.hmm.EM;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.HashMap;
import java.util.Map;


/**
 * @author Armin Töpfer (armin.toepfer [at] gmail.com)
 */
public class ModelSelectionStorage {

    private Map<Integer, SelectionResult> srMap = new HashMap<>();
    private int bestK;

    public void add(EM em, int K) {
        srMap.put(K, new SelectionResult(em.getOr(), em.getMaxBIC()));
    }

    private void select() {
        double maxValue = Double.NEGATIVE_INFINITY;
        int maxK = 0;
        for (Map.Entry<Integer, SelectionResult> entry : srMap.entrySet()) {
            SelectionResult srTmp = entry.getValue();
            if (maxValue < srTmp.max) {
                maxValue = srTmp.max;
                maxK = entry.getKey();
            }
        }
        if (maxK == Globals.getINSTANCE().getK_MIN()) {
            this.bestK = maxK;
        } else {
            while (maxK - 1 > 0) {
                if (srMap.containsKey(maxK - 1)) {
                    SelectionResult previous = srMap.get(maxK - 1);
                    if (srMap.get(maxK).max < previous.max) {
                        maxK--;
                    } else {
                        this.bestK = maxK;
                        break;
                    }
                }
            }
        }
    }

    public OptimalResult getBestOR() {
        this.select();
        return this.srMap.get(bestK).or;
    }

    public Multimap<Integer, Double> getMaxBICs() {
        Multimap<Integer,Double> bics = ArrayListMultimap.create();
        for (Map.Entry<Integer, SelectionResult> entry : srMap.entrySet()) {
            SelectionResult srTmp = entry.getValue();
            bics.put(entry.getKey(), srTmp.max);
        }
        return bics;
    }
}
class SelectionResult {

    OptimalResult or;
    double max;

    public SelectionResult(OptimalResult or, double max) {
        this.or = or;
        this.max = max;
    }
}