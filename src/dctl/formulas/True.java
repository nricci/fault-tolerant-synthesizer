package dctl.formulas;

import java.util.Set;

import util.XMLBuilder;

public final class True extends Atom {
	
	public String toString() {
		return "True";
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof True)
			return true;
		return false;
	}
	
	@Override
	public int hashCode() {
		return 1;
	}
	
	@Override
	public Formula obligation_formula() {
		return new True();
	}

	@Override
	public boolean is_propositional() {
		return true;
	}

	@Override
	protected boolean sat(Set<StateFormula> set) {
		return true;
	}

	@Override
	public StateFormula negate() {
		return new False();
	}
	
	@Override
	public void to_xml(XMLBuilder b) {
		b.open_self_close("true");		
	}
}
