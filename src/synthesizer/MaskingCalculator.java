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
import static util.SetUtils.all;
import static util.SetUtils.some;
import static util.SetUtils.union;

public class MaskingCalculator {
	
	private Tableaux _t;
	
	private Relation<AndNode,AndNode> _relation;
	
	public MaskingCalculator(Tableaux t) {
		_t = t;
		_relation = new Relation<>();
	}
	
	public MaskingCalculator(Tableaux t, Relation<AndNode,AndNode> r) {
		_t = t;
		_relation = r;
	}	
	
	private int initialize() {
		int changes = _relation.size();
		Set<AndNode> and_nodes = _t.and_nodes();
		Set<AndNode> new_ones = minus(and_nodes, union(_relation.domain(),_relation.img()));
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
		
		//Relation<AndNode,AndNode> transitive_post = _t.transitive_succ();
		
		boolean change = false;
		do {
			//System.out.println("relation size : " + _relation.size());
			
			change = false;
			remove.clear();
			int i = 0;
			for(Pair<AndNode,AndNode> p : _relation) {
				if(p.first.equals(p.second)) continue;
				//System.out.print("\r\r\r\r\r" + String.format("%05d", i++));
				AndNode n1 = p.first;
				AndNode n2 = p.second;
				//System.out.print("(" + n1 +","+ n2 + ")");
				boolean global_ok = true;
				
				boolean ok = all(_t.postN(n1), 
					_n1 -> some(_t.postN(n2),
							_n2 -> _relation.contains(new Pair(_n1,_n2))
							
							)	
				);
				
				/*boolean ok = false;
				for(AndNode _n1 : _t.postN(n1)) {
					boolean ok2 = false;
					for(AndNode _n2 : _t.postN(n2)) {
						ok2 |= _relation.contains(new Pair(_n1,_n2));
					}
					ok &= ok2;
				}*/
				//System.out.print("\tZig : " + ok);
				global_ok &= ok;
				
				ok = all(_t.postN(n2), 
						_n2 -> some(_t.postN(n1),
								_n1 -> _relation.contains(new Pair(_n2,_n1))								
								)	
					);
				/*ok = false;
				for(AndNode _n2 : _t.postN(n2)) {
					boolean ok2 = false;
					for(AndNode _n1 : _t.postN(n1)) {
						ok2 |= _relation.contains(new Pair(_n2,_n1));
					}		
					ok &= ok2;
				}*/
				//System.out.print("\tZag : " + ok);
				global_ok &= ok;
				
				/*ok = false;
				for(AndNode _n2 : _t.postF(n2)) {
					boolean ok2 = _relation.contains(new Pair(_n2,n1));
					if(!ok) {
						for(AndNode _n1 : _t.postN(n1)) {
							ok2 |= _relation.contains(new Pair(_n2,_n1));
						}	
					}	
					ok &= ok2;
				}*/
				ok = all(_t.postF(n2), 
						_n2 -> _relation.contains(new Pair(_n2,n1)) || some(_t.postN(n1),
								_n1 -> _relation.contains(new Pair(_n2,_n1))								
								)	
					);
				//System.out.println("\tFZig : " + ok);
				global_ok &= ok;
				
				if(!global_ok) {
					remove.add(p);
					change = true;
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
