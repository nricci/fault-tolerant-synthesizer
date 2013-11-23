package tableaux;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.alg.ConnectivityInspector;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

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

	
	/*
	 * 
	 *  Auxiliary Functions
	 * 
	 * 
	 * 
	*/
	
	private DirectedGraph<TableauxNode, DefaultEdge> dag(AndNode n, Quantifier g) {
		Map<TableauxNode,Integer> tag = new HashMap<TableauxNode,Integer>();
		Until u = (Until) g.arg();
		StateFormula f = u.arg_left();
		StateFormula h = u.arg_left();		
		if(g instanceof Forall) {
			for(TableauxNode _n : graph.vertexSet()) {
				if((_n.formulas.contains(h) || h instanceof True) && _n instanceof AndNode) {
					tag.put(_n, 0);
				} else {
					tag.put(_n, null);
				}
			}
			for(TableauxNode dummy : graph.vertexSet()) {
				for(TableauxNode _n : graph.vertexSet()) {
					boolean crazy_ass_condition = true;
					crazy_ass_condition &= _n instanceof AndNode;
					crazy_ass_condition &= _n.formulas.contains(g);
					crazy_ass_condition &= _n.formulas.contains(f);
					crazy_ass_condition &= tag.get(_n) == null;
					for(DefaultEdge e : graph.outgoingEdgesOf(_n)) {
						crazy_ass_condition &= tag.get(graph.getEdgeTarget(e)) != null;
					}				
					if(crazy_ass_condition) {
						Integer i = -1;
						for(DefaultEdge e : graph.outgoingEdgesOf(_n)) {
							i = tag.get(graph.getEdgeTarget(e))>i?tag.get(graph.getEdgeTarget(e)):i;
						}
						tag.put(_n, 1 + i);
					} 
					
					crazy_ass_condition = true;
					crazy_ass_condition &= _n instanceof OrNode;
					crazy_ass_condition &= _n.formulas.contains(g);
					crazy_ass_condition &= tag.get(_n) == null;
					boolean x = false;
					for(DefaultEdge e : graph.outgoingEdgesOf(_n)) {
						x |= tag.get(graph.getEdgeTarget(e)) != null;
					}
					crazy_ass_condition &= x;
					if(crazy_ass_condition) {
						Integer i = -1;
						for(DefaultEdge e : graph.outgoingEdgesOf(_n)) {
							i = tag.get(graph.getEdgeTarget(e))<i?tag.get(graph.getEdgeTarget(e)):i;
						}
						tag.put(_n, i);
					}
				}
			}		
			
			DirectedGraph<TableauxNode, DefaultEdge> _dag = new DefaultDirectedGraph(DefaultEdge.class);
			Set<TableauxNode> _frontier = new HashSet<TableauxNode>();
			
			_dag.addVertex(n);
			_frontier.add(n);
			
			boolean _halt = true;
			for(TableauxNode x : _frontier)
				_halt &= x instanceof AndNode && tag.get(x) == 0;  
			
			while(!_halt) {
				for(TableauxNode x : _frontier) {
					if(x instanceof OrNode) {
						
					}
					
					
				}			
				
				_halt = true;
				for(TableauxNode x : _frontier)
					_halt &= x instanceof AndNode && tag.get(x) == 0;  				
			}
			
			
		} else if(g instanceof Exists) {
			
			
			
			
			
		}
		
		return null;
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

	private boolean reach(TableauxNode root, 
			Set<TableauxNode> visited, 
			StateFormula g) 
	{
		if(!visited.contains(root)) {
			Set<TableauxNode> new_visited = new HashSet<TableauxNode>();
			new_visited.addAll(visited);
			new_visited.add(root);
			if(root.formulas.contains(g))
				return true;
			else {
				boolean res = false;
				for(DefaultEdge s : graph.outgoingEdgesOf(root)) {
					visited.add(root);
					res = res || reach(graph.getEdgeTarget(s),new_visited,g);
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

	private boolean full_subdag(TableauxNode root, 
			Set<TableauxNode> dag,
			StateFormula g) 
	{
		if(dag.contains(root)) 
			return false;

		Set<TableauxNode> new_dag = new HashSet<TableauxNode>();
		new_dag.addAll(dag);
		new_dag.add(root);

		if(root instanceof AndNode) {
			if(root.formulas.contains(g))
				return true;
			else {
				boolean res = true;
				for(DefaultEdge s : graph.outgoingEdgesOf(root)) {
					res = res && full_subdag(graph.getEdgeTarget(s),new_dag,g);
				}
				return res;
			}
		} else if(root instanceof OrNode) {
				boolean res = false;
				for(DefaultEdge s : graph.outgoingEdgesOf(root)) {
					res = res || full_subdag(graph.getEdgeTarget(s),new_dag,g);
				}
				return res;		
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
