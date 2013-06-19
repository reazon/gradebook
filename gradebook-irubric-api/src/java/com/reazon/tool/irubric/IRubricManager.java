package com.reazon.tool.irubric;

import java.util.*;

import org.sakaiproject.tool.gradebook.iRubric.GradableObjectRubric;
import org.sakaiproject.service.gradebook.shared.ConflictingAssignmentNameException;
import org.sakaiproject.service.gradebook.shared.StaleObjectModificationException;
import org.sakaiproject.service.gradebook.shared.GradebookNotFoundException;
import org.sakaiproject.tool.gradebook.LetterGradePercentMapping;

public interface IRubricManager {
   
	/**
     * Updates a GradableObjectRubric object
	 * 
	 * @param gradableObjectRubric A GradableObjectRubric object
	 * @return void
     */
    public void updateGradableObjectRubric(GradableObjectRubric gradableObjectRubric)
    						throws ConflictingAssignmentNameException, StaleObjectModificationException;

    /**
     * Get a GradableObjectRubric object by assignment
     *
     * @param assignmentId The assignment ID
     * @return A GradableObjectRubric object
     */
    public GradableObjectRubric getGradableObjectRubric(Long gradableObjectId);
    
    /**
     * Get all GradableObjectRubrics object by a list of assignment parameters
     *
     * @param assignmentIds The assignment IDs
     * @return A List of GradableObjectRubric objects
     */
    public List getGradableObjectRubrics(List<Long> gradableObjectIds);
    
    /**
     * convert point to letter
     * @param gradebook
     * @param pointsPossible
     * @param point
     * @return String
     * 			the letter is converted from point
     */
    public String convertPointToLetterGrade(LetterGradePercentMapping lgpm, Double pointsPossible, Double point);
    
    /*
     * DN 2012-05-28: defined function get studentUIds by gradebookItemId/assignemntId
     * @param gradebookItemId
     * @return String: studentUIds
     */
    public String getStudentUIdsByGradebookItemId(Long gradebookItemId);
    
    /*
     * DN 2012-09-21: get gradebookitemId by name gradebookItem and gradebookUId
     * @param name of gradebook item
     * @param gradebookUid of gradebook
     * @return Long: gradebookItemId
     */
    public Long getGradableObjectId(final String name,final String gradebookUid);
    
    /*
     * DN 2012-09-26: get gradebookitemId by externalId and gradebookUId
     * @param name: externalId of gradebook item
     * @param gradebookUid of gradebook
     * @return Long: gradebookItemId
     */
    public Long getGradableObjectIdByExternalId(final String name,final String gradebookUid) ;

    public List getAssignmentsByGradebookUId(String gradebookUId);

 //DN 2013-04-18: get rubric site
    public boolean getIsiRubricSite();
    
    public boolean isShowiRubricLink();
}
