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

public class FaultInjectorIII {
	
	/*	Attributes 
	*/
	
	private Tableaux _t;
	
	private Set<Pair<AndNode,DeonticProposition>> _injected_faults;
	
	private Relation<AndNode,AndNode> _mask;
	
	
	/*	Constructor
	*/
	
	public FaultInjectorIII(Tableaux t) {
		this._t = t;
		this._injected_faults = new HashSet<>();
		
	}
	
	
	/*	Public API
	*/

	public Relation<AndNode,AndNode> run() {
		initialize_mask();
		Set<Pair<OrNode, AndNode>> gens = inject_generators();
		
		// Since many generator can share their injection points we
		// turn the info into a map: gen -> i_points
		Relation<OrNode,AndNode> gens_to_ipoints = new Relation<>();
		gens_to_ipoints.addAll(gens);
		
		int loop = 0;
		for(OrNode gen : gens_to_ipoints.domain()) {
			_t.to_dot("output/gtab/" + loop + "_pre.dot", Debug.full_node_render, _mask);
			guided_tableaux(gen, gens_to_ipoints.get(gen));		
			_t.to_dot("output/gtab/" + loop + "_post.dot", Debug.full_node_render, _mask);
			loop++;
		}
		
		
		System.out.println("check mask : " + check_mask());
		return _mask;
	}
	

	
	private void guided_tableaux(OrNode o, Set<AndNode> masks) {
		LinkedList<AndNode> ands = new LinkedList<>();
		LinkedList<OrNode> ors = new LinkedList<>();
		
		ands.addAll(expandOR(o));
		
		for(AndNode m : masks)
			for(AndNode a : ands)
				if(_t.sublabeling(m).equals(_t.sublabeling(a)))
					_mask.put(m, a);
				
		while(!ands.isEmpty()) {
			ors.addAll(expandAND(ands.pop()));
		}
		
		while(!ors.isEmpty()) {
			OrNode _o = ors.pop();
			
			Set<AndNode> new_masks = new HashSet<>();
			new_masks.addAll(_mask.stream()
					.filter(p -> _t.predecesors(_o).contains(p.second))
					.map(p -> p.first)
					.collect(Collectors.toSet())
					);
			
			Set<AndNode> aux = new_masks
					.stream()
					.map(a -> _t.postN(a))
					.reduce(make_set(),SetUtils::union);
			new_masks.addAll(aux);
			
			
			guided_tableaux(_o,new_masks);
		}
		
	}
	
	private Set<AndNode> expandOR(OrNode o) {
		List<TableauxNode> l = _t.expand(o, false);
		l.removeAll(_t.flush());
		//new DeletionRules(_t).apply();
		l = identify_redundancies(_t, l);
		l.retainAll(_t.frontier());
		Set<AndNode> ands = l.stream().map(x -> (AndNode) x).collect(Collectors.toSet());
		
		return ands;
	}
	
	
	private Set<OrNode> expandAND(AndNode a) {
		Set<OrNode> res = new HashSet<OrNode>();
		for(TableauxNode t : _t.expand(a))
			if(t instanceof OrNode)
				res.add((OrNode) t);
			else
				assert false;
		
		res.retainAll(_t.frontier());
		return res;		
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
		/*_mask = new Relation<>();
		
		for(AndNode n : _t.normal_nodes()) {
			_mask.put(n, n);
		}*/
		
		_mask = new MaskingCalculator(_t).compute();
	}
	
	
	/*	Injection of Generators
	 * 
	 *	Generator are regarded as the Or-Node that is added as an
	 *	immediate succesor of an And-Node, representing a fault.
	*/
	
	// Injects and returns the set of generators.
	private Set<Pair<OrNode, AndNode>> inject_generators() {
		Set<Pair<OrNode, AndNode>> result = new HashSet<>();
		Pair p;
		
		//while ((p = get_fault_injection_point()) != null)
			//result.addAll(inject_fault(p));
		
		return result;
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
	
	// Gets the next pair of (AndNode,ObligationFormula) that is suitable
	// for fault injection. That is: the node  
	private Pair<AndNode,DeonticProposition> get_fault_injection_point() {
		for(TableauxNode n : _t.get_graph().vertexSet()) {
			if(n instanceof AndNode) {
				// We check whether we have an AND-Node with obligations to violate.
				Set<DeonticProposition> obligations = new HashSet<DeonticProposition>();
				for(StateFormula f : n.formulas) {
					if(f instanceof DeonticProposition)
						if(prop_sat(n.formulas,((DeonticProposition) f).get_prop())) {
							Pair p = new Pair(n,f);
							if (!_injected_faults.contains(p)) {
								_injected_faults.add(p);
								return p;
							}
						}
				}
			}		
		}
		return null;
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
}
