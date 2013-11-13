package util.binarytree;

import java.util.HashSet;
import java.util.Set;

public class BinaryTree<E> {


	private  BinaryTree<E> left;
	private  BinaryTree<E> right;
	private  E value;

	public BinaryTree(E value) {
		this.left = null ;
		this.right = null ;
		this.value = value;
	} 

	public BinaryTree(E value, BinaryTree<E> left, BinaryTree<E> right) {
		this.value = value;
		this.left= left;
		this.right = right;
	}
	
	public void set_left(BinaryTree<E> left) {
		this.left = left;
	}
	
	public void set_right(BinaryTree<E> right) {
		this.right = right;
	}
	
	public BinaryTree<E> left() {return this.left;}
	
	public BinaryTree<E> right() {return this.right;}

	
	
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
