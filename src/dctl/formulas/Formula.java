package dctl.formulas;

import java.util.Set;

public class Formula implements DCTLFormula {
	
	private Formula[] args;
	
	private Type t;
	
	public Formula(Type t) {
		
	}
	
	public Formula(Type t, DCTLFormula arg) {
		
	}
	
	public Formula(Type t, DCTLFormula arg1, DCTLFormula arg2) {
		
	}
	

	@Override
	public boolean is_atom() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean is_unary() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean is_binary() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean is_state_formula() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean is_path_formula() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean is_elementary() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean is_alpha() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean is_beta() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public DCTLFormula get_argument(int n) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<DCTLFormula> get_decomposition() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Type type() {
		// TODO Auto-generated method stub
		return null;
	}

}
