package com.statnlp.projects.entity.utils;

/**
 * an entity in an sentence;
 * @author allanjie
 *
 */
public class EntitySpan {

	private String entityType;
	private int left;
	private int right;
	
	/**
	 * Left and right indices are inclusive.
	 * @param entityType
	 * @param left
	 * @param right
	 */
	public EntitySpan(String entityType,int left,int right){
		this.entityType = entityType;
		this.left = left;
		this.right = right;
	}

	public String getEntityType() {
		return entityType;
	}

	public int getLeft() {
		return left;
	}

	public int getRight() {
		return right;
	}

	@Override
	public String toString() {
		return "Entity [entityType=" + entityType + ", left=" + left + ", right=" + right + "]";
	}
	
	
}
