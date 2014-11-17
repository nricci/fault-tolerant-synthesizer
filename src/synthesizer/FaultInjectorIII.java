package synthesizer;

import static dctl.formulas.DCTLUtils.prop_sat;
import static util.SetUtils.make_set;
import static util.SetUtils.union;

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

	public Relation<AndNode,AndNode> inject_faults() {
		initialize_mask();
		Set<Pair<OrNode, AndNode>> gens = inject_generators();
		
		// Since many generator can share their injection points we
		// turn the info into a map: gen -> i_points
		Relation<OrNode,AndNode> gens_to_ipoints = new Relation<>();
		gens_to_ipoints.addAll(gens);
		
		int loop = 0;
		for(OrNode gen : gens_to_ipoints.domain()) {
			_t.to_dot("output/gtab/" + loop + "_pre.dot", Debug.default_node_render, _mask);
			guided_tableaux(gen, gens_to_ipoints.get(gen));			
			_t.to_dot("output/gtab/" + loop + "_post.dot", Debug.default_node_render, _mask);
			loop++;
		}
		
		return _mask;
	}
	
	
	
	private void guided_tableaux(OrNode generator, Set<AndNode> ipoints) {
		LinkedList<TableauxNode> to_expand = new LinkedList<TableauxNode>();
		
		
		// We establish that the first generation of faulty nodes
		// Are masked by the injection points.
		List<?> l = _t.expand(generator, false);
		l.removeAll(_t.flush());		
		Set<AndNode> ands = l.stream().map(o -> (AndNode) o).collect(Collectors.toSet());
				
		for(AndNode a : ands)
			for(AndNode i : ipoints)
				_mask.put(a, i);
		
		to_expand.addAll(ands);
				
		while(!to_expand.isEmpty()) {
			if(to_expand.peek() instanceof AndNode) {
				AndNode n = (AndNode) to_expand.poll();
				to_expand.addAll(_t.expand(n));			
			} else if (to_expand.peek() instanceof OrNode) {
				OrNode n = (OrNode) to_expand.poll();
				
				// Tableaux expansion and remotion of any potentially
				// inconsistent nodes.
				l = _t.expand(generator, false);
				l.removeAll(_t.flush());		
				ands = l.stream().map(o -> (AndNode) o).collect(Collectors.toSet());
				
				for(AndNode a : ands) {
					Set<>_t.predecesors2(a);
					
					
					
					
				}
				
				
				
				
				
				to_expand.addAll(ands);
			} else 
				assert false : "Should not get here";
					
		}
		
		
		
		
	}
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	


	/* 	Initialization of normal nodes
	 * 
	 * 	Initially we set up the relation with the identity over
	 * 	normal nodes.
	*/	
	private void initialize_mask() {
		_mask = new Relation<>();
		
		for(AndNode n : _t.normal_nodes()) {
			_mask.put(n, n);
		}		
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
		
		while ((p = get_fault_injection_point()) != null)
			result.addAll(inject_fault(p));
		
		return result;
	}
	
	// Injects one fault
	private Set<Pair<OrNode, AndNode>> inject_fault(Pair<AndNode,DeonticProposition> p ) {
		return inject_fault(p.first,p.second.get_prop());
	}
	
	// Inject one fault
	private Set<Pair<OrNode, AndNode>> inject_fault(AndNode n, StateFormula p) {
		//assert n.formulas.contains(p);
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
		
		//System.out.println("deontically_affected_props : " + deontically_affected_props);
		
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
		
		// Override!!!
		next_obligations = make_set();
		
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
		Set<OrNode> faulty_succs = ghost.tiles();
		faulty_succs.stream().forEach(o -> o.faulty = true);
		//assert faulty_succs.size() == 1;
		
		Set<OrNode> res = new HashSet<>();
		for(OrNode f : faulty_succs) {
			res.add((OrNode) _t.add_node(f));//.get_graph().addVertex(f);
			_t.add_edge(n,f);//.get_graph().addEdge(n, f);
		}
		
		return res.stream().map(o_node -> new Pair(o_node,n)).collect(Collectors.toSet());
	}
	
	// Gets the next pair of (Node,Obligation) that is suitable for fault injection 
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