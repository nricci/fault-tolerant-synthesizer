package dctl.formulas;

import java.util.Set;

public class Globally extends PathFormula implements UnaryExpr {

	private StateFormula _arg;
		
	public Globally(StateFormula a) {
		_arg = a;
	}
	
	@Override
	public StateFormula arg() {
		return _arg;
	}

	@Override
	public boolean is_alpha() {
		return true;
	}

	@Override
	public boolean is_beta() {
		return false;
	}

	public String toString() {
		return "G" + _arg.toString();
	}

	@Override
	public boolean is_elementary() {
		return false;
	}



	@Override
	public Formula obligation_formula() {
		//throw new Error("Not Supported Yet. Class Only for parsing.");
		return new Globally((StateFormula) _arg.obligation_formula());
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
		Globally other = (Globally) obj;
		if (_arg == null) {
			if (other._arg != null)
				return false;
		} else if (!_arg.equals(other._arg))
			return false;
		return true;
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
