package dctl.formulas;

import java.util.Set;

import util.XMLBuilder;

public final class Proposition extends Atom {

	private String _name;
	
	public Proposition(String name) {
		_name = name;
	}
	
	public String name() {
		return _name;
	}
	
	public String toString() {
		return _name;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_name == null) ? 0 : _name.hashCode());
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
		Proposition other = (Proposition) obj;
		if (_name == null) {
			if (other._name != null)
				return false;
		} else if (!_name.equals(other._name))
			return false;
		return true;
	}

	@Override
	public Formula obligation_formula() {
		return new DeonticProposition(this);
	}

	@Override
	public boolean is_propositional() {
		return true;
	}
	
	@Override
	public boolean is_literal() {
		return true;
	}

	@Override
	protected boolean sat(Set<StateFormula> set) {
		return set.contains(this);
	}

	@Override
	public StateFormula negate() {
		return new Negation(this);
	}
	
	@Override
	public void to_xml(XMLBuilder b) {
		b.open_self_close("proposition", "name", this._name);		
	}
	
	
}
