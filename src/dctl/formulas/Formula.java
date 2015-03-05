package dctl.formulas;

import static util.SetUtils.minus;
import static util.SetUtils.pick;
import static util.SetUtils.union;
import static util.SetUtils.make_set;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import util.XMLBuilder;
import util.binarytree.Tree;

public abstract class Formula {
	
	// non-static interface
	
	public abstract boolean is_state_formula();

	public abstract boolean is_path_formula();
	
	public abstract boolean is_elementary();

	public abstract boolean is_alpha();

	public abstract boolean is_beta();
	
	public abstract Formula obligation_formula();
	
	public abstract boolean is_propositional();
	
	public boolean is_literal() {
		return false;
	}
	
	protected abstract boolean sat(Set<StateFormula> set);
	
	public abstract void to_xml(XMLBuilder b);
	

}
