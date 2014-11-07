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

public class ModelExtractor {
	
	private Tableaux _t;
	
	private DirectedGraph<TableauxNode, DefaultEdge> graph;
	
	
	public ModelExtractor(Tableaux t) {
		_t = t;
		graph = _t.get_graph();
	}
	
	
	
	/* *******************************************
	 * 
	 * 					DAG
	 * 
	 * ******************************************* 
	*/
	
	
	
	public DirectedGraph<TableauxNode, DefaultEdge> dag(AndNode n, Quantifier g) {
//		Map<TableauxNode,Integer> tag = new HashMap<TableauxNode,Integer>();
		Until u = (Until) g.arg();
		StateFormula f = u.arg_left();
		StateFormula h = u.arg_right();	
		
		if(g instanceof Forall) {
			Map<TableauxNode,Integer> graph_tagging = dag_tagAU(n, (Forall) g, f, h);
			//System.out.print("AU tagging: ");
			//System.out.println(graph_tagging);
			DirectedGraph<TableauxNode, DefaultEdge> dag = dag_extract_AU_dag(n, graph_tagging); 
			
			
			Map<TableauxNode,String> cast_graph_tagging = new HashMap<TableauxNode, String>();
			for(Entry<TableauxNode,Integer> e : graph_tagging.entrySet())
				cast_graph_tagging.put(e.getKey(), e.getValue()!=null?e.getValue().toString():"null");
			//to_dot_with_tags("output/graph_tagging_" + g + ".dot", this.graph, cast_graph_tagging);
				
			return extract_AND_induced_graph(dag);
		} else if(g instanceof Exists) {
			Map<TableauxNode,Integer> graph_tagging = dag_tagEU(n, (Exists) g, f, h);
			
			assert graph_tagging != null;
			// Assertion
			//for(TableauxNode k : this.nodes()) 
			//	assert graph_tagging.keySet().contains(k) : "graph tagging contains no mapping for " + k;
			
			DirectedGraph<TableauxNode, DefaultEdge> dag = dag_extract_EU_dag(n, graph_tagging); 
			return extract_AND_induced_graph(dag);
		}
		return null;		
	}
	
	/*
	 *  dag method helper functions.
	*/
	
	private Integer max(Integer x, Integer y) {
		if (x == null || y == null) return null;
		return (x<y)?y:x;
	}
	
	private Integer min(Integer x, Integer y) {
		if (x == null) return y;
		if (y == null) return x;
		return (x<y)?x:y;
	}
	
	private boolean lt(Integer x, Integer y) {
		if (x == null) return false;
		if (y == null) return true;
		return (x<y);
	}
	
	private boolean eq(Integer x, Integer y) {
		if (x == null ||y == null) return false;
		return x==y;
	}	
	
	private <T> Set<T> frontier(DirectedGraph<T, DefaultEdge> g) {
		HashSet<T> res = new HashSet<T>();
		for(T n : g.vertexSet())
			if(g.outgoingEdgesOf(n).isEmpty()) 
				res.add(n);
		return res;
	}

	private Set<TableauxNode> succesors(TableauxNode n, DirectedGraph<TableauxNode, DefaultEdge> graph) {
		HashSet<TableauxNode> res = new HashSet<TableauxNode>();
		for(DefaultEdge e : graph.outgoingEdgesOf(n))
			res.add(graph.getEdgeTarget(e));
		return res;
	}
	
	private Set<TableauxNode> predecesors(TableauxNode n, DirectedGraph<TableauxNode, DefaultEdge> graph) {
		HashSet<TableauxNode> res = new HashSet<TableauxNode>();
		for(DefaultEdge e : graph.incomingEdgesOf(n))
			res.add(graph.getEdgeSource(e));
		return res;
	}
	
	
	
	
	
	
	
	private DirectedGraph<TableauxNode, DefaultEdge> dag_extract_AU_dag(AndNode n, Map<TableauxNode,Integer> tag) {
		assert n != null;
		assert tag != null;
		//System.out.println("dag_extract_AU_dag hi");
		
		// Extracting the dag
		DirectedGraph<TableauxNode, DefaultEdge> _dag = new DefaultDirectedGraph<TableauxNode, DefaultEdge>(DefaultEdge.class);
		boolean _halt = true;
		
		_dag.addVertex(n);
		for(TableauxNode x : frontier(_dag))
			_halt &= x instanceof AndNode && tag.get(x) != null && tag.get(x) == 0;  
		
		while(!_halt) {
			for(TableauxNode x : frontier(_dag)) {
				if(x instanceof OrNode) {
					TableauxNode pick = succesors(x,graph).iterator().next();
					for(TableauxNode _x : succesors(x,graph))
						if(lt(tag.get(_x),tag.get(pick))) {
							pick = _x;
						} else if (eq(tag.get(_x),tag.get(pick))) {
							if(succesors(_x,graph).size() > succesors(pick,graph).size()) {
								pick = x;
							}
						}
					_dag.addVertex(pick);
					_dag.addEdge(x,pick);
				}
				if(x instanceof AndNode) {
					for(TableauxNode _x : succesors(x,graph)) {
						_dag.addVertex(_x);
						_dag.addEdge(x,_x);
					}
				}
			}			
			_halt = true;
			for(TableauxNode x : frontier(_dag))
				_halt &= x instanceof AndNode && tag.get(x) != null && tag.get(x) == 0;    				
		}
		
		//System.out.println("dag_extract_AU_dag bye");
		assert _dag != null;
		return _dag;
	}
	
	public Map<TableauxNode,Integer> dag_tagAU(AndNode n, Forall g, StateFormula f, StateFormula h) {
		Map<TableauxNode,Integer> tag = new HashMap<TableauxNode,Integer>();
		Until u = (Until) g.arg();
		assert u.arg_left() == f;
		assert u.arg_right() == h;
		assert !(h instanceof True);
		
//		System.out.println("tag AU - h : " + h);
		
		// Initialization for tagging. For the purposes of this algorithm we take null to be infinity.
		for(TableauxNode _n : graph.vertexSet())
			if(_n.formulas.contains(h) && _n instanceof AndNode)
				tag.put(_n, 0);
			else
				tag.put(_n, null);
		
		// Tagging the graph
		int passes = graph.vertexSet().size();
		while (passes > 0) {
			
//			Map<TableauxNode,String> cast_graph_tagging = new HashMap<TableauxNode, String>();
//			for(Entry<TableauxNode,Integer> e : tag.entrySet())
//				cast_graph_tagging.put(e.getKey(), e.getValue()!=null?e.getValue().toString():"null");
//			to_dot_with_tags("output/graph_tagging_" + g + "_" + (graph.vertexSet().size()-passes) + ".dot", this.graph, cast_graph_tagging);
			
			for(TableauxNode _n : graph.vertexSet()) {
				// A pass over the graph
				if(_n.formulas.contains(g) && tag.get(_n) == null) {
					if (_n instanceof AndNode) {
						boolean all_succ_set = true;
						for(TableauxNode __n : succesors(_n,graph))
							all_succ_set &= tag.get(__n) != null;
						if(all_succ_set && (_n.formulas.contains(f) || f instanceof True)) {
							if(!succesors(_n,graph).isEmpty()) {
								Integer i = -1;
								for(TableauxNode __n : succesors(_n,graph))
									i = max(i,tag.get(__n));
								tag.put(_n, 1 + i);
							}
						}
					} else if (_n instanceof OrNode) {
						boolean some_succ_set = false;
						for(TableauxNode __n : succesors(_n,graph))
							some_succ_set |= tag.get(__n) != null;
						if(some_succ_set) {
							if(!succesors(_n,graph).isEmpty()) {
								Integer i = null;
								for(TableauxNode __n : succesors(_n,graph))
									i = min(i,tag.get(__n));
								tag.put(_n, i);
							}
						}						
					}
				}
			// End of a pass
			}
			passes--;
		}
		
//		System.out.println("tag AU bye");
//		Map<TableauxNode,String> cast_graph_tagging = new HashMap<TableauxNode, String>();
//		for(Entry<TableauxNode,Integer> e : tag.entrySet())
//			cast_graph_tagging.put(e.getKey(), e.getValue()!=null?e.getValue().toString():"null");
//		to_dot_with_tags("output/graph_tagging_" + g + "_" + (graph.vertexSet().size()-passes) + ".dot", this.graph, cast_graph_tagging);
		
		assert tag != null;
		return tag;
	}
	
	public DirectedGraph<TableauxNode, DefaultEdge> extract_AND_induced_graph(DirectedGraph<TableauxNode, DefaultEdge> g) {
		LinkedList<TableauxNode> to_delete = new LinkedList<TableauxNode>();
		for(TableauxNode n : g.vertexSet())
			if(n instanceof OrNode) {
				for(TableauxNode from : predecesors(n,g))
					for(TableauxNode to : succesors(n,g))
						g.addEdge(from, to);
				to_delete.add(n);
			}
		g.removeAllVertices(to_delete);		
		return g;
	}
	
	
	
	private DirectedGraph<TableauxNode, DefaultEdge> dag_extract_EU_dag(AndNode n, Map<TableauxNode,Integer> tag) {
		DirectedGraph<TableauxNode, DefaultEdge> _dag = new DefaultDirectedGraph<TableauxNode, DefaultEdge>(DefaultEdge.class);
		
		int _halt = tag.get(n);
		
		_dag.addVertex(n);
		TableauxNode current = n;
		while(_halt > 0) {		
			assert current instanceof AndNode;
			
			// Given the current AndNode we find the minimum tag succesor.
			// And the one with maximum spread. Every OrNode succ is added.
			OrNode or_pick = (OrNode) succesors(current,graph).iterator().next();
			for(TableauxNode x : succesors(current,graph)) {
				assert x instanceof OrNode;				
				if(lt(tag.get(x),tag.get(or_pick))) {
					or_pick = (OrNode) x;
				} else if (eq(tag.get(x),tag.get(or_pick))) {
					if(succesors(x,graph).size() > succesors(or_pick,graph).size()) {
						or_pick = (OrNode) x;
					}
				}
				_dag.addVertex(x);
				_dag.addEdge(current,x);
			}
			
			assert or_pick instanceof OrNode;
			// Given the current OR pick we find the minimum tag succesor.
			AndNode and_pick = (AndNode) succesors(or_pick,graph).iterator().next();
			for(TableauxNode x : succesors(or_pick,graph)) {
				assert x instanceof AndNode;				
				if(lt(tag.get(x),tag.get(and_pick))) {
					and_pick = (AndNode) x;
				} else if (eq(tag.get(x),tag.get(and_pick))) {
					if(succesors(x,graph).size() > succesors(and_pick,graph).size()) {
						and_pick = (AndNode) x;
					}
				}
			}			
			_dag.addVertex(and_pick);
			_dag.addEdge(or_pick,and_pick);
			current = and_pick;	
			_halt--; 				
		}
		assert current instanceof AndNode;
		
		Set<TableauxNode> _dag_nodes = new HashSet<TableauxNode>(_dag.vertexSet());
		for(TableauxNode x : _dag_nodes) {
			if(succesors(x,_dag).isEmpty()) {
				if (x instanceof AndNode) 
					assert x == current;
				else {
					AndNode and_pick = (AndNode) succesors(x,graph).iterator().next();
					for(TableauxNode y : succesors(x,graph)) {
						assert y instanceof AndNode;				
						if(lt(tag.get(y),tag.get(and_pick))) {
							and_pick = (AndNode) y;
						} else if (eq(tag.get(y),tag.get(and_pick))) {
							if(succesors(y,graph).size() > succesors(and_pick,graph).size()) {
								and_pick = (AndNode) y;
							}
						}
					}
					_dag.addVertex(and_pick);
					_dag.addEdge(x,and_pick);
				}
			}
		}
		
		return _dag;
	}
	

	
	
	
	public Map<TableauxNode,Integer> dag_tagEU(AndNode n, Exists g, StateFormula f, StateFormula h) {
		Map<TableauxNode,Integer> tag = new HashMap<TableauxNode,Integer>();
		Until u = (Until) g.arg();
		assert u.arg_left() == f;
		assert u.arg_right() == h;
		
		// Initialization for tagging. For the purposes of this algorithm we take null to be infinity.
		for(TableauxNode _n : graph.vertexSet())
			if((_n.formulas.contains(h) || h instanceof True) && _n instanceof AndNode)
				tag.put(_n, 0);
			else
				tag.put(_n, null);
		
		// Tagging the graph
		int passes = graph.vertexSet().size();
		while (passes > 0) {
			for(TableauxNode _n : graph.vertexSet()) {
				// A pass over the graph
				if(_n.formulas.contains(g) && tag.get(_n) == null) {
					if (_n instanceof AndNode) {
						boolean some_succ_set = false;
						for(TableauxNode __n : succesors(_n,graph))
							some_succ_set |= tag.get(__n) != null;
						if(some_succ_set && (_n.formulas.contains(f) || f instanceof True)) {
							Integer i = null;
							for(TableauxNode __n : succesors(_n,graph))
								i = min(i,tag.get(__n));
							tag.put(_n, 1 + i);
						}
					} else if (_n instanceof OrNode) {
						boolean some_succ_set = false;
						for(TableauxNode __n : succesors(_n,graph))
							some_succ_set |= tag.get(__n) != null;
						if(some_succ_set) {
							Integer i = null;
							for(TableauxNode __n : succesors(_n,graph))
								i = min(i,tag.get(__n));
							tag.put(_n, i);
						}						
					}
				}
			// End of a pass
			}
			passes--;
		}
		return tag;
	}
	
	
	
	
	
	
	/* *******************************************
	 * 
	 * 					FRAG
	 * 
	 * ******************************************* 
	*/
	
	public DirectedGraph<ModelNode, DefaultEdge> frag(AndNode n) {		
		// Filtering eventuality formulas
		LinkedList<Quantifier> eventuality_formulas = new LinkedList<Quantifier>();
		for(StateFormula f : n.formulas) {
			if(f instanceof Quantifier) {
				Quantifier q = (Quantifier) f;
				if(q.arg() instanceof Until) {
					eventuality_formulas.add(q);
				}
			}
		}
		
		// Constructing Fragment
		DirectedGraph<ModelNode, DefaultEdge> res = new DefaultDirectedGraph<ModelNode, DefaultEdge>(DefaultEdge.class);
		if(!eventuality_formulas.isEmpty())	{
			DirectedGraph<TableauxNode, DefaultEdge> current_dag;
			Quantifier f = eventuality_formulas.poll();
			current_dag = dag(n,f);
			copy(res,null,current_dag,n);
			
			while(!eventuality_formulas.isEmpty()) {
				Quantifier current_formula = eventuality_formulas.poll();
				for(ModelNode m : frag_frontier(res)) {
					if(m.formulas.contains(current_formula)) {
						AndNode node_in_tableaux = pick_by_formula_set(m.formulas);
						assert node_in_tableaux != null;
						current_dag = dag(node_in_tableaux, current_formula);
						copy(res, m, current_dag, node_in_tableaux);
					}				
				}
			}		
		} else {
			ModelNode frag_root = new ModelNode(n);
			res.addVertex(frag_root);
			for(TableauxNode o : succesors(n, graph)) {
				assert graph.vertexSet().contains(o);
				//System.out.println("Node : " + o);
				ModelNode new_node = new ModelNode(succesors(o,graph).stream().findAny().get());
				res.addVertex(new_node);
				res.addEdge(frag_root, new_node);
			}
			
		}
		
		assert res.vertexSet().size() >= 1;
		assert root(res).copyOf.equals(n);
		return res;
	}	
	
	private Set<ModelNode> frag_frontier(DirectedGraph<ModelNode, DefaultEdge> g) {
		HashSet<ModelNode> res = new HashSet<ModelNode>();
		for(ModelNode n : g.vertexSet())
			if(g.outgoingEdgesOf(n).isEmpty()) 
				res.add(n);
		return res;
	}
	
	private AndNode pick_by_formula_set(Set<StateFormula> s) {
		for(TableauxNode n : graph.vertexSet())
			if(n instanceof AndNode && n.formulas.equals(s))
				return (AndNode) n;
		return null;
	}
	
	private void copy(
			DirectedGraph<ModelNode, DefaultEdge> fragment,
			ModelNode insertion_point,
			DirectedGraph<TableauxNode, DefaultEdge> dag,
			TableauxNode dag_root			
			) 
	{		
		HashMap<ModelNode,TableauxNode> map = new HashMap<ModelNode, TableauxNode>();
		HashMap<TableauxNode, ModelNode> _map = new HashMap<TableauxNode,ModelNode>();
		
		if(insertion_point != null) {
			map.put(insertion_point, dag_root);
			_map.put(dag_root, insertion_point);
		}
		for(TableauxNode n : dag.vertexSet()) {
			if(n != dag_root || insertion_point == null) {
				ModelNode _n = new ModelNode(n);
				fragment.addVertex(_n);
				map.put(_n, n);
				_map.put(n, _n);
			}
		}
		
		for(ModelNode _n : map.keySet()) {
			TableauxNode n = map.get(_n);
			for(DefaultEdge e : dag.outgoingEdgesOf(n)) {
				fragment.addEdge(_n, _map.get(dag.getEdgeTarget(e)));
			}
		}
		
	}
	
	private ModelNode root(DirectedGraph<ModelNode, DefaultEdge> g) {
		ModelNode res = g.vertexSet().stream().filter(x -> g.incomingEdgesOf(x).isEmpty()).findFirst().get();
		//System.out.println("Debugging root@Tableaux: res = " + res);
		return res;
	}
	
	
	
	
	/* *******************************************
	 * 
	 * 					MODEL
	 * 
	 * ******************************************* 
	*/
	
	public DirectedGraph<ModelNode, DefaultEdge> extract_model() {
		
		DirectedGraph<ModelNode, DefaultEdge> model = new DefaultDirectedGraph<ModelNode, DefaultEdge>(DefaultEdge.class);
		HashMap<ModelNode,DirectedGraph<ModelNode, DefaultEdge>> fragment_roots = new HashMap<>();
		
		// Initial step
		DirectedGraph<ModelNode, DefaultEdge> current_frag = frag(choose_block((OrNode) _t.root));
		ModelNode current_frag_root = root(current_frag);
		fragment_roots.put(current_frag_root, current_frag);
		copy(model,null,current_frag,current_frag_root);
	/*	
		int i = 0;
		System.out.println("Model " + i);
		System.out.println("fragment_roots " + fragment_roots.keySet());
		System.out.println("current_root " + current_frag_root);
		Debug.to_file(Debug.to_dot(current_frag, Debug.model_node_render_min), "output/"+i+"_frag.dot");
		Debug.to_file(Debug.to_dot(model, Debug.model_node_render_min), "output/"+i+"_model.dot");
	*/	
		while(!frontier(model).isEmpty()) {
		//	i++;
		// 	System.out.println("Iteration " + i);
			
			ModelNode current = frontier(model).stream().findAny().get();
			Optional<ModelNode> candidate = minus(model.vertexSet(),frontier(model))
					.stream()
					.filter(x -> x.copyOf.equals(current.copyOf))
					.filter(x -> fragment_roots.get(x) != null)	
					.findFirst();
			if(candidate.isPresent()) {
				identify(model,candidate.get(),current);
			} else {
				current_frag = frag((AndNode) current.copyOf);
				current_frag_root = root(current_frag);
				fragment_roots.put(current, current_frag);
				copy(model,current,current_frag,current_frag_root);
				assert current.copyOf.equals(current_frag_root.copyOf);
			}
			/*
			System.out.println("fragment_roots " + fragment_roots.keySet());
			System.out.println("current_root " + current_frag_root);
			Debug.to_file(Debug.to_dot(current_frag, Debug.model_node_render_min), "output/"+i+"_frag.dot");
			Debug.to_file(Debug.to_dot(model, Debug.model_node_render_min), "output/"+i+"_model.dot");
			*/
			
			for(ModelNode rt : fragment_roots.keySet())
				assert model.containsVertex(rt);
			
		}
			
		return model;		
	}	
	
	
	
	private AndNode choose_block(OrNode _node) {
		assert graph.vertexSet().contains(_node);
				
		// Given the current OR pick we find the minimum tag succesor.
		AndNode and_pick = (AndNode) succesors(_node,graph).iterator().next();
		int frag_size_min = frag(and_pick).vertexSet().size();
		for(TableauxNode x : succesors(_node,graph)) {
			assert x instanceof AndNode;
			int frag_size_candidate = frag((AndNode)x).vertexSet().size();
			if(frag_size_candidate < frag_size_min) {
				and_pick = (AndNode) x;
				frag_size_min = frag(and_pick).vertexSet().size();
			} else if (frag_size_candidate == frag_size_min) {
				if(succesors(x,graph).size() > succesors(and_pick,graph).size()) {
					and_pick = (AndNode) x;
					frag_size_min = frag(and_pick).vertexSet().size();
				}
			}
		}	
		return and_pick;
	}
	
	private void copy(
			DirectedGraph<ModelNode, DefaultEdge> model,
			ModelNode insertion_point,
			DirectedGraph<ModelNode, DefaultEdge> fragment,
			ModelNode root
			) 
	{
		//assert root == null || fragment.containsVertex(root) 
		//: "fragment : " + fragment + "does not contain root " + root;
		
		//assert insertion_point == null || model.containsVertex(insertion_point)	
		//: "model : " + model + "does not insertion point " + insertion_point;
		
		for(ModelNode n : fragment.vertexSet()) {
			model.addVertex(n);
		}	
	
		for(DefaultEdge e : fragment.edgeSet()) {
			model.addEdge(fragment.getEdgeSource(e), fragment.getEdgeTarget(e));
		}
		
		if(insertion_point != null && root != null)
			identify(model,insertion_point,root);
	}
	
	private void identify(
			DirectedGraph<ModelNode, DefaultEdge> g,
			ModelNode x,
			ModelNode y			
	){

		assert g.vertexSet().contains(x);
		assert g.vertexSet().contains(y);
		
		g.addEdge(x, y);
		if(x.equals(y)) return;
		for(DefaultEdge e : g.incomingEdgesOf(y))
			g.addEdge(g.getEdgeSource(e),x);
		for(DefaultEdge e : g.outgoingEdgesOf(y))
			g.addEdge(x,g.getEdgeTarget(e));
		
		g.removeVertex(y);
		
	}
	
	
	
	
	
	
	
	
	

}
