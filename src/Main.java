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
					} catch (IOException e) {
						e.printStackTrace();
					}
	            }  
			});
			
			long start_time = System.currentTimeMillis();
			
			// Parsing
			Specification s = Parser.parse_specification(args[0]);
			//System.out.println("specification:\n\n" + s.toString());
			
			
			
			// Tableaux
			Tableaux t = new Tableaux(s);
			t.do_tableau(true);
			
			//t.to_dot("output/tableaux.dot", Debug.default_node_render);
			System.out.println("tableau finished: " +  t.get_graph().vertexSet().size() + " nodes, "
					+ t.get_graph().edgeSet().size() + " edges.");
			
			assert t.root != null;
			
			
			
			// Deletion
			int changes = new DeletionRules(t).apply().size();
			System.out.println("delete: " + changes + " nodes removed.");
			
			if(t.root == null) {
				System.out.println("Specification is inconsistent.");
				return;
			}

			//t.to_dot("output/tableaux_after_delete.dot", Debug.default_node_render);
			System.out.println("tableau after delete: " +  t.get_graph().vertexSet().size() + " nodes, "
					+ t.get_graph().edgeSet().size() + " edges.");
			
			assert t.root != null;
			t.commit();		

			
			
			// ALTERNATIVAS PARA FALLAS
			int method = 2;
			Relation<AndNode,AndNode> rel = null;
			
			switch(method) {
			case 1:
				// ON THE FLY ORIGINAL
				
				System.out.print("[fault-injection (original)] ... ");
				t.inject_faults();
				System.out.println("done.");
				System.out.print("[non-masking relation] ... ");
				rel =  t.masking_relation;
				System.out.println("Untollerated Fault : " + t.nonmasking_faults);
				System.out.println("done.");
			break;
			case 2:
				// EL QUE FUNCA
				
				System.out.print("[fault-injection (good)] ... ");
				new FaultInjector(t).inject_faults();
				System.out.println("done.");
				System.out.print("[non-masking relation] ... ");
				rel =  new NonMaskingCalculator(t).compute();
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
			break;	
			case 3:
				// ON THE FLY NUEVO
				
				System.out.print("[fault-injection (new onthefly)] ... ");
				rel = new FaultInjectorII(t).inject_faults();
				System.out.println("done.");
				System.out.print("[non-masking relation] ... ");
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
			break;
			case 4:
				// Masking y nomask por iteraciones
				
				System.out.print("[fault-injection (TryIV)] ... ");
				
				t.to_dot("output/pre_faults.dot", Debug.default_node_render);
				rel = new FaultInjectorIII(t).inject_faults();
				t.to_dot("output/pos_faults.dot", Debug.default_node_render,rel);
				
				System.out.println("done.");
				System.out.print("[non-masking relation] ... ");
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
			break;
			
			default:
				assert false;	
			}
			
			assert rel != null;
			
			//t.to_dot("output/final_tableaux_rel.dot", Debug.node_render_min, rel);
			t.to_dot("output/final_tableaux.dot", Debug.default_node_render);
			t.to_dot("output/final_tableaux_elem.dot", Debug.node_render_elem);
			t.to_dot_levels_of_tolerance("output/levels.dot", null, rel);
			
		
			long end_time = System.currentTimeMillis();
			System.out.println("total synthesis time: " +  (end_time - start_time) + " ms.");
			System.out.println("final tableau : " +  t.get_graph().vertexSet().size() + " nodes, "
					+ t.get_graph().edgeSet().size() + " edges.");
			
			
			
			// Model Extraction
			ModelExtractor ex = new ModelExtractor(t);
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