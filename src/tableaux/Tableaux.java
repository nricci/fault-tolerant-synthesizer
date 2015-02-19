package tableaux;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.Subgraph;

import synthesizer.MaskingCalculator;
import synthesizer.ModelNode;
import util.Debug;
import util.Pair;
import util.Relation;
import util.SetUtils;
import util.binarytree.Tree;
import dctl.formulas.*;
import static dctl.formulas.DCTLUtils.closure;
import static dctl.formulas.DCTLUtils.is_consistent;
import static dctl.formulas.DCTLUtils.is_closed;
import static dctl.formulas.DCTLUtils.prop_sat;
import static util.SetUtils.map;
import static util.SetUtils.minus;
import static util.SetUtils.union;
import static util.SetUtils.intersection;
import static util.SetUtils.make_set;
import static util.SetUtils.times;





public class Tableaux {
	
	private Specification _spec;
	
	public TableauxNode root;
	
	private LinkedList<TableauxNode> frontier;
	
	private Set<TableauxNode> injected;	
		
	private Set<TableauxNode> to_check;
	
	private Set<TableauxNode> to_delete;	
	
	private DirectedGraph<TableauxNode, DefaultEdge> graph;
	
	private Set<Pair<AndNode,DeonticProposition>> injected_faults;

	private Set<TableauxNode> commited_nodes;
	
	
	
	public Tableaux(Specification _spec) {
		graph = new DefaultDirectedGraph(DefaultEdge.class);
		frontier = new LinkedList<TableauxNode>();
		to_check = new HashSet<TableauxNode>();
		to_delete = new HashSet<TableauxNode>();
		injected = new HashSet<TableauxNode>();
		injected_faults = new HashSet<Pair<AndNode,DeonticProposition>>();
		commited_nodes = new HashSet<TableauxNode>();
		this._spec = _spec;
		
		root = new OrNode(_spec._init);
		graph.addVertex(root);		
		frontier.add(root);
		
	}
	
	/*
	 * 
	 * 		AUXILIARY STUFF
	 * 
	 * 
	*/
	
	public boolean megatest() {
		// Mega Test
		boolean test = false;
		for(TableauxNode parent : graph.vertexSet()) {
			Set<TableauxNode> siblings = succesors(parent, graph);
			for(TableauxNode n1 : siblings) {
				for(TableauxNode n2 : siblings) {
					test |= 
							n1.formulas.stream()
							.filter(x -> x.is_elementary())
							.collect(Collectors.toSet())
							.equals(
									n2.formulas.stream()
									.filter(x -> x.is_elementary())
									.collect(Collectors.toSet())
									)
							&&
							!succesors(n1, graph).equals(succesors(n2, graph));
				}	
			}
		}
		return test;
	}
	
	public boolean megatestII() {
		// Mega Test
		boolean test = false;
		for(TableauxNode parent : graph.vertexSet()) {
			Set<TableauxNode> siblings = succesors(parent, graph);
			for(TableauxNode n1 : siblings) {
				for(TableauxNode n2 : siblings) {
					if(n1.formulas.stream()
							.filter(x -> x.is_elementary())
							.collect(Collectors.toSet())
							.equals(
									n2.formulas.stream()
									.filter(x -> x.is_elementary())
									.collect(Collectors.toSet())
									))
					{
						assert(succesors(n1, graph).equals(succesors(n2, graph)));
					}
				}	
			}
		}
		return true;
	}
	
	
	
	public DirectedGraph<TableauxNode, DefaultEdge> get_graph() {
		return this.graph;
	}
	
	public Set<TableauxNode> nodes() {
		return this.graph.vertexSet();
	}
	
	public Set<AndNode> and_nodes() {
		return this
				.graph
				.vertexSet()
				.stream()
				.filter(n -> n instanceof AndNode)
				.map(n -> (AndNode) n)
				.collect(Collectors.toSet());
	}
	
	
	public TableauxNode add_node(TableauxNode n) {
		if(!graph.addVertex(n))
			return graph.vertexSet().stream().filter(x -> x.equals(n)).findFirst().get();
		return n;
	}
	
	public void delete_node(TableauxNode n) {
		if(n.equals(root)) root = null;
		graph.removeVertex(n);
	}
	
	public Set<TableauxNode> flush() {
		Set<TableauxNode> res = new HashSet<TableauxNode>();
		res.addAll(to_delete);
		for(TableauxNode n : to_delete) 
			delete_node(n);
		to_delete.clear();
		return res;		
	}
	
	public void add_edge(TableauxNode x, TableauxNode y) {
		graph.addEdge(x, y);
	}
	
	public Set<TableauxNode> frontier() {
		HashSet<TableauxNode> res = new HashSet<TableauxNode>();
		//System.out.println("hashes : " + graph.vertexSet().stream().map(x -> x.hashCode()));
		for(TableauxNode n : graph.vertexSet()) {
			//System.out.println("n hash : " + n.hashCode());
			//assert graph.containsVertex(n);
			if(graph.outgoingEdgesOf(n).isEmpty()) 
				res.add(n);
		}
		return res;
	}
	
	public Set<TableauxNode> succesors(TableauxNode n) {
		HashSet<TableauxNode> res = new HashSet<TableauxNode>();
		for(DefaultEdge e : graph.outgoingEdgesOf(n))
			res.add(graph.getEdgeTarget(e));
		return res;
	}
	
	private Set<TableauxNode> succesors(Set<TableauxNode> ns) {
		Set<TableauxNode> empty = new HashSet<TableauxNode>();
		BinaryOperator<Set<TableauxNode>> union = (Set<TableauxNode> x, Set<TableauxNode> y) -> union(x,y);
		return ns.stream()
				.map((TableauxNode x) -> succesors(x))
				.reduce(empty,union);
	}
	
	private Set<TableauxNode> succesors(TableauxNode n, DirectedGraph<TableauxNode, DefaultEdge> graph) {
		HashSet<TableauxNode> res = new HashSet<TableauxNode>();
		for(DefaultEdge e : graph.outgoingEdgesOf(n))
			res.add(graph.getEdgeTarget(e));
		return res;
	}
	
	public Set<TableauxNode> predecesors(TableauxNode n) {
		return predecesors(n, this.graph);
	}
	
	private Set<TableauxNode> predecesors(TableauxNode n, DirectedGraph<TableauxNode, DefaultEdge> graph) {
		HashSet<TableauxNode> res = new HashSet<TableauxNode>();
		for(DefaultEdge e : graph.incomingEdgesOf(n))
			res.add(graph.getEdgeSource(e));
		return res;
	}
	
	public Set<AndNode> predecesors2(AndNode n) {
		HashSet<OrNode> or_pre = new HashSet<OrNode>();
		for(DefaultEdge e : graph.incomingEdgesOf(n))
			or_pre.add((OrNode)graph.getEdgeSource(e));
		
		HashSet<AndNode> and_pre = new HashSet<AndNode>();
		for(OrNode o : or_pre)
			for(DefaultEdge e : graph.incomingEdgesOf(o))
				and_pre.add((AndNode)graph.getEdgeSource(e));
		return and_pre;
	}
	
	
	
	
	
	
	private <T> Set<T> frontier(DirectedGraph<T, DefaultEdge> g) {
		HashSet<T> res = new HashSet<T>();
		for(T n : g.vertexSet())
			if(g.outgoingEdgesOf(n).isEmpty()) 
				res.add(n);
		return res;
	}
	
	public Set<TableauxNode> reachable_nodes(TableauxNode n) {
		return reachable_nodes(make_set(n));
	}
	
	public Set<TableauxNode> reachable_nodes(Set<TableauxNode> ns) {
		Set<TableauxNode> reachable = new HashSet<TableauxNode>();
		reachable.addAll(ns);
		
		int k;
		do {
			k = reachable.size();
			reachable.addAll(succesors(reachable));
		} while(reachable.size() > k);
		
		return reachable;
	}
	
	/*
	 *	Commits nodes generated so far so they can't be deleted by the
	 *	deletion algorithm.
	 *
	 * This method is used to succesively commit iterations of successfuly
	 * generated faults.
	 * 
	*/	
	public void commit() {
		this.commited_nodes.addAll(graph.vertexSet());
	}
	
	
	/*
	 * 
	 * 		TABLEAU ALGORITHM
	 * 
	 * 
	*/
	
	public List<TableauxNode> do_tableau(boolean deontic_filter) {
		List<TableauxNode> res = new LinkedList<>();
		int step = 0;
		while (this.frontier().stream()
				.filter(x -> !to_delete.contains(x))
				.findFirst()
				.isPresent()) 
		{
			res.addAll(expand(deontic_filter));
			
			//System.out.println("[do_tableau] step : " + step++);
			/*Debug.to_file(
					Debug.to_dot(this.graph, Debug.default_node_render), 
					"output/do_tableaux"+ step++ +".dot"
				);*/
			
		}
		return res;
	}
	
	protected List<TableauxNode> expand(boolean deontic_filter) {
		TableauxNode n = this.frontier()
				.stream()
				.filter(x -> !to_delete.contains(x))
				.findFirst().get();
		if (n instanceof AndNode)
			return expand((AndNode) n);
		else if (n instanceof OrNode)
			return expand((OrNode) n, deontic_filter);
		else
			throw new Error("Found node that's neither And nor Or.");
	}
	
	/*
	 *	And node expansion 
	*/
	public List<TableauxNode> expand(AndNode n) {
		List<TableauxNode> res = new LinkedList<TableauxNode>();
		
		Set<StateFormula> ax_formulas =  n.formulas
				.stream()
				.filter(f -> f instanceof Forall)
				.map(f -> ((Forall) f).arg())
				.filter(f -> f instanceof Next)
				.map(f -> ((Next) f).arg())
				.collect(Collectors.toSet());
		
		Set<StateFormula> ex_formulas = n.formulas
				.stream()
				.filter(f -> f instanceof Exists)
				.map(f -> ((Exists) f).arg())
				.filter(f -> f instanceof Next)
				.map(f -> ((Next) f).arg())
				.collect(Collectors.toSet());
		
		if(ex_formulas.isEmpty() && ax_formulas.isEmpty()) {
			// create dummy succesor
			OrNode _dummy = new OrNode(n.formulas);
			graph.addVertex(_dummy);
			graph.addEdge(n, _dummy);
			graph.addEdge(_dummy,n);
			res.add(_dummy);
			return res;		
		}
		
		if (ex_formulas.isEmpty() && !ax_formulas.isEmpty())
			ex_formulas.add(new True());
		
		if (!ex_formulas.isEmpty()) {
			Set<Set<StateFormula>> _succs = ex_formulas
					.stream()
					.map(f -> union(ax_formulas, f))
					.collect(Collectors.toSet());
			
			/*_succs = _succs
					.stream()
					.filter(x -> is_consistent(x))
					.collect(Collectors.toSet());
			*/
			//System.out.println("[expand(AndNode)] :  " + _succs + ex_formulas + ax_formulas);
			
			for(Set<StateFormula> _s : _succs) {
				OrNode _node = new OrNode(_s);
				graph.addVertex(_node);
				graph.addEdge(n, _node);
				res.add(_node);
			}			
		}	
		
		return res;
	}
	
	/*
	 * 		OrNode Expansion 
	*/
	public List<TableauxNode> expand(OrNode n, boolean deontic_filter) {
		// Local debuging mesasges flag
		boolean debug = false;
		
		if(debug) System.out.println("expanding : " + n);
		List<TableauxNode> res = new LinkedList<TableauxNode>();
		Set<AndNode> _nodes = new HashSet<AndNode>();
				
		Set<Set<StateFormula>> _succs = closure(n.formulas);
		if(debug) System.out.println("\n _succs : " + _succs);
		if(_succs.isEmpty())
			to_delete.add(n);
		
		//_succs = _succs.stream().filter(s -> is_consistent(s)).collect(Collectors.toSet());
		
		_nodes = _succs.stream().map(s -> new AndNode(s)).collect(Collectors.toSet());
		
		/*for(Set<StateFormula> s : _succs) {
			assert is_closed(s);
			Set<StateFormula> triggered_guards = new HashSet<>();
			Set<Set<StateFormula>> guard_closure = new HashSet<>();
			
			for(StateFormula guard : _spec._global_rules.keySet())
				if(prop_sat(s, guard))
					triggered_guards.addAll(_spec._global_rules.get(guard));
					
			guard_closure = closure(triggered_guards);
			//System.out.println("\n _guard_closure : " + guard_closure);
			for(Set<StateFormula> _g : guard_closure) {
				_g = union(_g,s);
				if(debug) System.out.println("_g : " + _g);
				if(is_consistent(_g))
					_nodes.add(new AndNode(_g));
			}				
		}*/

		if(debug) System.out.println("_nodes : " + _nodes);
		if(deontic_filter) {
			// Filtrado de nodos fallidos que sean relizables sin fallas.
			// Las fallas son insertadas posteriormente en fault injection
			Set<AndNode> _nodes_deontic_filter = new HashSet<AndNode>();
			Function<AndNode,Set<Proposition>> props = ((AndNode node) -> 
				node.formulas
				.stream()
				.filter(x -> x instanceof Proposition)
				.map(x -> (Proposition) x)
				.collect(Collectors.toSet()));
			for(AndNode _m : _nodes) {
				if(_m.faulty) {
					Set<Proposition> _m_props = props.apply(_m);
					if(!_nodes.stream().anyMatch(_m2 -> !_m2.faulty && props.apply(_m2).equals(_m_props)))
						_nodes_deontic_filter.add(_m);
				} else {
					_nodes_deontic_filter.add(_m);
				}
			}
			if(debug) System.out.println(_nodes.size() - _nodes_deontic_filter.size() + " spurious faulty nodes filtered.");
			_nodes = _nodes_deontic_filter;
			//System.out.println("_nodes : " + _nodes);
		}
		
		
		// Cociente modulo formulas elementary.
		
		Set<AndNode> _nodes_elementary_filter = new HashSet<AndNode>();
		Function<AndNode,Set<StateFormula>> elem_flas = ((AndNode node) -> 
			node.formulas
			.stream()
			.filter(x -> x.is_elementary())
			.collect(Collectors.toSet()));
		for(AndNode _m : _nodes) {
			if(!_nodes_elementary_filter.stream().anyMatch(
					x -> elem_flas.apply(x).equals(elem_flas.apply(_m))
					)
				)
			{
				_nodes_elementary_filter.add(_m);
			}
		}
		if(debug) System.out.println(_nodes.size() - _nodes_elementary_filter.size() + " elem-equivalent nodes filtered.");
		_nodes = _nodes_elementary_filter;
		
		
		
		for(AndNode _m : _nodes) {
			graph.addVertex(_m);
			graph.addEdge(n, _m);
			res.add(_m);
		}
		if(_nodes.isEmpty())
			to_delete.add(n);
		
		if(debug) System.out.println("res : " + res);
		return res;
	}
	
	private Set<StateFormula> triggered_guards(Set<StateFormula> set) {
		Set<StateFormula> res = new HashSet<>();
		for(StateFormula guard : _spec._global_rules.keySet())
			if(prop_sat(set, guard))
				res.addAll(_spec._global_rules.get(guard));
		return res;
	}
	
	
	/*private Set<Set<StateFormula>> downward_closure(Set<StateFormula> init) {
		Tree<Set<StateFormula>> tree = new Tree<Set<StateFormula>>(init);
		List<Tree<Set<StateFormula>>> frontier = new LinkedList<>();
		frontier.add(tree);
		
		
		
		return null;
	}*/	
	
	
	/* GARBAGE
	public List<TableauxNode> expand(TableauxNode n) {
		List<TableauxNode> res = new LinkedList<TableauxNode>();

		assert n != null;
		assert succesors(n, graph).isEmpty();
		
		if (n instanceof OrNode) {
			OrNode _n = (OrNode) n;
			Set<AndNode> succ = _n.blocks();
			for(AndNode _m : succ) {
				if(!graph.containsVertex(_m)) {
					graph.addVertex(_m);
					frontier.add(_m);
				}
				graph.addEdge(_n, _m);
				res.add(_m);
			}
			frontier.remove(_n);
		} 
		else if (n instanceof AndNode) {
			AndNode _n = (AndNode) n;
			Set<OrNode> succ = _n.tiles();
			if (succ.isEmpty()) {
				OrNode _dummy = new OrNode(_n.formulas);
				graph.addVertex(_dummy);
				graph.addEdge(_n, _dummy);
				graph.addEdge(_dummy,_n);
				res.add(_dummy);
			} else {
				for(OrNode _m : succ) {
					if(!graph.containsVertex(_m)) {
						graph.addVertex(_m);
						frontier.add(_m);
					}
					graph.addEdge(_n, _m);
					res.add(_m);
				}
				frontier.remove(_n);			
			}
		}
		
		return res;
	}
	*/
	
	
	
	/* **************************************
	 * 
	 * 				DELETION
	 * 
	 * **************************************
	*/
	
	public int deletion_rules() {
		int count = 0;
		
		do {
			count += delete_inconsistent_prop();
			count += delete_AU();
			count += delete_EU();
			count += delete_neighboursII();
		
			//System.out.println(to_check);
			System.out.println(to_delete);
		
			for(TableauxNode n : to_delete) {
				graph.removeVertex(n);
			}
			to_delete.clear();
			to_check.clear();
			
		} while(count > 0);
		
		return count;
	}
	
	
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
				remove_node(n);
				count++;
			}
		}
		//for(TableauxNode n : to_delete) {
		//	graph.removeVertex(n);
		//}
		//to_delete.clear();
		return count;
	}
	
	private int delete_inconsistent_prop() {
		int count = 0;
		
		for(TableauxNode n : graph.vertexSet()) {
			if(!is_consistent(n.formulas)) {
				remove_node(n);
				count++;
			}
		}
		return count;
	}
	
	private int delete_neighboursII() {
		int count = 0;
		for(TableauxNode n : graph.vertexSet()) {
			if(n instanceof AndNode) {
				remove_node(n);
				count++;
			} else if (n instanceof OrNode) {
				if(graph.outgoingEdgesOf(n).isEmpty()) {
					remove_node(n);
					count++;
				}
			}
		}
		return count;
	}
	
	private int delete_neighbours() {
		int count = 0;
		for(TableauxNode n : to_check) {
			if(n instanceof AndNode) {
				remove_node(n);
				count++;
			} else if (n instanceof OrNode) {
				if(graph.outgoingEdgesOf(n).isEmpty()) {
					remove_node(n);
					count++;
				}
			}
		}
		return count;
	}
	
	
	private int delete_EU() {
		int count = 0;
		
		for(TableauxNode n : graph.vertexSet()) {
			if(n instanceof AndNode)
			for(StateFormula f : n.formulas) {
				if ((f instanceof Exists)
					&& (((Exists) f).arg() instanceof Until)
					&& dag((AndNode) n, (Quantifier) f) == null)
					/*(!reach(n,new HashSet<TableauxNode>(),
						((Until)(((Exists) f).arg())).arg_left(),
						((Until)(((Exists) f).arg())).arg_right())))*/ {
					remove_node(n);
					count++;
				}
			}
		}
		return count;
	}
	
	
	private int delete_AU() {
		int count = 0;
		
		for(TableauxNode n : graph.vertexSet()) {
			if(n instanceof AndNode)
			for(StateFormula f : n.formulas) {
				if ((f instanceof Forall)
					&& (((Forall) f).arg() instanceof Until)
					&& dag((AndNode) n, (Quantifier) f) == null) 
					/*(!full_subdag(n,new HashSet<TableauxNode>(),
						((Until)(((Forall) f).arg())).arg_left(),
						((Until)(((Forall) f).arg())).arg_right())))*/ {			
					remove_node(n);
					count++;
				}
			}
		}
		return count;
	}
	
	
	private void remove_node(TableauxNode n) {
		for(DefaultEdge e : graph.incomingEdgesOf(n))
			if(
				graph.getEdgeSource(e) != n && 
				!to_delete.contains(graph.getEdgeSource(e)) &&
				!commited_nodes.contains(graph.getEdgeSource(e))
				)
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
	
	private boolean full_subdag(TableauxNode root, 
								Set<TableauxNode> dag, 
								StateFormula f, 
								StateFormula g) 
	{
		if(dag.contains(root)) 
			// There is a cycle
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
			//if(!root.formulas.contains(f) && !(f instanceof True))
			//	return false;
			//else  {
				boolean res = false;
				for(DefaultEdge s : graph.outgoingEdgesOf(root)) {
					res = res || full_subdag(graph.getEdgeTarget(s),new_dag,f,g);
				}
				return res;
			//}				
		} else
			throw new Error("Should not get here...");
	}
	



	/* *******************************************
	 * 
	 * 					DEONTIC - FAULT DETECTION
	 * 
	 * ******************************************* 
	*/
	
	public int detect_faults() {
		int count = 0;
		count += detect_elementary_faults();
		return count;
	}
	
	public int detect_elementary_faults() {
		int count = 0;
		
		List<TableauxNode> l = new LinkedList<TableauxNode>(graph.vertexSet());
		for(TableauxNode n : l) {
			assert graph.containsVertex(n);
			if(!n.faulty && n instanceof AndNode) {
				AndNode _n = (AndNode) n;
				for(StateFormula f : _n.formulas) {
					if(f instanceof DeonticProposition) {
						if(!prop_sat(_n.formulas,((DeonticProposition) f).get_prop())) {
							toogle_faulty(n);
							count++;
							break;	
						}
					}
				}	
			}
		}
		return count;
	}
	
	private void toogle_faulty(TableauxNode n) {
		TableauxNode new_node = null;
		if(n instanceof OrNode) new_node = new OrNode(n.formulas);
		if(n instanceof AndNode) new_node = new AndNode(n.formulas);
		assert new_node != null;
		new_node.faulty = !n.faulty;
		graph.addVertex(new_node);
		for(TableauxNode pre : predecesors(n, graph)) graph.addEdge(pre, new_node);
		for(TableauxNode post : succesors(n, graph)) graph.addEdge(new_node, post);
		graph.removeVertex(n);	
	}
	
	
	
	/* VER ESTO COMO HACERLO BIEN PORQUE ES UN PERNO
	 * 
	 * 
	public int detect_eventuality_faults() {
		int count = 0;
		
		for(TableauxNode _n : graph.vertexSet()) {
			if(_n instanceof AndNode) {
				AndNode n = (AndNode) _n;
				Set<Proposition> props = new HashSet<Proposition>();
				Set<DeonticProposition> o_props = new HashSet<DeonticProposition>();
				for(StateFormula f : n.formulas) {
					if(f instanceof Proposition)
						props.add((Proposition) f);
					else if(f instanceof DeonticProposition)
						o_props.add((DeonticProposition) f);
				}
				for(DeonticProposition d : o_props) {
					Proposition p = new Proposition(d.name().substring(3));
					if(!props.contains(p)) {
						n.faulty = true;
						count++;
						break;
					}
				}
			}
		}
		return count;
	}
	
	private int detect_OU() {
		int count = 0;
		
		for(TableauxNode n : graph.vertexSet()) {
			if(n instanceof AndNode && !((AndNode) n).faulty)
				for(StateFormula f : n.formulas) {
					if ((f instanceof Obligation)
						&& (((Obligation) f).arg() instanceof Until)
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
	
	
	private int detect_PU() {
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
	
	private boolean deontic_reach(TableauxNode root, 
			Set<TableauxNode> visited, 
			StateFormula f, 
			StateFormula g,
			boolean positive) 
	
	{
		if(!visited.contains(root)) {
			if(root instanceof AndNode && !((AndNode) root).faulty) {
				Set<TableauxNode> new_visited = new HashSet<TableauxNode>();
				new_visited.addAll(visited);
				new_visited.add(root);
				if(positive) {
					if(root.formulas.contains(g))
						return true;
					else if(root.formulas.contains(f) || f instanceof True) {
						boolean res = false;
						for(DefaultEdge s : graph.outgoingEdgesOf(root)) {
							visited.add(root);
							res = res || reach(graph.getEdgeTarget(s),new_visited,f,g);
						}
						return res;
					}
				} else {
					if(!root.formulas.contains(g))
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
			}
		}
		return false;
	}
	
	*/
	
	/* *******************************************
	 * 
	 * 					DEONTIC - FAULT INJECTION
	 * 
	 * ******************************************* 
	*/
	
	public Relation<AndNode,AndNode> masking_relation;
	public Set<AndNode> nonmasking_faults;
	
	public Relation<AndNode,AndNode> inject_faults() {
		boolean debug = true;
		
		// This mapping specifies all the point where a fault 
		// can be injected. Once injected the faults should be 
		// moved to "injected faults".
		Set<Pair<AndNode,DeonticProposition>> pending_faults = new HashSet<>();
		
		// Specifies which fault injection points have already 
		// been injected.
		Set<Pair<AndNode,DeonticProposition>> injected_faults = new HashSet<>();

		
		Relation<AndNode,AndNode> maskedBy = new Relation<AndNode, AndNode>();
		maskedBy = new MaskingCalculator(this,maskedBy).compute();
		Set<AndNode> nonmasking_faults = new HashSet<AndNode>();
		
		pending_faults = minus(detect_fault_injection_points(),injected_faults);
		
		int i = 0;
		final int step = 1;
		if(debug && i % step == 0)
			to_dot("output/inject_step" + i + ".dot",Debug.default_node_render,maskedBy);
			
		
		while(!pending_faults.isEmpty()) {
			i++;
			
			if(debug && i % step == 0) System.out.println("");
			//if(debug) System.out.println("[step " + i + "] pending_faults : " + pending_faults);
			//if(debug) System.out.println("[step " + i + "] injected_faults : " + injected_faults);
			
			Pair<AndNode,DeonticProposition> p = pending_faults.stream().findFirst().get();
           
			if(debug && i % step == 0) System.out.println("[step " + i + "] injecting  : " + p);
			Set<OrNode> fault_generator = inject_fault(p.first, p.second.get_prop());
			if(debug && i % step == 0) System.out.println("[step " + i + "] generators  : " + fault_generator);
			List<TableauxNode> faults = new LinkedList();
			for(OrNode o : fault_generator) {
				faults.addAll(expand(o,false));
			}
			for(TableauxNode n : faults) {
				AndNode fault = (AndNode) n;	
				
				AndNode injection_point = p.first;
				AndNode i_mask = maskedBy.get_one(injection_point);
				if (i_mask == null)
					nonmasking_faults.add(fault);
					// Add EF(reach_somesucc), don't think its nessesary
				else {	
					Set<AndNode> candidates = succesors(i_mask)
							.stream()
							.map(x -> succesors(x))
							.reduce(make_set(), SetUtils::union)
							.stream()
							.map((TableauxNode x) -> (AndNode) x)
							.collect(Collectors.toSet());
					Optional<AndNode> mask = candidates
							.stream()
							.filter(x -> sublabeling(x).equals(sublabeling(n)))
							.findAny();
					if(mask.isPresent()) {
						maskedBy.put(fault, mask.get());
					} else {
						// Can't just add formulas to the node. It will change the hash
						// And throw graph library off.
						// WORKAROUND
						Set<StateFormula> forms = union(
								fault.formulas,
								recovery_formula(candidates.stream().map(x -> x.formulas).collect(Collectors.toSet()))
								);
						AndNode new_node = new AndNode(forms);
						graph.addVertex(new_node);
						for(TableauxNode pre : predecesors(fault, graph)) graph.addEdge(pre, new_node);
						for(TableauxNode post : succesors(fault, graph)) graph.addEdge(new_node, post);
						graph.removeVertex(fault);
						nonmasking_faults.add(new_node);
						assert(new_node.formulas.equals(forms));
					}
						
						
				}
				
			}			
			if(debug && i % step == 0)
				System.out.println("[step " + i + "] injected faults : " + faults);
			if(debug && i % step == 0) 
				for (TableauxNode f : faults) {
					System.out.println("\t " + f + " is masked by " + maskedBy.get_one((AndNode) f));
				}

			for (AndNode a : nonmasking_faults)
				assert graph.vertexSet().contains(a);
				
			if (debug && i % step == 0)
				System.out.println("[step " + i + "] non_masking_faults : " + nonmasking_faults);
			
			
			do_tableau(false);
			if (debug && i % step == 0)
				System.out.println("[step " + i + "] to_delete : " + to_delete);
			
			if (debug && i % step == 0) 
				System.out.println("[step " + i + "] delete : " + delete_inconsistent());
			pending_faults.remove(p);
			injected_faults.add(p);
				
			if (debug && i % step == 0)
				to_dot("output/inject_step" + i + ".dot",Debug.default_node_render,maskedBy);
			
			if (debug && i % step == 0) 
				System.out.println("[step " + i + "] nodes : " + graph.vertexSet().size()
					+ " injected_faults : " + injected_faults.size() + " pending faults : " 
					+ pending_faults.size()); 
			
			pending_faults.addAll(minus(detect_fault_injection_points(),injected_faults));
			
			if (debug && i % step == 0)
				System.out.println("[step " + i + "] new pending faults : " 
					+ pending_faults.size()); 
			commit();
			
			
			// Remove untolerated faults
			
			//for(AndNode f : intersection(maskedBy.keySet(),nonmasking_faults)) {
				//maskedby
			//}
			
			
			if (!intersection(maskedBy.domain(),nonmasking_faults).isEmpty()) {
				System.out.println("Error :(:(:( : " + 
						intersection(maskedBy.domain(),nonmasking_faults)
						.stream()
						.map(x -> get_node(x))
						.collect(Collectors.toSet())
						);
				assert false;
			}
			
		}
		if (debug) System.out.println("non-masking faults : " + nonmasking_faults.size());
		if (debug) System.out.println("fault-injection success."); 
		
		this.masking_relation = maskedBy;
		this.nonmasking_faults = nonmasking_faults;
		
		return maskedBy;
	}
	
	/* AUXILIARY METHOD - REMOVE!!! */
	public AndNode get_node(AndNode n) {
		AndNode res = null;
		for(TableauxNode _n : graph.vertexSet()) {
			if (_n.equals(n)) {
				res = (AndNode) _n;
				break;		
			}
		}
		return res;
	}
	
	
	private StateFormula recovery_formula(Set<Set<StateFormula>> sets) {
		StateFormula res = new False();
		for(Set<StateFormula> s : sets) {
			StateFormula clause = s
					.stream()
					.filter(x -> x instanceof Proposition)
					.reduce(new False(), (x,y) -> new And(x,y));
			res = new Or(res, clause);
		}
		res = new Exists(new Next(res));
		return res;
	}
		
		
	/* Returns a map which domain represents the set of nodes where it is posible to
	 * inject faults. And the image of a node is a set of deontic variables to violate
	 * in order to generate a new fault.
	*/
	public Set<Pair<AndNode,DeonticProposition>> detect_fault_injection_points()
	{
		Set<Pair<AndNode,DeonticProposition>> res = new HashSet<>();
		for(TableauxNode n : graph.vertexSet()) {
			if(n instanceof AndNode) {
				// We check whether we have an AND-Node with obligations to violate.
				Set<DeonticProposition> obligations = new HashSet<DeonticProposition>();
				for(StateFormula f : n.formulas) {
					if(f instanceof DeonticProposition)
						if(prop_sat(n.formulas,((DeonticProposition) f).get_prop()))
							res.add(new Pair(n,f));
				}
			}		
		}
		return res;
	}
		
	public Set<OrNode> inject_fault(AndNode n, StateFormula p) {
		//assert n.formulas.contains(p);
		assert p.is_literal();		
				
		// Collecting propositions that are deontically affected
		Set<Proposition> deontically_affected_props = n.formulas
				.stream()
				.filter(x -> x instanceof DeonticProposition)
				.map(x -> (DeonticProposition) x)
				.map((DeonticProposition x) -> x.get_prop())
				.map(x -> (x instanceof Negation)?((Negation) x).arg():x)
				.map(x -> (Proposition) x)
				.collect(Collectors.toSet());
		
		//System.out.println("deontically_affected_props : " + deontically_affected_props);
		
		// Collecting literals that are based on a deontically affected prop
		Set<StateFormula> next_literals = n.formulas
				.stream()
				.filter(x -> x.is_literal())
				.filter(x -> 
							(x instanceof Proposition && deontically_affected_props.contains((Proposition) x)) ||
							(x instanceof Negation && deontically_affected_props.contains(((Negation) x).arg()))
						
						)
				.map(x -> x.equals(p)?x.negate():x)
				.collect(Collectors.toSet());
		
		// Collecting obligations in the current state
		Set<StateFormula> next_obligations = n.formulas
				.stream()
				.filter(x -> x instanceof DeonticProposition)
				.collect(Collectors.toSet());
		
		
		// This is it. Every formula to be passed on to the next node must be here.
		Set<StateFormula> next_formulas = union(next_literals,next_obligations);
		StateFormula next_state_descriptor = 				
			new Exists(
				new Next(
						next_formulas.stream()
						.reduce(new True(), (x,y) -> new And(x,y))
						)
			);
		
		//System.out.println("next_state_descriptor : " + next_state_descriptor);
		
		Set<StateFormula> new_forms = union(n.formulas, next_state_descriptor);
		
		AndNode ghost = new AndNode(new_forms);
		Set<OrNode> faulty_succs = ghost.tiles();
		
		//for(OrNode o : faulty_succs) {
		//	o.formulas.addAll(carry_on_formulas);
		//}
		
		/*System.out.println("------------------");
		System.out.println("AndNode : " + n + ", Proposition : "+p);
		System.out.println("FaultySuccs : " + faulty_succs);
		System.out.println("n Succs : " + succesors(n, graph));
		System.out.println("------------------");*/
		for(OrNode f : faulty_succs) {
			//if(graph.vertexSet().contains(f))
			//	System.out.println("Fault injection: Strange! Please check this!!");
			graph.addVertex(f);
			graph.addEdge(n, f);
		}
	
		return faulty_succs;
	}
	
	
	
	

	/* *******************************************
	 * 
	 * 
	 * 
	 * 
	 * 				NON MASKING STUFF
	 * 
	 * 
	 * 
	 * 
	 * ******************************************* 
	*/
	
	public void generate_non_masking_faults() {
	
	}
	
	public Set<Pair<AndNode,AndNode>> calc_non_masking_relation() {
		return non_masking_relation_dull();
	}
	
	public Set<Pair<AndNode,AndNode>> non_masking_relation_dull() {
		Set<Pair<AndNode,AndNode>> rel = new HashSet<Pair<AndNode,AndNode>>();
		Set<Pair<AndNode,AndNode>> remove = new HashSet<Pair<AndNode,AndNode>>();
		List<AndNode> nodes = new LinkedList<>();
		nodes.addAll(normal_nodes());
		nodes.addAll(faulty_nodes());
		
		for(AndNode n : normal_nodes()) {
			for(AndNode f : faulty_nodes()) {
				if(sublabeling(n).equals(sublabeling(f)))
						rel.add(new Pair<AndNode,AndNode>(n,f));
			}
		}
		
		boolean change = false;
		do {
			//System.out.println("non_masking_relation_debug : " + rel);
			change = false;
			remove.clear();
			for(Pair<AndNode,AndNode> p : rel) {
				AndNode n1 = p.first;
				AndNode f1 = p.second;
				assert(!n1.faulty);
				assert(f1.faulty);
				
				for(AndNode n2 : postN(n1)) {
					boolean ok = false;
					for(AndNode f2 : transitive_post(f1)) {
						if(rel.contains(new Pair(n2,f2)))
							ok = true;
						if(!f2.faulty)
							ok = true;
					}
					if(!ok) {
						remove.add(p);
						change = true;
					}						
				}				
			}
			rel.removeAll(remove);
		} while (change);
	
		//System.out.println("non_masking_relation_debug : " + rel);
		return rel;		
	}
	
	
	
	
	
	
	public Set<Pair<AndNode,AndNode>> non_masking_relation_efficient() {
		Map<AndNode,Set<AndNode>> mask = new HashMap<AndNode, Set<AndNode>>();
		Map<AndNode,Set<AndNode>> remove = new HashMap<AndNode, Set<AndNode>>();
		Set<AndNode> non_mask = new HashSet<AndNode>();
		
		// Inisialization loop
		for(AndNode n : normal_nodes()) {
			mask.put(n, make_set());
		}
		for(AndNode s2 : faulty_nodes()) {
			mask.put(s2, 
					normal_nodes().stream()
					.filter(n -> sublabeling(n).equals(sublabeling(s2)))
					.collect(Collectors.toSet())
				);
			remove.put(s2,
					minus(normal_nodes(),preN(mask.get(s2)))
				);
			if(mask.get(s2).isEmpty())
				non_mask.add(s2);
		}
		
		System.out.println("<non_masking_relation_debug>");
		System.out.println(mask);
		System.out.println(non_mask);
		System.out.println("</non_masking_relation_debug> \n");
		
		// Main Loop
		Optional<AndNode> pick = faulty_nodes().stream().filter(x -> !remove.get(x).isEmpty()).findAny();
		
		while(pick.isPresent()) {
			AndNode _s2 = pick.get();
			for(AndNode s1 : remove.get(_s2)) {
				for(AndNode s2 : preN(_s2)) {
					if(preN(_s2).contains(s1)) {
						mask.put(s2, minus(mask.get(s2),s1));
						if(mask.get(s2).isEmpty())
							non_mask.add(s2);
						for(AndNode s : preN(s1)) {
							if(intersection(postN(s), mask.get(s2)).isEmpty())
								remove.put(s2, union(remove.get(s2),s));
						}
					}
				}
			}
			remove.put(_s2, SetUtils.make_set());
			pick = faulty_nodes().stream().filter(x -> !remove.get(x).isEmpty()).findAny();
			System.out.println("<non_masking_relation_debug>");
			System.out.println(mask);
			System.out.println(non_mask);
			System.out.println("</non_masking_relation_debug> \n");
		}
		for(AndNode s2 : faulty_nodes()) {
			if(intersection(transitive_post(s2),normal_nodes()).isEmpty()) {
				mask.put(s2, make_set());
			}
		}
		Set<AndNode> new_non_mask = make_set();
		new_non_mask.addAll(non_mask);
		for(AndNode s2 : non_mask) {
			if(intersection(preF(s2),normal_nodes()).isEmpty())
				if(intersection(transitive_post(s2),normal_nodes()).isEmpty())
					new_non_mask.remove(s2);
		}
		non_mask = new_non_mask;
		
		System.out.println("<non_masking_relation_debug>");
		System.out.println(mask);
		System.out.println(non_mask);
		System.out.println("</non_masking_relation_debug> \n");
		
		
		
		Set<Pair<AndNode,AndNode>> res = make_set();
		for(AndNode f : mask.keySet())
			for(AndNode m : mask.get(f))
				res.add(new Pair<AndNode,AndNode>(m,f));
		//res.addAll(times(non_mask,normal_nodes()));
		
		for(Pair p : res)
			assert(p.first instanceof AndNode && p.second instanceof AndNode);
		return res;		
	}
	
	
	
	
	/*	NON Masking Helper Functions
	 * 
	*/
	
	public Set<AndNode> faulty_nodes() {
		return this.graph.vertexSet()
				.stream()
				.filter(n -> n instanceof AndNode && n.faulty)
				.map(n -> (AndNode) n)
				.collect(Collectors.toSet());
	}
	
	public Set<AndNode> normal_nodes() {
		return this.graph.vertexSet()
				.stream()
				.filter(n -> n instanceof AndNode && !n.faulty)
				.map(n -> (AndNode) n)
				.collect(Collectors.toSet());
	}
	
	
	public Set<AndNode> preN(AndNode n) {
		return predecesors(n, graph)
				.stream()
				.map(x -> predecesors(x,graph))
				.reduce(SetUtils.make_set(),SetUtils::union)
				.stream()
				.filter(x -> !x.faulty)
				.map(x -> (AndNode) x)
				.collect(Collectors.toSet());
	}
	
	public Set<AndNode> preF(AndNode n) {
		return predecesors(n, graph)
				.stream()
				.map(x -> predecesors(x,graph))
				.reduce(SetUtils.make_set(),SetUtils::union)
				.stream()
				.filter(x -> x.faulty)
				.map(x -> (AndNode) x)
				.collect(Collectors.toSet());
	}
	
	public Set<AndNode> post(AndNode n) {
		return union(postN(n), postF(n));
	}
	
	public Set<AndNode> postN(AndNode n) {
		return succesors(n, graph)
				.stream()
				.map(x -> succesors(x,graph))
				.reduce(SetUtils.make_set(),SetUtils::union)
				.stream()
				.filter(x -> !x.faulty)
				.map(x -> (AndNode) x)
				.collect(Collectors.toSet());
	}
	
	public Set<AndNode> postF(AndNode n) {
		return succesors(n, graph)
				.stream()
				.map(x -> succesors(x,graph))
				.reduce(SetUtils.make_set(),SetUtils::union)
				.stream()
				.filter(x -> x.faulty)
				.map(x -> (AndNode) x)
				.collect(Collectors.toSet());
	}
	
	public Set<AndNode> preN(Set<AndNode> nodes) {
		return nodes
				.stream()
				.map(x -> preN(x))
				.reduce(SetUtils.make_set(),SetUtils::union);
	}
	
	public Set<AndNode> transitive_post(AndNode n) {
		Set<AndNode> res = make_set(n);
		Set<AndNode> res_old = make_set();
		
		do {
			res_old.clear();
			res_old.addAll(res);
			
			res.addAll(
					res
					.stream()
					.map((TableauxNode x) -> succesors(x,graph))
					.reduce(make_set(), SetUtils::union)
					.stream()
					.map((TableauxNode x) -> succesors(x,graph))
					.reduce(make_set(), SetUtils::union)
					.stream()
					.map(x -> (AndNode) x)
					.collect(Collectors.toSet())
					);
		} while(!res.equals(res_old));		
		
		return res;
	}
	
	public Relation<AndNode,AndNode> transitive_succ() {
		Relation<AndNode,OrNode> tiles = new Relation<>();
		Relation<OrNode,AndNode> blocks = new Relation<>();
		
		for(DefaultEdge e : this.graph.edgeSet()) {
			TableauxNode src = this.graph.getEdgeSource(e);
			TableauxNode tgt = this.graph.getEdgeTarget(e);
			if(src instanceof AndNode && tgt instanceof OrNode) {
				tiles.add(new Pair(src,tgt));
			} else if(src instanceof OrNode && tgt instanceof AndNode) {
				blocks.add(new Pair(src,tgt));
			} else
				throw new Error("Fatal: Malformed Tableaux");
		}
		
		Relation<AndNode,AndNode> and_and = Relation.compose(tiles, blocks);
		return Relation.closure(and_and);
	}
	
	public Set<Proposition> sublabeling(TableauxNode n) {
		Set<Proposition> props = n.formulas.stream()
				.filter(x -> x instanceof Proposition)
				.map(x -> (Proposition) x)
				.collect(Collectors.toSet());
		return intersection(props,this._spec._interface);
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
				System.out.println("Node : " + o);
				ModelNode new_node = new ModelNode(succesors(o,graph).stream().findAny().get());
				res.addVertex(new_node);
				res.addEdge(frag_root, new_node);
			}
			
		}
		
		assert res.vertexSet().size() >= 1;
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
		
		
	
		DirectedGraph<ModelNode, DefaultEdge> current_frag = frag(choose_block((OrNode) this.root));
		ModelNode current_frag_root = root(current_frag);
		fragment_roots.put(current_frag_root, current_frag);
		copy(model,null,current_frag,null);
		int i = 0;
		Debug.to_file(Debug.to_dot(model, Debug.model_node_render_min), "output/model"+ i++ + ".dot");
		while(!frontier(model).isEmpty()) {
			ModelNode current = frontier(model).stream().findAny().get();
			Optional<ModelNode> candidate = model
					.vertexSet()
					.stream()
					.filter(x -> x.copyOf.equals(current.copyOf))
					.filter(x -> fragment_roots.get(x) != null)	
					.findFirst();
			if(candidate.isPresent())
				identify(model,candidate.get(),current);
			else {
				current_frag = frag((AndNode) current.copyOf);
				current_frag_root = root(current_frag);
				fragment_roots.put(current_frag_root, current_frag);
				copy(model,current,current_frag,current_frag_root);
			}
			Debug.to_file(Debug.to_dot(model, Debug.model_node_render_min), "output/model"+ i++ + ".dot");
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
	
	
	/* *******************************************
	 * 
	 * 					PRINTING
	 * 
	 * ******************************************* 
	*/
	
	public void to_dot(
			String path,
			Function<TableauxNode,String> node_renderer
		) {
		to_dot(path, node_renderer, new Relation<>());
	}

	public void to_dot(
				String path,
				Function<TableauxNode,String> node_renderer,
				Relation<AndNode,AndNode> nmask
			) 
	{
		String res = "";	
		res += "digraph {\n";
		
		int i = 0;
		HashMap<TableauxNode,String> map = new HashMap<TableauxNode, String>();
		
		for(TableauxNode n : graph.vertexSet()) {
			map.put(n, "n"+i);
			res += "n"+i + node_renderer.apply(n) + "\n";
			i++;
		}
		res += "\n";
		for(DefaultEdge e : graph.edgeSet()) {
			res += map.get(graph.getEdgeSource(e)) + "->" + map.get(graph.getEdgeTarget(e)) + ";\n";
		}
		
		for(Pair<AndNode,AndNode> p : nmask) {
			res += map.get(p.second) + "->" + map.get(p.first) + " [color=red];\n";
		}
		
		res += "}";		
		to_file(res, path);
	}
	
	
	public Set<AndNode> tolerated_faults(Relation<AndNode,AndNode> nmask) {
		Set<AndNode> tolerated_faults = nmask
				.stream()
				.filter(p -> normal_nodes().contains(p.first))
				.map(p -> p.second)
				.collect(Collectors.toSet());
		
		// Supongo que la relacion es transitiva, asi que no hace
		// falta lo proximo, en principio.
		Set<AndNode> _new;
		/*do {
			_new = nmask
				.stream()
				.filter(p -> tolerated_faults.contains(p.first))
				.map(p -> p.second)
				.collect(Collectors.toSet());
			tolerated_faults.addAll(_new);
		} while (!_new.isEmpty());*/
		return intersection(tolerated_faults,faulty_nodes());
	}
	
	
	public void to_dot_levels_of_tolerance(
			String path,
			Function<TableauxNode,String> node_renderer,
			Relation<AndNode,AndNode> nmask
		) 
	{		

		Set<AndNode> norm = normal_nodes();
		Set<AndNode> yellow = tolerated_faults(nmask);		
		Set<AndNode> red = minus(faulty_nodes(), yellow);
		
		String res = "";	
		res += "digraph {\n";
		
		int i = 0;
		HashMap<TableauxNode,String> map = new HashMap<TableauxNode, String>();
		
		for(TableauxNode n : graph.vertexSet()) {
			map.put(n, "n"+i);
			res += "n"+i;
			String style = null;
			if(norm.contains(n))
				style = "color=green";
			else if (yellow.contains(n))
				style = "color=yellow";
			else if (red.contains(n))
				style = "color=red";
			
			res +=  "[shape=" + ((n instanceof AndNode)?"box":"circle") +
					((style != null)?("," + style): "") +
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
		for(DefaultEdge e : graph.edgeSet()) {
			res += map.get(graph.getEdgeSource(e)) + "->" + map.get(graph.getEdgeTarget(e)) + ";\n";
		}
		
		//for(Pair<AndNode,AndNode> p : nmask) {
			//res += map.get(p.first) + "->" + map.get(p.second) + " [color=red];\n";
		//}
		
		res += "}";		
		to_file(res, path);
	}
	
	
	private void to_file(String content, String path) {
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
