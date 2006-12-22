/**********************************************************************************
*
* $Id$
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

package org.sakaiproject.tool.gradebook.business;

import java.util.*;

import org.sakaiproject.service.gradebook.shared.ConflictingAssignmentNameException;
import org.sakaiproject.service.gradebook.shared.ConflictingSpreadsheetNameException;
import org.sakaiproject.service.gradebook.shared.GradebookNotFoundException;
import org.sakaiproject.service.gradebook.shared.StaleObjectModificationException;
import org.sakaiproject.tool.gradebook.*;

/**
 * Manages Gradebook persistence.
 *
 * @author <a href="mailto:jholtzman@berkeley.edu">Josh Holtzman</a>
 */
public interface GradebookManager {

    /**
     * Updates a gradebook's representation in persistence.
     *
     * If the gradebook's selected grade mapping has been modified, the sort
     * value on all explicitly graded course grade records must be updated when
     * the gradebook is updated.
     *
     * A gradebook's selected grade mapping may only change (to a different kind
     * of mapping) if there are no explicitly graded course grade records.
     *
     * @param gradebook The gradebook to update
     * @throws StaleObjectModificationException
     */
    public void updateGradebook(Gradebook gradebook) throws StaleObjectModificationException;

    /**
     * Fetches a gradebook based on its surrogate key
     *
     * @param id The ID of the gradebook
     * @return The gradebook
     */
    public Gradebook getGradebook(Long id);

    /**
     * Internal services use a Long ID to identify a gradebook.
     * External facades use a String UID instead. This method
     * translates.
     */
    public String getGradebookUid(Long id);

    /**
     * Fetches a gradebook based on its unique string id
     *
     * @param uid The UID of the gradebook
     * @return The gradebook
     */
    public Gradebook getGradebook(String uid) throws GradebookNotFoundException;

    public Gradebook getGradebookWithGradeMappings(Long id);

    /**
     * Removes an assignment from a gradebook.  The assignment should not be
     * deleted, but the assignment and all grade records associated with the
     * assignment should be ignored by the application.  A removed assignment
     * should not count toward the total number of points in the gradebook.
     *
     * @param assignmentId The assignment id
     */
    public void removeAssignment(Long assignmentId) throws StaleObjectModificationException;
    
    /**
     * Get all assignment score records for the given set of student UIDs.
     * 
     * @param assignment
     * @param studentUids
     * @return AssignmentGradeRecord list
     */
    public List getAssignmentGradeRecords(Assignment assignment, Collection studentUids);

    /**
     * Get all course grade records (with autocalculated fields) for the given
     * gradebook and the given set of student UIDs
     *
     * @param gradebookId
     * @param studentUids
     * @return CourseGradeRecord list
     */
    public List getPointsEarnedCourseGradeRecords(CourseGrade courseGrade, Collection studentUids);

    /**
     * As a side-effect, this version of the method calculates the mean course grade.
     * The proliferation of method signatures is meant to cut back as much as possible on
     * redundant reads from the assignment grade records.
     * 
     * @param courseGrade This input argument is modified to include statistical information
     * @param studentUids
     * @return
     */
    public List getPointsEarnedCourseGradeRecordsWithStats(CourseGrade courseGrade, Collection studentUids);
    
    public List getPointsEarnedCourseGradeRecords(CourseGrade courseGrade, Collection studentUids, Collection assignments, Map scoreMap);
    public void addToGradeRecordMap(Map gradeRecordMap, List gradeRecords);
   
    /**
     * Gets all grade records that belong to a collection of enrollments in a
     * gradebook.
     *
     * @param gradebookId
     * @param studentUids
     */
    public List getAllAssignmentGradeRecords(Long gradebookId, Collection studentUids);

    /**
     * Gets whether there are explicitly entered course grade records in a gradebook.
     *
     * @param gradebookId The gradebook
     * @return Whether there are course grade records that have a non-null enteredGrade field
     */
    public boolean isExplicitlyEnteredCourseGradeRecords(Long gradebookId);

    /**
     * Gets whether scores have been entered for the given assignment.
     * (This may include scores for students who are not currently enrolled.)
     *
     * @param assignmentId The assignment
     * @return How many scores have been entered for the assignment
     */
    public boolean isEnteredAssignmentScores(Long assignmentId);

    /**
     * Updates the grade records in the GradeRecordSet.
     * Implementations of this method should add a new GradingEvent for each
     * grade record modified, and should update the autocalculated value for
     * each graded student's CourseGradeRecord.
     *
     * @return The set of student UIDs who were given scores higher than the
     * assignment's value.
     */
    public Set updateAssignmentGradeRecords(Assignment assignment, Collection gradeRecords)
    	throws StaleObjectModificationException;

    public Set updateAssignmentGradesAndComments(Assignment assignment, Collection gradeRecords, Collection comments)
		throws StaleObjectModificationException;
 
    public void updateComments(Collection comments)
		throws StaleObjectModificationException;

    /**
     * Updates the grade records for the keys (student IDs) in the studentsToPoints map.
     * Map values must be valid strings (that exist in the gradebook's grade
     * mapping) or nulls.
     *
     * @param studentsToPoints A Map of student IDs to grades
     */
    public void updateCourseGradeRecords(CourseGrade courseGrade, final Collection gradeRecords)
        throws StaleObjectModificationException;

    /**
     * Gets all grade records for a single student in a single gradebook,
     * not including the course grade.
     *
     * @param gradebookId The gradebook id
     * @param studentId The unique student identifier
     *
     * @return A List of all of this student's grade records in the gradebook
     */
    public List getStudentGradeRecords(Long gradebookId, String studentId);

    /**
     * Gets the course grade for a single student.
     */
    public CourseGradeRecord getStudentCourseGradeRecord(Gradebook gradebook, String studentId);

    /**
     * Gets the grading events for the enrollments on the given gradable object.
     *
     * @param gradableObject
     * @param enrollments
     * @return
     */
    public GradingEvents getGradingEvents(GradableObject gradableObject, Collection studentUids);

    /**
     * Fetches a List of Assignments, but does not populate non-persistent
     * fields.
     *
     * @param gradebookId The gradebook ID
     * @param sortBy The field by which to sort the list.
     * @return A list of Assignments with only persistent fields populated
     */
    public List getAssignments(Long gradebookId, String sortBy, boolean ascending);

    /**
     * Convenience method to get assignments with the default sort ordering
     *
     * @param gradebookId The gradebook ID
     */
    public List getAssignments(Long gradebookId);

    /**
     * Fetches a List of Assignments for a given gradebook, and populates the
     * Assignments with all of the statistics fields available in the Assignment
     * object.
     *
     * @param gradebookId The gradebook ID
     * @param studentUids The current enrollment list to filter dropped students
     *        from the calculation
     * @param sortBy The field by which to sort the list.
     * @return A list of Assignments with their statistics fields populated
     */
    public List getAssignmentsWithStats(Long gradebookId, String sortBy, boolean ascending);

    /**
     * Same as the other getAssignmentsWithStats except for tacking the
     * CourseGrade (with statistics) at the end of the list. This is
     * combined into one call as a way to avoid either exposing the
     * full enrollment list for the site or fetching it twice.
     */
    public List getAssignmentsAndCourseGradeWithStats(Long gradebookId, String sortBy, boolean ascending);

    /**
     * Fetches an assignment
     *
     * @param assignmentId The assignment ID
     * @return The assignment
     */
    public Assignment getAssignment(Long assignmentId);

    /**
     * Fetches an assignment and populates its non-persistent statistics
     * fields.
     *
     * @param assignmentId The assignment ID
     * @param studentUids The current enrollment list to filter dropped students
     *        from the calculation
     * @return The GradableObject with all statistics fields populated
     */
    public Assignment getAssignmentWithStats(Long assignmentId);


   /**
     * Add a new assignment to a gradebook
     *
     * @param gradebookId The gradebook ID to which this new assignment belongs
     * @param name The assignment's name (must be unique in the gradebook and not be null)
     * @param points The number of points possible for this assignment (must not be null)
     * @param dueDate The due date for the assignment (optional)
     * @param isNotCounted True if the assignment should not count towards the final course grade (optional)
     * @param isReleased  True if the assignment should be release/ or visble to students
     * @return The ID of the new assignment
     */

    public Long createAssignment(Long gradebookId, String name, Double points, Date dueDate, Boolean isNotCounted, Boolean isReleased)
            throws ConflictingAssignmentNameException, StaleObjectModificationException;


    /**
     * Updates an existing assignment
     */
    public void updateAssignment(Assignment assignment)
        throws ConflictingAssignmentNameException, StaleObjectModificationException;

    /**
     * Fetches the course grade for a gradebook as found in the database.
     * No non-persistent fields (such as points earned) are filled in.
     *
     * @param gradebookId The gradebook id
     * @return The course grade
     */
    public CourseGrade getCourseGrade(Long gradebookId);

    public double getTotalPoints(Long gradebookId);

    /**
     * Fetches a spreadsheet that has been saved
     *
      * @param spreadsheetId
     * @return  The saved spreadsheet object
     */
    public Spreadsheet getSpreadsheet(Long spreadsheetId);

    /**
     *
     * @param gradebookId
     * @return  a Collection of spreadsheets
     */
    public List getSpreadsheets(Long gradebookId);

    /**
     *
     * @param spreadsheetid
     * @throws StaleObjectModificationException
     */

    public void removeSpreadsheet(Long spreadsheetid) throws StaleObjectModificationException;

    /**
     * create a net spreadsheet
     *
     * @param gradebookId
     * @param name
     * @param creator
     * @param dateCreated
     * @param content
     * @return
     * @throws ConflictingSpreadsheetNameException StaleObjectModificationException
     */
    public Long createSpreadsheet(Long gradebookId, String name, String creator, Date dateCreated, String content) throws ConflictingSpreadsheetNameException, StaleObjectModificationException;

    /**
     *
     * @param assignment
     * @param studentIds
     * @return
     */
    public List getComments(Assignment assignment, Collection studentIds);

    /**method to get comments for a assignments for a student in a gradebook
     *
     * @param studentId
     * @param gradebookId
     * @return
     */
    public List getStudentAssignmentComments(String studentId, Long gradebookId);
}
