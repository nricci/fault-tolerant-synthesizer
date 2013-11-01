package dctl.formulas;

public class Next extends PathFormula implements DCTLUnaryExpression {

	@Override
	public boolean is_constant() {
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
	public DCTLExpression arg() {
		// TODO Auto-generated method stub
		return null;
	}

}
