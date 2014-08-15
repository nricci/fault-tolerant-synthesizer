package util;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import tableaux.AndNode;
import tableaux.TableauxNode;
import dctl.formulas.StateFormula;

public class Debug {
	
	public static final Function<TableauxNode,String> default_node_render = 
			(TableauxNode n) -> (
					"[shape=" + ((n instanceof AndNode)?"box":"circle") +
					(n.faulty?",style=dotted":"") +
					",label=\"" + n.toString() + "\n" +
					n.formulas
					.stream()
					.filter(x -> true)
					.map(x -> x.toString() + "\n")
					.sorted((String x, String y) -> y.length() - x.length())
					.reduce("",String::concat) 
					+ "\"];"
				);
			
	public static final Function<TableauxNode,String> node_render_elem = 
			(TableauxNode n) -> (
					"[shape=" + ((n instanceof AndNode)?"box":"circle") +
					(n.faulty?",style=dotted":"") +
					",label=\"" + n.toString() + "\n" +
					n.formulas
					.stream()
					.filter(x -> x.is_elementary())
					.map(x -> x.toString() + "\n")
					.sorted((String x, String y) -> y.length() - x.length())
					.reduce("",String::concat) 
					+ "\"];"
				);	
	

	public static String to_dot(
			DirectedGraph<TableauxNode, DefaultEdge> g,
			Function<TableauxNode, String> node_renderer
	) {
		String res = "";	
		res += "digraph {\n";
		
		int i = 0;
		HashMap<TableauxNode,String> map = new HashMap<TableauxNode, String>();
		
		for(TableauxNode n : g.vertexSet()) {
			map.put(n, "n"+i);
			res += "n"+i + node_renderer.apply(n) + "\n";
			i++;
		}
		res += "\n";
		for(DefaultEdge e : g.edgeSet()) {
			res += map.get(g.getEdgeSource(e)) + "->" + map.get(g.getEdgeTarget(e)) + ";\n";
		}
		res += "}";		
		return res;
	}
	
	public static String to_dot(
			DirectedGraph<TableauxNode, DefaultEdge> g,
			Function<TableauxNode, String> node_renderer,
			Set<Pair<AndNode,AndNode>> nonmask
			
	) {
		String res = "";	
		res += "digraph {\n";
		
		int i = 0;
		HashMap<TableauxNode,String> map = new HashMap<TableauxNode, String>();
		
		for(TableauxNode n : g.vertexSet()) {
			map.put(n, "n"+i);
			res += "n"+i + node_renderer.apply(n) + "\n";
			i++;
		}
		res += "\n";
		for(DefaultEdge e : g.edgeSet()) {
			res += map.get(g.getEdgeSource(e)) + "->" + map.get(g.getEdgeTarget(e)) + ";\n";
		}
		for(Pair p : nonmask) {
			res += map.get(p.first) + "->" + map.get(p.second) + "[style=dotted,color=red];\n";
		}
		
		res += "}";		
		return res;
	}
	

	
	
	
	
	
	
	
	
	public static void to_file(String content, String path) {
		if(path != null) {
			try {
				FileWriter w = new FileWriter(new File(path));
				w.write(content);
				w.close();
			} catch (Exception e) {
				System.out.println("Could not write to file.");
				e.printStackTrace();
			}
		}
	}
	
	
}
