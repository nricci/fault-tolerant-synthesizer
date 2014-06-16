package tableaux;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import dctl.formulas.*;

public class Specification {

	private Set<Proposition> _interface;
	
	private Set<StateFormula> _init; 
	
	private Map<StateFormula,StateFormula> _global_rules;
	
	
	
	public Specification() {
		this._interface = new HashSet<Proposition>();
		this._init = new HashSet<StateFormula>();
		this._global_rules = new HashMap<StateFormula, StateFormula>();
	}
	
	public void add_ivar(Proposition p) {
		_interface.add(p);
	}
	
	
	public void add_formula(StateFormula f) {
		if(f instanceof Forall) {
			PathFormula pf = ((Forall) f).arg();
			if(pf instanceof Globally) {
			
				StateFormula ag_formula = (StateFormula) ((Globally) pf).arg();
				
				// if we have an AG-type formula we add it as a global rule
				if(ag_formula instanceof Implication) {
					// if its an implication we split it into guard and command
					StateFormula guard = ((Implication) ag_formula).arg_left();
					StateFormula command = ((Implication) ag_formula).arg_right();
					
					_global_rules.put(guard,command);
				} else if (ag_formula instanceof Equivalence) {
					// if its an equivalence we generate duplicate the above processing
					StateFormula left = ((Equivalence) ag_formula).arg_left();
					StateFormula right = ((Equivalence) ag_formula).arg_right();
					
					_global_rules.put(left,right);
					_global_rules.put(right, left);
				} else {
					_global_rules.put(new True(),ag_formula);
				}
			} else {
				_init.add(f);
			}
		} else {
			_init.add(f);
		}
	}
	
	
	@Override
	public String toString() {
		String res = "";
		res += "\tinterface:\n";
		res += "\t\t" + _interface + "\n";
		res += "\tinitial_state:\n";
		for(StateFormula s : _init) 
			res += "\t\t" + s.toString() + "\n";
		res += "\tglobal_rules:\n";
		for(Entry e : _global_rules.entrySet()) 
			res += "\t\t" + e.getKey().toString() + " -> " + e.getValue().toString() + "\n";		
		res += "\tend\n";
		return res;
	}
	
}
