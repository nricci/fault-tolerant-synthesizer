package tableaux;

import java.util.Set;
import dctl.formulas.Formula;
import dctl.formulas.StateFormula;


public class ModelNode {

	public Set<StateFormula> formulas;
	
	public TableauxNode copyOf;
	
	public boolean faulty;

	public ModelNode(TableauxNode n) {
		this.formulas = n.formulas;
		copyOf = n;
		faulty = n.faulty;
	}
	
	
}
