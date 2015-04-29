package synthesizer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import dctl.formulas.Exists;
import dctl.formulas.Forall;
import dctl.formulas.Quantifier;
import dctl.formulas.StateFormula;
import dctl.formulas.True;
import dctl.formulas.Until;
import tableaux.AndNode;
import tableaux.OrNode;
import tableaux.Tableaux;
import tableaux.TableauxNode;
import util.Debug;
import static util.SetUtils.minus;

public class NaiveModelExtractor {
	
	private Tableaux _t;
	
	private DirectedGraph<TableauxNode, DefaultEdge> graph;
	
	
	public NaiveModelExtractor(Tableaux t) {
		_t = t;
		graph = _t.get_graph();
	}
	
		
	public DirectedGraph<ModelNode, DefaultEdge> extract_model() {
		
		DirectedGraph<ModelNode, DefaultEdge> model = new DefaultDirectedGraph<ModelNode, DefaultEdge>(DefaultEdge.class);
		
		Map<AndNode, ModelNode> tab_to_model = new HashMap<>();
		
		for(AndNode n : _t.and_nodes()) {
			tab_to_model.put(n, new ModelNode(n));
			model.addVertex(tab_to_model.get(n));
		}
		
		for(AndNode n : _t.and_nodes()) {
			for(AndNode _n : _t.succesors2(n)) {
				model.addEdge(tab_to_model.get(n), tab_to_model.get(_n));
			}
		}
			
		return model;		
	}	
	
	

}
