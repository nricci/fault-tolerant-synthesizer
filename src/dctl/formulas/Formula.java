package dctl.formulas;

public abstract class Formula {

	public abstract boolean is_state_formula();

	public abstract boolean is_path_formula();
	
	public abstract boolean is_elementary();

	public abstract boolean is_alpha();

	public abstract boolean is_beta();
	
	public abstract Formula obligation_formula();
	
	public abstract boolean is_propositional();

	
}
