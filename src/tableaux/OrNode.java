package tableaux;

import java.util.HashSet;
import java.util.Set;
import dctl.formulas.Formula;

import util.*;
import static util.SetUtils.*;
import util.binarytree.BinaryTree;

public class OrNode extends TableauxNode {

	public OrNode(Set<Formula> s) {
		this.formulas = s;
	}
	
	public Set<AndNode> blocks() {
		Set<Set<Formula>> decomposition = new HashSet<Set<Formula>>();
		decomposition.add(this.formulas);
		
		
		
		SetUtils.filter(this.formulas, new Predicate<Formula>() {
			public boolean eval(Formula _arg) {return _arg.is_elementary();}
		});
		
		return null;
	}
	
	/*	Auxiliary Predicates
	 * 
	*/
	private final Predicate<Formula> is_non_elementary = new Predicate<Formula>() {
		public boolean eval(Formula _arg) {return !_arg.is_elementary();}
	};
	
	
	private Set<Set<Formula>> decompose(Set<Formula> set) {

		return null;
			
	}
	
	public Set<Set<Formula>> closure(BinaryTree<Set<Formula>> t) {
		Formula f = pick(t.val(),is_non_elementary);
		if (f == null) {
			Set<Set<Formula>> r = new HashSet<Set<Formula>>();
			r.add(t.val());
			return r;
		} else {
			if (f.is_alpha()) {
				BinaryTree<Set<Formula>> son = new 
			}
			
		}				
		return null;
	}
	
	
}
