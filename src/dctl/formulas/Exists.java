package dctl.formulas;

import java.util.HashSet;
import java.util.Set;

public final class Exists extends Quantifier {

	public Exists(PathFormula arg) {
		this._arg = arg;
	}
	
	@Override
	public Set<StateFormula> get_decomposition() {
		if (_arg instanceof Next) 
			return null;
		else if (_arg instanceof Until) {
			Set<StateFormula> deco = new HashSet<StateFormula>();
			deco.add(((Until) _arg).arg_right());
			StateFormula p = new Exists(new Next(this));
			p = new And(((Until) _arg).arg_left(),p);
			deco.add(p);
			return deco;
		} else if (_arg instanceof WeakUntil) {
			Set<StateFormula> deco = new HashSet<StateFormula>();
			deco.add(((WeakUntil) _arg).arg_right());
			StateFormula p = new Exists(new Next(this));
			p = new And(((WeakUntil) _arg).arg_left(),p);
			deco.add(p);
			return deco;
		}
		return null;
	}
	
	public String toString() {
		return "E(" + arg().toString() + ")";
	}

	@Override
	public Formula obligation_formula() {
		return new Exists((PathFormula) _arg.obligation_formula());
	}
	
}
