package synthesizer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import util.Debug;
import dctl.formulas.*;
import static util.SetUtils.union;
import static util.SetUtils.make_set;
import static util.SetUtils.clone_set;


public class ProgramExtractor {
	
	DirectedGraph<ModelNode, DefaultEdge> _model;
		
	public ProgramExtractor(DirectedGraph<ModelNode, DefaultEdge> model) {
		_model = model;
	}
	
	
	public String extract_ftp() {
		DirectedGraph<ModelNode, Transition> program = new DefaultDirectedGraph<ModelNode, Transition>(Transition.class);
		
		// Set of bins for clasifying nodes acording to their sets of formulae
		// The new nodes (with only prop vars) will be put here
		Map<Set<StateFormula>, Set<ModelNode>> bins = new HashMap<>();
		
		Map<ModelNode,ModelNode> translation = new HashMap<>();
		
		for(ModelNode n : _model.vertexSet()) {
			Set<ModelNode> bin = bins.get(props(n));
			if(bin == null) {
				bin = new HashSet<ModelNode>();
			}
			Proposition ctrl_prop = new Proposition("_ctrl" + (bin.size() + 1));
			Set<StateFormula> new_formulas = props(n);
			new_formulas.add(ctrl_prop);
			ModelNode newnode = new ModelNode(new_formulas,n.faulty);
			translation.put(n, newnode);
			bin.add(newnode);
			bins.put(props(n), bin);			
		}
		
		for(Entry<?, Set<ModelNode>> e : bins.entrySet())
			for(ModelNode n : e.getValue())
				program.addVertex(n);
		
		for(DefaultEdge e : _model.edgeSet())
			program.addEdge(
					translation.get(_model.getEdgeSource(e)), 
					translation.get(_model.getEdgeTarget(e)),
					new Transition()
					);
		
		//int i = 0;
		//to_dot(program,"output/program_"+i++ +".dot");
		
		for(ModelNode n : program.vertexSet()) {
			Set<Transition> incoming = program.incomingEdgesOf(n);
			for(StateFormula f : n.formulas) {
				assert f instanceof Proposition || f instanceof Negation;
				if(f instanceof Proposition) {
					if(((Proposition) f).name().startsWith("_ctrl"))
						incoming.stream().forEach(t -> t.set_ctrl(
								Integer.parseInt(
										((Proposition) f).name().substring(5))
										)
								);
					else	
						incoming.stream().forEach(t -> t.add_update((Proposition) f, new True()));
				} else {
					incoming.stream().forEach(t -> t.add_update((Proposition)((Negation)f).arg(), new False()));
				}
			}
			
			Set<Transition> outgoing = program.outgoingEdgesOf(n);
			outgoing.forEach(t -> t.set_guard(clone_set(n.formulas)));
			
			n.formulas.clear();
			//to_dot(program,"output/program_"+i++ +".dot");
		}
		
		String res = program.edgeSet()
				.stream()
				.map(t -> t.toString() + "\n")
				.reduce("" , String::concat);
			
		return res;
	}
	
	
	// Get literals of a given state
	private Set<StateFormula> props(ModelNode n) {
		return n.formulas
				.stream()
				.filter(f -> f.is_literal())
				.collect(Collectors.toSet());
	}
	
	
	// For debbuging purposes
	private void to_dot(DirectedGraph<ModelNode, Transition> program, String path) {
		String res = "";	
		res += "digraph {\n";
		
		int i = 0;
		HashMap<ModelNode,String> map = new HashMap<ModelNode, String>();
		
		for(ModelNode n : program.vertexSet()) {
			map.put(n, "n"+i);
			
			// Rendering node
			res += "n"+i;
			res += "[shape=box" +
					(n.faulty?",style=dotted":"") +
					",label=\"" +
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
		for(Transition e : program.edgeSet()) {
			res += map.get(program.getEdgeSource(e)) + "->" + map.get(program.getEdgeTarget(e)) + 
					"[label=\"" + e.toString() + "\"];\n";
		}
	
		
		res += "}";		
		Debug.to_file(res, path);
	}
	
	
	
	
	
	

}
