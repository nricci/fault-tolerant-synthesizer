package dctl.formulas;

import java.util.HashSet;
import java.util.Set;

public final class Negation extends PropositionalFormula implements UnaryExpr {

	private StateFormula _arg;
	
	public Negation(StateFormula arg) {
		_arg = arg;
	}

	@Override
	public StateFormula arg() {
		return _arg;
	}
	
	@Override
	public boolean is_elementary() {
		return arg().is_elementary();
	}	
	
	@Override
	public boolean is_alpha() {
		if (arg().is_elementary())
			return false;
		else
			return !arg().is_alpha();
	}

	@Override
	public boolean is_beta() {
		if (arg().is_elementary())
			return false;
		else
			return !arg().is_alpha();
	}

	@Override
	public Set<StateFormula> get_decomposition() {
		if (arg().is_elementary())
			return null;
		else {
			Set<StateFormula> deco = new HashSet<StateFormula>();
			for(StateFormula _f : arg().get_decomposition())
				deco.add(_f.negate());
			return deco;
		}
			
	}
	
	public StateFormula negate() {
		return this.arg();
	}

	public String toString() {
		return "!" + arg().toString();
	}
}
