import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
			Tableaux t = new Tableaux(s);
			
			int stage = 0;
			//Tableaux.to_dot("output/tableaux" + (stage++) + ".dot",t.get_graph());
			
			int changes = 0;
			do {
				changes = t.expand();
				System.out.println("expand: " + changes + " changes introduced.");
				Tableaux.to_dot("output/tableaux" + (stage++) + ".dot",t.get_graph());
			} while (t.frontier());
			
			changes = 0;
			do {
				changes = t.delete_inconsistent();
				System.out.println("delete: " + changes + " nodes removed.");
				Tableaux.to_dot("output/tableaux" + (stage++) + ".dot",t.get_graph());

			} while (changes > 0);
			
			Tableaux.to_dot("output/tableaux" + (stage++) + ".dot",t.get_graph());
			System.out.println("delete: " + t.delete_unreachable() + " unreachable nodes removed.");
			Graph g = t.get_graph();
			HashMap<TableauxNode,String> tags = new HashMap<TableauxNode, String>();
			for(Object o : g.vertexSet()) {
				TableauxNode n = (TableauxNode) o;				
				tags.put(n, n.toString());	
			}
			Tableaux.to_dot_with_tags("output/final_tableaux.dot", t.get_graph(),tags);
			System.out.println(stage);
			
			
			int i = 0;
			
			for(Object o : g.vertexSet()) {
				TableauxNode n = (TableauxNode) o;				
				if (n instanceof OrNode) continue;
				/*
				 * 		TESTING DAG(-,-)
				 * 
				*/
				for(StateFormula f : n.formulas) {
					if(f instanceof Quantifier) {
						Quantifier q = (Quantifier) f;
						if(q.arg() instanceof Until) {
							try {
								System.out.println("Testing dag with: " + n + " and formula " + f);
								Tableaux.to_dot("output/dag" + f.toString() + "_" + n + ".dot", t.dag((AndNode) n,(Quantifier) f));
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
				/*
				 * 		TESTING FRAG(-)
				*/	
				try {
					System.out.println("Testing frag with: " + n);
					Tableaux.frag_to_dot("output/frag" + n + ".dot", t.frag((AndNode) n));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	public static String to_dot(String file, Graph graph) {
		DOTExporter<TableauxNode, DefaultEdge> expo = new DOTExporter<TableauxNode, DefaultEdge>(
				new VertexNameProvider<TableauxNode>() {
	
					
					@Override
					public String getVertexName(TableauxNode arg0) {
						int i = arg0.hashCode();
						if (i<0) i = i*-1;
						return "node"+i;
					}
				},
				new VertexNameProvider<TableauxNode>() {
	
					@Override
					public String getVertexName(TableauxNode arg0) {
						return arg0.formulas.toString().replace(',', '\n');
					}
				},
				new EdgeNameProvider<DefaultEdge>() {
	
					@Override
					public String getEdgeName(DefaultEdge arg0) {
						return "";
					}
				}			
				);
		try {
			expo.export(new FileWriter(new File(file)), graph);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	





}