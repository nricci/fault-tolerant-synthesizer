package dctl.formulas;

import java.util.HashSet;
import java.util.Set;

public final class And extends PropositionalFormula implements BinaryExpr {

	private StateFormula _left;
	
	private StateFormula _right; 
	
	public And(StateFormula l, StateFormula r) {
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
		return true;
	}

	@Override
	public final boolean is_beta() {
		return false;
	}

	@Override
	public Set<StateFormula> get_decomposition() {
		Set<StateFormula> deco = new HashSet<StateFormula>();
		deco.add(_left);
		deco.add(_right);
		return deco;
	}

	public String toString() {
		return arg_left().toString() + "&&" + arg_right().toString();
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
		And other = (And) obj;
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
		return new And((StateFormula)_left.obligation_formula(),(StateFormula)_right.obligation_formula());
	}


	@Override
	public boolean is_propositional() {
		return _left.is_propositional() && _right.is_propositional();
	}
	
	




}
