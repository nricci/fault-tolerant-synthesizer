package util;

import java.io.File;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;

public class XMLBuilder {

	private String res;
	
	private LinkedList<String> tag_stack;
	
	public XMLBuilder() {
		res = "";
		tag_stack = new LinkedList<String>();
	}
	
	private int level() {
		return tag_stack.size();
	}
	
	private String indent() {
		String _res = "";
		for(int i = 0; i < level(); i++)
			_res += "\t";
		//assert _res.length() == tag_stack.size();	
		return _res;
	}
	
	public void open(String tag_name, String ... attributes) {
		//System.out.println(tag_name);
		assert(tag_name != null);
		assert(tag_name != "");
		assert(attributes.length % 2 == 0);
		
		
		res +=  indent() + "<" + tag_name;
		for(int i = 0; i < attributes.length; i = i + 2)
			res += " " + attributes[i] + "=\"" + attributes[i+1] + "\""; 
		res += ">\n";
		tag_stack.push(tag_name);
	}
	
	public void close() {
		assert(tag_stack.size() > 0); 
		String tag = tag_stack.pop();
		res +=  indent() + "</" + tag + ">\n";
	}
	
	public void open_self_close(String tag_name, String ... attributes) {
		assert(tag_name != null);
		assert(tag_name != "");
		assert(attributes.length % 2 == 0);
		
		
		res +=  indent() + "<" + tag_name;
		for(int i = 0; i < attributes.length; i = i + 2)
			res += " " + attributes[i] + "=\"" + attributes[i+1] + "\""; 
		res += "/>\n";
	}
	
	public void add_text(String text) {
		res += indent() + text + "\n";
	}
	
	public void to_file(String path) {
		if(path != null) {
			try {
				FileWriter w = new FileWriter(new File(path));
				w.write(this.res);
				w.close();
			} catch (Exception e) {
				System.out.println("Could not write to file.");
				e.printStackTrace();
			}
		}
	}
	
}
