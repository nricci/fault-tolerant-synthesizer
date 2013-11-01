package dctl.formulas;

public class Until extends PathFormula implements DCTLBinaryExpression {

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
	public DCTLExpression arg1() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DCTLExpression arg2() {
		// TODO Auto-generated method stub
		return null;
	}

}
