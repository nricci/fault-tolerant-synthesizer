package dctl.formulas;

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
	public boolean equals(Object o) {
		if (o instanceof Proposition) {
			return ((Proposition) o).name().equals(this.name());
		}
		return false;
	}
	
}
