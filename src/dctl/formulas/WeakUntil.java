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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_left == null) ? 0 : _left.hashCode());
		result = prime * result + ((_right == null) ? 0 : _right.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		WeakUntil other = (WeakUntil) obj;
		if (_left == null) {
			if (other._left != null)
				return false;
		} else if (!_left.equals(other._left))
			return false;
		if (_right == null) {
			if (other._right != null)
				return false;
		} else if (!_right.equals(other._right))
			return false;
		return true;
	}
	
	@Override
	public Formula obligation_formula() {
		return new WeakUntil((StateFormula) _left.obligation_formula(),(StateFormula) _right.obligation_formula());
	}

	@Override
	public boolean is_propositional() {
		return false;
	}

	@Override
	protected boolean sat(Set<StateFormula> set) {
		throw new Error("Inaplicable operation");
	}


}
