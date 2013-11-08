package dctl.formulas;

import java.util.HashSet;
import java.util.Set;

public final class Permission extends Quantifier {

	public Permission(PathFormula arg) {
		this._arg = arg;
	}	
	
	@Override
	public Set<StateFormula> get_decomposition() {
		if (_arg instanceof Next) 
			return null;
		else if (_arg instanceof Until) {
			Set<StateFormula> deco = new HashSet<StateFormula>();
			deco.add(((Until) _arg).arg_right());
			StateFormula p = new Permission(new Next(this));
			p = new And(((Until) _arg).arg_left(),p);
			deco.add(p);
			return deco;
		} else if (_arg instanceof WeakUntil) {
			Set<StateFormula> deco = new HashSet<StateFormula>();
			deco.add(((WeakUntil) _arg).arg_right());
			StateFormula p = new Permission(new Next(this));
			p = new And(((WeakUntil) _arg).arg_left(),p);
			deco.add(p);
			return deco;
		}
		return null;
	}
	
	public String toString() {
		return "P(" + arg().toString() + ")";
	}

}
