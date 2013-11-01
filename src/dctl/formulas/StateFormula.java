package dctl.formulas;

public abstract class StateFormula implements DCTLFormula {

	public abstract boolean is_elementary();
	
	public abstract boolean is_alpha(); 
	
	public abstract boolean is_beta();
	
	
}
