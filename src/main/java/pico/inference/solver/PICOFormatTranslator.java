package pico.inference.solver;

import checkers.inference.solver.backend.maxsat.MaxSatFormatTranslator;
import checkers.inference.solver.frontend.Lattice;
import checkers.inference.util.ConstraintVerifier;

/**
 * PICOSolverEngine encoder that encodes constraints to format that underlying solver can understand.
 * This class additionally encodes viewpoint adaptation logic by delegatee combineConstraintEncoder
 */
public class PICOFormatTranslator extends MaxSatFormatTranslator{

    public PICOFormatTranslator(Lattice lattice, ConstraintVerifier verifier) {
        super(lattice, verifier);
        postInit();
    }

    @Override
    protected PICOCombineConstraintEncoder createCombineConstraintEncoder(ConstraintVerifier verifier) {
        return new PICOCombineConstraintEncoder(lattice, verifier, typeToInt);
    }
}
