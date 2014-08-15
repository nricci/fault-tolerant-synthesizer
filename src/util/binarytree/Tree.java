package util.binarytree;

import java.util.HashSet;
import java.util.Set;

public class Tree<E> {


	private  Tree<E> left;
	private  Tree<E> right;
	private  E value;

	public Tree(E value) {
		this.left = null ;
		this.right = null ;
		this.value = value;
	} 

	public Tree(E value, Tree<E> left, Tree<E> right) {
		this.value = value;
		this.left= left;
		this.right = right;
	}
	
	public void set_left(Tree<E> left) {
		this.left = left;
	}
	
	public void set_right(Tree<E> right) {
		this.right = right;
	}
	
	public Tree<E> left() {return this.left;}
	
	public Tree<E> right() {return this.right;}

	
	
	public E val() {return this.value;}
	
	public void printInOrder(){
		if (this!=null && this.value != null) {                  
			if (this.left != null)
				this.left.printInOrder();
			System.out.print(" " + this.value + " ");
			if (this.right != null)
				this.right.printInOrder();
		}
	}

	public void printPreOrder(){
		if (this!=null && this.value != null) {                  
			System.out.print(" " + this.value + " ");
			if (this.left != null)
				this.left.printPreOrder();
			if (this.right != null)
				this.right.printPreOrder();           
		}
	}

	/* public void printByLevel(){
    int level = 0;
    QueueList



        if (this!=null && this.value != null) {                  
            System.out.println("          " + this.value + " ");
            System.out.println("     /    " + this.value + "     \ ");

            if (this.left != null){
                this.left.printInOrder();
            }
	    else{
                 System.out.println("  null ");
            }

            if (this.right != null){
                this.right.printInOrder();
            }
            else{
                 System.out.println("  null ");
            }
	}
    }*/

}
