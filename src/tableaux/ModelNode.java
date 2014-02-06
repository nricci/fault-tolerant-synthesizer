package tableaux;

import java.util.Set;
import dctl.formulas.Formula;
import dctl.formulas.StateFormula;


public class ModelNode {

	public Set<StateFormula> formulas;

	public ModelNode(TableauxNode n) {
		this.formulas = n.formulas;
	}
	
	
}
