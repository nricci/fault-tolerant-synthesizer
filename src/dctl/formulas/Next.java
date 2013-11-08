package dctl.formulas;

import java.util.Set;

public final class Next extends PathFormula implements UnaryExpr {

	private StateFormula _arg;
	
	public Next(StateFormula arg) {
		_arg = arg;
	}
	
	@Override
	public StateFormula arg() {
		return _arg;
	}

	@Override
	public boolean is_alpha() {
		return false;
	}

	@Override
	public boolean is_beta() {
		return false;
	}
	
	public String toString() {
		return "X(" + arg().toString() + ")";
	}

	@Override
	public boolean is_elementary() {
		return true;
	}

}
