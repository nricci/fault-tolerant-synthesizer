package dctl.formulas;

import static util.SetUtils.minus;
import static util.SetUtils.pick;
import static util.SetUtils.union;
import static util.SetUtils.make_set;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import util.Predicate;
import util.binarytree.Tree;

public abstract class Formula {
	
	// non-static interface
	
	public abstract boolean is_state_formula();

	public abstract boolean is_path_formula();
	
	public abstract boolean is_elementary();

	public abstract boolean is_alpha();

	public abstract boolean is_beta();
	
	public abstract Formula obligation_formula();
	
	public abstract boolean is_propositional();
	
	public boolean is_literal() {
		return false;
	}
	
	protected abstract boolean sat(Set<StateFormula> set);
	
	
	
	
	// Static interface
	
	// Returns true iff the node is not immediately inconsistent 
	public static boolean is_consistent(Set<StateFormula> s) {
		for(StateFormula f : s) {
			if(f instanceof False)
				return false;
			StateFormula not_f = new Negation(f);
			if(s.contains(not_f))
				return false;
		}		
		return true;
	}
	
	public static boolean prop_sat(Set<StateFormula> set, StateFormula formula) {
		return formula.sat(set);
	}
	
	public static boolean is_closed(Set<StateFormula> set) {
		for(StateFormula s : set) {
			boolean contains_some = true;
			if(s.get_decomposition() != null) {
				contains_some = false;
				for(StateFormula sdeco : s.get_decomposition()) {
					if(set.contains(sdeco))
						contains_some = true;
				}
			}
			if(!contains_some)
				return false;
		}
		return true;
	}
	
	
	@SuppressWarnings("unchecked")
	public static Set<Set<StateFormula>> closure(Set<StateFormula> set) {
		//System.out.println("closure_runnerup");
		//System.out.println(set);
		
		assert(set != null);
		
		if(!is_consistent(set)) {
			//System.out.println("Prune!!!");
			return new HashSet<>();
		}
		
		Optional<StateFormula> op = set.stream().filter(x -> !x.is_elementary()).findAny();
		StateFormula f = op.isPresent()?op.get():null;
		//System.out.println(f);
		if (f == null) 
			return make_set(set);
		else if (f.is_alpha()) {
			Set<StateFormula> temp = union(minus(set,f),f.get_decomposition());
					
			Set<Set<StateFormula>> recursion_res = closure(temp);
			
			//Set<Set<StateFormula>> res = new HashSet<Set<StateFormula>>();
			//for (Set<StateFormula> s : recursion_res) {
			//	res.add(union(s,set));
			//}
			return recursion_res.stream().map(x -> union(set, x)).collect(Collectors.toSet());
		} else if(f.is_beta()) {
			
			/*Set<Set<StateFormula>> temp = f.get_decomposition().stream().map(x -> union(minus(set,f),x)).collect(Collectors.toSet());
			
			Set<Set<Set<StateFormula>>> temp2 = temp
					.stream()
					.map(x -> closure_runnerup(x))
					.filter(x -> !x.isEmpty())
					.collect(Collectors.toSet());
			
			if (temp2.isEmpty()) return new HashSet<Set<StateFormula>>();
			
			temp = temp2.stream().reduce(new HashSet<Set<StateFormula>>(), (Set<Set<StateFormula>> x, Set<Set<StateFormula>> y) -> union(x,y));
			
			temp = temp.stream().map(x -> union(set, x)).collect(Collectors.toSet());
			*/
			Set<Set<StateFormula>> res = f.get_decomposition()
					.stream()
					.map(x -> union(minus(set,f),x))
					.map(x -> closure(x))
					.filter(x -> !x.isEmpty())
					.reduce(new HashSet<Set<StateFormula>>(), (x,y) -> union(x,y))
					.stream()
					.map(x -> union(set, x))
					.collect(Collectors.toSet());
			
			//System.out.println(res);
			//System.out.println(temp);
			
			//assert (res.equals(temp));
			
			return res;
		} else {
			// Should not get here...
			throw new Error("Should not have got here: " + f + " is neither alpha nor beta. " +f.getClass());
		}		
	}
	
	
	
	/*	Auxiliary Predicates
	 * 
	*/
	/*private static Predicate<StateFormula> is_non_elementary = new Predicate<StateFormula>() {
		public boolean eval(StateFormula _arg) {return !_arg.is_elementary();}
	};
	
	@SuppressWarnings("unchecked")
	public static Set<Set<StateFormula>> closure_impl_1(Set<StateFormula> set) {
		//System.out.println("closure_impl_1");
		//System.out.println(set);
		
		assert(set != null);
		
		if(!is_consistent(set)) {
			//System.out.println("Prune!!!");
			return new HashSet<>();
		}
		
		StateFormula f = pick(set,is_non_elementary);
		//System.out.println(f);
		if (f == null) 
			return make_set(set);
		else if (f.is_alpha()) {
			Set<StateFormula> temp = union(minus(set,f),f.get_decomposition());
					
			Set<Set<StateFormula>> recursion_res = closure_impl_1(temp);
			
			Set<Set<StateFormula>> res = new HashSet<Set<StateFormula>>();
			for (Set<StateFormula> s : recursion_res) {
				res.add(union(s,set));
			}
			return res;
		} else if(f.is_beta()) {
		
			StateFormula[] deco = new StateFormula[2];
			int i = 0;
			for(StateFormula f2 : f.get_decomposition()) {
				deco[i] = f2;
				i++;
			}
			Set<StateFormula> l_temp = union(minus(set,f),deco[0]);
			Set<StateFormula> r_temp = union(minus(set,f),deco[1]);

			Set<Set<StateFormula>> recursion_res_l = closure_impl_1(l_temp);
			Set<Set<StateFormula>> recursion_res_r = closure_impl_1(r_temp);
			
			Set<Set<StateFormula>> res = new HashSet<Set<StateFormula>>();
			for (Set<StateFormula> s : recursion_res_l) {
				res.add(union(s,set));
			}
			for (Set<StateFormula> s : recursion_res_r) {
				res.add(union(s,set));
			}				
			return res;
		} else {
			// Should not get here...
			throw new Error("Should not have got here: " + f + " is neither alpha nor beta. " +f.getClass());
		}		
	}
	
	
	
	
	
	public static Set<Set<StateFormula>> closure_impl_2(Tree<Set<StateFormula>> t) {
		assert(t != null);
		
		if(!is_consistent(t.val())) return new HashSet<Set<StateFormula>>();
		
		StateFormula f = pick(t.val(),is_non_elementary);
		if (f == null) {
			Set<Set<StateFormula>> r = new HashSet<Set<StateFormula>>();
			r.add(t.val());
			return r;
		} else {
			if (f.is_alpha()) {
				Set<StateFormula> temp = union(minus(t.val(),f),f.get_decomposition());
				Tree<Set<StateFormula>> son = new Tree<Set<StateFormula>>(temp);
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

				Tree<Set<StateFormula>> l_son = new Tree<Set<StateFormula>>(l_temp);
				t.set_left(l_son);
				
				Tree<Set<StateFormula>> r_son = new Tree<Set<StateFormula>>(r_temp);
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
	}*/
	
	
	
	
	

	

	
}
