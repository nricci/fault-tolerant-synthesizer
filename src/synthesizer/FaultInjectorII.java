package synthesizer;

import static dctl.formulas.DCTLUtils.prop_sat;
import static util.SetUtils.intersection;
import static util.SetUtils.make_set;
import static util.SetUtils.minus;
import static util.SetUtils.union;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import dctl.formulas.And;
import dctl.formulas.DeonticProposition;
import dctl.formulas.Exists;
import dctl.formulas.False;
import dctl.formulas.Negation;
import dctl.formulas.Next;
import dctl.formulas.Or;
import dctl.formulas.Proposition;
import dctl.formulas.StateFormula;
import dctl.formulas.True;
import tableaux.AndNode;
import tableaux.OrNode;
import tableaux.Tableaux;
import tableaux.TableauxNode;
import util.Debug;
import util.Pair;
import util.Relation;
import util.SetUtils;

public class FaultInjectorII {
	
	private Tableaux _t;
	
	private Set<Pair> injected_faults;
	
	public FaultInjectorII(Tableaux t) {
		_t = t;
		injected_faults = new HashSet<Pair>();
	}

	public Relation<AndNode,AndNode> inject_faults() {
		boolean debug = true;
		
		// Injected Faults
		Set<Pair<AndNode,DeonticProposition>> injected_faults = new HashSet<>();
		// Pending Faults
		Set<Pair<AndNode,DeonticProposition>> pending_faults = 
				minus(detect_fault_injection_points(),injected_faults);
		// Masking Relation
		Relation<AndNode,AndNode> masked_by = new MaskingCalculator(_t).compute();
		// Nonmasking Faults
		Set<AndNode> nonmasking_faults = new HashSet<>();
		// Faults injected in a given step
		Set<OrNode> fault_generators;
		
		int step = 0;
		
		/*
		 * 	FAULT GENERATION LOOP
		 * 
		*/
		while(!pending_faults.isEmpty()) { 
			fault_generators = new HashSet<>();
			if(debug) _t.to_dot("output/inject_"+step+"_start.dot", Debug.node_render_min, masked_by);
			
			// INJECT GENERATORS
			while(!pending_faults.isEmpty()) {				
				Pair<AndNode,DeonticProposition> p = pending_faults.stream().findFirst().get();
				
				if(debug) System.out.println("[inject step " + step + "] : injecting " + p);
				
				fault_generators.addAll(inject_fault(p.first, p.second.get_prop()));				
				pending_faults.remove(p);
				injected_faults.add(p);
			}	
			if(debug) _t.to_dot("output/inject_"+step+"_gens.dot", Debug.node_render_min, masked_by);
			
			_t.do_tableau(false);
			if(debug) _t.to_dot("output/inject_"+step+"_tab.dot", Debug.node_render_min, masked_by);
			
			 masked_by = new MaskingCalculator(_t,masked_by).compute();
			
			 if(debug) _t.to_dot("output/inject_"+step+"_remask.dot", Debug.node_render_min, masked_by);

			pending_faults = 
					minus(detect_fault_injection_points(),injected_faults);
			
			step++;
		}
		
		
		
		
		
			/*List<TableauxNode> faults = new LinkedList();
			for(OrNode o : fault_generator) {
				faults.addAll(expand(o,false));
			}
			for(TableauxNode n : faults) {
				AndNode fault = (AndNode) n;	
				
				AndNode injection_point = p.first;
				AndNode i_mask = maskedBy.get(injection_point);
				if (i_mask == null)
					nonmasking_faults.add(fault);
					// Add EF(reach_somesucc), don't think its nessesary
				else {	
					Set<AndNode> candidates = succesors(i_mask)
							.stream()
							.map(x -> succesors(x))
							.reduce(make_set(), SetUtils::union)
							.stream()
							.map((TableauxNode x) -> (AndNode) x)
							.collect(Collectors.toSet());
					Optional<AndNode> mask = candidates
							.stream()
							.filter(x -> sublabeling(x).equals(sublabeling(n)))
							.findAny();
					if(mask.isPresent()) {
						maskedBy.put(fault, mask.get());
					} else {
						// Can't just add formulas to the node. It will change the hash
						// And throw graph library off.
						// WORKAROUND
						Set<StateFormula> forms = union(
								fault.formulas,
								recovery_formula(candidates.stream().map(x -> x.formulas).collect(Collectors.toSet()))
								);
						AndNode new_node = new AndNode(forms);
						graph.addVertex(new_node);
						for(TableauxNode pre : predecesors(fault, graph)) graph.addEdge(pre, new_node);
						for(TableauxNode post : succesors(fault, graph)) graph.addEdge(new_node, post);
						graph.removeVertex(fault);
						nonmasking_faults.add(new_node);
						assert(new_node.formulas.equals(forms));
					}
						
						
				}
				
			}			
			if(debug && i % step == 0)
				System.out.println("[step " + i + "] injected faults : " + faults);
			if(debug && i % step == 0) 
				faults.stream().forEach(x -> 
					System.out.println("\t " + x + " is masked by " + maskedBy.get(x))				
					);

			for (AndNode a : nonmasking_faults)
				assert graph.vertexSet().contains(a);
				
			if (debug && i % step == 0)
				System.out.println("[step " + i + "] non_masking_faults : " + nonmasking_faults);
			
			
			do_tableau(false);
			if (debug && i % step == 0)
				System.out.println("[step " + i + "] to_delete : " + to_delete);
			
			if (debug && i % step == 0) 
				System.out.println("[step " + i + "] delete : " + delete_inconsistent());
			pending_faults.remove(p);
			injected_faults.add(p);
				
			if (debug && i % step == 0)
				Debug.to_file(
					Debug.to_dot(this.graph, Debug.default_node_render), 
					"output/inject_step" + i + ".dot"
				);
			
			if (debug && i % step == 0) 
				System.out.println("[step " + i + "] nodes : " + graph.vertexSet().size()
					+ " injected_faults : " + injected_faults.size() + " pending faults : " 
					+ pending_faults.size()); 
			
			pending_faults.addAll(minus(detect_fault_injection_points(),injected_faults));
			
			if (debug && i % step == 0)
				System.out.println("[step " + i + "] new pending faults : " 
					+ pending_faults.size()); 
			commit();
			
			
			// Remove untolerated faults
			
			//for(AndNode f : intersection(maskedBy.keySet(),nonmasking_faults)) {
				//maskedby
			//}
			
			/* REDO THIS!!!
			 * 
			 * 
			 * 
			if (!intersection(masked_by.keySet(),nonmasking_faults).isEmpty()) {
				System.out.println("It pet : " + 
						intersection(masked_by.keySet(),nonmasking_faults)
						.stream()
						.map(x -> get_node(x))
						.collect(Collectors.toSet())
						);
				assert false;
			}
			
		}*/
		if (debug) System.out.println("non-masking faults : " + nonmasking_faults.size());
		if (debug) System.out.println("fault-injection success.");
		
		return masked_by;
	}
	
	
	
	
	
	
	
	private Set<OrNode> inject_fault(Pair<AndNode,DeonticProposition> p ) {
		return inject_fault(p.first,p.second.get_prop());
	}
	
	private Set<OrNode> inject_fault(AndNode n, StateFormula p) {
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
		
		//assert faulty_succs.size() == 1;
		
		Set<OrNode> res = new HashSet<>();
		for(OrNode f : faulty_succs) {
			res.add((OrNode) _t.add_node(f));//.get_graph().addVertex(f);
			_t.add_edge(n,f);//.get_graph().addEdge(n, f);
		}
	
		return res;
	}
	
	/* Returns a map which domain represents the set of nodes where it is posible to
	 * inject faults. And the image of a node is a set of deontic variables to violate
	 * in order to generate a new fault.
	*/
	public Set<Pair<AndNode,DeonticProposition>> detect_fault_injection_points()
	{
		Set<Pair<AndNode,DeonticProposition>> res = new HashSet<>();
		for(TableauxNode n : _t.get_graph().vertexSet()) {
			if(n instanceof AndNode) {
				// We check whether we have an AND-Node with obligations to violate.
				Set<DeonticProposition> obligations = new HashSet<DeonticProposition>();
				for(StateFormula f : n.formulas) {
					if(f instanceof DeonticProposition)
						if(prop_sat(n.formulas,((DeonticProposition) f).get_prop()))
							res.add(new Pair(n,f));
				}
			}		
		}
		return res;
	}
	
	private StateFormula recovery_formula(Set<Set<StateFormula>> sets) {
		StateFormula res = new False();
		for(Set<StateFormula> s : sets) {
			StateFormula clause = s
					.stream()
					.filter(x -> x instanceof Proposition)
					.reduce(new False(), (x,y) -> new And(x,y));
			res = new Or(res, clause);
		}
		res = new Exists(new Next(res));
		return res;
	}
	
	
	

}