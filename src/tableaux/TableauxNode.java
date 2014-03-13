package tableaux;

import java.util.Set;

import dctl.formulas.Formula;
import dctl.formulas.StateFormula;


public abstract class TableauxNode {

	public Set<StateFormula> formulas;
	
	public boolean faulty;

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (faulty ? 1231 : 1237);
		result = prime * result
				+ ((formulas == null) ? 0 : formulas.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TableauxNode other = (TableauxNode) obj;
		if (faulty != other.faulty)
			return false;
		if (formulas == null) {
			if (other.formulas != null)
				return false;
		} else if (!formulas.equals(other.formulas))
			return false;
		return true;
	}
	
	
	
}
