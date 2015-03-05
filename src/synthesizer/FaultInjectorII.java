package synthesizer;

import static dctl.formulas.DCTLUtils.prop_sat;
import static util.SetUtils.make_set;
import static util.SetUtils.union;
import static util.SetUtils.minus;


import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import tableaux.*;
import util.*;
import dctl.formulas.*;
import static util.SetUtils.minus;
import static util.SetUtils.times;
import static util.SetUtils.lift;
import static util.SetUtils.intersection;


public class FaultInjectorII {
	
	/*	Attributes 
	*/
	
	private Tableaux _t;
	
	private Set<Pair<AndNode,DeonticProposition>> _injected_faults;
	
	private LinkedList<Pair<AndNode,DeonticProposition>> _injection_stack;
	
	private Relation<AndNode,AndNode> _mask;
	
	
	/*	Constructor
	*/
	
	public FaultInjectorII(Tableaux t) {
		this._t = t;
		this._injected_faults = new HashSet<>();
		this._injection_stack = new LinkedList<Pair<AndNode,DeonticProposition>>();
	}
	
	
	/*	Public API
	*/

	public Relation<AndNode,AndNode> run() {
		initialize_mask();
	
		_injection_stack.addAll(get_injection_points(_t.normal_nodes()));		
		
		int loop = 1;
		while(!_injection_stack.isEmpty()) {
			int inner = 0;
			
			// DEBUG
			System.out.println();
			System.out.println("loop " + loop);
			//System.out.println("injection stack = " + _injection_stack);
	
			_t.to_dot("output/gtab/" + loop + "_" + inner++ + "_entry.dot", Debug.node_render_no_AX_AG_OG, _mask);
			
			Pair<AndNode,DeonticProposition> injection_point = _injection_stack.pollLast();
			System.out.println(injection_point);
		
			// Copy of the tableaux's nodes. To rollback in case of unmasked faults.
			Set<TableauxNode> old_nodes = _t.nodes().stream().collect(Collectors.toSet());

			Set<AndNode> new_ands = new HashSet<>();
			new_ands.addAll(inject_fault(injection_point.first, injection_point.second));
			// DEBUG
			_t.to_dot("output/gtab/" + loop + "_" + inner++ + "_post_inject.dot", Debug.node_render_no_AX_AG_OG, _mask);
			
			for(AndNode _n : new_ands)
				new_ands.addAll(gtab(_n));
			
			// DEBUG
			_t.to_dot("output/gtab/" + loop + "_" + inner++ + "_post_gtab.dot", Debug.node_render_no_AX_AG_OG, _mask);
			
			// check _mask and filter out invalid pairs.
			filter_mask();
			
			// DEBUG
			_t.to_dot("output/gtab/" + loop + "_" + inner++ + "_post_filter.dot", Debug.node_render_no_AX_AG_OG, _mask);
			
			for(AndNode n : new_ands)
				if(_mask.pre_img(n).isEmpty()) {
					if(_t.normal_nodes().contains(n)) 
						assert false : "Deletion attempt of node " + n + "due to empty mask.";
					_t.delete_node(n);
					System.out.println("Untollerated fault : " + n);
				}
			
			new_ands = intersection(new_ands,_t.and_nodes());
					
			_t.to_dot("output/gtab/" + loop + "_" + inner++ + "_end_of_loop.dot", Debug.node_render_no_AX_AG_OG, _mask);
			
			_injection_stack.addAll(get_injection_points(new_ands));
			
			loop++;
		}
		_t.to_dot("output/gtab/" + loop + "_tableaux_final.dot", Debug.node_render_no_AX_AG_OG, _mask);
		
		System.out.println("check mask : " + check_mask());
		return _mask;
	}
	

	
	
	
	
	/** Routine for fixing redundant And nodes. Given a set of And nodes
	 * it checks for the existence of And nodes which may contain both
	 * OB(p) and OB(!p) obligations. Such nodes are considered redundant
	 * and deleted.
	 * 
	 * @param ands set of And Nodes to filtrate.
	 * @return set of And nodes that made it through the filtration. 
	*/
	private Set<AndNode> identify_redundancies(Set<AndNode> ands) {
		Set<AndNode> res = new HashSet<>();
		res.addAll(ands);
		for(AndNode n : ands) {
			Set<StateFormula> oblit = n.formulas
					.stream()
					.filter(f -> f instanceof DeonticProposition)
					.map(f -> (DeonticProposition) f)
					.map(ob -> ob.get_prop())
					.collect(Collectors.toSet());
			if(!DCTLUtils.is_consistent(oblit)) {
				_t.delete_node(n);
				res.remove(n);
			}
			
		}
		return res;
	}
	
	
	/**	Method for checking pairs of the masking relation stored in 
	 * the local field _mask. The pairs violating masking conditions
	 * are removed.
	*/	
	private void filter_mask() {
		Set<Pair<?,?>> to_remove = new HashSet<>();
				
		for(Pair<AndNode, AndNode> p : _mask) {
			AndNode s1 = p.first;
			AndNode s2 = p.second;
			
			// Condition 1 : labelings
			if(!_t.sublabeling(s1).equals(_t.sublabeling(s2))) {
				to_remove.add(p);
				continue;
			}
			
			// Condition 2
			for(AndNode _s1 : _t.postN(s1)) {
				boolean r = false;
				for(AndNode _s2 : _t.post(s2)) {
					if(_mask.contains(new Pair(_s1,_s2)))
						r = true;
				}	
				if(!r) {
					to_remove.add(p);
					continue;
				}
			}
			
			// Condition 3
			for(AndNode _s2 : _t.postN(s2)) {
				boolean r = false;
				for(AndNode _s1 : _t.postN(s1)) {
					if(_mask.contains(new Pair(_s1,_s2)))
						r = true;
				}
				if(!r) {
					to_remove.add(p);
					continue;
				}
			}
			
			// Condition 4
			for(AndNode _s2 : _t.postF(s2)) {
				boolean r = false;
				for(AndNode _s1 : _t.postN(s1)) {
					if(_mask.contains(new Pair(_s1,_s2)))
						r = true;
				}	
				if(!r && !_mask.contains(new Pair(s1,_s2))){
					to_remove.add(p);
					continue;
				}
			}
		}
		_mask.removeAll(to_remove);
	}
	
	/**	Method for checking validity of the masking relation stored in 
	 * the local field _mask.
	 * 
	 *  @return true iff _mask is and actual masking relation
	*/	
	private boolean check_mask() {
		
		for(Pair<AndNode, AndNode> p : _mask) {
			AndNode s1 = p.first;
			AndNode s2 = p.second;
			
			// Condition 1 : labelings
			if(!_t.sublabeling(s1).equals(_t.sublabeling(s2)))
				return false;
			
			// Condition 2
			for(AndNode _s1 : _t.postN(s1)) {
				boolean r = false;
				for(AndNode _s2 : _t.post(s2)) {
					if(_mask.contains(new Pair(_s1,_s2)))
						r = true;
				}	
				if(!r)
					return false; 
				
			}
			
			// Condition 3
			for(AndNode _s2 : _t.postN(s2)) {
				boolean r = false;
				for(AndNode _s1 : _t.postN(s1)) {
					if(_mask.contains(new Pair(_s1,_s2)))
						r = true;
				}
				if(!r)
					return false;
			}
			
			// Condition 4
			for(AndNode _s2 : _t.postF(s2)) {
				boolean r = false;
				for(AndNode _s1 : _t.postN(s1)) {
					if(_mask.contains(new Pair(_s1,_s2)))
						r = true;
				}	
				if(!r && !_mask.contains(new Pair(s1,_s2)))
					return false; 
			}
			
		}		
		return true;
	}
	
	
	
	
	
	
	
	
	
	


	/* 	Initialization of normal nodes
	 * 
	 * 	Initially we set up the relation with the identity over
	 * 	normal nodes.
	*/	
	private void initialize_mask() {		
		_mask = new MaskingCalculator(_t).compute();
	}
	
	
	
	/**
	 * Given an AndNode and an obligation to violate, it injects a fault (OrNode generator)
	 * characterizing the ocurrence of that fault and develops the or node into AndNode.
	 * Let n be the normal nodes aginst which the injection is performed, and let n' range
	 * through every fault injected, then _mask(n') = {n} + succs(n). 
	 * 
	 * @param n the AndNode where the fault is to be injected.
	 * @param ob the obligation to violate. 
	 * @return the Set<AndNode> set of actual faults.
	 */
	private Set<AndNode> inject_fault(AndNode n, DeonticProposition ob) {
		
		assert n.formulas.contains(ob); 
		
		StateFormula p = ob.get_prop();
		assert p.is_literal();		
				
		// Collecting propositions that are deontically affected
		Set<Proposition> deontically_affected_props = n.formulas
				.stream()
				.filter(x -> x instanceof DeonticProposition)
				.map(x -> (DeonticProposition) x)
				.map((DeonticProposition x) -> x.get_prop())
				.map(x -> (x instanceof Negation)?((Negation) x).arg():x)
				.map(x -> (Proposition) x)
				.collect(Collectors.toSet());
		
		// Collecting literals that are based on a deontically affected prop
		Set<StateFormula> next_literals = n.formulas
				.stream()
				.filter(x -> x.is_literal())
				.filter(x -> 
							(x instanceof Proposition && deontically_affected_props.contains((Proposition) x)) ||
							(x instanceof Negation && deontically_affected_props.contains(((Negation) x).arg()))
						
						)
				.map(x -> x.equals(p)?x.negate():x)
				.collect(Collectors.toSet());
		
		// Collecting obligations in the current state
		Set<StateFormula> next_obligations = n.formulas
				.stream()
				.filter(x -> x instanceof DeonticProposition)
				.collect(Collectors.toSet());
		
		// This is it. Every formula to be passed on to the next node must be here.
		Set<StateFormula> next_formulas = union(next_literals,next_obligations);
		StateFormula next_state_descriptor = 				
			new Exists(
				new Next(
						next_formulas.stream()
						.reduce(new True(), (x,y) -> new And(x,y))
						)
			);
		
		Set<StateFormula> new_forms = union(n.formulas, next_state_descriptor);
		
		AndNode ghost = new AndNode(new_forms);		
		// this needs to be done to get the generator and
		// filter out spurious succesors that will already be
		// in the tableaux
		Set<OrNode> faulty_succs = minus(ghost.tiles(), _t.succesors(n));
		assert faulty_succs.size() == 1;
		OrNode generator = faulty_succs.stream().findFirst().get();
		
		generator = (OrNode) _t.add_node(generator);
		_t.add_edge(n,generator);
		
		_injected_faults.add(new Pair(n,ob));
	
		// Expansion of the OrNode and filtration in case of
		// redundant AndNodes
		Set<AndNode> faults = _t.expand(generator, false)
				.stream()
				.map(x -> (AndNode) x)
				.collect(Collectors.toSet());
		faults = identify_redundancies(faults);
		
		//Update on _mask
		for(AndNode f : faults) {
			_mask.put(n, f);
			for(AndNode _n : _t.succesors2(n))
				if(!_n.faulty)
					_mask.put(_n, f);
		}
			
		return faults;		
	}
	
	/**	Permorms guided tableaux expansion on the given And node.
	 * The node's Or node succesors are generated, and then the 
	 * And succesors of these are generated in turn. The _mask relation
	 * is updated acordingly.
	 * 
	 * @param n the And node to expand.
	 * @return the set of new And nodes generated by the expansion.
	*/
	public Set<AndNode> gtab(AndNode n) {
		Set<TableauxNode> ors = _t.expand(n)
				.stream()
				.collect(Collectors.toSet());
		Set<TableauxNode> new_ors = ors.stream()
				.filter(x -> !_t.nodes().contains(x))
				.collect(Collectors.toSet());
		Set<TableauxNode> old_ors = minus(ors,new_ors);
		
		Set<AndNode> ands = new HashSet<>();
		for(TableauxNode o : new_ors)
			ands.add((AndNode) _t.expand((OrNode) o, false));
		for(TableauxNode o : old_ors)
			for(TableauxNode _n : _t.succesors(o))
				ands.add((AndNode) _n);
		
		_mask.addAll(times(_t.succesors2(_mask.pre_img(n)),ands));
		
		return ands;	
	}
	
	
	
	
	
	// Gets every obligation to violate for a given node
	private LinkedList<Pair<AndNode,DeonticProposition>> get_injection_points(Set<AndNode> ns) {
		LinkedList<Pair<AndNode,DeonticProposition>> res = new LinkedList();
			
		for(AndNode n : ns)
			res.addAll(get_injection_points(n));
				
		return res;
	}
	
	
	// Gets every obligation to violate for a given node
	private LinkedList<Pair<AndNode,DeonticProposition>> get_injection_points(AndNode n) {
		LinkedList<Pair<AndNode,DeonticProposition>> res = new LinkedList();
		
		for(StateFormula f : n.formulas)
			if(f instanceof DeonticProposition)
				if(prop_sat(n.formulas,((DeonticProposition) f).get_prop()))
					res.add(new Pair(n,f));
				
		res.removeAll(_injected_faults);
		return res;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
