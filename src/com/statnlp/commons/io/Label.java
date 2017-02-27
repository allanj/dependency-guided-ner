package com.statnlp.commons.io;

public class Label {

	public int ID;
	public String tag;
	public Label(int iD, String tag) {
//		super();
		ID = iD;
		this.tag = tag;
	}
	public int getID() {
		return ID;
	}
	public void setID(int iD) {
		ID = iD;
	}
	public String getTag() {
		return tag;
	}
	public void setTag(String tag) {
		this.tag = tag;
	}
	
	
	
}
