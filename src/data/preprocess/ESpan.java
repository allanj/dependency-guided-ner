package data.preprocess;

public class ESpan {

	int left; //inclusive
	int right; //inclusive
	String entity;

	public ESpan(int left, int right, String entity){
		this.left = left;
		this.right = right;
		this.entity = entity;
	}

	@Override
	public String toString() {
		return "ESpan [left=" + left + ", right=" + right + ", entity=" + entity + "]";
	}
	
	
}
