package dctl.formulas;

public final class DeonticProposition extends Atom {

	private StateFormula _proposition;
	
	public DeonticProposition(StateFormula prop) {
		if(prop instanceof Proposition)
			_proposition = prop;
		else {
			if(prop instanceof Negation) {
				Negation _prop = (Negation) prop;
				if(_prop.arg() instanceof Proposition) {
					_proposition = prop;
				} else {
					assert false;
				}
			} else {
				assert false;
			}
		}
	}
	
	public String toString() {
		return "OB("+_proposition.toString()+")";
	}

	public StateFormula get_prop() {
		return _proposition;
	}
	
	@Override
	public Formula obligation_formula() {
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((_proposition == null) ? 0 : _proposition.hashCode());
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
		DeonticProposition other = (DeonticProposition) obj;
		if (_proposition == null) {
			if (other._proposition != null)
				return false;
		} else if (!_proposition.equals(other._proposition))
			return false;
		return true;
	}

	@Override
	public boolean is_propositional() {
		return false;
	}

	
}
