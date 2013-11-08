package dctl.formulas;

public abstract class Quantifier extends StateFormula implements UnaryExpr {
	
	protected PathFormula _arg;
	
	@Override
	public final PathFormula arg() {
		return _arg;
	}

	@Override
	public boolean is_elementary() {
		return _arg.is_elementary();
	}

	@Override
	public boolean is_alpha() {
		return _arg.is_alpha();
	}

	@Override
	public boolean is_beta() {
		return _arg.is_beta();
	}

}
