/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package dctl.formulas;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import util.Pair;
import static util.SetUtils.union;
import static util.SetUtils.intersection;
import static util.SetUtils.make_set;

/**
 *
 * @author nricci
 */
public class Closure {
	
	
	/**
	 *
	 *	Special kind of set for easy implementation of the closure algorithm.
	 *	 
	 */	
	private static class CLSet {
		
		Set<StateFormula> alpha;
		
		Set<StateFormula> beta;
				
		Set<StateFormula> used;
		
		/* ****************** */
		
		Set<Proposition> pvars;
		
		Set<Proposition> nvars;
		
		boolean consistent;
		
		/*
		 *	Constructor
		*/
		public CLSet(Set<StateFormula> set) {
			used = new HashSet<>();
			alpha = new HashSet<>();
			beta = new HashSet<>();
			pvars = new HashSet<>();
			nvars = new HashSet<>();
			consistent = true;
			if(set != null) {
				for(StateFormula f : set) {
					add(f);
				}
			}
			check_consistent();
		} 
	
		
		/**
		 *	Returns the set of formulas represented by this set. 
		*/
		public Set<StateFormula> formulas() {
			assert closed() : "Formulas available only when closed.";
			assert consistent : "The target set is inconsistent";
			return used;
		}
		
		/**
		 *	Returns true iff the current set is closed.
		*/
		public boolean closed() {
			assert consistent : "The target set is inconsistent";
			return alpha.isEmpty() && beta.isEmpty();
		}
		
		/**
		 *	Adds the given formula to the CLSet.
		*/
		private void add(StateFormula f) {
			if(f.is_alpha())
				alpha.add(f);
			else if (f.is_beta())
				beta.add(f);
			else if (f.is_elementary()) {
				used.add(f);
				if(f.is_literal()) {
					if (f instanceof Proposition) {
						pvars.add((Proposition) f);
						if(nvars.contains((Proposition) f))
							consistent = false;
					}	
					if (f instanceof Negation) {
						Proposition p =(Proposition)((Negation)f).arg();
						nvars.add(p);
						if(pvars.contains(p))
							consistent = false;
					}
				}
			} 
			else assert false : "Corrupt formula";
		}
		
		/**
		 * Performs one step in Fischer-Lander Closure. If the selected formula
		 * is a beta formula the set splits in two, one of them being the 
		 * current instance and the other the returned set. If the formula is
		 * alpha no splitting is needed and null is returned.
		 * 
		*/
		public CLSet process() {
			assert !closed() : "Can not process a closed set.";
			assert consistent : "The target set is inconsistent";
			
			if (!alpha.isEmpty()) {
				StateFormula f = alpha.iterator().next();
				alpha.remove(f);
				
				Pair<StateFormula,StateFormula> deco = 
						deco_to_pair(f.get_decomposition());
				add(deco.first);
				add(deco.second);
				
				return null;
			}
			if (!beta.isEmpty()) {
				StateFormula f = beta.iterator().next();
				beta.remove(f);
				
				Pair<StateFormula,StateFormula> deco = 
						deco_to_pair(f.get_decomposition());
				
				CLSet branch = this.clone();
				add(deco.first);
				branch.add(deco.second);
				
				return branch;
			}			
			return null;
		}
		
		@Override
		public CLSet clone() {
			CLSet res = new CLSet(null);
			res.alpha.addAll(this.alpha);
			res.beta.addAll(this.beta);
			res.used.addAll(this.used);
			res.nvars.addAll(this.nvars);
			res.pvars.addAll(this.pvars);
			res.consistent = this.consistent;
			
			res.rep_ok();
			return res;
		}
		
		
		/*
		 *	Utils
		 *
		*/
		
		Pair<StateFormula,StateFormula> deco_to_pair(Set<StateFormula> s) {
			assert s != null;
			assert s.size() == 2;
			
			Iterator<StateFormula> i = s.iterator();
			StateFormula fst = i.next();
			StateFormula snd = i.next();
					
			
			Pair<StateFormula,StateFormula> res = 
					new Pair<StateFormula,StateFormula>(fst,snd);
			return res;			
		}
		
		
		boolean rep_ok() {
			if (consistent != check_consistent()) return false;
			if (!intersection(alpha,beta).isEmpty()) return false;
			if (!intersection(alpha,used).isEmpty()) return false;
			if (!intersection(beta,used).isEmpty()) return false;
			
			return true;
		}
		
		boolean check_consistent() {
			for(Proposition p : pvars)
				if (nvars.contains(p)) return false;
			return true;
		}
		
		
		
		
	}
	
	
	
	
	/**
	 *
	 *	Computes the Fischer-Lander Closure of the given set of formulas.
	 */
	public static Set<Set<StateFormula>> closure(Set<StateFormula> set) {
		Set<Set<StateFormula>> res = closure_impl(set);
		return res;
	}
	
	
	/**
	 *
	 *	Closure implementation.
	 */
	private static Set<Set<StateFormula>> closure_impl(Set<StateFormula> set) {
		LinkedList<CLSet> _sets = new LinkedList<>();
		LinkedList<CLSet> _closedsets = new LinkedList<>();
		
		CLSet current = new CLSet(set);
		if(!current.consistent) return null;
		if(current.closed()) return make_set(current.formulas());
		_sets.push(current);
		
		while(!_sets.isEmpty()) {
			current = _sets.pop();
			
			// This should be an invariant over _sets.
			assert current.consistent && !current.closed();
			
			CLSet branch = current.process();
			
			if(current.consistent) {
				if (current.closed()) {
					_closedsets.push(current);
				} else {
					_sets.push(current);
				}
			}
			if (branch != null) {
				if(branch.consistent) {
					if (branch.closed()) {
						_closedsets.push(branch);
					} else {
						_sets.push(branch);
					}
				}
			}			
		}		
		
		return _closedsets
				.stream()
				.map(cl -> cl.formulas())
				.collect(Collectors.toSet());
	}
	
	
	
	
	
	
	
	
}
