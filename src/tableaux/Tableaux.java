package tableaux;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import dctl.formulas.StateFormula;

public class Tableaux {
	
	private TableauxNode root;
	
	private LinkedList<TableauxNode> frontier;
	
	private DirectedGraph<TableauxNode, DefaultEdge> graph; 
	
	public Tableaux(Set<StateFormula> spec) {
		graph = new DefaultDirectedGraph(DefaultEdge.class);
		frontier = new LinkedList<TableauxNode>();
		root = new OrNode(spec);
		graph.addVertex(root);		
		frontier.add(root);
	}
	
	public void expand() {
		if (!frontier.isEmpty()) {
			TableauxNode n = frontier.poll();
			if (n instanceof OrNode) {
				OrNode _n = (OrNode) n;
				Set<AndNode> succ = _n.blocks();
				for(AndNode _m : succ) {
					if(!graph.containsVertex(_m)) {
						graph.addVertex(_m);
						frontier.add(_m);
					}
					graph.addEdge(_n, _m);
				}
				frontier.remove(_n);
			} 
			else if (n instanceof AndNode) {
				AndNode _n = (AndNode) n;
				Set<OrNode> succ = _n.tiles();
				for(OrNode _m : succ) {
					if(!graph.containsVertex(_m)) {
						graph.addVertex(_m);
						frontier.add(_m);
					}
					graph.addEdge(_n, _m);
				}
				frontier.remove(_n);			
			}
		}
	}
	
	
	public String to_dot() {
		DOTExporter<TableauxNode, DefaultEdge> expo = new DOTExporter<TableauxNode, DefaultEdge>(
				new VertexNameProvider<TableauxNode>() {

					
					@Override
					public String getVertexName(TableauxNode arg0) {
						return "" + arg0.hashCode();
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
			expo.export(new FileWriter(new File("tableaux.dot")), graph);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	

}
