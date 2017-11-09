package pico.inference.solver;

import checkers.inference.solver.SolverEngine;
import checkers.inference.solver.backend.FormatTranslator;
import checkers.inference.solver.backend.SolverType;
import checkers.inference.solver.frontend.Lattice;
import checkers.inference.util.ConstraintVerifier;

/**
 * PICOSolverEngine that passes PICOFormatTranslator to actual underlying MaxSat solver to solve the constraints
 */
public class PICOSolverEngine extends SolverEngine {
    @Override
    protected FormatTranslator<?, ?, ?> createFormatTranslator(SolverType solverType, Lattice lattice, ConstraintVerifier verifier) {
        // Injects PICOFormatTranslator that has the custom logic for encoding viewpoint adaptation to underlying solver
        return new PICOFormatTranslator(lattice, verifier);
    }
}
