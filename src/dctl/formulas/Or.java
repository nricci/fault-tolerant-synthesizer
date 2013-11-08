package dctl.formulas;

import java.util.HashSet;
import java.util.Set;

public final class Or extends PropositionalFormula implements BinaryExpr {

	private StateFormula _left;
	
	private StateFormula _right; 
	
	public Or(StateFormula l, StateFormula r) {
		_left = l;
		_right = r;
	}
	
	
	@Override
	public StateFormula arg_left() {
		return _left;
	}

	@Override
	public StateFormula arg_right() {
		return _right;
	}

	@Override
	public final boolean is_elementary() {
		return false;
	}
	
	@Override
	public final boolean is_alpha() {
		return false;
	}

	@Override
	public final boolean is_beta() {
		return true;
	}

	@Override
	public Set<StateFormula> get_decomposition() {
		Set<StateFormula> deco = new HashSet<StateFormula>();
		deco.add(_left);
		deco.add(_right);
		return deco;
	}

	public String toString() {
		return arg_left().toString() + "||" + arg_right().toString();
	}



}
