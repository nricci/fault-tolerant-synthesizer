import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import parser.Parser;
import tableaux.AndNode;
import tableaux.OrNode;
import tableaux.Tableaux;
import util.binarytree.BinaryTree;
import dctl.formulas.*;

public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Set<StateFormula> s = Parser.parse_specification(args[0]);
			Tableaux t = new Tableaux(s);
			
			for(int i = 0; i<10; i++) t.expand();
			t.to_dot();
			System.out.println(t);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
