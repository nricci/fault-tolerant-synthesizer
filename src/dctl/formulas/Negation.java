package dctl.formulas;

import java.util.HashSet;
import java.util.Set;

public final class Negation extends PropositionalFormula implements UnaryExpr {

	private StateFormula _arg;
	
	public Negation(StateFormula arg) {
		_arg = arg;
	}

	@Override
	public StateFormula arg() {
		return _arg;
	}
	
	@Override
	public boolean is_elementary() {
		return arg().is_elementary();
	}	
	
	@Override
	public boolean is_alpha() {
		if (arg().is_elementary())
			return false;
		else
			return !arg().is_alpha();
	}

	@Override
	public boolean is_beta() {
		if (arg().is_elementary())
			return false;
		else
			return !arg().is_beta();
	}

	@Override
	public Set<StateFormula> get_decomposition() {
		if (arg().is_elementary())
			return null;
		else {
			Set<StateFormula> deco = new HashSet<StateFormula>();
			for(StateFormula _f : arg().get_decomposition())
				deco.add(_f.negate());
			return deco;
		}
			
	}
	
	public StateFormula negate() {
		return this.arg();
	}

	public String toString() {
		return "!" + arg().toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_arg == null) ? 0 : _arg.hashCode());
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
		Negation other = (Negation) obj;
		if (_arg == null) {
			if (other._arg != null)
				return false;
		} else if (!_arg.equals(other._arg))
			return false;
		return true;
	}
	
	
	@Override
	public Formula obligation_formula() {
		if(_arg instanceof Proposition)
			return new DeonticProposition(this);
		else
			return new Negation((StateFormula)_arg.obligation_formula());
	}

	@Override
	public boolean is_propositional() {
		return _arg.is_propositional();
	}
	
	@Override
	public boolean is_literal() {
		return this.arg() instanceof Proposition;
	}

	@Override
	protected boolean sat(Set<StateFormula> set) {
		return !_arg.sat(set);
	}
	
	
}
