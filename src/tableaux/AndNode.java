package tableaux;

import static util.SetUtils.minus;
import static util.SetUtils.pick;
import static util.SetUtils.union;
import static util.SetUtils.filter;
import static util.SetUtils.map;


import java.util.HashSet;
import java.util.Set;

import util.Function;
import util.Predicate;
import util.binarytree.BinaryTree;
import dctl.formulas.Exists;
import dctl.formulas.Forall;
import dctl.formulas.Formula;
import dctl.formulas.Next;
import dctl.formulas.Quantifier;
import dctl.formulas.StateFormula;

public class AndNode extends TableauxNode {
	
	public AndNode(Set<StateFormula> s) {
		this.formulas = s;
	}
	
	public Set<OrNode> tiles() {
		Set<OrNode> res = new HashSet<OrNode>();
		Set<Set<StateFormula>> succs = generate_succesors(formulas);
		for(Set<StateFormula> s : succs) {
			res.add(new OrNode(s));
		}	
		return res;
	}
	
	/*	Auxiliary Predicates
	 * 
	*/
	
	// Formulas of the type EX
	private Predicate<StateFormula> is_existential = new Predicate<StateFormula>() {
		public boolean eval(StateFormula _arg) {
			if (_arg instanceof Exists) {
				Exists e = (Exists) _arg;
				if(e.arg() instanceof Next)
					return true;
			}				
			return false;
		}
	};
	
	// Formulas of the type AX
	private Predicate<StateFormula> is_universal = new Predicate<StateFormula>() {
		public boolean eval(StateFormula _arg) {
			if (_arg instanceof Forall) {
				Forall e = (Forall) _arg;
				if(e.arg() instanceof Next)
					return true;
			}				
			return false;
		}
	};	
	
	
	private StateFormula remove_quantifier(StateFormula f) {
		try {
			Quantifier _f = (Quantifier) f;
			Next __f = (Next) _f.arg();
			return __f.arg();
		} catch (Exception e) { 
			return null; 
		}
	}
	
	private Function<StateFormula> remove_quantifier = new Function<StateFormula>() {
		@Override
		public StateFormula eval(StateFormula arg) {
			return remove_quantifier(arg);
		}		
	};
	
	public Set<Set<StateFormula>> generate_succesors(Set<StateFormula> s) {
		Set<StateFormula> existential_formulas = filter(s,is_existential);
		Set<StateFormula> universal_formulas = filter(s,is_universal);
		
		Set<Set<StateFormula>> res = new HashSet<Set<StateFormula>>();
		
		if (!existential_formulas.isEmpty()) {
			for(StateFormula f : existential_formulas) {
				res.add(union(map(universal_formulas,remove_quantifier), remove_quantifier(f)));
			}		
		}
		return res;
	}

	
	
}
