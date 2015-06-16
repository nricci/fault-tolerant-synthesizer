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

public class FaultInjector {
	
	/*	Attributes 
	*/
	
	private Tableaux _t;
	
	private Set<Pair<AndNode,DeonticProposition>> _injected_faults;
	
	private LinkedList<Pair<AndNode,DeonticProposition>> _injection_stack;
	
	private Relation<AndNode,AndNode> _mask;
	
	
	/*	Constructor
	*/
	
	public FaultInjector(Tableaux t) {
		this._t = t;
		this._injected_faults = new HashSet<>();
		this._injection_stack = new LinkedList<Pair<AndNode,DeonticProposition>>();
		
	}
	
	
	/*	Public API
	*/

	public Relation<AndNode,AndNode> run() {
		initialize_mask();
	
		_injection_stack.addAll(get_injection_points(_t.normal_nodes()));		
	
		// Since many generator can share their injection points we
		// turn the info into a map: gen -> i_points
		Relation<OrNode,AndNode> gens_to_ipoints = new Relation<>();
		
		// Simulator set for each node
		Relation<AndNode,AndNode> _sim = new Relation<>();
		
		int loop = 1;
		while(!_injection_stack.isEmpty()) {

			// DEBUG
			//System.out.println();
			//System.out.println("loop " + loop);
			//System.out.println("injection stack = " + _injection_stack);
	
			//_t.to_dot("output/gtab/" + loop + "_entry.dot", Debug.full_node_render, _mask);
			
			
			
			Pair<AndNode,DeonticProposition> injection_point = _injection_stack.pollLast();
			//System.out.println(injection_point);
			
			
			inject_fault(injection_point.first, injection_point.second);

			// DEBUG
			//_t.to_dot("output/gtab/" + loop + "_inject.dot", Debug.full_node_render, _mask);
			
			Set<AndNode> new_nodes = new HashSet<>();
			for(TableauxNode n : identify_redundancies(_t, _t.do_tableau(false))) 
				if(n instanceof AndNode)
					new_nodes.add((AndNode) n);
			
			
			
			
			//DEBUG
			//System.out.println("new nodes = " + new_nodes);
	
			//_t.to_dot("output/gtab/" + loop + "_tableaux_expansion.dot", Debug.full_node_render, _mask);
			//_t.to_dot("output/gtab/" + loop + "_tableaux_final.dot", Debug.node_render_no_AX_AG_OG, _mask);
			
			
			_injection_stack.addAll(get_injection_points(new_nodes));
			
			loop++;
		}
		//_t.to_dot("output/gtab/" + loop + "_tableaux_final.dot", Debug.node_render_no_AX_AG_OG, _mask);
		
		MaskingCalculator m = new MaskingCalculator(_t);
		_mask = m.compute();
		//System.out.println("check mask : " + check_mask());
		return _mask;
	}
	

	
	
	
	
	
	
	private List<TableauxNode> identify_redundancies(Tableaux t, List<TableauxNode> ands) {
		List<TableauxNode> res = new LinkedList<>();
		res.addAll(ands);
		for(TableauxNode n : ands) {
			if(!(n instanceof AndNode)) continue;
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
	
	
	
	/*	Methods for checking validity of the
	 *	masking relation.
	 * 
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
	 * characterizing the ocurrence of that fault.
	 * @param n the AndNode where the fault is to be injected.
	 * @param ob the obligation to violate. 
	 * @return the OrNode generator of that fault.
	 */
	private OrNode inject_fault(AndNode n, DeonticProposition ob) {
		
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
	
		return generator;
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
