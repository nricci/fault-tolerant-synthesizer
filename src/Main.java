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

import synthesizer.*;
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
					try {
						System.out.print("Calling dot2jpeg.sh...\n");
						Runtime.getRuntime().exec("./dot2jpeg.sh");
						//Runtime.getRuntime().exec("echo hola");
					} catch (IOException e) {
						e.printStackTrace();
					}
	            }  
			});
			
			
			
			
			
			
			long start_time = System.currentTimeMillis();
			
			// Parsing
			Specification s = Parser.parse_specification(args[0]);
					
			// Tableaux
			boolean filter = Boolean.parseBoolean(args[2]);
			Tableaux t = new Tableaux(s);
			t.do_tableau(filter);
			
			System.out.println("tableau finished: " 
					+ t.get_graph().vertexSet().size() + " nodes, "
					+ t.get_graph().edgeSet().size() + " edges.");
			
			assert t.root != null;
			
			// Deletion
			int changes = new DeletionRules(t).apply().size();
			System.out.println("delete: " + changes + " nodes removed.");
			
			if(t.root == null) {
				System.out.println("Specification is inconsistent.");
				return;
			}

			System.out.println("tableau after delete: "
					+ t.get_graph().vertexSet().size() + " nodes, "
					+ t.get_graph().edgeSet().size() + " edges.");
			
			assert t.root != null;
	
			t.to_dot("output/tableaux.dot", Debug.default_node_render);
			t.to_dot("output/tableaux_min.dot", Debug.node_render_min);
			
			long end_time = System.currentTimeMillis();
			System.out.println("total synthesis time: " +  (end_time - start_time) + " ms.");
			System.out.println("final tableau : " +  t.get_graph().vertexSet().size() + " nodes, "
					+ t.get_graph().edgeSet().size() + " edges.");
			
			
			
			// Model Extraction
			ModelExtractor ex = new ModelExtractor(t);
			//NaiveModelExtractor ex = new NaiveModelExtractor(t);
			
			DirectedGraph<ModelNode,DefaultEdge> model = ex.extract_model();
			Debug.to_file(Debug.to_dot(model, Debug.model_node_render_min), "output/model.dot");
			
			
			// Program Extraction
			ProgramExtractor p = new ProgramExtractor(model);
			Debug.to_file(p.extract_ftp(), "output/program.txt");
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	

	



}