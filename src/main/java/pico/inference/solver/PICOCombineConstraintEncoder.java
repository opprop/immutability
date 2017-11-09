package pico.inference.solver;

import checkers.inference.model.ConstantSlot;
import checkers.inference.model.VariableSlot;
import checkers.inference.solver.backend.encoder.combine.CombineConstraintEncoder;
import checkers.inference.solver.backend.maxsat.MathUtils;
import checkers.inference.solver.backend.maxsat.VectorUtils;
import checkers.inference.solver.backend.maxsat.encoder.MaxSATAbstractConstraintEncoder;
import checkers.inference.solver.frontend.Lattice;
import checkers.inference.util.ConstraintVerifier;
import exceptions.solver.EncodingStuckException;
import org.checkerframework.javacutil.AnnotationUtils;
import org.sat4j.core.VecInt;
import pico.inference.PICOInferenceChecker;

import javax.lang.model.element.AnnotationMirror;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Contains encoding viewpoint adaptation logic for PICOInfer. Specifies how qualifiers are
 * combined and what result the combination yields.
 */
public class PICOCombineConstraintEncoder extends MaxSATAbstractConstraintEncoder implements CombineConstraintEncoder<VecInt[]> {

    /**AnnotationMirrors that are used in viewpoint adaptation encoding*/
    private final AnnotationMirror READONLY;
    private final AnnotationMirror MUTABLE;
    private final AnnotationMirror IMMUTABLE;
    private final AnnotationMirror BOTTOM;
    private final AnnotationMirror RECEIVERDEPENDANTMUTABLE;

    public PICOCombineConstraintEncoder(Lattice lattice, ConstraintVerifier verifier, Map<AnnotationMirror, Integer> typeToInt) {
        super(lattice, verifier, typeToInt);
        READONLY = PICOInferenceChecker.READONLY;
        MUTABLE = PICOInferenceChecker.MUTABLE;
        IMMUTABLE = PICOInferenceChecker.IMMUTABLE;
        BOTTOM = PICOInferenceChecker.BOTTOM;
        RECEIVERDEPENDANTMUTABLE = PICOInferenceChecker.RECEIVERDEPENDANTMUTABLE;
    }

    /**Wrapper method to get integer id of an AnnotationMirror to avoid Map get operations*/
    private int id(AnnotationMirror am) {
        return typeToInt.get(am);
    }

    private boolean isReceiverDependantMutable(ConstantSlot cSlot) {
        if (AnnotationUtils.areSame(cSlot.getValue(), RECEIVERDEPENDANTMUTABLE)) {
            return true;
        } else if (AnnotationUtils.areSame(cSlot.getValue(), READONLY) ||
                AnnotationUtils.areSame(cSlot.getValue(), MUTABLE) ||
                AnnotationUtils.areSame(cSlot.getValue(), IMMUTABLE) ||
                AnnotationUtils.areSame(cSlot.getValue(), BOTTOM)) {
            return false;
        } else {
            throw new EncodingStuckException("Unknown qualifier: " + cSlot.getValue());
        }
    }

    @Override
    public VecInt[] encodeVariable_Variable(VariableSlot target, VariableSlot declared, VariableSlot result) {
        List<VecInt> resultClauses = new ArrayList<VecInt>();
        resultClauses.add(VectorUtils.asVec(
                -MathUtils.mapIdToMatrixEntry(declared.getId(), id(READONLY), lattice),
                MathUtils.mapIdToMatrixEntry(result.getId(), id(READONLY), lattice)));
        resultClauses.add(VectorUtils.asVec(
                -MathUtils.mapIdToMatrixEntry(declared.getId(), id(MUTABLE), lattice),
                MathUtils.mapIdToMatrixEntry(result.getId(), id(MUTABLE), lattice)));
        resultClauses.add(VectorUtils.asVec(
                -MathUtils.mapIdToMatrixEntry(declared.getId(), id(IMMUTABLE), lattice),
                MathUtils.mapIdToMatrixEntry(result.getId(), id(IMMUTABLE), lattice)));
        resultClauses.add(VectorUtils.asVec(
                -MathUtils.mapIdToMatrixEntry(declared.getId(), id(BOTTOM), lattice),
                MathUtils.mapIdToMatrixEntry(result.getId(), id(BOTTOM), lattice)));
        resultClauses.add(VectorUtils.asVec(
                -MathUtils.mapIdToMatrixEntry(declared.getId(), id(RECEIVERDEPENDANTMUTABLE), lattice),
                -MathUtils.mapIdToMatrixEntry(target.getId(), id(READONLY), lattice),
                MathUtils.mapIdToMatrixEntry(result.getId(), id(READONLY), lattice)));
        resultClauses.add(VectorUtils.asVec(
                -MathUtils.mapIdToMatrixEntry(declared.getId(), id(RECEIVERDEPENDANTMUTABLE), lattice),
                -MathUtils.mapIdToMatrixEntry(target.getId(), id(MUTABLE), lattice),
                MathUtils.mapIdToMatrixEntry(result.getId(), id(MUTABLE), lattice)));
        resultClauses.add(VectorUtils.asVec(
                -MathUtils.mapIdToMatrixEntry(declared.getId(), id(RECEIVERDEPENDANTMUTABLE), lattice),
                -MathUtils.mapIdToMatrixEntry(target.getId(), id(IMMUTABLE), lattice),
                MathUtils.mapIdToMatrixEntry(result.getId(), id(IMMUTABLE), lattice)));
        resultClauses.add(VectorUtils.asVec(
                -MathUtils.mapIdToMatrixEntry(declared.getId(), id(RECEIVERDEPENDANTMUTABLE), lattice),
                -MathUtils.mapIdToMatrixEntry(target.getId(), id(BOTTOM), lattice),
                MathUtils.mapIdToMatrixEntry(result.getId(), id(BOTTOM), lattice)));
        resultClauses.add(VectorUtils.asVec(
                -MathUtils.mapIdToMatrixEntry(declared.getId(), id(RECEIVERDEPENDANTMUTABLE), lattice),
                -MathUtils.mapIdToMatrixEntry(target.getId(), id(RECEIVERDEPENDANTMUTABLE), lattice),
                MathUtils.mapIdToMatrixEntry(result.getId(), id(RECEIVERDEPENDANTMUTABLE), lattice)));
        return resultClauses.toArray(new VecInt[resultClauses.size()]);
    }

    @Override
    public VecInt[] encodeVariable_Constant(VariableSlot target, ConstantSlot declared, VariableSlot result) {
        List<VecInt> resultClauses = new ArrayList<VecInt>();
        if (!isReceiverDependantMutable(declared)) {
            resultClauses.add(VectorUtils.asVec(
                    MathUtils.mapIdToMatrixEntry(result.getId(), id(declared.getValue()), lattice)));
        } else {
            resultClauses.add(VectorUtils.asVec(
                    -MathUtils.mapIdToMatrixEntry(target.getId(), id(READONLY), lattice),
                    MathUtils.mapIdToMatrixEntry(result.getId(), id(READONLY), lattice)));
            resultClauses.add(VectorUtils.asVec(
                    -MathUtils.mapIdToMatrixEntry(target.getId(), id(MUTABLE), lattice),
                    MathUtils.mapIdToMatrixEntry(result.getId(), id(MUTABLE), lattice)));
            resultClauses.add(VectorUtils.asVec(
                    -MathUtils.mapIdToMatrixEntry(target.getId(), id(IMMUTABLE), lattice),
                    MathUtils.mapIdToMatrixEntry(result.getId(), id(IMMUTABLE), lattice)));
            resultClauses.add(VectorUtils.asVec(
                    -MathUtils.mapIdToMatrixEntry(target.getId(), id(RECEIVERDEPENDANTMUTABLE), lattice),
                    MathUtils.mapIdToMatrixEntry(result.getId(), id(RECEIVERDEPENDANTMUTABLE), lattice)));
            resultClauses.add(VectorUtils.asVec(
                    -MathUtils.mapIdToMatrixEntry(target.getId(), id(BOTTOM), lattice),
                    MathUtils.mapIdToMatrixEntry(result.getId(), id(BOTTOM), lattice)));

        }
        return resultClauses.toArray(new VecInt[resultClauses.size()]);
    }

    @Override
    public VecInt[] encodeConstant_Variable(ConstantSlot target, VariableSlot declared, VariableSlot result) {
        List<VecInt> resultClauses = new ArrayList<VecInt>();
        resultClauses.add(VectorUtils.asVec(
                -MathUtils.mapIdToMatrixEntry(declared.getId(), id(READONLY), lattice),
                MathUtils.mapIdToMatrixEntry(result.getId(), id(READONLY), lattice)));
        resultClauses.add(VectorUtils.asVec(
                -MathUtils.mapIdToMatrixEntry(declared.getId(), id(MUTABLE), lattice),
                MathUtils.mapIdToMatrixEntry(result.getId(), id(MUTABLE), lattice)));
        resultClauses.add(VectorUtils.asVec(
                -MathUtils.mapIdToMatrixEntry(declared.getId(), id(IMMUTABLE), lattice),
                MathUtils.mapIdToMatrixEntry(result.getId(),id(IMMUTABLE), lattice)));
        resultClauses.add(VectorUtils.asVec(
                -MathUtils.mapIdToMatrixEntry(declared.getId(), id(BOTTOM), lattice),
                MathUtils.mapIdToMatrixEntry(result.getId(), id(BOTTOM), lattice)));
        resultClauses.add(VectorUtils.asVec(
                -MathUtils.mapIdToMatrixEntry(declared.getId(), id(RECEIVERDEPENDANTMUTABLE), lattice),
                MathUtils.mapIdToMatrixEntry(result.getId(), id(target.getValue()), lattice)));
        return resultClauses.toArray(new VecInt[resultClauses.size()]);
    }

    @Override
    public VecInt[] encodeConstant_Constant(ConstantSlot target, ConstantSlot declared, VariableSlot result) {
        List<VecInt> resultClauses = new ArrayList<VecInt>();
        if (!isReceiverDependantMutable(declared)) {
            resultClauses.add(VectorUtils.asVec(
                    MathUtils.mapIdToMatrixEntry(result.getId(), id(declared.getValue()), lattice)));
        } else {
            resultClauses.add(VectorUtils.asVec(
                    MathUtils.mapIdToMatrixEntry(result.getId(), id(target.getValue()), lattice)));
        }
        return resultClauses.toArray(new VecInt[resultClauses.size()]);
    }
}
