import java.util.Set;

import parser.Parser;
import dctl.formulas.*;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Set<DCTLFormula> s = Parser.parse_specification(args[0]);
			for(DCTLFormula f : s) {
				System.out.println(f);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
