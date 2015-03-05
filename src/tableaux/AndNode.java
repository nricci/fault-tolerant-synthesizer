package tableaux;

import static dctl.formulas.DCTLUtils.prop_sat;
import static util.SetUtils.minus;
import static util.SetUtils.pick;
import static util.SetUtils.union;
import static util.SetUtils.filter;
import static util.SetUtils.map;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import util.binarytree.Tree;
import dctl.formulas.*;

public class AndNode extends TableauxNode {
	

	
	public AndNode(Set<StateFormula> s) {
		this.id = id_gen++;
		faulty = false;
		this.formulas = s;
		formulas.add(new True());
		
		for(StateFormula f : this.formulas) {
			if(f instanceof DeonticProposition) {
				if(!prop_sat(this.formulas,((DeonticProposition) f).get_prop())) {
					faulty = true;
					break;	
				}
			}
		}	
	}
	
	public Set<OrNode> tiles() {
		Set<OrNode> res = new HashSet<OrNode>();

		Set<StateFormula> ax_formulas =  this.formulas
				.stream()
				.filter(f -> f instanceof Forall)
				.map(f -> ((Forall) f).arg())
				.filter(f -> f instanceof Next)
				.map(f -> ((Next) f).arg())
				.collect(Collectors.toSet());

		Set<StateFormula> ex_formulas = this.formulas
				.stream()
				.filter(f -> f instanceof Exists)
				.map(f -> ((Exists) f).arg())
				.filter(f -> f instanceof Next)
				.map(f -> ((Next) f).arg())
				.collect(Collectors.toSet());

		if (ex_formulas.isEmpty() && !ax_formulas.isEmpty())
			ex_formulas.add(new True());

		
		Set<Set<StateFormula>> _succs = ex_formulas
				.stream()
				.map(f -> union(ax_formulas, f))
				.collect(Collectors.toSet());	
			
		
		res = _succs.stream().map(s -> new OrNode(s)).collect(Collectors.toSet());
		
		return res;
		
	}
	
	

	@Override
	public String toString() {
		Integer.toUnsignedLong(id);
		return (faulty?"F":"") + "And-" + Integer.toHexString(this.hashCode());
	}
	
	
}
