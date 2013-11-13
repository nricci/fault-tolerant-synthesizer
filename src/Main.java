import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import parser.Parser;
import tableaux.OrNode;
import util.binarytree.BinaryTree;
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
				if(!f.is_elementary())
				for(StateFormula g : f.get_decomposition())
					System.out.println("\t" + g.toString());
					
				System.out.println();
			}
			OrNode n = new OrNode(s);
			
			System.out.println(n.closure(new BinaryTree<Set<StateFormula>>(s)));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
