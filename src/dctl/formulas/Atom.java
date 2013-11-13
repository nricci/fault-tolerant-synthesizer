package dctl.formulas;

import java.util.Set;


public abstract class Atom extends StateFormula {

	@Override
	public final boolean is_elementary() {
		return true;
	}

	@Override
	public final boolean is_alpha() {
		return false;
	}

	@Override
	public final boolean is_beta() {
		return false;
	}

	@Override
	public Set<StateFormula> get_decomposition() {
		return null;	
	}
	
	

}
