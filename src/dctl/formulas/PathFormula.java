package dctl.formulas;

public abstract class PathFormula extends Formula {

	@Override
	public final boolean is_state_formula() {
		return false;
	}

	@Override
	public final boolean is_path_formula() {
		return true;
	}
	
	
}
