package synthesizer;

import java.util.Set;

import tableaux.TableauxNode;
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
	
	public ModelNode(Set<StateFormula> fs, boolean faulty) {
		this.formulas = fs;
		copyOf = null;
		this.faulty = faulty;
	}

	@Override
	public String toString() {
		return formulas.toString();
	}
	
}
