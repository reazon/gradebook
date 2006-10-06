/**********************************************************************************
*
* $Id: AbstractGradeRecord.java 732 2006-07-11 17:50:23Z josh $
*
***********************************************************************************
*
* Copyright (c) 2005 The Regents of the University of California, The MIT Corporation
*
* Licensed under the Educational Community License, Version 1.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*      http://www.opensource.org/licenses/ecl1.php
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*
**********************************************************************************/

package org.sakaiproject.tool.gradebook;

import java.io.Serializable;
import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * AbstractGradeRecord is the abstract base class for Grade Records, which are
 * records of instructors (or the application, in the case of autocalculated
 * gradebooks) assigning a grade to a student for a particular GradableObject.
 *
 * @author <a href="mailto:jholtzman@berkeley.edu">Josh Holtzman</a>
 */
public abstract class AbstractGradeRecord implements Serializable {
    protected Long id;
    protected int version;
    protected String studentId;
    protected String graderId;
    protected GradableObject gradableObject;
    protected Date dateRecorded;
    protected Double pointsEarned;

    public abstract Double getGradeAsPercentage();

    /**
     * @return Whether this is a course grade record
     */
    public abstract boolean isCourseGradeRecord();

    /**
     * @return Returns the pointsEarned
     */
    public Double getPointsEarned() {
        return pointsEarned;
    }

	/**
	 * @param pointsEarned The pointsEarned to set.
	 */
	public void setPointsEarned(Double pointsEarned) {
		this.pointsEarned = pointsEarned;
	}
	/**
	 * @return Returns the dateRecorded.
	 */
	public Date getDateRecorded() {
		return dateRecorded;
	}
	/**
	 * @param dateRecorded The dateRecorded to set.
	 */
	public void setDateRecorded(Date dateRecorded) {
		this.dateRecorded = dateRecorded;
	}
	/**
	 * @return Returns the gradableObject.
	 */
	public GradableObject getGradableObject() {
		return gradableObject;
	}
	/**
	 * @param gradableObject The gradableObject to set.
	 */
	public void setGradableObject(GradableObject gradableObject) {
		this.gradableObject = gradableObject;
	}
	/**
	 * @return Returns the id.
	 */
	public Long getId() {
		return id;
	}
	/**
	 * @param id The id to set.
	 */
	public void setId(Long id) {
		this.id = id;
	}
	/**
	 * @return Returns the version.
	 */
	public int getVersion() {
		return version;
	}
	/**
	 * @param version The version to set.
	 */
	public void setVersion(int version) {
		this.version = version;
	}
	/**
	 * @return Returns the graderId.
	 */
	public String getGraderId() {
		return graderId;
	}
	/**
	 * @param graderId The graderId to set.
	 */
	public void setGraderId(String graderId) {
		this.graderId = graderId;
	}
	/**
	 * @return Returns the studentId.
	 */
	public String getStudentId() {
		return studentId;
	}
	/**
	 * @param studentId The studentId to set.
	 */
	public void setStudentId(String studentId) {
		this.studentId = studentId;
	}

    public String toString() {
        return new ToStringBuilder(this).
		append("id", id).
		append("studentId", studentId).
        append("graderId", graderId).toString();
    }

}



