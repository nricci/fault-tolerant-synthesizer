package dctl.formulas;

import java.util.HashSet;
import java.util.Set;

public final class Obligation extends Quantifier {

	public Obligation(PathFormula arg) {
		this._arg = arg;
	}	
	
	@Override
	public Set<StateFormula> get_decomposition() {
		if (_arg instanceof Next) 
			return null;
		else if (_arg instanceof Until) {
			Set<StateFormula> deco = new HashSet<StateFormula>();
			deco.add((StateFormula) ((Until) _arg).arg_right().obligation_formula());
			StateFormula p = new Forall(new Next(this));
			p = new And((StateFormula) ((Until) _arg).arg_left().obligation_formula(),p);
			deco.add(p);
			return deco;
		} else if (_arg instanceof WeakUntil) {
			Set<StateFormula> deco = new HashSet<StateFormula>();
			deco.add((StateFormula) ((WeakUntil) _arg).arg_right().obligation_formula());
			StateFormula p = new Forall(new Next(this));
			p = new And((StateFormula) ((WeakUntil) _arg).arg_left().obligation_formula(),p);
			deco.add(p);
			return deco;
		}
		return null;
	}
	
	public String toString() {
		return "O(" + arg().toString() + ")";
	}

	@Override
	public Formula obligation_formula() {
		return new Obligation((PathFormula) _arg.obligation_formula());
	}
	
}
