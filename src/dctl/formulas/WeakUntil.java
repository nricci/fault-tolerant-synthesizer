package dctl.formulas;

import java.util.Set;

public final class WeakUntil extends PathFormula implements BinaryExpr {

	private StateFormula _left;
	
	private StateFormula _right;
	
	public WeakUntil(StateFormula l, StateFormula r) {
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
	public boolean is_alpha() {
		return false;
	}

	@Override
	public boolean is_beta() {
		return true;
	}

	public String toString() {
		return arg_left().toString() + "W" + arg_right().toString();
	}

	@Override
	public boolean is_elementary() {
		return false;
	}

}
