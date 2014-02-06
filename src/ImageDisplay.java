import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.*;

import org.jgrapht.Graph;
import org.jgrapht.ext.DOTExporter;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.VertexNameProvider;
import org.jgrapht.graph.DefaultEdge;

import tableaux.TableauxNode;


public class ImageDisplay extends JFrame {

	JLabel label;
	JTextField name;
	JButton display;

	JPanel upperPanel;

	public static void display_graph(Graph g) {
		//JFrame frame=new JFrame("Image Demo");

		to_dot("viz.dot",g);
		try {
			Process child = Runtime.getRuntime().exec("dot -Tpng viz.dot > viz.png");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ImageDisplay img = new ImageDisplay("viz.png");
		//frame.add(img);
		img.setSize(800,600);
		img.setVisible(true);
		//img.setDefaultCloseOperation(EXIT_ON_CLOSE);
		img.setResizable(false);
	}
	
	ImageDisplay(String file)
	{
		super("Image Demo");
		label=new JLabel("Enter name of the Image you want to open");
		name=new JTextField(15);
		name.setText(file);
		display=new JButton("Display Image");

		DrawPanel panel=new DrawPanel();
		display.addActionListener(panel);
		upperPanel=new JPanel();
		setLayout(new BorderLayout());
		upperPanel.add(label);
		upperPanel.add(name);
		upperPanel.add(display);


		add(upperPanel,BorderLayout.NORTH);
		add(panel,BorderLayout.CENTER);

//		try {
//			Process child = Runtime.getRuntime().exec("rm viz.png; rm viz.dot");
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	private class DrawPanel extends JPanel implements ActionListener{
		Image image;
		public void actionPerformed(ActionEvent e)
		{
			String text=name.getText().trim();
			image=new ImageIcon(text).getImage();
			repaint();

		}
		public void paintComponent(Graphics g)
		{	
			setBackground(Color.WHITE);
			g.setColor(Color.WHITE);
			g.fillRect(10,10,800,600);
			g.drawImage(image,10,10,this);
		}

	}
	
	
	public static String to_dot(String file, Graph graph) {
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
			expo.export(new FileWriter(new File(file)), graph);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}


}

