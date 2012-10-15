/**
 * Copyright (c) 2011-2012 Armin Töpfer
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
package ch.ethz.bsse.quasirecomb.model;

import ch.ethz.bsse.quasirecomb.informationholder.Globals;
import ch.ethz.bsse.quasirecomb.informationholder.Read;
import ch.ethz.bsse.quasirecomb.model.hmm.ModelSelection;
import ch.ethz.bsse.quasirecomb.modelsampling.ModelSampling;
import ch.ethz.bsse.quasirecomb.utils.Plot;
import ch.ethz.bsse.quasirecomb.utils.Utils;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsible for orchestrating parsing, proper read placement in alignment and
 * forwards parameters to ModelSelection.
 *
 * @author Armin Töpfer (armin.toepfer [at] gmail.com)
 */
public class Preprocessing {

    /**
     * Entry point. Forwards invokes of the specified workflow.
     *
     * @param L length of the reads
     * @param exp is it an experimental dataset
     * @param input path to the fasta file
     * @param Kmin minimal amount of generators
     * @param Kmax minimal amount of generators
     * @param n size of the alphabet
     * @param f distribution of the haplotypes if sampling has to be done
     * @param N amount of reads in case exp is false
     */
    public static void workflow(String input, int Kmin, int Kmax) {
        Read[] reads = Utils.parseInput(input);

        for (Read r : reads) {
            Globals.getINSTANCE().setALIGNMENT_BEGIN(Math.min(r.getBegin(), Globals.getINSTANCE().getALIGNMENT_BEGIN()));
            Globals.getINSTANCE().setALIGNMENT_END(Math.max(r.getEnd(), Globals.getINSTANCE().getALIGNMENT_END()));
        }
        int L = Globals.getINSTANCE().getALIGNMENT_END() - Globals.getINSTANCE().getALIGNMENT_BEGIN();

        int[][] alignment = new int[L][5];
        int count = 0;
        for (Read r : reads) {
            int begin = r.getWatsonBegin() - Globals.getINSTANCE().getALIGNMENT_BEGIN();
            for (int i = 0; i < r.getSequence().length; i++) {
                try {
                alignment[i + begin][r.getSequence()[i]] += r.getCount();
                } catch(ArrayIndexOutOfBoundsException e) {
                    System.out.println(e);
                }
            }
            if (r.isPaired()) {
                begin = r.getCrickBegin() - Globals.getINSTANCE().getALIGNMENT_BEGIN();
                for (int i = 0; i < r.getCrickSequence().length; i++) {
                    alignment[i + begin][r.getCrickSequence()[i]] += r.getCount();
                }
            }
            count += r.getCount();
        }
//        saveUnique(reads);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < L; i++) {
            int hits = 0;
            for (int v = 0; v < 4; v++) {
                sb.append(alignment[i][v]).append("\n");
                if (alignment[i][v] != 0) {
                    hits++;
                }
            }
            if (hits == 0) {
                System.out.println("Position " + i + " is not covered.");
            }
        }
        Utils.saveFile(Globals.getINSTANCE().getSAVEPATH() + "hit_dist.txt", sb.toString());
        int n = countChars(reads);
        Plot.plotCoverage(reads);
        ModelSelection ms = new ModelSelection(reads, Kmin, Kmax, reads.length, L, n);
        ModelSampling modelSampling = new ModelSampling(L, n, ms.getOptimalResult().getK(), ms.getOptimalResult().getRho(), ms.getOptimalResult().getPi(), ms.getOptimalResult().getMu());
        modelSampling.save();
        System.out.println("Quasispecies saved: " + Globals.getINSTANCE().getSAVEPATH() + "quasispecies.fasta");
    }

    private static int countChars(Read[] rs) {
        Map<Byte, Boolean> map = new HashMap<>();
        for (Read r : rs) {
            for (byte b : r.getSequence()) {
                map.put(b, Boolean.TRUE);
            }
        }
        return map.keySet().size();
    }

    private static void saveUnique(Read[] reads) {
        if (Globals.getINSTANCE().isDEBUG()) {
            StringBuilder sb = new StringBuilder();
            for (Read r : reads) {
                sb.append(r.getCount()).append("\t");
                if (r.getCount() < 1000) {
                    sb.append("\t");
                }
                for (int i = Globals.getINSTANCE().getALIGNMENT_BEGIN(); i < r.getBegin(); i++) {
                    sb.append(" ");
                }
                sb.append(Utils.reverse(r.getSequence())).append("\n");
            }
            Utils.saveFile(Globals.getINSTANCE().getSAVEPATH() + File.separator + "in.fasta", sb.toString());
        }
    }
}
