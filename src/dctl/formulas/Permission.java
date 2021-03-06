package dctl.formulas;

import java.util.HashSet;
import java.util.Set;

import util.XMLBuilder;

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
			// beta 1
			deco.add((StateFormula) ((Until) _arg).arg_right().obligation_formula());
			// beta 2
			StateFormula p = new Exists(new Next(this));
			p = new And((StateFormula) ((Until) _arg).arg_left().obligation_formula(),p);
			deco.add(p);
			return deco;
		} else if (_arg instanceof WeakUntil) {
			Set<StateFormula> deco = new HashSet<StateFormula>();
			// beta 1
			deco.add((StateFormula) ((WeakUntil) _arg).arg_right().obligation_formula());
			StateFormula p = new Exists(new Next(this));
			// beta 2
			p = new And((StateFormula) ((WeakUntil) _arg).arg_left().obligation_formula(),p);
			deco.add(p);
			return deco;
		} else if (_arg instanceof Globally) {
			Set<StateFormula> deco = new HashSet<StateFormula>();
			// alpha 1
			deco.add((StateFormula) ((Globally) _arg).arg().obligation_formula());
			// alpha 2 
			StateFormula p = new Exists(new Next(this));
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

	@Override
	public void to_xml(XMLBuilder b) {
		b.open("permission");
		this._arg.to_xml(b);
		b.close();		
	}
	
}
