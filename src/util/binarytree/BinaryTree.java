package util.binarytree;

public class BinaryTree {


	private  BinaryTree left;
	private  BinaryTree right;
	private  Object value;

	public BinaryTree(Object value) {
		this.left = null ;
		this.right = null ;
		this.value = value ;
	} 

	public BinaryTree(Object value, BinaryTree left, BinaryTree right) {
		this.value = value;
		this.left= left;
		this.right = right;
	}

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
