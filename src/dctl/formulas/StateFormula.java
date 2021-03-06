package dctl.formulas;

import java.util.Set;

public abstract class StateFormula extends Formula {

	@Override
	public final boolean is_state_formula() {
		return true;
	}

	@Override
	public final boolean is_path_formula() {
		return false;
	}
	
	public abstract StateFormula negate();
	
	public abstract Set<StateFormula> get_decomposition();

}
