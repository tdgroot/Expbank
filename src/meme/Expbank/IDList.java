package meme.Expbank;

import java.util.ArrayList;

public class IDList {

	public IDList() {
		
	}
	public ArrayList <String> names = new ArrayList<String>();
	String[] derp = new String[0x0003f];

	public boolean getListed(String name) {
		for (String player : names) {
			if (player == name)
				return true;
		}
		return false;
	}

	public boolean addToList(String name, int id) {
		names.add(name);
		return true;
	}
}
