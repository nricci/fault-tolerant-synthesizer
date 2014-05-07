package tableaux;

import java.util.HashSet;
import java.util.Set;

import dctl.formulas.Forall;
import dctl.formulas.Proposition;
import dctl.formulas.StateFormula;

public class Specification {

	public Set<Proposition> _interface;
	
	public Set<StateFormula> _spec;
	
	public Set<Forall> _gcommands;
	
	public Specification() {
		this._interface = new HashSet<Proposition>();
		this._spec = new HashSet<StateFormula>();
		this._gcommands = new HashSet<Forall>();
	}
	
	
}
