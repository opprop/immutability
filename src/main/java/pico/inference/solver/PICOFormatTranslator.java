package pico.inference.solver;

import checkers.inference.solver.backend.encoder.ConstraintEncoderFactory;
import checkers.inference.solver.backend.encoder.combine.CombineConstraintEncoder;
import checkers.inference.solver.backend.maxsat.MaxSatFormatTranslator;
import checkers.inference.solver.backend.maxsat.encoder.MaxSATConstraintEncoderFactory;
import checkers.inference.solver.frontend.Lattice;
import checkers.inference.util.ConstraintVerifier;
import org.sat4j.core.VecInt;

/**
 * {@link checkers.inference.solver.backend.FormatTranslator} that encodes constraints to format that
 * underlying solver can understand. Difference from super class is this class also encodes viewpoint
 * adaptation logic by delegating to {@link PICOCombineConstraintEncoder}
 */
public class PICOFormatTranslator extends MaxSatFormatTranslator{

    public PICOFormatTranslator(Lattice lattice) {
        super(lattice);

    }

    @Override
    protected ConstraintEncoderFactory<VecInt[]> createConstraintEncoderFactory(ConstraintVerifier verifier) {
        return new MaxSATConstraintEncoderFactory(lattice, verifier, typeToInt, this){
            @Override
            public CombineConstraintEncoder<VecInt[]> createCombineConstraintEncoder() {
                return new PICOCombineConstraintEncoder(lattice, verifier, typeToInt);
            }
        };
    }
}
