package com.statnlp.commons.types;


public class WordToken extends InputToken{
	
	private static final long serialVersionUID = -1296542134339296118L;
	
	private String tag;
	private int headIndex; 
	private String entity;
	private String depLabel;
	
	private String[] fs; //feature string, useless for general purpose.
	
	public WordToken(String name) {
		super(name);
	}
	
	public WordToken(String name, String tag) {
		super(name);
		this.tag = tag;
	}
	
	public WordToken(String name, String tag, int headIndex) {
		super(name);
		this.tag = tag;
		this.headIndex = headIndex;
		this.entity = "O";
	}
	
	public WordToken(String name, String tag, int headIndex, String entity) {
		super(name);
		this.tag = tag;
		this.headIndex = headIndex;
		this.entity = entity;
	}
	
	public WordToken(String name, String tag, int headIndex, String entity, String depLabel) {
		super(name);
		this.tag = tag;
		this.headIndex = headIndex;
		this.entity = entity;
		this.depLabel = depLabel;
	}
	
	
	public String getTag(){
		return this.tag;
	}
	
	public String getATag(){
		return this.tag.substring(0, 1);
	}
	
	public void setHead(int index){
		this.headIndex = index;
	}
	
	public int getHeadIndex(){
		return this.headIndex;
	}
	
	public void setEntity(String entity){
		this.entity = entity;
	}
	
	public String getEntity(){
		return this.entity;
	}
	
	public void setTag(String tag) {
		 this.tag = tag;
	}
	
	public String getDepLabel() {
		return depLabel;
	}

	public void setDepLabel(String depLabel) {
		this.depLabel = depLabel;
	}

	@Override
	public boolean equals(Object o) {
		if(o instanceof WordToken){
			WordToken w = (WordToken)o;
			return w._name.equals(this._name) && w.tag.equals(this.tag) && (w.headIndex == this.headIndex) && w.entity.equals(this.entity);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return this._name.hashCode() +this.tag.hashCode() + this.headIndex + this.entity.hashCode() + 7;
	}
	
	@Override
	public String toString() {
		if(!tag.equals("")) return "Word:"+this._name+"/"+tag+","+headIndex+","+entity;
		return "WORD:"+this._name;
	}
	
	public String[] getFS(){return this.fs;}
	public void setFS(String[] fs){this.fs = fs;}
}