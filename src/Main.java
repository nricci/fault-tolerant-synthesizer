import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultEdge;

import parser.Parser;
import tableaux.AndNode;
import tableaux.OrNode;
import tableaux.Tableaux;
import tableaux.TableauxNode;
import util.binarytree.BinaryTree;
import dctl.formulas.*;

public class Main {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Set<StateFormula> s = Parser.parse_specification(args[0]);
			long start_time = System.currentTimeMillis();
			
			Tableaux t = new Tableaux(s);
			
			int stage = 0;
			//Tableaux.to_dot("output/tableaux" + (stage++) + ".dot",t.get_graph());
			
			int changes = 0;
			do {
				System.out.println("expand: " + t.expand() + " changes introduced.");
				//Tableaux.to_dot("output/tableaux" + (stage++) + ".dot",t.get_graph());
			} while (t.frontier());
			
			changes = 0;
			do {
				changes = t.delete_inconsistent();
				System.out.println("delete: " + changes + " nodes removed.");
				//Tableaux.to_dot("output/tableaux" + (stage++) + ".dot",t.get_graph());

			} while (changes > 0);
			
			//Tableaux.to_dot("output/tableaux" + (stage++) + ".dot",t.get_graph());
			System.out.println("delete: " + t.delete_unreachable() + " unreachable nodes removed.");
			System.out.println("deontic: " + t.detect_elementary_faults() + " faults detected.");
			System.out.println("deontic: " + t.inject_faults() + " faults injected.");
			
			while (t.frontier()) System.out.println("expand: " + t.expand() + " changes introduced.");
						
			
			
			Graph g = t.get_graph();
			HashMap<TableauxNode,String> tags = new HashMap<TableauxNode, String>();
			for(Object o : g.vertexSet()) {
				TableauxNode n = (TableauxNode) o;				
				tags.put(n, n.toString());	
			}
			Tableaux.to_dot_with_tags("output/final_tableaux.dot", t.get_graph(),tags);
			Tableaux.to_dot_with_tags_only_elem("output/final_tableaux_elem.dot", t.get_graph(),tags);
			
			System.out.println("tableaux stage " + stage);
			System.out.println("tableaux stage " + t.get_graph().vertexSet().size());
			
			
//			int i = 0;
//			
//			for(Object o : g.vertexSet()) {
//				TableauxNode n = (TableauxNode) o;				
//				if (n instanceof OrNode) continue;
//				/*
//				 * 		TESTING DAG(-,-)
//				 * 
//				*/
//				for(StateFormula f : n.formulas) {
//					if(f instanceof Quantifier) {
//						Quantifier q = (Quantifier) f;
//						if(q.arg() instanceof Until) {
//							try {
//								System.out.println("Testing dag with: " + n + " and formula " + f);
//								Tableaux.to_dot("output/dag" + f.toString() + "_" + n + ".dot", t.dag((AndNode) n,(Quantifier) f));
//							} catch (Exception e) {
//								e.printStackTrace();
//							}
//						}
//					}
//				}
//				/*
//				 * 		TESTING FRAG(-)
//				*/	
//				try {
//					System.out.println("Testing frag with: " + n);
//					Tableaux.frag_to_dot("output/frag" + n + ".dot", t.frag((AndNode) n));
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}

			long end_time = System.currentTimeMillis();
			System.out.println("total synthesis time: " +  (end_time - start_time) + " ms.");	
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}



}