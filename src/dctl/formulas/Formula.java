package dctl.formulas;

import java.util.HashSet;
import java.util.Set;

public class Formula implements DCTLFormula {
	
	private DCTLFormula[] args;
	
	private Type t;
	
	private String _prop_name;
	
	
	public Formula(Type t, String prop, DCTLFormula... args) throws Exception {
		if (t.is_atom()) {
			if (t.equals(Type.PROPOSITION)) 
				if (prop == null) 
					throw new Exception("Misusage of constructor: for creating a proposition formula the name of the proposition must be supplied.");
				else 
					_prop_name = prop;
			this.t = t;
		}
		else if (t.is_unary()) {
			if (args.length < 1)
				throw new Exception("Misusage of constructor: for creating a unary formula at least one argument must be supplied.");
			else
				this.t = t;
				this.args = new Formula[1];
				this.args[0] = args[0];	
		}
		else if (t.is_binary()) {
			if (args.length < 2)
				throw new Exception("Misusage of constructor: for creating a binary formula at least two arguments must be supplied.");
			else
				this.t = t;
				this.args = new Formula[2];
				this.args[0] = args[0];	
				this.args[1] = args[1];
		}
		else {
			throw new Error("The type must be exactly one of, atom, unary or binary. Check Implementation.");
		}
	}	

	@Override
	public boolean is_atom() {
		return this.t.is_atom();
	}

	@Override
	public boolean is_unary() {
		return this.t.is_unary();
	}

	@Override
	public boolean is_binary() {
		return this.t.is_binary();
	}

	@Override
	public boolean is_state_formula() {
		return !this.t.is_path_operator();
	}

	@Override
	public boolean is_path_formula() {
		return this.t.is_path_operator();
	}

	@Override
	public boolean is_elementary() {
		return this.t.equals(Type.NEXT) || this.is_atom();
	}

	@Override
	public boolean is_alpha() {
		if(this.t.equals(Type.NEGATION)) {
			return this.args[0].is_beta();
		} else if(this.t.equals(Type.AND) || this.t.equals(Type.FORALL)
				|| this.t.equals(Type.OBLIGATION)) {
			return true;
		}		
		return false;
	}

	@Override
	public boolean is_beta() {
		if(this.t.equals(Type.NEGATION)) {
			return this.args[0].is_alpha();
		} else if(this.t.equals(Type.EXISTS) || this.t.equals(Type.IMPLIES)
				|| this.t.equals(Type.OR) || this.t.equals(Type.PERMISSION)) {
			return true;
		}		
		return false;
	}

	@Override
	public DCTLFormula get_argument(int n) {
		return args[n]; 
	}

	@Override
	public Set<DCTLFormula> get_decomposition() {
		Set<DCTLFormula> deco = new HashSet<DCTLFormula>();
		return null;
	}

	@Override
	public Type type() {
		return this.t;
	}

	@Override
	public String prop_name() {
		if (this.t.equals(Type.PROPOSITION)) {
			return this._prop_name;
		}
		return null;
	}
	
	@Override
	public String toString() {
		String _op = null, _res = null;
		switch(this.t) {
			//Atoms
			case TRUE: _op = "True"; break;
			case FALSE: _op = "False"; break;
			case PROPOSITION: _op = this._prop_name; break;
			
			// Propositional operators
			case NEGATION: _op = "!"; break;
			case OR: _op = "||"; break;
			case AND: _op = "&&"; break;
			case IMPLIES: _op = "->"; break;
			
			// Quantifiers
			case FORALL: _op = "A"; break;
			case EXISTS: _op = "E"; break;
			case PERMISSION: _op = "P"; break;
			case OBLIGATION: _op = "O"; break;
			
			// Path operators
			case NEXT: _op = "X"; break;
			case UNTIL: _op = "U"; break;
			case WEAKUNTIL: _op = "W"; break;
			case GLOBALLY: _op = "G"; break;
			case FUTURE: _op = "F"; break;
		}
		
		if (this.t.is_atom()) {
			_res = _op;
		} else if (this.t.is_unary()) {
			_res = _op + "(" + this.args[0].toString() + ")";
		} else if (this.t.is_binary()) {
			_res = "(" + this.args[0].toString() + " " + _op + " " + this.args[1].toString() + ")";
		} 
		
		return _res;
	}
	

}
