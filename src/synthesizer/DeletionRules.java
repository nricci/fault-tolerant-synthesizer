package synthesizer;

import static dctl.formulas.DCTLUtils.is_consistent;

import java.util.HashSet;
import java.util.Set;

import org.jgrapht.alg.ConnectivityInspector;
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
import util.Pair;
import static util.SetUtils.make_set;
import static util.SetUtils.minus;


public class DeletionRules {

	private Tableaux _t;
	
	private Set<TableauxNode> remaining_nodes;
	
	private Set<TableauxNode> deleted_nodes;
	
	public DeletionRules(Tableaux t) {
		_t = t;
	}
	

	public Set<TableauxNode> apply() {
		remaining_nodes = new HashSet();
		remaining_nodes.addAll(_t.nodes());
		deleted_nodes = new HashSet();
		int i = 0;
		int new_deletions = 0;
		do {
			//_t.to_dot("output/delete_stage_"+ i++ +".dot", Debug.default_node_render);
			int old_deletions = deleted_nodes.size();
			
			check_empty_succs();
			delete_inconsistent_prop();
			delete_AU();
			delete_EU();
		
			new_deletions = deleted_nodes.size() - old_deletions;
		} while(new_deletions > 0);

		//_t.to_dot("output/delete_stage_"+ i++ +".dot", Debug.default_node_render);
		delete_unreachable();		
		return deleted_nodes;
	}
	
	private void check_empty_succs() {
		Set<TableauxNode> i = new HashSet<>();
		i.addAll(remaining_nodes);
		
		for(TableauxNode n : i)
			if(remaining_nodes.contains(n))
				if(_t.succesors(n).isEmpty())
					delete_node(n);
	}
	private void delete_inconsistent_prop() {
		Set<TableauxNode> i = new HashSet<>();
		i.addAll(remaining_nodes);
		
		for(TableauxNode n : i)
			if(remaining_nodes.contains(n))
				if(!is_consistent(n.formulas))
					delete_node(n);
	}
	
	private void delete_EU() {
		Set<TableauxNode> i = new HashSet<>();
		i.addAll(remaining_nodes);
		
		for(TableauxNode n : i)
			if(remaining_nodes.contains(n) && n instanceof AndNode)
				for(StateFormula f : n.formulas) {
					if (f instanceof Exists) {
						Exists _e = (Exists) f;
						if (_e.arg() instanceof Until) {
							Until _u = (Until) _e.arg();
							StateFormula _l = _u.arg_left();
							StateFormula _r = _u.arg_right();
							if(_t.dag_tagEU((AndNode) n, _e, _l, _r).get(n) == null)
								delete_node(n);
						}
					}
				}
				
	}
	
	private void delete_AU() {
		Set<TableauxNode> i = new HashSet<>();
		i.addAll(remaining_nodes);
		
		for(TableauxNode n : i)
			if(remaining_nodes.contains(n) && n instanceof AndNode)
				for(StateFormula f : n.formulas) {
					if (f instanceof Forall) {
						Forall _e = (Forall) f;
						if (_e.arg() instanceof Until) {
							Until _u = (Until) _e.arg();
							StateFormula _l = _u.arg_left();
							StateFormula _r = _u.arg_right();
							if(_t.dag_tagAU((AndNode) n, _e, _l, _r).get(n) == null)
								delete_node(n);
						}
					}
				}
	}
	
	
	
	private void check_or_node(OrNode p) {
		if(_t.succesors(p).isEmpty())
			delete_node(p);
	}


	private void delete_node(TableauxNode n) {
		if (deleted_nodes.contains(n)) return;
		if(n instanceof AndNode) {
			for(TableauxNode p : _t.predecesors(n)) {
				assert p instanceof OrNode;
				check_or_node((OrNode) p);
			}	
			_t.delete_node(n);
			deleted_nodes.add(n);
			remaining_nodes.remove(n);
		}
		if(n instanceof OrNode) {
			for(TableauxNode p : _t.predecesors(n)) {
				assert p instanceof AndNode;
				delete_node(p);
			}		
			_t.delete_node(n);
			deleted_nodes.add(n);
			remaining_nodes.remove(n);
		}
	}
	
	public void delete_unreachable() {
		Set<TableauxNode> reach = new HashSet<>();
		if(_t.root != null)
			 reach.addAll(_t.reachable_nodes(_t.root));
			
		Set<TableauxNode> to_delete = minus(_t.nodes(), reach);	
		for(TableauxNode n : to_delete) {
			_t.delete_node(n);
			deleted_nodes.add(n);
			remaining_nodes.remove(n);
		}
	}

	
	
}
