import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import dctl.formulas.StateFormula;
import dctl.formulas.True;


public class FormulaTest {

	@Test
	public void test1() {
		StateFormula a = new True();
		StateFormula b = new True();
		assertEquals(a,b);
	}
	
	@Test
	public void test2() {
		Set<StateFormula> set = new HashSet<StateFormula>();
		StateFormula a = new True();
		StateFormula b = new True();
		set.add(a);
		set.add(b);
		assertTrue(set.contains(b));
		assertTrue(set.contains(a));
		assertTrue(set.size() == 1);
	}	
	
	
	

}
