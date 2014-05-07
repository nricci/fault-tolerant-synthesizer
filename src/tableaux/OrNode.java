package tableaux;

import java.util.HashSet;
import java.util.Set;

import dctl.formulas.Formula;
import dctl.formulas.StateFormula;
import dctl.formulas.True;
import util.*;
import static util.SetUtils.*;
import util.binarytree.BinaryTree;
import static tableaux.Tableaux.is_consistent;

public class OrNode extends TableauxNode {

	public OrNode(Set<StateFormula> s) {
		faulty = false;
		this.formulas = s;
		formulas.add(new True());
		is_non_elementary = new Predicate<StateFormula>() {
			public boolean eval(StateFormula _arg) {return !_arg.is_elementary();}
		};
	}
	
	public Set<AndNode> blocks() {
		
		Set<Set<StateFormula>> t = new HashSet<Set<StateFormula>>();
		t.add(formulas);
		Set<AndNode> result = new HashSet<AndNode>();		
		for(Set<StateFormula> s : closure(formulas)) {
			result.add(new AndNode(s));
		}		
		return result;
	}
	
	public Set<Set<StateFormula>> closure(Set<StateFormula> set) {
		BinaryTree<Set<StateFormula>> t = new BinaryTree<Set<StateFormula>>(formulas);
		return closure_impl_2(t);
		//Set<Set<StateFormula>> t = new HashSet<Set<StateFormula>>();
		//t.add(set);
		//return closure_impl_1(t);
	}
	
	/*	Auxiliary Predicates
	 * 
	*/
	private Predicate<StateFormula> is_non_elementary = new Predicate<StateFormula>() {
		public boolean eval(StateFormula _arg) {return !_arg.is_elementary();}
	};
	
	@SuppressWarnings("unchecked")
	public Set<Set<StateFormula>> closure_impl_1(Set<Set<StateFormula>> set) {
		boolean closed = false;
		while(!closed) {
			System.out.println(set);
			// Look for the first set with non_elementary formula to decompose.
			Set<StateFormula> set_to_process = null;
			StateFormula fla_to_process = null;
			
			for(Set<StateFormula> current_s : set) {
				for(StateFormula current_f : current_s) {
					if(!current_f.is_elementary()) {
						fla_to_process = current_f;
						set_to_process = current_s;
						break;
					}
				}
				if(fla_to_process != null) break;
			}
			
			assert((fla_to_process == null) == (set_to_process == null));
			
			if(fla_to_process == null) {
				closed = true;
			} else {
				// Proceed with closure
				if (fla_to_process.is_alpha())
					// The alpha case
				{
					Set<StateFormula> new_set = union(set_to_process,fla_to_process.get_decomposition());
					set = minus(set, set_to_process);
					set = union(set, new_set);
				} 
				else if(fla_to_process.is_beta())
					// The beta case
				{
					// Super atada con alambre. Prueba de que Java es una gar**a
					StateFormula[] deco = new StateFormula[2];
					int i = 0;
					for(StateFormula f : fla_to_process.get_decomposition()) {
						deco[i] = f;
						i++;
					}
					Set<StateFormula> new_set0 = union(set_to_process,deco[0]);
					Set<StateFormula> new_set1 = union(set_to_process,deco[1]);
					set = minus(set, set_to_process);
					set = union(set, new_set0);
					set = union(set, new_set1);
				} 
				else {
					// Just in case... Should not get here!
					throw new Error("Should not have got here: " + fla_to_process + 
							" is neither alpha nor beta. " +fla_to_process.getClass());
				}
			}
		}	
		return set;
	}
	
	
	
	
	
	public Set<Set<StateFormula>> closure_impl_2(BinaryTree<Set<StateFormula>> t) {
		if(!is_consistent(t.val())) return new HashSet<Set<StateFormula>>();
		
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
				
				Set<Set<StateFormula>> recursion_res = closure_impl_2(t.left());
				
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
				Set<StateFormula> l_temp = union(minus(t.val(),f),deco[0]);
				Set<StateFormula> r_temp = union(minus(t.val(),f),deco[1]);

				BinaryTree<Set<StateFormula>> l_son = new BinaryTree<Set<StateFormula>>(l_temp);
				t.set_left(l_son);
				
				BinaryTree<Set<StateFormula>> r_son = new BinaryTree<Set<StateFormula>>(r_temp);
				t.set_right(r_son);
				
				Set<Set<StateFormula>> recursion_res_l = closure_impl_2(l_son);
				Set<Set<StateFormula>> recursion_res_r = closure_impl_2(r_son);
				
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
				throw new Error("Should not have got here: " + f + " is neither alpha nor beta. " +f.getClass());
			}
			
		}
	}
	
	
	
	
	
}
