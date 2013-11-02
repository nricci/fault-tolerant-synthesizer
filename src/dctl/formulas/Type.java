package dctl.formulas;

public enum Type {
	
	// Atoms
	TRUE,
	FALSE,
	PROPOSITION,
	
	// Propositional operators
	NEGATION,
	OR,
	AND,
	IMPLIES,
	
	// Quantifiers
	FORALL,
	EXISTS,
	PERMISSION,
	OBLIGATION,
	
	// Path operators
	NEXT,
	UNTIL,
	WEAKUNTIL,
	GLOBALLY,
	FUTURE;
	
	
	public boolean is_atom() {
		return this.equals(TRUE) 
				|| this.equals(FALSE)
				|| this.equals(PROPOSITION);
	}
	
	public boolean is_propositional_operator() {
		return this.equals(NEGATION)
				|| this.equals(OR)
				|| this.equals(AND)
				|| this.equals(IMPLIES);
	}
	
	public boolean is_quantifier() {
		return this.equals(FORALL)
				|| this.equals(EXISTS)
				|| this.equals(OBLIGATION)
				|| this.equals(PERMISSION);
	}
	
	public boolean is_path_operator() {
		return this.equals(NEXT)
				|| this.equals(UNTIL)
				|| this.equals(WEAKUNTIL)
				|| this.equals(GLOBALLY)
				|| this.equals(FUTURE);
	}
	
	public boolean is_unary() {
		return this.equals(NEXT)
				|| this.equals(FUTURE)
				|| this.equals(GLOBALLY)
				|| this.equals(NEGATION)
				|| this.is_quantifier();
	}
	
	public boolean is_binary() {
		return this.equals(OR)
				|| this.equals(AND)
				|| this.equals(IMPLIES)
				|| this.equals(UNTIL)
				|| this.equals(WEAKUNTIL);				
	}
	
	
	
}
