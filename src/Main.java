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
			
			int stage = 0;
			//t.to_dot("tableaux" + (stage++) + ".dot");
			
			int changes = 0;
			do {
				changes = t.expand();
				System.out.println("expand: " + changes + " changes introduced.");
				t.to_dot("tableaux" + (stage++) + ".dot");
			} while (t.frontier());
			
			changes = 0;
			do {
				changes = t.delete_inconsistent();
				System.out.println("delete: " + changes + " nodes removed.");
				//t.to_dot("tableaux" + (stage++) + ".dot");

			} while (changes > 0);
			
			t.to_dot("tableaux" + (stage++) + ".dot");
			System.out.println("delete: " + t.delete_unreachable() + " unreachable nodes removed.");
			
			t.to_dot("tableaux" + (stage++) + ".dot");
			//System.out.println(t);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
