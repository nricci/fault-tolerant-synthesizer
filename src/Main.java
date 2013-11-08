import java.util.Set;

import parser.Parser;
import dctl.formulas.*;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Set<StateFormula> s = Parser.parse_specification(args[0]);
			for(StateFormula f : s) {
				System.out.println("Formula : " + f);
				System.out.println("Decomposition: "+f.is_elementary()+f.is_alpha()+f.is_beta());
				for(StateFormula g : f.get_decomposition())
					System.out.println("\t" + g.toString());
				System.out.println();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
