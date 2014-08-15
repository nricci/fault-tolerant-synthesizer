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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((_arg == null) ? 0 : _arg.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Quantifier other = (Quantifier) obj;
		if (_arg == null) {
			if (other._arg != null)
				return false;
		} else if (!_arg.equals(other._arg))
			return false;
		return true;
	}

	@Override
	public StateFormula negate() {
		return new Negation(this);
	}
	
	
}
