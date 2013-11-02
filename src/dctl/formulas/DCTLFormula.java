package dctl.formulas;

import java.util.Set;

public interface DCTLFormula {
	
	public Type type();
	
	public boolean is_atom();
	
	public boolean is_unary();
	
	public boolean is_binary();
	
	public boolean is_state_formula();
	
	public boolean is_path_formula();
	
	public boolean is_elementary();
	
	public boolean is_alpha();
	
	public boolean is_beta();
	
	public DCTLFormula get_argument(int n);
	
	public String prop_name();

	public Set<DCTLFormula> get_decomposition();

}
