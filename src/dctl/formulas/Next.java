package dctl.formulas;

import java.util.Set;

import util.XMLBuilder;

public final class Next extends PathFormula implements UnaryExpr {

	private StateFormula _arg;
	
	public Next(StateFormula arg) {
		_arg = arg;
	}
	
	@Override
	public StateFormula arg() {
		return _arg;
	}

	@Override
	public boolean is_alpha() {
		return false;
	}

	@Override
	public boolean is_beta() {
		return false;
	}
	
	public String toString() {
		return "X(" + arg().toString() + ")";
	}

	@Override
	public boolean is_elementary() {
		return true;
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
		Next other = (Next) obj;
		if (_arg == null) {
			if (other._arg != null)
				return false;
		} else if (!_arg.equals(other._arg))
			return false;
		return true;
	}

	@Override
	public Formula obligation_formula() {
		return new Next((StateFormula) _arg.obligation_formula());
	}

	@Override
	public boolean is_propositional() {
		return false;
	}

	@Override
	protected boolean sat(Set<StateFormula> set) {
		throw new Error("Inaplicable operation");
	}

	@Override
	public void to_xml(XMLBuilder b) {
		b.open("next");
		this._arg.to_xml(b);
		b.close();		
	}
	


}
