package dctl.formulas;

import java.util.Set;

import util.XMLBuilder;

public final class False extends Atom {
	
	public String toString() {
		return "False";
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof False)
			return true;
		return false;
	}
	
	@Override
	public int hashCode() {
		return 1;
	}

	@Override
	public Formula obligation_formula() {
		return new False();
	}

	@Override
	public boolean is_propositional() {
		return true;
	}

	@Override
	protected boolean sat(Set<StateFormula> set) {
		return false;
	}

	@Override
	public StateFormula negate() {
		return new True();
	}

	@Override
	public void to_xml(XMLBuilder b) {
		b.open_self_close("false");		
	}
	
}
