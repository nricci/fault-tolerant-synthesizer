package tableaux;

import java.util.HashSet;
import java.util.Set;

import dctl.formulas.Formula;
import dctl.formulas.StateFormula;
import dctl.formulas.True;
import util.*;
import static util.SetUtils.*;
import util.binarytree.Tree;
import static dctl.formulas.DCTLUtils.closure;

public class OrNode extends TableauxNode {

	public OrNode(Set<StateFormula> s) {
		this.id = id_gen++;
		faulty = false;
		this.formulas = s;
		formulas.add(new True());
	}
	
	public Set<AndNode> blocks() {
		
		Set<Set<StateFormula>> t = new HashSet<Set<StateFormula>>();
		t.add(formulas);
		Set<AndNode> result = new HashSet<AndNode>();		
		for(Set<StateFormula> s : closure(formulas)) {
			result.add(new AndNode(s));
		}		
		return result;
	}
	
	
	@Override
	public String toString() {
		return (faulty?"F":"") + "Or-" + id;
	}
	
	
	
	
}
