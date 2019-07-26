package pico.inference.solver;

import checkers.inference.DefaultInferenceResult;
import checkers.inference.InferenceResult;
import checkers.inference.model.Constraint;
import checkers.inference.model.Slot;
import checkers.inference.solver.SolverEngine;
import checkers.inference.solver.backend.SolverFactory;
import checkers.inference.solver.backend.maxsat.MaxSatFormatTranslator;
import checkers.inference.solver.backend.maxsat.MaxSatSolverFactory;
import checkers.inference.solver.frontend.Lattice;
import java.io.File;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import org.checkerframework.framework.type.QualifierHierarchy;

/**
 * {@link SolverEngine} that creates {@link PICOFormatTranslator} and pass it to actual underlying
 * MaxSat solver to solve constraints
 */
public class PICOSolverEngine extends SolverEngine {
    @Override
    public InferenceResult solve(
            Map<String, String> configuration,
            Collection<Slot> slots,
            Collection<Constraint> constraints,
            QualifierHierarchy qualHierarchy,
            ProcessingEnvironment processingEnvironment) {
        InferenceResult result =
                super.solve(
                        configuration, slots, constraints, qualHierarchy, processingEnvironment);
        if (collectStatistics && result.hasSolution()) {
            writeInferenceResult(
                    "pico-inference-result.txt",
                    ((DefaultInferenceResult) result).varIdToAnnotation);
        }
        return result;
    }

    public static void writeInferenceResult(
            String filename, Map<Integer, AnnotationMirror> result) {
        String writePath =
                new File(new File("").getAbsolutePath()).toString() + File.separator + filename;
        StringBuilder sb = new StringBuilder();

        Map<AnnotationMirror, Integer> inferredAnnotationsCount = new HashMap<>();
        for (AnnotationMirror inferedAnnotation : result.values()) {;

            if (!inferredAnnotationsCount.containsKey(inferedAnnotation)) {
                inferredAnnotationsCount.put(inferedAnnotation, 1);
            } else {
                inferredAnnotationsCount.put(
                        inferedAnnotation, inferredAnnotationsCount.get(inferedAnnotation) + 1);
            }
        }

        recordKeyValue(sb, "TotalSlots", String.valueOf(result.size()), String.valueOf(100));
        for (Map.Entry<AnnotationMirror, Integer> e : inferredAnnotationsCount.entrySet()) {
            recordKeyValue(
                    sb,
                    e.getKey().toString(),
                    String.valueOf(e.getValue()),
                    String.format("%.2f", (100 * (float) e.getValue() / result.size())));
        }

        try {
            PrintWriter pw = new PrintWriter(writePath);
            pw.write(sb.toString());
            pw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void recordKeyValue(
            StringBuilder sb, String key, String value, String percentage) {
        sb.append(key + "," + value + "," + percentage + "%\n");
    }

    @Override
    protected SolverFactory createSolverFactory() {
        return new MaxSatSolverFactory() {
            @Override
            public MaxSatFormatTranslator createFormatTranslator(Lattice lattice) {
                // Injects PICOFormatTranslator that has the custom logic for encoding viewpoint
                // adaptation
                // to underlying solver
                return new PICOFormatTranslator(lattice);
            }
        };
    }
}
