/*
 * Copyright (C) Reazon Systems, Inc.  All rights reserved.
 */
package org.sakaiproject.tool.gradebook.iRubric;

import java.io.Serializable;

public class GradableObjectRubric implements Serializable {
	
	public GradableObjectRubric(Long gradableObjectId, String rubricId,
			String rubricTitle) {
		this.gradableObjectId = gradableObjectId;
		iRubricId = rubricId;
		iRubricTitle = rubricTitle;
	}
	
	public GradableObjectRubric() {
	}
	

	private Long id;
	private Long gradableObjectId;
	private String iRubricId;
	private String iRubricTitle;
	/**
	 * @return the id
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id the id to set
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * @return the assignmentId
	 */
	public Long getGradableObjectId() {
		return gradableObjectId;
	}
	/**
	 * @param assignmentId the assignmentId to set
	 */
	public void setGradableObjectId(Long gradableObjectId) {
		this.gradableObjectId = gradableObjectId;
	}

	/**
	 * @return the iRubricId
	 */
	public String getiRubricId() {
		return iRubricId;
	}

	/**
	 * @param rubricId the iRubricId to set
	 */
	public void setiRubricId(String rubricId) {
		iRubricId = rubricId;
	}

	/**
	 * @return the iRubricTitle
	 */
	public String getiRubricTitle() {
		return iRubricTitle;
	}

	/**
	 * @param rubricTitle the iRubricTitle to set
	 */
	public void setiRubricTitle(String rubricTitle) {
		iRubricTitle = rubricTitle;
	}	

}
