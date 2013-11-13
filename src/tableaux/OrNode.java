package tableaux;

import java.util.HashSet;
import java.util.Set;
import dctl.formulas.Formula;
import dctl.formulas.StateFormula;

import util.*;
import static util.SetUtils.*;
import util.binarytree.BinaryTree;

public class OrNode extends TableauxNode {

	public OrNode(Set<StateFormula> s) {
		this.formulas = s;
		is_non_elementary = new Predicate<StateFormula>() {
			public boolean eval(StateFormula _arg) {return !_arg.is_elementary();}
		};
	}
	
	public Set<AndNode> blocks() {
		Set<Set<StateFormula>> decomposition = new HashSet<Set<StateFormula>>();
		decomposition.add(this.formulas);
		
		
		
		SetUtils.filter(this.formulas, new Predicate<StateFormula>() {
			public boolean eval(StateFormula _arg) {return _arg.is_elementary();}
		});
		
		return null;
	}
	
	/*	Auxiliary Predicates
	 * 
	*/
	private Predicate<StateFormula> is_non_elementary = new Predicate<StateFormula>() {
		public boolean eval(StateFormula _arg) {return !_arg.is_elementary();}
	};
	
	
	private Set<Set<Formula>> decompose(Set<Formula> set) {

		return null;
			
	}
	
	public Set<Set<StateFormula>> closure(BinaryTree<Set<StateFormula>> t) {
		System.out.println("Closure of : " + t.val());
		StateFormula f = pick(t.val(),is_non_elementary);
		if (f == null) {
			Set<Set<StateFormula>> r = new HashSet<Set<StateFormula>>();
			r.add(t.val());
			return r;
		} else {
			if (f.is_alpha()) {
				Set<StateFormula> temp = union(minus(t.val(),f),f.get_decomposition());
				BinaryTree<Set<StateFormula>> son = new BinaryTree<Set<StateFormula>>(temp);
				t.set_left(son);
				
				Set<Set<StateFormula>> recursion_res = closure(t.left());
				
				Set<Set<StateFormula>> res = new HashSet<Set<StateFormula>>();
				for (Set<StateFormula> s : recursion_res) {
					res.add(union(s,t.val()));
				}
				return res;
			} else if(f.is_beta()) {
				
				StateFormula[] deco = new StateFormula[2];
				int i = 0;
				for(StateFormula f2 : f.get_decomposition()) {
					deco[i] = f2;
					i++;
				}
				System.out.println(deco);
				Set<StateFormula> l_temp = union(minus(t.val(),f),deco[0]);
				Set<StateFormula> r_temp = union(minus(t.val(),f),deco[1]);

				BinaryTree<Set<StateFormula>> l_son = new BinaryTree<Set<StateFormula>>(l_temp);
				t.set_left(l_son);
				
				BinaryTree<Set<StateFormula>> r_son = new BinaryTree<Set<StateFormula>>(r_temp);
				t.set_right(r_son);
				
				Set<Set<StateFormula>> recursion_res_l = closure(l_son);
				Set<Set<StateFormula>> recursion_res_r = closure(r_son);
				
				Set<Set<StateFormula>> res = new HashSet<Set<StateFormula>>();
				for (Set<StateFormula> s : recursion_res_l) {
					res.add(union(s,t.val()));
				}
				for (Set<StateFormula> s : recursion_res_r) {
					res.add(union(s,t.val()));
				}				
				return res;
			} else {
				// Should not get here...
				throw new Error("Should not have got here...");
			}
			
		}
	}
	
	
}
