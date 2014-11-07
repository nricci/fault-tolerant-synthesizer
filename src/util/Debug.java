package util;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import synthesizer.ModelNode;
import tableaux.AndNode;
import tableaux.OrNode;
import tableaux.TableauxNode;
import dctl.formulas.DeonticProposition;
import dctl.formulas.StateFormula;

public class Debug {
	
	public static final Function<TableauxNode,String> full_node_render = 
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
			
	public static final Function<TableauxNode,String> node_render_min = 
			(TableauxNode n) -> (
					"[shape=" + ((n instanceof AndNode)?"box":"circle") +
					(n.faulty?",style=dotted":"") +
					",label=\"" + n.toString() + "\n" +
					n.formulas
					.stream()
					.filter(x -> x.is_elementary())
					.filter(x -> x.is_literal() || x instanceof DeonticProposition)
					.map(x -> x.toString() + "\n")
					.sorted((String x, String y) -> y.length() - x.length())
					.reduce("",String::concat) 
					+ "\"];"
			);			
	
	public static final Function<ModelNode,String> model_node_render_min = 
					(ModelNode n) -> (
					"[shape=box" +
					(n.faulty?",style=dotted":"") +
					",label=\"" + n.hashCode() + "\n" +
					"copy=" + n.copyOf + "\n" +
					n.formulas
					.stream()
					.filter(x -> x.is_elementary())
					.filter(x -> x.is_literal() || x instanceof DeonticProposition)
					.map(x -> x.toString() + "\n")
					.sorted((String x, String y) -> y.length() - x.length())
					.reduce("",String::concat) 
					+ "\"];"
			);			
			
	public static final Function<TableauxNode,String> default_node_render = full_node_render;				
			

	public static <N> String to_dot(
			DirectedGraph<N, DefaultEdge> g,
			Function<N, String> node_renderer
	) {
		String res = "";	
		res += "digraph {\n";
		
		int i = 0;
		HashMap<N,String> map = new HashMap<N, String>();
		
		for(N n : g.vertexSet()) {
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
	
	
	public static String model_to_dot(
			DirectedGraph<ModelNode, DefaultEdge> g,
			Map<AndNode,AndNode> mask	
	) {
		String res = "";	
		res += "digraph {\n";
		
		int i = 0;
		HashMap<ModelNode,String> map = new HashMap<ModelNode, String>();
		
		for(ModelNode n : g.vertexSet()) {
			map.put(n, "n"+i);
			
			// Rendering node
			res += "n"+i;
			res += "[shape=box" +
					(!n.faulty?
							",style=filled,color=green"
							:
							mask.get(n.copyOf)!=null?
									",style=filled,color=yellow"
									:
									",style=filled,color=red"	
					) +
					",label=\"" + n.toString() + "\n" +
					n.formulas
					.stream()
					.filter(x -> x.is_elementary())
					.filter(x -> x.is_literal() || x instanceof DeonticProposition)
					.map(x -> x.toString() + "\n")
					.sorted((String x, String y) -> y.length() - x.length())
					.reduce("",String::concat) 
					+ "\"];";
			res += "\n";
						
			i++;
		}
		res += "\n";
		for(DefaultEdge e : g.edgeSet()) {
			res += map.get(g.getEdgeSource(e)) + "->" + map.get(g.getEdgeTarget(e)) + ";\n";
		}
		//for(Entry<AndNode,AndNode> p : mask.entrySet()) {
		//	res += map.get(p.getKey()) + "->" + map.get(p.getValue()) + "[style=dotted,color=red];\n";
		//}
		
		res += "}";		
		return res;
	}	
	
	
	
	
	
	
	
	
	
	
	
	public static void to_xml(DirectedGraph<TableauxNode, DefaultEdge> g, String path) {
		XMLBuilder builder = new XMLBuilder();
		int i = 0;
		HashMap<TableauxNode,String> map = new HashMap<TableauxNode, String>();
		for(TableauxNode n : g.vertexSet())
			map.put(n, "n"+i++);
		
		builder.open("tableau");
		// Nodes
		for(TableauxNode n : g.vertexSet()) {
			builder.open("node",
					"id", map.get(n),
					"type", n instanceof OrNode?"Or":"And",
					"faulty", "" + n.faulty
						);
			
			// Formulas
			builder.open("formulas");
			for(StateFormula f : n.formulas) {
				builder.add_text(f.toString());
				//builder.open("formula");
				//	f.to_xml(builder);
				//builder.close();
			}
			builder.close();
			
			// Node Successors
			builder.open("succesors");
			for(DefaultEdge _e : g.outgoingEdgesOf(n)) {
				TableauxNode _n = g.getEdgeTarget(_e);
				builder.open_self_close("node",
						"id", map.get(_n),
						"type", _n instanceof OrNode?"Or":"And",
						"faulty", "" + _n.faulty
							);
			}
			builder.close();
			
			builder.close();
		}
		
		builder.close();
		builder.to_file(path);
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
