package pico.inference.solver;

import checkers.inference.solver.SolverEngine;
import checkers.inference.solver.backend.SolverFactory;
import checkers.inference.solver.backend.maxsat.MaxSatFormatTranslator;
import checkers.inference.solver.backend.maxsat.MaxSatSolverFactory;
import checkers.inference.solver.frontend.Lattice;

/**
 * {@link SolverEngine} that creates {@link PICOFormatTranslator} and pass it to actual underlying MaxSat solver
 * to solve constraints
 */
public class PICOSolverEngine extends SolverEngine {

    @Override
    protected SolverFactory createSolverFactory() {
        return new MaxSatSolverFactory(){
            @Override
            public MaxSatFormatTranslator createFormatTranslator(Lattice lattice) {
                // Injects PICOFormatTranslator that has the custom logic for encoding viewpoint adaptation to underlying solver
                return new PICOFormatTranslator(lattice);
            }
        };
    }
}
