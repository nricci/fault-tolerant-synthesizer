package util;

import java.util.HashSet;
import java.util.Set;

public final class SetUtils {
	
	public static <E> Set<E> make_set() { return new HashSet<E>(); }
	
	public static <E> Set<E> make_set(E ... elems) { 
		Set<E> res = new HashSet<E>();
		for (E x : elems)
			res.add(x);
		return res;
	}
	
	
	
	public static <E> Set<E> union(Set<E> s1, Set<E> s2) {
		assert s1 != null && s2 != null : 
			"s1 != null && s2 != null : s1="+s1+",s2="+s2;
		
		Set<E> r = new HashSet<E>();
		r.addAll(s1);
		r.addAll(s2);		
		return r;
	}
	
	public static <E> Set<E> union(Set<E> s, E ... elems) {
		assert(s != null);
		assert(elems != null);
		
		Set<E> r = new HashSet<E>();
		r.addAll(s);
		for (E x : elems)
			r.add(x);		
		return r;
	}
	
	public static <E> Set<E> intersection(Set<E> s1, Set<E> s2) {
		assert(s1 != null && s2 != null);
		
		Set<E> r = new HashSet<E>();
		for(E x : s1)
			if (s2.contains(x))
				r.add(x);		
		return r;
	}
	
	public static <E> Set<E> minus(Set<E> s1, Set<E> s2) {
		assert(s1 != null && s2 != null);
		
		Set<E> r = new HashSet<E>();
		for(E x : s1)
			if (!s2.contains(x))
				r.add(x);		
		return r;
	}
	
	public static <E> Set<E> minus(Set<E> s, E ... elems) {
		Set<E> r = new HashSet<E>();
		r.addAll(s);
		for(E x : elems)
			if (r.contains(x))
				r.remove(x);		
		return r;
	}
	
	public static <E,F> Set<Pair<E,F>> times(Set<E> s1, Set<F> s2) {
		assert(s1 != null && s2 != null);
		
		Set<Pair<E,F>> r = new HashSet<>();
		for(E x : s1)
			for(F y : s2)
				r.add(new Pair(x,y));		
		return r;
	}
	
	
	public static <E> boolean some(Set<E> set, Predicate<E> prop) {
		for(E _elem : set)
			if(prop.eval(_elem))
				return true;
		return false;
	}
	
	public static <E> boolean all(Set<E> set, Predicate<E> prop) {
		for(E _elem : set)
			if(!prop.eval(_elem))
				return false;
		return true;
	}
	
	public static <E> Set<E> filter(Set<E> set, Predicate<E> prop) {
		Set<E> res = new HashSet<E>();
		for(E _elem : set)
			if(prop.eval(_elem))
				res.add(_elem);
		return res;
	}
	
	public static <E> E pick(Set<E> set, Predicate<E> prop) {
		Set<E> res = filter(set,prop);
		if (res.isEmpty()) 
			return null;
		else
			return res.iterator().next();
	}	
	
	public static <E> Set<E> map(Set<E> set, Function<E> f) {
		Set<E> r = new HashSet<E>();
		for (E x : set)
			r.add(f.eval(x));
		return r;
	}
	
	
	
}
