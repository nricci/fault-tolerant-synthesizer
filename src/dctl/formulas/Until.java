package dctl.formulas;

public final class Until extends PathFormula implements BinaryExpr {

	private StateFormula _left;
	
	private StateFormula _right;
	
	public Until(StateFormula l, StateFormula r) {
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
		return arg_left().toString() + "U" + arg_right().toString();
	}

	@Override
	public boolean is_elementary() {
		return false;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Until) {
			return ((Until) o).arg_left().equals(arg_left()) 
				&& ((Until) o).arg_right().equals(arg_right());
		}
		return false;
	}
	
	
}