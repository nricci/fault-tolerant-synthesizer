package util;

public class Gauge {

	String val = "";

	public void print(String s) {
		for(int i = 0; i<val.length(); i++)
			System.out.print("\r");
		
		val = s;
		System.out.print(val);		
	}
	
}
