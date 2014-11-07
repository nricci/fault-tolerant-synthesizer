package synthesizer;

import java.util.HashSet;
import java.util.Set;

import util.Pair;
import dctl.formulas.*;

public class Transition {

	private Set<StateFormula> _guard;
	
	private Set<Pair<Proposition,StateFormula>> _updates;
	
	private Integer _control_update;
	
	public Transition() {
		_guard = new HashSet<>();
		_updates = new HashSet<>();
		_control_update = null;
	}
	
	public Transition add_update(Proposition lhs, StateFormula rhs) {
		assert rhs.is_propositional() : "Invalid Transition rhs : " + rhs;
		_updates.add(new Pair(lhs,rhs));
		return this;
	}
	
	public Transition set_ctrl(int ctrl) {
		this._control_update = ctrl;
		return this;
	}
	
	public Transition set_guard(Set<StateFormula> g) {
		_guard = g;
		return this;
	}
	
	@Override
	public String toString() {
		return _guard.toString() + " -> ctrl := " + _control_update + ", " +
				_updates.stream()
				.map(p -> " " + p.first + " := " + p.second + " ")
				.reduce("", String::concat);
				
	}
	
	
}
