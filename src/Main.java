import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultEdge;

import com.sun.corba.se.spi.ior.MakeImmutable;

import synthesizer.FaultInjector;
import synthesizer.FaultInjectorII;
import synthesizer.NonMaskingCalculator;
import tableaux.ModelNode;
import parser.Parser;
import tableaux.AndNode;
import tableaux.OrNode;
import tableaux.Specification;
import tableaux.Tableaux;
import tableaux.TableauxNode;
import util.Debug;
import util.Pair;
import util.Relation;
import util.SetUtils;
import util.binarytree.Tree;
import dctl.formulas.*;
import static util.SetUtils.union;
import static util.SetUtils.intersection;

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
			
			Runtime.getRuntime().addShutdownHook(new Thread() {
				@Override
	            public void run() {
					
					System.out.print("End of Execution. ");
					/*try {
						System.out.print("Calling dot2jpeg.sh...\n");
						Runtime.getRuntime().exec("./dot2jpeg.sh");
					} catch (IOException e) {
						e.printStackTrace();
					}*/
	            }  
			});
			
			
			
			Specification s = Parser.parse_specification(args[0]);
			long start_time = System.currentTimeMillis();
			
			System.out.println("specification:\n\n" + s.toString());
			
			
			Tableaux t = new Tableaux(s);
			
			int stage = 0;
			//Tableaux.to_dot("output/tableaux" + (stage++) + ".dot",t.get_graph());
			
			int changes = 0;
			assert t.root != null;
			
			t.do_tableau(true);
			System.out.println("tableau finished: " +  t.get_graph().vertexSet().size() + " nodes, "
					+ t.get_graph().edgeSet().size() + " edges.");
			
			assert t.root != null;
			
			changes = 0;
			do {
				changes = t.delete_inconsistent();
				System.out.println("delete: " + changes + " nodes removed.");
				//Tableaux.to_dot("output/tableaux" + (stage++) + ".dot",t.get_graph());

			} while (changes > 0);
			assert t.root != null;
			//Tableaux.to_dot("output/tableaux" + (stage++) + ".dot",t.get_graph());
			System.out.println("delete: " + t.delete_unreachable() + " unreachable nodes removed.");
			System.out.println("deontic: " + t.detect_elementary_faults() + " faults detected.");
			
			
			System.out.println("tableau after delete: " +  t.get_graph().vertexSet().size() + " nodes, "
					+ t.get_graph().edgeSet().size() + " edges.");
			t.commit();		

			
			assert t.root != null;
			
			// ALTERNATIVAS PARA FALLAS
			
			
			System.out.print("[fault-injection] ... ");
			Relation<AndNode,AndNode> rel = new FaultInjectorII(t).inject_faults();
			System.out.println("done.");
			System.out.print("[non-masking relation] ... ");
			//Relation<AndNode,AndNode> rel =  new NonMaskingCalculator(t).compute();
			System.out.println("done.");
			
			for(TableauxNode n : t.get_graph().vertexSet()) {
				if (n instanceof AndNode && n.faulty) {
					Set<?> set = rel
							.stream()
							.filter(p -> p.first.equals(n))
							.map(p -> p.second)
							.filter(x -> !x.faulty)
							.collect(Collectors.toSet());
					if(set.isEmpty())
						System.out.println("Untollerated Fault : " + n);
				}
			}
			
			/*
			System.out.print("[fault-injection] ... ");
			t.inject_faults();
			System.out.println("done.");
			System.out.print("[non-masking relation] ... ");
			Relation<AndNode,AndNode> rel =  new Relation(t.masking_relation);
			System.out.println("Untollerated Fault : " + t.nonmasking_faults);
			System.out.println("done.");		
			*/
			
			t.to_dot("output/tableaux_nmask.dot", Debug.node_render_min, rel);
			
			
			
			Debug.to_file(
					Debug.to_dot(t.get_graph(), Debug.default_node_render, SetUtils.make_set()), 
					"output/final_tableaux.dot"
				);
			Debug.to_file(
					Debug.to_dot(t.get_graph(), Debug.node_render_min, SetUtils.make_set()), 
					"output/final_tableaux_min.dot"
				);	
			Debug.to_file(
					Debug.to_dot(t.extract_AND_induced_graph(t.get_graph()), Debug.node_render_min, rel), 
					"output/collapsed_tableaux_min.dot"
				);
			/*Debug.to_file(
					Debug.to_mega_dot(
							t.get_graph(), 
							Debug.node_render_min,
							t.masking_relation,
							t.nonmasking_faults
							), 
					"output/mega_tableaux.dot"
				);
			
		*/
			long end_time = System.currentTimeMillis();
			System.out.println("total synthesis time: " +  (end_time - start_time) + " ms.");
			System.out.println("final tableau : " +  t.get_graph().vertexSet().size() + " nodes, "
					+ t.get_graph().edgeSet().size() + " edges.");
			
			
			//DirectedGraph<ModelNode,DefaultEdge> model = t.extract_model();
			
			
			//assert union(t.masking_relation.keySet(),t.nonmasking_faults).equals(t.get_graph().vertexSet().stream().filter(x -> x instanceof AndNode).collect(Collectors.toSet()));
			
			//Debug.to_file(
			//		Debug.model_to_dot(model,t.masking_relation), 
			//		"output/model.dot"
			//	);
			
			
			
			
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	

	



}