/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dctl.formulas;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import util.Predicate;
import static util.SetUtils.make_set;
import static util.SetUtils.minus;
import static util.SetUtils.pick;
import static util.SetUtils.union;
import util.binarytree.Tree;

/**
 *
 * @author nricci
 */
public class DCTLUtils {
	
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
	
	public static Set<Set<StateFormula>> closure(Set<StateFormula> set) {
		System.out.println("set : \n"); 
		set.stream().forEach(x -> System.out.println(x));
		
		Set<Set<StateFormula>> res = Closure.closure(set);//closure_impl_2(set);
		
		System.out.println("res : \n"); 
		res.stream().forEach(x -> System.out.println(x));
		return res;
	}
	
	
	@SuppressWarnings("unchecked")
	private static Set<Set<StateFormula>> closure_impl_1(Set<StateFormula> set) {
        	assert(set != null);
		
		if(!is_consistent(set)) {
			return new HashSet<>();
		}
		
		Optional<StateFormula> op = set.stream().filter(x -> !x.is_elementary()).findAny();
		StateFormula f = op.isPresent()?op.get():null;
		if (f == null) 
			return make_set(set);
		else if (f.is_alpha()) {
			Set<StateFormula> temp = union(minus(set,f),f.get_decomposition());				
			Set<Set<StateFormula>> recursion_res = closure(temp);
			return recursion_res.stream().map(x -> union(set, x)).collect(Collectors.toSet());
		} else if(f.is_beta()) {
			Set<Set<StateFormula>> res = f.get_decomposition()
					.stream()
					.map(x -> union(minus(set,f),x))
					.map(x -> closure(x))
					.filter(x -> !x.isEmpty())
					.reduce(new HashSet<Set<StateFormula>>(), (x,y) -> union(x,y))
					.stream()
					.map(x -> union(set, x))
					.collect(Collectors.toSet());
			return res;
		} else {
			// Should not get here...
			throw new Error("Should not have got here: " + f + " is neither alpha nor beta. " +f.getClass());
		}		
	}
	
	// Auxiliary Predicates
	private static Predicate<StateFormula> is_non_elementary = new Predicate<StateFormula>() {
		public boolean eval(StateFormula _arg) {return !_arg.is_elementary();}
	};
	
	@SuppressWarnings("unchecked")
	private static Set<Set<StateFormula>> closure_impl_2(Set<StateFormula> set) {
		assert(set != null);
		boolean debug = true;
		
		Tree<Set<StateFormula>> decomp = new Tree(set);
		LinkedList<Tree<Set<StateFormula>>> frontier = new LinkedList();
		frontier.add(decomp);
		
		Set<Set<StateFormula>> closed_sets = new HashSet();
		int inconsistencies = 0;
		
		int loop = 0;
		while(!frontier.isEmpty()) {
			if(debug && (loop++ % 500) == 0) System.out.println(frontier.size());
			
			//assert deco_ok(decomp);
			Tree<Set<StateFormula>> current = frontier.pollLast();
			if(is_consistent(current.val())) {
				//StateFormula f = pick(current.val(),is_non_elementary);
				
				//Pick some formula prioritize alphas.
				StateFormula f, a_pick = null, b_pick = null;
				for(StateFormula _f : current.val()) {
					if(_f.is_alpha()) {
						a_pick = _f;
						break;
					}
					if(b_pick == null && _f.is_beta()) {
						b_pick = _f;
					}
				}
				f = (a_pick != null ? a_pick : b_pick);	
				//System.out.println("pick : " + f);
				if (f == null) { 
					closed_sets.add(current.val());
				} else if (f.is_alpha()) {
					current.set_left(
							new Tree(union(minus(current.val(),f),f.get_decomposition()))
					);
					frontier.push(current.left());
				} else if(f.is_beta()) {
					StateFormula[] deco = new StateFormula[2];
					int i = 0;
					for(StateFormula f2 : f.get_decomposition()) {
						deco[i] = f2;
						i++;
					}
					Tree<Set<StateFormula>> l = new Tree(union(minus(current.val(),f),deco[0]));
					Tree<Set<StateFormula>> r = new Tree(union(minus(current.val(),f),deco[1]));

					current.set_left(l);
					current.set_right(r);

					frontier.add(l);
					frontier.add(r);
				} else
					assert false;
			} else {
				inconsistencies++;
			}
		}
		
		if(debug) System.out.println("closure exit...\n"
				+ "\ttree size : " + decomp.size() + "\n"
				+ "\ttree height : " + decomp.height() + "\n"
				+ "\tclosed sets: " + closed_sets.size() + "\n"
				+ "\tinconcistencies: " + inconsistencies + "\n");
		return flatten(decomp);
	}
	
	private static Set<Set<StateFormula>> flatten(Tree<Set<StateFormula>> t) {
		boolean debug = true;
		
		LinkedList<Tree<Set<StateFormula>>> trees = new LinkedList<Tree<Set<StateFormula>>>();
		Set<Set<StateFormula>> res = new HashSet<Set<StateFormula>>();
		trees.add(t);
		
		int loop = 0;
		while(!trees.isEmpty()) {
			if(debug && (loop++ % 500) == 0) System.out.println(trees.size() + "\n" + res.size() + "\n");
			
			Tree<Set<StateFormula>> current = trees.pop();
			if(current.left() != null && current.right() != null) {
				current.left().val().addAll(current.val());
				trees.push(current.left());
				current.right().val().addAll(current.val());
				trees.push(current.right());
			} else if(current.left() != null) {
				current.left().val().addAll(current.val());
				trees.push(current.left());
			} else if(current.right() != null) {
				current.right().val().addAll(current.val());
				trees.push(current.right());
			} else {
				assert is_consistent(current.val());
				res.add(current.val());
			}			
		}
		
		if(debug) System.out.println("flattening ended. result size: " + res.size());
		return res;
	}
	
	private static boolean check_loop_variant(Tree<Set<StateFormula>> root) {
		boolean res = true;
		long non_elem_forms = root.val().stream().filter(x -> !x.is_elementary()).count();
		if(root.left() != null) {
			long left_non_elem_forms = root.left().val().stream().filter(x -> !x.is_elementary()).count();
			if(non_elem_forms <= left_non_elem_forms) {
				System.out.println(minus(root.val(),root.left().val()));
				System.out.println(minus(root.left().val(),root.val()));
				System.out.println();
				System.out.println(root.val().stream().map(x -> x.toString() + "\n" ).reduce(String::concat));
				System.out.println();
				System.out.println(root.left().val().stream().map(x -> x.toString() + "\n" ).reduce(String::concat));
				return false;
			}	
			else
				res &= check_loop_variant(root.left());
		}
		if(root.right() != null) {
			long right_non_elem_forms = root.right().val().stream().filter(x -> !x.is_elementary()).count();
			if(non_elem_forms <= right_non_elem_forms) {
				System.out.println(minus(root.val(),root.right().val()));
				System.out.println(minus(root.right().val(),root.val()));
				System.out.println();
				System.out.println(root.val().stream().map(x -> x.toString() + "\n" ).reduce(String::concat));
				System.out.println();
				System.out.println(root.right().val().stream().map(x -> x.toString() + "\n" ).reduce(String::concat));
				return false;
			} 
			else
				res &= check_loop_variant(root.right());
		}			
		return res;
	}
	
	
	
	@SuppressWarnings("unchecked")
	private static Set<Set<StateFormula>> closure_impl_3(Set<StateFormula> set) {
		assert(set != null);
		boolean debug = true;
		
		Tree<Set<StateFormula>> decomp = new Tree(set);
		LinkedList<Tree<Set<StateFormula>>> frontier = new LinkedList();
		frontier.add(decomp);
		
		Set<Set<StateFormula>> closed_sets = new HashSet();
		int inconsistencies = 0;
		
		
		
		int loop = 0;
		while(!frontier.isEmpty()) {
			if(debug && (loop++ % 500) == 0) System.out.println(frontier.size());
			
			//assert deco_ok(decomp);
			Tree<Set<StateFormula>> current = frontier.pop();
			if(is_consistent(current.val())) {
				//StateFormula f = pick(current.val(),is_non_elementary);
				
				//Pick some formula prioritize alphas.
				StateFormula f, a_pick = null, b_pick = null;
				for(StateFormula _f : current.val()) {
					if(_f.is_alpha()) {
						a_pick = _f;
						break;
					}
					if(b_pick == null && _f.is_beta()) {
						b_pick = _f;
					}
				}
				f = (a_pick != null ? a_pick : b_pick);	
				//System.out.println("pick : " + f);
				if (f == null) { 
					closed_sets.add(current.val());
				} else if (f.is_alpha()) {
					current.set_left(
							new Tree(union(minus(current.val(),f),f.get_decomposition()))
					);
					frontier.push(current.left());
				} else if(f.is_beta()) {
					StateFormula[] deco = new StateFormula[2];
					int i = 0;
					for(StateFormula f2 : f.get_decomposition()) {
						deco[i] = f2;
						i++;
					}
					Tree<Set<StateFormula>> l = new Tree(union(minus(current.val(),f),deco[0]));
					Tree<Set<StateFormula>> r = new Tree(union(minus(current.val(),f),deco[1]));

					current.set_left(l);
					current.set_right(r);

					frontier.add(l);
					frontier.add(r);
				} else
					assert false;
			} else {
				inconsistencies++;
			}
		}
		
		if(debug) System.out.println("closure exit... flattening tree. "
				+ "inconcistencies: " + inconsistencies);
		return flatten(decomp);
	}
    
    
    
    
    
    
    
    
    
    
    
}
