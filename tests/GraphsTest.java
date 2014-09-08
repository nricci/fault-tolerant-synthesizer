import static org.junit.Assert.*;

import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.junit.Test;

import tableaux.AndNode;
import tableaux.OrNode;
import tableaux.TableauxNode;


public class GraphsTest {

	@Test
	public void test() {
		DirectedGraph<TableauxNode, DefaultEdge> graph = new DefaultDirectedGraph<TableauxNode, DefaultEdge>(DefaultEdge.class); 
		
		TableauxNode node1 = new OrNode(null);
		TableauxNode node2 = new AndNode(null);
		graph.addVertex(node1);
		graph.addVertex(node2);
		node1.faulty = true;
		graph.addEdge(node1, node2);
		assertTrue(graph.outgoingEdgesOf(node1).size() == 1);
	}
	
	@Test
	public void test2() {
		DirectedGraph<TableauxNode, DefaultEdge> graph = new DefaultDirectedGraph<TableauxNode, DefaultEdge>(DefaultEdge.class); 
		
		TableauxNode node1 = new OrNode(null);
		graph.addVertex(node1);
		node1.faulty = true;
		assertTrue(graph.vertexSet().contains(node1));
	}
	

}
