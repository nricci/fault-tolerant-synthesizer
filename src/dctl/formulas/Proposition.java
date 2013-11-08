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
	
}
