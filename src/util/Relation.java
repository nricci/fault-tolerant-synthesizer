package util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

public class Relation<A,B> implements Set<Pair<A,B>> {

	private Set<Pair<A,B>> _impl;
	
	public Relation() {
		_impl = new HashSet<Pair<A,B>>();
	}
	
	public Relation(Map<A,B> map) {
		_impl = new HashSet<Pair<A,B>>();
		for(Entry<A, B> e : map.entrySet()) {
			_impl.add(new Pair(e.getKey(),e.getValue()));
		}
	}
	

	public Set<A> domain() {
		return _impl.stream().map(p -> p.first).collect(Collectors.toSet());
	}

	public Set<B> img() {
		return _impl.stream().map(p -> p.second).collect(Collectors.toSet());
	}
	
	public Set<B> get(A a) {
		return _impl
				.stream()
				.filter(p -> p.first.equals(a))
				.map(p -> p.second)
				.collect(Collectors.toSet());
	}
	
	
	public static <A,B,C> Relation<A,C> compose(Relation<A,B> r1, Relation<B,C> r2) {
		Relation<A,C> res = new Relation<>();
		for(Pair<A,B> p1 : r1) {
			for(Pair<B,C> p2 : r2) {
				if(p1.second.equals(p2.first)) {
					res.add(new Pair(p1.first,p2.second));
				}
			}
		}
		return res;
	}
	
	public static <A> Relation<A,A> closure(Relation<A,A> rel) {
		Relation<A,A> r = rel;
		int old_size;
		do {
			old_size = r.size();
			r.addAll(compose(r,r));
		} while(old_size < r.size());
		return r;	
	}
	
	
	/*
	 * 
	*/
	
	@Override
	public String toString() {
		return _impl.toString();
	}
	
	
	/* 
	 * 		Methods from Set
	 * 
	*/	
	
	@Override
	public int size() {
		return _impl.size();
	}

	@Override
	public boolean isEmpty() {
		return _impl.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return _impl.contains(o);
	}

	@Override
	public Iterator<Pair<A, B>> iterator() {
		return _impl.iterator();
	}

	@Override
	public Object[] toArray() {
		return _impl.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return _impl.toArray(a);
	}

	@Override
	public boolean add(Pair<A, B> e) {
		return _impl.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return _impl.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return _impl.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends Pair<A, B>> c) {
		return _impl.addAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return _impl.retainAll(c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return _impl.removeAll(c);
	}

	@Override
	public void clear() {
		_impl.clear();		
	}
	
	
	
	
}
