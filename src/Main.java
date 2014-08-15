import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultEdge;

import parser.Parser;
import tableaux.AndNode;
import tableaux.OrNode;
import tableaux.Specification;
import tableaux.Tableaux;
import tableaux.TableauxNode;
import util.Debug;
import util.Pair;
import util.binarytree.Tree;
import dctl.formulas.*;

public class Main {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {			
			// Make and clean output directory
			File out_dir = new File("output/");
			if(!out_dir.exists())
				out_dir.mkdir();
			else
				Runtime.getRuntime().exec("rm output/*");
			
			
			Specification s = Parser.parse_specification(args[0]);
			long start_time = System.currentTimeMillis();
			
			System.out.println("specification:\n\n" + s.toString());
			
			
			Tableaux t = new Tableaux(s);
			
			int stage = 0;
			//Tableaux.to_dot("output/tableaux" + (stage++) + ".dot",t.get_graph());
			
			int changes = 0;
			t.do_tableau();
			
			
			changes = 0;
			do {
				changes = t.delete_inconsistent();
				System.out.println("delete: " + changes + " nodes removed.");
				//Tableaux.to_dot("output/tableaux" + (stage++) + ".dot",t.get_graph());

			} while (changes > 0);
			
			//Tableaux.to_dot("output/tableaux" + (stage++) + ".dot",t.get_graph());
			System.out.println("delete: " + t.delete_unreachable() + " unreachable nodes removed.");
			System.out.println("deontic: " + t.detect_elementary_faults() + " faults detected.");
			
			t.commit();
			//t.generate_non_masking_faults();
			
			/*int fault_count = 0;
			do {
				fault_count = t.inject_faults().size();
				System.out.println("deontic: " + fault_count + " faults injected.");
				t.do_tableau();
			} while (fault_count > 0);*/
			
			t.inject_faults();
			
			

			//Tableaux.to_dot_with_tags("output/final_tableaux.dot", t.get_graph(), x -> true);
			Debug.to_file(
					Debug.to_dot(t.get_graph(), Debug.default_node_render), 
					"output/final_tableaux.dot"
				);
			Debug.to_file(
					Debug.to_dot(t.get_graph(), Debug.node_render_elem), 
					"output/final_tableaux_elem.dot"
				);
			//Tableaux.to_dot_with_tags("output/final_tableaux_elem.dot", t.get_graph(), x -> x.is_elementary());
			
			
			
			
			/*Debug.to_file(
					Debug.to_dot(t.get_graph(), Debug.default_node_render, t.non_masking_relation()), 
					"output/final_tableaux_nonmask.dot"
				);
			*/
			//System.out.println("tableaux size " + t.get_graph().vertexSet().size());

	
			
			
		
			long end_time = System.currentTimeMillis();
			System.out.println("total synthesis time: " +  (end_time - start_time) + " ms.");
			System.out.println("final tableau : " +  t.get_graph().vertexSet().size() + " nodes, "
					+ t.get_graph().edgeSet().size() + " edges.");
			
			//System.out.println("MegaTest : " + t.megatest());
			System.out.println("MegaTest OK? : " + t.megatestII());
			
			System.out.println("calling dot2jpeg...");
			Runtime.getRuntime().exec("./dot2jpeg.sh");
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	

	



}