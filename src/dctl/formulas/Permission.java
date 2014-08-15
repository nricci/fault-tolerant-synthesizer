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
			deco.add((StateFormula) ((Until) _arg).arg_right().obligation_formula());
			StateFormula p = new Exists(new Next(this));
			p = new And((StateFormula) ((Until) _arg).arg_left().obligation_formula(),p);
			deco.add(p);
			return deco;
		} else if (_arg instanceof WeakUntil) {
			Set<StateFormula> deco = new HashSet<StateFormula>();
			deco.add((StateFormula) ((WeakUntil) _arg).arg_right().obligation_formula());
			StateFormula p = new Exists(new Next(this));
			p = new And((StateFormula) ((WeakUntil) _arg).arg_left().obligation_formula(),p);
			deco.add(p);
			return deco;
		} else if (_arg instanceof Globally) {
			Set<StateFormula> deco = new HashSet<StateFormula>();
			deco.add(((Globally) _arg).arg());
			StateFormula p = new Exists(new Next(this));
			p = new And(((Globally) _arg).arg(),p);
			deco.add(p);
			return deco;
		} 
		return null;
	}
	
	public String toString() {
		return "P(" + arg().toString() + ")";
	}
	
	@Override
	public Formula obligation_formula() {
		return new Permission((PathFormula) _arg.obligation_formula());
	}

	@Override
	public boolean is_propositional() {
		return false;
	}

	@Override
	protected boolean sat(Set<StateFormula> set) {
		throw new Error("Inaplicable operation");
	}

}
