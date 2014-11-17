package synthesizer;

import static dctl.formulas.DCTLUtils.prop_sat;
import static util.SetUtils.union;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.sun.corba.se.spi.ior.MakeImmutable;

import dctl.formulas.And;
import dctl.formulas.DCTLUtils;
import dctl.formulas.DeonticProposition;
import dctl.formulas.Exists;
import dctl.formulas.False;
import dctl.formulas.Negation;
import dctl.formulas.Next;
import dctl.formulas.Obligation;
import dctl.formulas.Or;
import dctl.formulas.Proposition;
import dctl.formulas.StateFormula;
import dctl.formulas.True;
import tableaux.AndNode;
import tableaux.OrNode;
import tableaux.Tableaux;
import tableaux.TableauxNode;
import util.Pair;
import static util.SetUtils.make_set;

public class FaultInjector {
	
	private Tableaux _t;
	
	private Set<Pair> injected_faults;
	
	public FaultInjector(Tableaux t) {
		_t = t;
		injected_faults = new HashSet<Pair>();
	}

	public void inject_faults() {
		Pair p;
		//System.out.println("\n injection\t\t OrNodes\t\t AndNodes\t\t Deletions\n");
		while ((p = get_fault_injection_point()) != null) {
			
			Set<OrNode> or_nodes = inject_fault(p);
			List<TableauxNode> and_nodes = _t.do_tableau(false);
			identify_redundancies(_t,and_nodes);
			
			
			int deletions = _t.delete_inconsistent();
			
			//System.out.print(p +"\t\t "+ or_nodes +"\t\t "+ and_nodes +"\t\t "+ deletions+"\n");			
		}
	}
	
	
	
	private void identify_redundancies(Tableaux t, List<TableauxNode> ands) {
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
			}
			
		}
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
		
		// Override!!!
		//next_obligations = make_set();
		
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
		new_forms = new_forms
				.stream()
				.filter(x -> !(x instanceof Obligation || x instanceof DeonticProposition))
				.collect(Collectors.toSet());
		
		AndNode ghost = new AndNode(new_forms);
		Set<OrNode> faulty_succs = ghost.tiles();
		faulty_succs.stream().forEach(o -> o.faulty = true);
		/*faulty_succs.stream().forEach(
				o -> o.formulas = o.formulas.stream().filter(
						f -> !(f instanceof DeonticProposition || f instanceof Obligation)
						)
						.collect(Collectors.toSet())
		);*/
		
		//assert faulty_succs.size() == 1;
		
		Set<OrNode> res = new HashSet<>();
		for(OrNode f : faulty_succs) {
			res.add((OrNode) _t.add_node(f));//.get_graph().addVertex(f);
			_t.add_edge(n,f);//.get_graph().addEdge(n, f);
		}
	
		return res;
	}
	
	private Pair<AndNode,DeonticProposition> get_fault_injection_point() {
		for(TableauxNode n : _t.get_graph().vertexSet()) {
			if(n instanceof AndNode) {
				// We check whether we have an AND-Node with obligations to violate.
				Set<DeonticProposition> obligations = new HashSet<DeonticProposition>();
				for(StateFormula f : n.formulas) {
					if(f instanceof DeonticProposition)
						if(prop_sat(n.formulas,((DeonticProposition) f).get_prop())) {
							Pair p = new Pair(n,f);
							if (!injected_faults.contains(p)) {
								injected_faults.add(p);
								return p;
							}
						}
				}
			}		
		}
		return null;
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
