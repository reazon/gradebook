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

package org.sakaiproject.tool.gradebook.ui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.event.ActionEvent;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.section.api.coursemanagement.EnrollmentRecord;
import org.sakaiproject.service.gradebook.shared.StaleObjectModificationException;
import org.sakaiproject.tool.gradebook.AbstractGradeRecord;
import org.sakaiproject.tool.gradebook.Assignment;
import org.sakaiproject.tool.gradebook.AssignmentGradeRecord;
import org.sakaiproject.tool.gradebook.Comment;
import org.sakaiproject.tool.gradebook.GradingEvent;
import org.sakaiproject.tool.gradebook.GradingEvents;
import org.sakaiproject.tool.gradebook.jsf.FacesUtil;

public class AssignmentDetailsBean extends EnrollmentTableBean {
	private static final Log logger = LogFactory.getLog(AssignmentDetailsBean.class);

	/**
	 * The following variable keeps bean initialization from overwriting
	 * input fields from the database.
	 */
	private boolean workInProgress;

	private List scoreRows;
	private List updatedGradeRecords;
	private List updatedComments;

	private Long assignmentId;
    private Assignment assignment;
	private Assignment previousAssignment;
	private Assignment nextAssignment;

	private boolean isAllCommentsEditable;

    public class ScoreRow implements Serializable {
        private AssignmentGradeRecord gradeRecord;
        private EnrollmentRecord enrollment;
        private Comment comment;
        private List eventRows;
 
		public ScoreRow() {
		}
		public ScoreRow(EnrollmentRecord enrollment, AssignmentGradeRecord gradeRecord, Comment comment, List gradingEvents) {
            Collections.sort(gradingEvents);
            this.enrollment = enrollment;
            this.gradeRecord = gradeRecord;
            this.comment = comment;
 
            eventRows = new ArrayList();
            for (Iterator iter = gradingEvents.iterator(); iter.hasNext();) {
            	GradingEvent gradingEvent = (GradingEvent)iter.next();
            	eventRows.add(new GradingEventRow(gradingEvent));
            }
		}

		public Double getScore() {
			return gradeRecord.getPointsEarned();
		}
		public void setScore(Double score) {
			Double originalScore = gradeRecord.getPointsEarned();
			if ( (originalScore != null && !originalScore.equals(score)) ||
					(originalScore == null && score != null) ) {
				gradeRecord.setPointsEarned(score);
				updatedGradeRecords.add(gradeRecord);
			}
		}

        public EnrollmentRecord getEnrollment() {
            return enrollment;
        }

        public String getCommentText() {
        	return comment.getCommentText();
        }
        public void setCommentText(String commentText) {
        	if (!StringUtils.stripToEmpty(commentText).equals(StringUtils.stripToEmpty(comment.getCommentText()))) {
        		comment.setCommentText(commentText);
        		updatedComments.add(comment);
        	}
        }

        public List getEventRows() {
        	return eventRows;
        }
        public String getEventsLogTitle() {
        	return FacesUtil.getLocalizedString("assignment_details_log_title", new String[] {enrollment.getUser().getDisplayName()});
        }

        public boolean isCommentEditable() {
        	return (isAllCommentsEditable && !assignment.isExternallyMaintained());
        }
	}

	protected void init() {
		if (logger.isDebugEnabled()) logger.debug("loadData assignment=" + assignment + ", previousAssignment=" + previousAssignment + ", nextAssignment=" + nextAssignment);
		
		if (logger.isDebugEnabled()) logger.debug("workInProgress=" + workInProgress);
		if (workInProgress) {			
			// Keeping the current form values in memory is a one-shot deal at
			// present. The next time the user does anything, the form will be
			// refreshed from the database.
			workInProgress = false;
			return;
		}

		super.init();

        // Clear view state.
        previousAssignment = null;
        nextAssignment = null;
		scoreRows = new ArrayList();
		updatedComments = new ArrayList();
		updatedGradeRecords = new ArrayList();

		if (assignmentId != null) {
			assignment = getGradebookManager().getAssignmentWithStats(assignmentId);
			if (assignment != null) {
                // Get the list of assignments.  If we are sorting by mean, we
                // need to fetch the assignment statistics as well.
				List assignments;
                if(Assignment.SORT_BY_MEAN.equals(getAssignmentSortColumn())) {
                    assignments = getGradebookManager().getAssignmentsWithStats(getGradebookId(),
                            getAssignmentSortColumn(), isAssignmentSortAscending());
                } else {
                    assignments = getGradebookManager().getAssignments(getGradebookId(),
                            getAssignmentSortColumn(), isAssignmentSortAscending());
                }

                // Set up next and previous links, if any.
                int thisIndex = assignments.indexOf(assignment);
				if (thisIndex > 0) {
					previousAssignment = (Assignment)assignments.get(thisIndex - 1);
				}
				if (thisIndex < (assignments.size() - 1)) {
					nextAssignment = (Assignment)assignments.get(thisIndex + 1);
				}

				// Set up score rows.
				Map enrollmentMap = getOrderedEnrollmentMap();
				List studentUids = new ArrayList(enrollmentMap.keySet());
				List gradeRecords = getGradebookManager().getAssignmentGradeRecords(assignment, studentUids);

				if (!isEnrollmentSort()) {
					// Need to sort and page based on a scores column.
					List scoreSortedStudentUids = new ArrayList();
					for(Iterator iter = gradeRecords.iterator(); iter.hasNext();) {
						AbstractGradeRecord agr = (AbstractGradeRecord)iter.next();
						scoreSortedStudentUids.add(agr.getStudentId());
					}

					// Put enrollments with no scores at the beginning of the final list.
					studentUids.removeAll(scoreSortedStudentUids);

					// Add all sorted enrollments with scores into the final list
					studentUids.addAll(scoreSortedStudentUids);

					studentUids = finalizeSortingAndPaging(studentUids);
				}

                // Get all of the grading events for these enrollments on this assignment
                GradingEvents allEvents = getGradebookManager().getGradingEvents(assignment, studentUids);

                Map gradeRecordMap = new HashMap();
                for (Iterator iter = gradeRecords.iterator(); iter.hasNext(); ) {
					AssignmentGradeRecord gradeRecord = (AssignmentGradeRecord)iter.next();
					if (studentUids.contains(gradeRecord.getStudentId())) {
						gradeRecordMap.put(gradeRecord.getStudentId(), gradeRecord);
					}
				}

                // If the table is not being sorted by enrollment information, then
                // we had to gather grade records for all students to set up the
                // current page. In that case, eliminate the undisplayed grade records
                // to reduce data contention.
                if (!isEnrollmentSort()) {
                	gradeRecords = new ArrayList(gradeRecordMap.values());
                }

                // Get all of the comments for these enrollments on this assignment.
                List comments = getGradebookManager().getComments(assignment, studentUids);
                Map commentMap = new HashMap();
                for (Iterator iter = comments.iterator(); iter.hasNext(); ) {
					Comment comment = (Comment)iter.next();
					commentMap.put(comment.getStudentId(), comment);
				}

				for (Iterator iter = studentUids.iterator(); iter.hasNext(); ) {
					String studentUid = (String)iter.next();
					EnrollmentRecord enrollment = (EnrollmentRecord)enrollmentMap.get(studentUid);
					AssignmentGradeRecord gradeRecord = (AssignmentGradeRecord)gradeRecordMap.get(studentUid);
		            if(gradeRecord == null) {
		                gradeRecord = new AssignmentGradeRecord(assignment, studentUid, null);
		                gradeRecords.add(gradeRecord);
		            }
		            Comment comment = (Comment)commentMap.get(studentUid);
		            if (comment == null) {
		            	comment = new Comment(studentUid, null, assignment);
		            }

					scoreRows.add(new ScoreRow(enrollment, gradeRecord, comment, allEvents.getEvents(studentUid)));
				}

			} else {
				// The assignment might have been removed since this link was set up.
				if (logger.isWarnEnabled()) logger.warn("No assignmentId=" + assignmentId + " in gradebookUid " + getGradebookUid());
                FacesUtil.addErrorMessage(getLocalizedString("assignment_details_assignment_removed"));
			}
		}
	}

	// Delegated sort methods for read-only assignment sort order
    public String getAssignmentSortColumn() {
        return getPreferencesBean().getAssignmentSortColumn();
    }
    public boolean isAssignmentSortAscending() {
        return getPreferencesBean().isAssignmentSortAscending();
    }

	/**
	 * Action listener to view a different assignment.
	 */
	public void processAssignmentIdChange(ActionEvent event) {
		Map params = FacesUtil.getEventParameterMap(event);
		if (logger.isDebugEnabled()) logger.debug("processAssignmentIdAction params=" + params + ", current assignmentId=" + assignmentId);
		Long idParam = (Long)params.get("assignmentId");
		if (idParam != null) {
			setAssignmentId(idParam);
		}
	}

	/**
	 * Action listener to update scores.
	 */
	public void processUpdateScores(ActionEvent event) {
		try {
			saveScores();
		} catch (StaleObjectModificationException e) {
            FacesUtil.addErrorMessage(getLocalizedString("assignment_details_locking_failure"));
		}
	}

	private void saveScores() throws StaleObjectModificationException {
		if (logger.isInfoEnabled()) logger.info("saveScores " + assignmentId);
		
		Set excessiveScores = getGradebookManager().updateAssignmentGradesAndComments(assignment, updatedGradeRecords, updatedComments);

		if (logger.isDebugEnabled()) logger.debug("About to save " + updatedComments.size() + " updated comments");

		String messageKey = (excessiveScores.size() > 0) ?
			"assignment_details_scores_saved_excessive" :
			"assignment_details_scores_saved";

        // Let the user know.
        FacesUtil.addMessage(getLocalizedString(messageKey));
	}

	public void toggleEditableComments(ActionEvent event) {
		// Don't write over any scores the user entered before pressing
		// the "Edit Comments" button.
		if (!isAllCommentsEditable) {
			workInProgress = true;
		}

		isAllCommentsEditable = !isAllCommentsEditable;
    }

    /**
	 * View maintenance methods.
	 */
	public Long getAssignmentId() {
		if (logger.isDebugEnabled()) logger.debug("getAssignmentId " + assignmentId);
		return assignmentId;
	}
	public void setAssignmentId(Long assignmentId) {
		if (logger.isDebugEnabled()) logger.debug("setAssignmentId " + assignmentId);
		this.assignmentId = assignmentId;
	}

	/**
	 * In IE (but not Mozilla/Firefox) empty request parameters may be returned
	 * to JSF as the string "null". JSF always "restores" some idea of the
	 * last view, even if that idea is always going to be null because a redirect
	 * has occurred. Put these two things together, and you end up with
	 * a class cast exception when redirecting from this request-scoped
	 * bean to a static page.
	 */
	public void setAssignmentIdParam(String assignmentIdParam) {
		if (logger.isDebugEnabled()) logger.debug("setAssignmentIdParam String " + assignmentIdParam);
		if ((assignmentIdParam != null) && (assignmentIdParam.length() > 0) &&
			!assignmentIdParam.equals("null")) {
			try {
				setAssignmentId(Long.valueOf(assignmentIdParam));
			} catch(NumberFormatException e) {
				if (logger.isWarnEnabled()) logger.warn("AssignmentId param set to non-number '" + assignmentIdParam + "'");
			}
		}
	}

	public boolean isFirst() {
		return (previousAssignment == null);
	}
	public String getPreviousTitle() {
		return (previousAssignment != null) ? previousAssignment.getName() : "";
	}

	public boolean isLast() {
		return (nextAssignment == null);
	}
	public String getNextTitle() {
		return (nextAssignment != null) ? nextAssignment.getName() : "";
	}

	public List getScoreRows() {
		return scoreRows;
	}
	public void setScoreRows(List scoreRows) {
		this.scoreRows = scoreRows;
	}

    // A desparate stab at reasonable embedded validation message formatting.
    // If the score column is an input box, it may have a wide message associated
    // with it, and we want the input field left-aligned to match up with
    // the non-erroroneous input fields (even though the actual input values
    // will be right-aligned). On the other hand, if the score column is read-only,
    // then we want to simply right-align the table column.
    public String getScoreColumnAlignment() {
    	if (assignment.isExternallyMaintained()) {
    		return "right";
    	} else {
    		return "left";
    	}
    }

	public String getEventsLogType() {
		return FacesUtil.getLocalizedString("assignment_details_log_type");
	}

    // Sorting
    public boolean isSortAscending() {
        return getPreferencesBean().isAssignmentDetailsTableSortAscending();
    }
    public void setSortAscending(boolean sortAscending) {
        getPreferencesBean().setAssignmentDetailsTableSortAscending(sortAscending);
    }
    public String getSortColumn() {
        return getPreferencesBean().getAssignmentDetailsTableSortColumn();
    }
    public void setSortColumn(String sortColumn) {
        getPreferencesBean().setAssignmentDetailsTableSortColumn(sortColumn);
    }
    public Assignment getAssignment() {
        return assignment;
    }
    public void setAssignment(Assignment assignment) {
        this.assignment = assignment;
    }
    public Assignment getNextAssignment() {
        return nextAssignment;
    }
    public void setNextAssignment(Assignment nextAssignment) {
        this.nextAssignment = nextAssignment;
    }
    public Assignment getPreviousAssignment() {
        return previousAssignment;
    }
    public void setPreviousAssignment(Assignment previousAssignment) {
        this.previousAssignment = previousAssignment;
    }

    public String getCommentsToggle() {
    	String messageKey = isAllCommentsEditable ?
    			"assignment_details_comments_read" :
				"assignment_details_comments_edit";
    	return getLocalizedString(messageKey);
    }

	public boolean isAllCommentsEditable() {
		return isAllCommentsEditable;
	}
}
