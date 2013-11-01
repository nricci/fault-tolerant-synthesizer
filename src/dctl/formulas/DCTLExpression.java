package dctl.formulas;

public interface DCTLExpression {

	/* Returns true iff the expression is 0-ary: 
	 * True, False and Proposition.
	*/
	public boolean is_constant();
	
	/* Returns true iff the expresion is unary:
	 * negation, next operator and quantifiers.
	*/	
	public boolean is_unary();
	
	/* Returns true iff the expression is binary:
	 * implication, until, etc.
	*/
	public boolean is_binary();
	
	
	public boolean is_state_formula();
	
	public boolean is_path_formula();	
	
	
}
