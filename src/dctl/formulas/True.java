package dctl.formulas;

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
	
}
