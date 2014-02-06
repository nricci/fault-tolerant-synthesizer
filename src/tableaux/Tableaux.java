package tableaux;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.Subgraph;

import dctl.formulas.*;

public class Tableaux {
	
	private TableauxNode root;
	
	private LinkedList<TableauxNode> frontier;
	
	
	private Set<TableauxNode> to_check;
	
	private Set<TableauxNode> to_delete;	
	
	private DirectedGraph<TableauxNode, DefaultEdge> graph; 
	
	public Tableaux(Set<StateFormula> spec) {
		graph = new DefaultDirectedGraph(DefaultEdge.class);
		frontier = new LinkedList<TableauxNode>();
		to_check = new HashSet<TableauxNode>();
		to_delete = new HashSet<TableauxNode>();
		root = new OrNode(spec);
		graph.addVertex(root);		
		frontier.add(root);
	}
	
	public DirectedGraph<TableauxNode, DefaultEdge> get_graph() {
		return this.graph;
	}
	
	public boolean frontier() {
		return !this.frontier.isEmpty();
	}
	
	public int expand() {
		int count = 0;
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
					count += graph.addEdge(_n, _m) != null?1:0;
				}
				frontier.remove(_n);
			} 
			else if (n instanceof AndNode) {
				AndNode _n = (AndNode) n;
				Set<OrNode> succ = _n.tiles();
				if (succ.isEmpty()) {
					OrNode _dummy = new OrNode(_n.formulas);
					graph.addVertex(_dummy);
					count += graph.addEdge(_n, _dummy) != null?1:0;
					count += graph.addEdge(_dummy,_n) != null?1:0;
				} else {
					for(OrNode _m : succ) {
						if(!graph.containsVertex(_m)) {
							graph.addVertex(_m);
							frontier.add(_m);
						}
						count += graph.addEdge(_n, _m) != null?1:0;
					}
					frontier.remove(_n);			
				}
			}
		}
		return count;
	}
	
	
	
	/* **************************************
	 * 
	 * 				DELETION
	 * 
	 * **************************************
	*/
	
	public int delete_inconsistent() {
		int count = 0;
		count += delete_inconsistent_prop();
		count += delete_AU();
		count += delete_EU();
		count += delete_neighbours();
		
		//System.out.println(to_check);
		//System.out.println(to_delete);
		
		for(TableauxNode n : to_delete) {
			graph.removeVertex(n);
		}
		to_delete.clear();
		to_check.clear();
		
		return count;
	}
	
	public int delete_unreachable() {
		ConnectivityInspector<TableauxNode, DefaultEdge> conn = new ConnectivityInspector<TableauxNode, DefaultEdge>(graph);
		Set<TableauxNode> reachable = conn.connectedSetOf(root);
		int count = 0;
		Set<TableauxNode> ll = graph.vertexSet();
		for(TableauxNode n : ll) {
			if(!reachable.contains(n)) {
				delete_node(n);
				count++;
			}
		}
		for(TableauxNode n : to_delete) {
			graph.removeVertex(n);
		}
		to_delete.clear();
		return count;
	}
	
	private int delete_inconsistent_prop() {
		int count = 0;
		
		for(TableauxNode n : graph.vertexSet()) {
			if(!is_consistent(n.formulas)) {
				delete_node(n);
				count++;
			}
		}
		return count;
	}
	

	private int delete_neighbours() {
		int count = 0;
		for(TableauxNode n : to_check) {
			if(n instanceof AndNode) {
				delete_node(n);
				count++;
			} else if (n instanceof OrNode) {
				if(graph.outgoingEdgesOf(n).isEmpty()) {
					delete_node(n);
					count++;
				}
			}
		}
		return count;
	}
	
	
	private int delete_EU() {
		int count = 0;
		
		for(TableauxNode n : graph.vertexSet()) {
			for(StateFormula f : n.formulas) {
				if ((f instanceof Exists)
					&& (((Exists) f).arg() instanceof Until)
					&& (!reach(n,new HashSet<TableauxNode>(),
						((Until)(((Exists) f).arg())).arg_left(),
						((Until)(((Exists) f).arg())).arg_right()))) {
					delete_node(n);
					count++;
				}
			}
		}
		return count;
	}
	
	
	private int delete_AU() {
		int count = 0;
		
		for(TableauxNode n : graph.vertexSet()) {
			for(StateFormula f : n.formulas) {
				if ((f instanceof Forall)
					&& (((Forall) f).arg() instanceof Until)
					&& (!full_subdag(n,new HashSet<TableauxNode>(),
						((Until)(((Forall) f).arg())).arg_left(),
						((Until)(((Forall) f).arg())).arg_right()))) {
					delete_node(n);
					count++;
				}
			}
		}
		return count;
	}
	
	
	private void delete_node(TableauxNode n) {
		for(DefaultEdge e : graph.incomingEdgesOf(n))
			if(graph.getEdgeSource(e) != n && !to_delete.contains(graph.getEdgeSource(e)))
				to_check.add(graph.getEdgeSource(e));
		//to_check.remove(n);
		to_delete.add(n);
	}
	
	
	
	private boolean reach(TableauxNode root, 
						Set<TableauxNode> visited, 
						StateFormula f, 
						StateFormula g) 
	{
		if(!visited.contains(root)) {
			Set<TableauxNode> new_visited = new HashSet<TableauxNode>();
			new_visited.addAll(visited);
			new_visited.add(root);
			if(root.formulas.contains(g) || g instanceof True)
				return true;
			else if(root.formulas.contains(f) || f instanceof True) {
				boolean res = false;
				for(DefaultEdge s : graph.outgoingEdgesOf(root)) {
					visited.add(root);
					res = res || reach(graph.getEdgeTarget(s),new_visited,f,g);
				}
				return res;
			}
		}
		return false;
	}
	

	@Deprecated
	private boolean full_subdag(TableauxNode root, 
								Set<TableauxNode> dag, 
								StateFormula f, 
								StateFormula g) 
	{
		if(dag.contains(root)) 
			return false;
		
		Set<TableauxNode> new_dag = new HashSet<TableauxNode>();
		new_dag.addAll(dag);
		new_dag.add(root);
		
		if(root instanceof AndNode) {
			if(root.formulas.contains(g) || g instanceof True)
				return true;
			else if (root.formulas.contains(f) || f instanceof True) {
				boolean res = true;
				for(DefaultEdge s : graph.outgoingEdgesOf(root)) {
					res = res && full_subdag(graph.getEdgeTarget(s),new_dag,f,g);
				}
				return res;
			} else
				return false;
		} else if(root instanceof OrNode) {
			if(!root.formulas.contains(f) && !(f instanceof True))
				return false;
			else  {
				boolean res = false;
				for(DefaultEdge s : graph.outgoingEdgesOf(root)) {
					res = res || full_subdag(graph.getEdgeTarget(s),new_dag,f,g);
				}
				return res;
			}				
		} else
			throw new Error("Should not get here...");
	}
	

	/*	Returns true iff the node is not immediately inconsistent 
	*/
	private boolean is_consistent(Set<StateFormula> s) {
		for(StateFormula f : s) {
			if(f instanceof False)
				return false;
			StateFormula not_f = new Negation(f);
			if(s.contains(not_f))
				return false;
		}		
		return true;
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
			to_dot_with_tags("output/graph_tagging_" + g + ".dot", this.graph, cast_graph_tagging);
				
			return extract_AND_induced_graph(dag);
		} else if(g instanceof Exists) {
			Map<TableauxNode,Integer> graph_tagging = dag_tagEU(n, (Exists) g, f, h);
			DirectedGraph<TableauxNode, DefaultEdge> dag = dag_extract_EU_dag(n, graph_tagging); 
			return extract_AND_induced_graph(dag);
		}
		return null;		
	}
	
	/*
	 *  dag method helper functions.
	*/
	
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
	
	private Set<TableauxNode> frontier(DirectedGraph<TableauxNode, DefaultEdge> g) {
		HashSet<TableauxNode> res = new HashSet<TableauxNode>();
		for(TableauxNode n : g.vertexSet())
			if(g.outgoingEdgesOf(n).isEmpty()) 
				res.add(n);
		return res;
	}
	
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
	
	

	private DirectedGraph<TableauxNode, DefaultEdge> dag_extract_AU_dag(AndNode n, Map<TableauxNode,Integer> tag) {
		assert n != null;
		assert tag != null;
		System.out.println("dag_extract_AU_dag hi");
		
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
		
		System.out.println("dag_extract_AU_dag bye");
		assert _dag != null;
		return _dag;
	}
	
	private Map<TableauxNode,Integer> dag_tagAU(AndNode n, Forall g, StateFormula f, StateFormula h) {
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
	
	private DirectedGraph<TableauxNode, DefaultEdge> extract_AND_induced_graph(DirectedGraph<TableauxNode, DefaultEdge> g) {
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
	

	
	
	
	private Map<TableauxNode,Integer> dag_tagEU(AndNode n, Exists g, StateFormula f, StateFormula h) {
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

		int i = 0;
		Map<ModelNode,String> cast_graph_tagging = new HashMap<ModelNode, String>();
		for(ModelNode m : res.vertexSet())
			cast_graph_tagging.put(m,m.toString());
		to_dot_with_tags_2("output/frag_" + n + "/iteration_" + i++ + ".dot", res, cast_graph_tagging);
		
		if(!eventuality_formulas.isEmpty())	{
			DirectedGraph<TableauxNode, DefaultEdge> current_dag;
			Quantifier f = eventuality_formulas.poll();
			current_dag = dag(n,f);
			copy(res,null,current_dag,n);
			
			System.out.println("first res : " + res.vertexSet().size());
			cast_graph_tagging = new HashMap<ModelNode, String>();
			for(ModelNode m : res.vertexSet())
				cast_graph_tagging.put(m,m.toString());
			to_dot_with_tags_2("output/frag_" + n + "/iteration_" + i++  + "_" + f + ".dot", res, cast_graph_tagging);
			
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
				System.out.println("one loop res : " + res.vertexSet().size());
				cast_graph_tagging = new HashMap<ModelNode, String>();
				for(ModelNode m : res.vertexSet())
					cast_graph_tagging.put(m,m.toString());
				to_dot_with_tags_2("output/frag_" + n + "/iteration_" + i++ + "_" + current_formula + ".dot", res, cast_graph_tagging);
			}		
		}		
		System.out.println("final res : " + res.vertexSet().size());
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
	
	
	
	
	/* *******************************************
	 * 
	 * 					MODEL
	 * 
	 * ******************************************* 
	*/
	
	
	public DirectedGraph<ModelNode, DefaultEdge> extract_model() {
		DirectedGraph<ModelNode, DefaultEdge> model = new DefaultDirectedGraph<ModelNode, DefaultEdge>(DefaultEdge.class);
		LinkedList<ModelNode> frontier = new LinkedList<ModelNode>();
		LinkedList<ModelNode> fragment_roots = new LinkedList<ModelNode>();
		
		ModelNode x = new ModelNode(choose_block((OrNode) this.root));
		model.addVertex(x);
		frontier.push(x);
		while(!frontier.isEmpty()) {
			AndNode current = (AndNode) pick_by_formula_set(frontier.poll().formulas);
			for(TableauxNode or_succ : succesors(current,graph)) {
				assert or_succ instanceof OrNode;
				Subgraph g = new Subgraph(graph,null);
				
				
				
			}
		}
		
		
		
		return model;		
	}	
	
	private AndNode choose_block(OrNode _node) {
		assert graph.containsVertex(_node);
		
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
		for(ModelNode n : fragment.vertexSet()) {
			model.addVertex(n);
		}
	
		for(DefaultEdge e : fragment.edgeSet()) {
			model.addEdge(fragment.getEdgeSource(e), fragment.getEdgeTarget(e));
		}
	}
	
	
	/* *******************************************
	 * 
	 * 					PRINTING
	 * 
	 * ******************************************* 
	*/

	
	
	public static String to_dot(String file, DirectedGraph<TableauxNode, DefaultEdge> g) {
		String res = "";
		
		res += "digraph {\n";
		int i = 0;
		HashMap<TableauxNode,String> map = new HashMap<TableauxNode, String>();
		for(TableauxNode n : g.vertexSet()) {
			map.put(n, "n"+i);
			res += "n" + i++ + " [shape=" + ((n instanceof AndNode)?"box":"hexagon") + ",label=\"";
			for(StateFormula f : n.formulas) {
				res += f + "\n";
			}
			res += "\"];";
		}
		res += "\n";
		for(DefaultEdge e : g.edgeSet()) {
			res += map.get(g.getEdgeSource(e)) + "->" + map.get(g.getEdgeTarget(e)) + ";\n";
		}
		res += "}";
		
		if(file != null) {
			try {
				FileWriter w = new FileWriter(new File(file));
				w.write(res);
				w.close();
			} catch (Exception e) {
				System.out.println("Tableaux.to_dot: could not write to file.");
				e.printStackTrace();
			}
		}
		
		return res;
	}
	
	public static String frag_to_dot(String file, DirectedGraph<ModelNode, DefaultEdge> g) {
		String res = "";
		
		res += "digraph {\n";
		int i = 0;
		HashMap<ModelNode,String> map = new HashMap<ModelNode, String>();
		for(ModelNode n : g.vertexSet()) {
			map.put(n, "n"+i);
			res += "n" + i++ + " [shape=box,label=\"";
			for(StateFormula f : n.formulas) {
				res += f + "\n";
			}
			res += "\"];";
		}
		res += "\n";
		for(DefaultEdge e : g.edgeSet()) {
			res += map.get(g.getEdgeSource(e)) + "->" + map.get(g.getEdgeTarget(e)) + ";\n";
		}
		res += "}";
		
		if(file != null) {
			try {
				FileWriter w = new FileWriter(new File(file));
				w.write(res);
				w.close();
			} catch (Exception e) {
				System.out.println("Tableaux.to_dot: could not write to file.");
				e.printStackTrace();
			}
		}
		
		return res;
	}
	
	
	
	public static String to_dot_with_tags(String file, DirectedGraph<TableauxNode, DefaultEdge> g, Map<TableauxNode,String> tags) {
		String res = "";
		
		res += "digraph {\n";
		int i = 0;
		HashMap<TableauxNode,String> map = new HashMap<TableauxNode, String>();
		for(TableauxNode n : g.vertexSet()) {
			map.put(n, "n"+i);
			res += "n" + i++ + " [shape=" + ((n instanceof AndNode)?"box":"hexagon") + ",label=\"";
			res += "tag : " + tags.get(n) + "\n";
			for(StateFormula f : n.formulas) {
				res += f + "\n";
			}
			res += "\"];";
		}
		res += "\n";
		for(DefaultEdge e : g.edgeSet()) {
			res += map.get(g.getEdgeSource(e)) + "->" + map.get(g.getEdgeTarget(e)) + ";\n";
		}
		res += "}";
		
		if(file != null) {
			try {
				FileWriter w = new FileWriter(new File(file));
				w.write(res);
				w.close();
			} catch (Exception e) {
				System.out.println("Tableaux.to_dot: could not write to file.");
				e.printStackTrace();
			}
		}
		
		return res;
	}
	
	
	public static String to_dot_with_tags_2(String file, DirectedGraph<ModelNode, DefaultEdge> g, Map<ModelNode,String> tags) {
		String res = "";
		
		res += "digraph {\n";
		int i = 0;
		HashMap<ModelNode,String> map = new HashMap<ModelNode, String>();
		for(ModelNode n : g.vertexSet()) {
			map.put(n, "n"+i);
			res += "n" + i++ + " [shape=box,label=\"";
			res += "tag : " + tags.get(n) + "\n";
			for(StateFormula f : n.formulas) {
				res += f + "\n";
			}
			res += "\"];";
		}
		res += "\n";
		for(DefaultEdge e : g.edgeSet()) {
			res += map.get(g.getEdgeSource(e)) + "->" + map.get(g.getEdgeTarget(e)) + ";\n";
		}
		res += "}";
		
		if(file != null) {
			try {
				file = file.replace('@', '_');
				File f = new File(file);
				f.getParentFile().mkdirs();
				FileWriter w = new FileWriter(f);
				w.write(res);
				w.close();
			} catch (Exception e) {
				System.out.println("Tableaux.to_dot: could not write to file.");
				e.printStackTrace();
			}
		}
		
		return res;
	}	
	
	public String to_dot(String file) {
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
			expo.export(new FileWriter(new File("output/"+file)), graph);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
		

}
