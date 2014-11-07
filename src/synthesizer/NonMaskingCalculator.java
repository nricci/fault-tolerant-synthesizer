package synthesizer;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import tableaux.AndNode;
import tableaux.Tableaux;
import util.Pair;
import util.Relation;
import static util.SetUtils.minus;

public class NonMaskingCalculator {
	
	private Tableaux _t;
	
	private Relation<AndNode,AndNode> _relation;
	
	private Relation<AndNode,AndNode> _transitive_post;
	
	public NonMaskingCalculator(Tableaux t) {
		_t = t;
		_relation = new Relation<>();
		_transitive_post = _t.transitive_succ();
	}
	
	private int initialize() {
		int changes = _relation.size();
		Set<AndNode> and_nodes = _t.and_nodes();
		Set<AndNode> new_ones = minus(and_nodes, _relation.domain());
		new_ones.stream().forEach(_new -> 
				and_nodes
				.stream()
				.filter(_old -> _t.sublabeling(_old).equals(_t.sublabeling(_new)))
				.forEach(_old -> _relation.add(new Pair(_new,_old)))
				);
		return _relation.size() - changes;
	}
	
	
	public Relation<AndNode,AndNode> compute() {
		if(initialize() == 0) return _relation;	
		
		Set<Pair<AndNode,AndNode>> remove = new HashSet<>();

		for(Pair<AndNode,AndNode> p : _relation)
			assert _t.sublabeling(p.first).equals(_t.sublabeling(p.second));
		
		
				
		boolean change = false;
		do {
			//System.out.println("relation size : " + _relation.size());
			
			change = false;
			remove.clear();
			int i = 0;
			for(Pair<AndNode,AndNode> p : _relation) {
				//System.out.print("\r\r\r\r\r" + String.format("%05d", i++));
				AndNode n1 = p.first;
				AndNode f1 = p.second;
				//assert(!n1.faulty);
				//assert(f1.faulty);
				for(AndNode n2 : _t.postN(n1)) {
					boolean ok = false;
					for(AndNode f2 : _transitive_post.get(f1)) {
						if(_relation.contains(new Pair(n2,f2)))
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
			//System.out.println("\t\tremoving " + remove.size() + "pairs.");
			_relation.removeAll(remove);
		} while (change);
		
		
		for(Pair<AndNode,AndNode> p : _relation)
			assert _t.sublabeling(p.first).equals(_t.sublabeling(p.second));
		
		return _relation;
	}
	
	

}
