package dctl.formulas;

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
	
}