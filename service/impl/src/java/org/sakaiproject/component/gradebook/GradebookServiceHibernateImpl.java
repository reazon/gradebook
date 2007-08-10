/**********************************************************************************
*
* $Id$
*
***********************************************************************************
*
* Copyright (c) 2005, 2006 The Regents of the University of California, The MIT Corporation
*
* Licensed under the Educational Community License Version 1.0 (the "License");
* By obtaining, using and/or copying this Original Work, you agree that you have read,
* understand, and will comply with the terms and conditions of the Educational Community License.
* You may obtain a copy of the License at:
*
*      http://www.opensource.org/licenses/ecl1.php
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
* INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
* AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
* DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
*
**********************************************************************************/

package org.sakaiproject.component.gradebook;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.sakaiproject.service.gradebook.shared.AssessmentNotFoundException;
import org.sakaiproject.service.gradebook.shared.AssignmentHasIllegalPointsException;
import org.sakaiproject.service.gradebook.shared.CommentDefinition;
import org.sakaiproject.service.gradebook.shared.ConflictingAssignmentNameException;
import org.sakaiproject.service.gradebook.shared.ConflictingExternalIdException;
import org.sakaiproject.service.gradebook.shared.GradebookExternalAssessmentService;
import org.sakaiproject.service.gradebook.shared.GradebookFrameworkService;
import org.sakaiproject.service.gradebook.shared.GradebookNotFoundException;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.tool.gradebook.Assignment;
import org.sakaiproject.tool.gradebook.AssignmentGradeRecord;
import org.sakaiproject.tool.gradebook.Comment;
import org.sakaiproject.tool.gradebook.GradeMapping;
import org.sakaiproject.tool.gradebook.Gradebook;
import org.sakaiproject.tool.gradebook.GradingEvent;
import org.sakaiproject.tool.gradebook.facades.Authz;
import org.sakaiproject.tool.gradebook.CourseGradeRecord;
import org.sakaiproject.tool.gradebook.CourseGrade;
import org.sakaiproject.tool.gradebook.Category;
import org.sakaiproject.section.api.coursemanagement.EnrollmentRecord;
import org.springframework.orm.hibernate3.HibernateCallback;

/**
 * A Hibernate implementation of GradebookService.
 */
public class GradebookServiceHibernateImpl extends BaseHibernateManager implements GradebookService {
    private static final Log log = LogFactory.getLog(GradebookServiceHibernateImpl.class);

    private GradebookFrameworkService frameworkService;
    private GradebookExternalAssessmentService externalAssessmentService;
    private Authz authz;

	public boolean isAssignmentDefined(final String gradebookUid, final String assignmentName)
        throws GradebookNotFoundException {
		if (!isUserAbleToViewAssignments(gradebookUid)) {
			log.error("AUTHORIZATION FAILURE: User " + getUserUid() + " in gradebook " + gradebookUid + " attempted to check for assignment " + assignmentName);
			throw new SecurityException("You do not have permission to perform this operation");
		}
        Assignment assignment = (Assignment)getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				return getAssignmentWithoutStats(gradebookUid, assignmentName, session);
			}
		});
        return (assignment != null);
    }

	private boolean isUserAbleToViewAssignments(String gradebookUid) {
		Authz authz = getAuthz();
		return (authz.isUserAbleToEditAssessments(gradebookUid) || authz.isUserAbleToGrade(gradebookUid));
	}

	public boolean isUserAbleToGradeItemForStudent(String gradebookUid, Long itemId, String studentUid) {
		return getAuthz().isUserAbleToGradeItemForStudent(gradebookUid, itemId, studentUid);
	}
	
	public boolean isUserAbleToGradeItemForStudent(String gradebookUid, String itemName, String studentUid) {
		
		if (itemName == null || studentUid == null) {
			throw new IllegalArgumentException("Null parameter(s) in GradebookServiceHibernateImpl.isUserAbleToGradeItemForStudent");
		}
		
		org.sakaiproject.service.gradebook.shared.Assignment assignment = getAssignment(gradebookUid, itemName);
		if (assignment != null) {
			return isUserAbleToGradeItemForStudent(gradebookUid, assignment.getId(), studentUid);
		}
		
		return false;

	}
	
	public boolean isUserAbleToViewItemForStudent(String gradebookUid, Long itemId, String studentUid) {
		return getAuthz().isUserAbleToViewItemForStudent(gradebookUid, itemId, studentUid);
	}
	
	public boolean isUserAbleToViewItemForStudent(String gradebookUid, String itemName, String studentUid) {
		
		if (itemName == null || studentUid == null) {
			throw new IllegalArgumentException("Null parameter(s) in GradebookServiceHibernateImpl.isUserAbleToGradeItemForStudent");
		}
		
		org.sakaiproject.service.gradebook.shared.Assignment assignment = getAssignment(gradebookUid, itemName);
		if (assignment != null) {
			return isUserAbleToViewItemForStudent(gradebookUid, assignment.getId(), studentUid);
		}
		
		return false;

	}
	
	public String getGradeViewFunctionForUserForStudentForItem(String gradebookUid, Long itemId, String studentUid) {
		return getAuthz().getGradeViewFunctionForUserForStudentForItem(gradebookUid, itemId, studentUid);
	}
	
	public String getGradeViewFunctionForUserForStudentForItem(String gradebookUid, String itemName, String studentUid) {
		if (itemName == null || studentUid == null) {
			throw new IllegalArgumentException("Null parameter(s) in G.isUserAbleToGradeItemForStudent");
		}
		
		org.sakaiproject.service.gradebook.shared.Assignment assignment = getAssignment(gradebookUid, itemName);
		if (assignment != null) {
			return getGradeViewFunctionForUserForStudentForItem(gradebookUid, assignment.getId(), studentUid);
		}
		
		return null;
	}

	public List<org.sakaiproject.service.gradebook.shared.Assignment> getAssignments(String gradebookUid)
		throws GradebookNotFoundException {
		if (!isUserAbleToViewAssignments(gradebookUid)) {
			log.error("AUTHORIZATION FAILURE: User " + getUserUid() + " in gradebook " + gradebookUid + " attempted to get assignments list");
			throw new SecurityException("You do not have permission to perform this operation");
		}

		final Long gradebookId = getGradebook(gradebookUid).getId();

        List internalAssignments = (List)getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                return getAssignments(gradebookId, session);
            }
        });

		List<org.sakaiproject.service.gradebook.shared.Assignment> assignments = new ArrayList<org.sakaiproject.service.gradebook.shared.Assignment>();
		for (Iterator iter = internalAssignments.iterator(); iter.hasNext(); ) {
			Assignment assignment = (Assignment)iter.next();
			assignments.add(getAssignmentDefinition(assignment));
		}
		return assignments;
	}

	public org.sakaiproject.service.gradebook.shared.Assignment getAssignment(final String gradebookUid, final String assignmentName) throws GradebookNotFoundException {
		if (!isUserAbleToViewAssignments(gradebookUid)) {
			log.error("AUTHORIZATION FAILURE: User " + getUserUid() + " in gradebook " + gradebookUid + " attempted to get assignment " + assignmentName);
			throw new SecurityException("You do not have permission to perform this operation");
		}
		Assignment assignment = (Assignment)getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				return getAssignmentWithoutStats(gradebookUid, assignmentName, session);
			}
		});
		if (assignment != null) {
			return getAssignmentDefinition(assignment);
		} else {
			return null;
		}
	}

	private org.sakaiproject.service.gradebook.shared.Assignment getAssignmentDefinition(Assignment internalAssignment) {
		org.sakaiproject.service.gradebook.shared.Assignment assignmentDefinition = new org.sakaiproject.service.gradebook.shared.Assignment();
    	assignmentDefinition.setName(internalAssignment.getName());
    	assignmentDefinition.setPoints(internalAssignment.getPointsPossible());
    	assignmentDefinition.setDueDate(internalAssignment.getDueDate());
    	assignmentDefinition.setCounted(internalAssignment.isCounted());
    	assignmentDefinition.setExternallyMaintained(internalAssignment.isExternallyMaintained());
    	assignmentDefinition.setExternalAppName(internalAssignment.getExternalAppName());
    	assignmentDefinition.setExternalId(internalAssignment.getExternalId());
    	assignmentDefinition.setReleased(internalAssignment.isReleased());
    	assignmentDefinition.setId(internalAssignment.getId());
    	return assignmentDefinition;
    }

	public Double getAssignmentScore(final String gradebookUid, final String assignmentName, final String studentUid)
		throws GradebookNotFoundException, AssessmentNotFoundException {
		final boolean studentRequestingOwnScore = authn.getUserUid().equals(studentUid);

		Double assignmentScore = (Double)getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				Assignment assignment = getAssignmentWithoutStats(gradebookUid, assignmentName, session);
				if (assignment == null) {
					throw new AssessmentNotFoundException("There is no assignment named " + assignmentName + " in gradebook " + gradebookUid);
				}
				
				if (!studentRequestingOwnScore && !isUserAbleToViewItemForStudent(gradebookUid, assignment.getId(), studentUid)) {
					log.error("AUTHORIZATION FAILURE: User " + getUserUid() + " in gradebook " + gradebookUid + " attempted to retrieve grade for student " + studentUid + " for assignment " + assignmentName);
					throw new SecurityException("You do not have permission to perform this operation");
				}
				
				// If this is the student, then the assignment needs to have
				// been released.
				if (studentRequestingOwnScore && !assignment.isReleased()) {
					log.error("AUTHORIZATION FAILURE: Student " + getUserUid() + " in gradebook " + gradebookUid + " attempted to retrieve score for unreleased assignment " + assignment.getName());
					throw new SecurityException("You do not have permission to perform this operation");					
				}
				
				AssignmentGradeRecord gradeRecord = getAssignmentGradeRecord(assignment, studentUid, session);
				if (log.isDebugEnabled()) log.debug("gradeRecord=" + gradeRecord);
				if (gradeRecord == null) {
					return null;
				} else {
					return gradeRecord.getPointsEarned();
				}
			}
		});
		if (log.isDebugEnabled()) log.debug("returning " + assignmentScore);
		return assignmentScore;
	}

	public void setAssignmentScore(final String gradebookUid, final String assignmentName, final String studentUid, final Double score, final String clientServiceDescription)
		throws GradebookNotFoundException, AssessmentNotFoundException {


		getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				Assignment assignment = getAssignmentWithoutStats(gradebookUid, assignmentName, session);
				if (assignment == null) {
					throw new AssessmentNotFoundException("There is no assignment named " + assignmentName + " in gradebook " + gradebookUid);
				}
				if (assignment.isExternallyMaintained()) {
					log.error("AUTHORIZATION FAILURE: User " + getUserUid() + " in gradebook " + gradebookUid + " attempted to grade externally maintained assignment " + assignmentName + " from " + clientServiceDescription);
					throw new SecurityException("You do not have permission to perform this operation");
				}

				if (!isUserAbleToGradeItemForStudent(gradebookUid, assignment.getId(), studentUid)) {
					log.error("AUTHORIZATION FAILURE: User " + getUserUid() + " in gradebook " + gradebookUid + " attempted to grade student " + studentUid + " from " + clientServiceDescription + " for item " + assignmentName);
					throw new SecurityException("You do not have permission to perform this operation");
				}

				Date now = new Date();
				String graderId = getAuthn().getUserUid();
				AssignmentGradeRecord gradeRecord = getAssignmentGradeRecord(assignment, studentUid, session);
				if (gradeRecord == null) {
					// Creating a new grade record.
					gradeRecord = new AssignmentGradeRecord(assignment, studentUid, score);
				} else {
					gradeRecord.setPointsEarned(score);
				}
				gradeRecord.setGraderId(graderId);
				gradeRecord.setDateRecorded(now);
				session.saveOrUpdate(gradeRecord);
				
				session.save(new GradingEvent(assignment, graderId, studentUid, score));
				
				// Sync database.
				session.flush();
				session.clear();
				return null;
			}
		});

		if (log.isInfoEnabled()) log.info("Score updated in gradebookUid=" + gradebookUid + ", assignmentName=" + assignmentName + " by userUid=" + getUserUid() + " from client=" + clientServiceDescription + ", new score=" + score);
	}
	
	private Comment getInternalComment(String gradebookUid, String assignmentName, String studentUid, Session session) {
		Query q = session.createQuery(
		"from Comment as c where c.studentId=:studentId and c.gradableObject.gradebook.uid=:gradebookUid and c.gradableObject.name=:assignmentName");
		q.setParameter("studentId", studentUid);
		q.setParameter("gradebookUid", gradebookUid);
		q.setParameter("assignmentName", assignmentName);
		return (Comment)q.uniqueResult();		
	}

	public CommentDefinition getAssignmentScoreComment(final String gradebookUid, final String assignmentName, final String studentUid) throws GradebookNotFoundException, AssessmentNotFoundException {
		CommentDefinition commentDefinition = null;
        Comment comment = (Comment)getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
            	return getInternalComment(gradebookUid, assignmentName, studentUid, session);
            }
        });
        if (comment != null) {
        	commentDefinition = new CommentDefinition();
        	commentDefinition.setAssignmentName(assignmentName);
        	commentDefinition.setCommentText(comment.getCommentText());
        	commentDefinition.setDateRecorded(comment.getDateRecorded());
        	commentDefinition.setGraderUid(comment.getGraderId());
        	commentDefinition.setStudentUid(comment.getStudentId());
        }
		return commentDefinition;
	}

	public void setAssignmentScoreComment(final String gradebookUid, final String assignmentName, final String studentUid, final String commentText) throws GradebookNotFoundException, AssessmentNotFoundException {
		getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
        		Comment comment = getInternalComment(gradebookUid, assignmentName, studentUid, session);
        		if (comment == null) {
        			comment = new Comment(studentUid, commentText, getAssignmentWithoutStats(gradebookUid, assignmentName, session));
        		} else {
        			comment.setCommentText(commentText);
        		}
				comment.setGraderId(authn.getUserUid());
				comment.setDateRecorded(new Date());
				session.saveOrUpdate(comment);
            	return null;
            }
		});
	}

	public String getGradebookDefinitionXml(String gradebookUid) {
		Gradebook gradebook = getGradebook(gradebookUid);
		
		GradebookDefinition gradebookDefinition = new GradebookDefinition();
		GradeMapping selectedGradeMapping = gradebook.getSelectedGradeMapping();
		gradebookDefinition.setSelectedGradingScaleUid(selectedGradeMapping.getGradingScale().getUid());
		gradebookDefinition.setSelectedGradingScaleBottomPercents(new HashMap<String,Double>(selectedGradeMapping.getGradeMap()));
		gradebookDefinition.setAssignments(getAssignments(gradebookUid));
		
		return VersionedExternalizable.toXml(gradebookDefinition);
	}

	public void mergeGradebookDefinitionXml(String toGradebookUid, String fromGradebookXml) {
		final Gradebook gradebook = getGradebook(toGradebookUid);
		GradebookDefinition gradebookDefinition = (GradebookDefinition)VersionedExternalizable.fromXml(fromGradebookXml);

		List<String> assignmentNames = (List<String>)getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(final Session session) throws HibernateException {
            	return session.createQuery(
            		"select asn.name from Assignment as asn where asn.gradebook.id=? and asn.removed=false").
            		setLong(0, gradebook.getId().longValue()).
            		list();
            }
        });
		
		// Add any non-externally-managed assignments with non-duplicate names.
		int assignmentsAddedCount = 0;
		for (org.sakaiproject.service.gradebook.shared.Assignment obj : gradebookDefinition.getAssignments()) {
			org.sakaiproject.service.gradebook.shared.Assignment assignmentDef = (org.sakaiproject.service.gradebook.shared.Assignment)obj;
			
			// Externally managed assessments should not be included.
			if (assignmentDef.isExternallyMaintained()) {
				continue;
			}

			// Skip any input assignments with duplicate names.
			if (assignmentNames.contains(assignmentDef.getName())) {
				if (log.isInfoEnabled()) log.info("Merge to gradebook " + toGradebookUid + " skipped duplicate assignment named " + assignmentDef.getName());
				continue;				
			}
			
			// All assignments should be unreleased even if they were released in the original.
			createAssignment(gradebook.getId(), assignmentDef.getName(), assignmentDef.getPoints(), assignmentDef.getDueDate(), !assignmentDef.isCounted(), false);
			assignmentsAddedCount++;
		}
		if (log.isInfoEnabled()) log.info("Merge to gradebook " + toGradebookUid + " added " + assignmentsAddedCount + " assignments");
		
		// Carry over the old gradebook's selected grading scheme if possible.
		String fromGradingScaleUid = gradebookDefinition.getSelectedGradingScaleUid();
		MERGE_GRADE_MAPPING: if (!StringUtils.isEmpty(fromGradingScaleUid)) {
			for (GradeMapping gradeMapping : gradebook.getGradeMappings()) {
				if (gradeMapping.getGradingScale().getUid().equals(fromGradingScaleUid)) {
					// We have a match. Now make sure that the grades are as expected.
					Map<String, Double> inputGradePercents = gradebookDefinition.getSelectedGradingScaleBottomPercents();
					Set<String> gradeCodes = (Set<String>)inputGradePercents.keySet();
					if (gradeCodes.containsAll(gradeMapping.getGradeMap().keySet())) {
						// Modify the existing grade-to-percentage map.
						for (String gradeCode : gradeCodes) {
							gradeMapping.getGradeMap().put(gradeCode, inputGradePercents.get(gradeCode));							
						}
						gradebook.setSelectedGradeMapping(gradeMapping);
						updateGradebook(gradebook);
						if (log.isInfoEnabled()) log.info("Merge to gradebook " + toGradebookUid + " updated grade mapping");
					} else {
						if (log.isInfoEnabled()) log.info("Merge to gradebook " + toGradebookUid + " skipped grade mapping change because the " + fromGradingScaleUid + " grade codes did not match");
					}
					break MERGE_GRADE_MAPPING;
				}
			}
			// Did not find a matching grading scale.
			if (log.isInfoEnabled()) log.info("Merge to gradebook " + toGradebookUid + " skipped grade mapping change because grading scale " + fromGradingScaleUid + " is not defined");
		}
	}

	public void addAssignment(String gradebookUid, org.sakaiproject.service.gradebook.shared.Assignment assignmentDefinition) {
		if (!getAuthz().isUserAbleToEditAssessments(gradebookUid)) {
			log.error("AUTHORIZATION FAILURE: User " + getUserUid() + " in gradebook " + gradebookUid + " attempted to add an assignment");
			throw new SecurityException("You do not have permission to perform this operation");
		}

        // Ensure that points is > zero.
		Double points = assignmentDefinition.getPoints();
        if ((points == null) || (points.doubleValue() <= 0)) {
            throw new AssignmentHasIllegalPointsException("Points must be > 0");
        }

		Gradebook gradebook = getGradebook(gradebookUid);
		createAssignment(gradebook.getId(), assignmentDefinition.getName(), points, assignmentDefinition.getDueDate(), !assignmentDefinition.isCounted(), assignmentDefinition.isReleased());
	}

	public void updateAssignment(final String gradebookUid, final String assignmentName, final org.sakaiproject.service.gradebook.shared.Assignment assignmentDefinition) {		
		if (!getAuthz().isUserAbleToEditAssessments(gradebookUid)) {
			log.error("AUTHORIZATION FAILURE: User " + getUserUid() + " in gradebook " + gradebookUid + " attempted to change the definition of assignment " + assignmentName);
			throw new SecurityException("You do not have permission to perform this operation");
		}
		
		// This method is for Gradebook-managed assignments only.
		if (assignmentDefinition.isExternallyMaintained()) {
			log.error("User " + getUserUid() + " in gradebook " + gradebookUid + " attempted to set assignment " + assignmentName + " to be externally maintained");
			throw new SecurityException("You do not have permission to perform this operation");
		}

		getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				Assignment assignment = getAssignmentWithoutStats(gradebookUid, assignmentName, session);
				if (assignment == null) {
					throw new AssessmentNotFoundException("There is no assignment named " + assignmentName + " in gradebook " + gradebookUid);
				}
				if (assignment.isExternallyMaintained()) {
					log.error("AUTHORIZATION FAILURE: User " + getUserUid() + " in gradebook " + gradebookUid + " attempted to change the definition of externally maintained assignment " + assignmentName);
					throw new SecurityException("You do not have permission to perform this operation");
				}
				assignment.setCounted(assignmentDefinition.isCounted());
				assignment.setDueDate(assignmentDefinition.getDueDate());
				assignment.setName(assignmentDefinition.getName());
				assignment.setPointsPossible(assignmentDefinition.getPoints());
				assignment.setReleased(assignmentDefinition.isReleased());
				updateAssignment(assignment, session);
				return null;
			}
		});
	}

    public Authz getAuthz() {
        return authz;
    }
    public void setAuthz(Authz authz) {
        this.authz = authz;
    }

    // Deprecated calls to new framework-specific interface.

	public void addGradebook(String uid, String name) {
		frameworkService.addGradebook(uid, name);
	}
	public void setAvailableGradingScales(Collection gradingScaleDefinitions) {
		frameworkService.setAvailableGradingScales(gradingScaleDefinitions);
	}
	public void setDefaultGradingScale(String uid) {
		frameworkService.setDefaultGradingScale(uid);
	}
	public void deleteGradebook( String uid)
		throws GradebookNotFoundException {
		frameworkService.deleteGradebook(uid);
	}
    public boolean isGradebookDefined(String gradebookUid) {
        return frameworkService.isGradebookDefined(gradebookUid);
    }

	public GradebookFrameworkService getFrameworkService() {
		return frameworkService;
	}
	public void setFrameworkService(GradebookFrameworkService frameworkService) {
		this.frameworkService = frameworkService;
	}

	// Deprecated calls to new interface for external assessment engines.

	public void addExternalAssessment(String gradebookUid, String externalId, String externalUrl,
			String title, double points, Date dueDate, String externalServiceDescription)
            throws ConflictingAssignmentNameException, ConflictingExternalIdException, GradebookNotFoundException {
		externalAssessmentService.addExternalAssessment(gradebookUid, externalId, externalUrl, title, points, dueDate, externalServiceDescription);
	}
    public void updateExternalAssessment(String gradebookUid, String externalId, String externalUrl,
                                         String title, double points, Date dueDate) throws GradebookNotFoundException, AssessmentNotFoundException,AssignmentHasIllegalPointsException {
    	externalAssessmentService.updateExternalAssessment(gradebookUid, externalId, externalUrl, title, points, dueDate);
	}
	public void removeExternalAssessment(String gradebookUid,
            String externalId) throws GradebookNotFoundException, AssessmentNotFoundException {
		externalAssessmentService.removeExternalAssessment(gradebookUid, externalId);
	}
	public void updateExternalAssessmentScore(String gradebookUid, String externalId,
			String studentUid, Double points) throws GradebookNotFoundException, AssessmentNotFoundException {
		externalAssessmentService.updateExternalAssessmentScore(gradebookUid, externalId, studentUid, points);
	}
	public void updateExternalAssessmentScores(String gradebookUid, String externalId, Map studentUidsToScores)
		throws GradebookNotFoundException, AssessmentNotFoundException {
		externalAssessmentService.updateExternalAssessmentScores(gradebookUid, externalId, studentUidsToScores);
	}
	public boolean isExternalAssignmentDefined(String gradebookUid, String externalId) throws GradebookNotFoundException {
		return externalAssessmentService.isExternalAssignmentDefined(gradebookUid, externalId);
	}

	public GradebookExternalAssessmentService getExternalAssessmentService() {
		return externalAssessmentService;
	}
	public void setExternalAssessmentService(
			GradebookExternalAssessmentService externalAssessmentService) {
		this.externalAssessmentService = externalAssessmentService;
	}

	public Map getImportCourseGrade(String gradebookUid)
	{
		HashMap returnMap = new HashMap();

		try
		{
			Gradebook thisGradebook = getGradebook(gradebookUid);
			
			List assignList = getAssignmentsCounted(thisGradebook.getId());
			boolean nonAssignment = false;
			if(assignList == null || assignList.size() < 1)
			{
				nonAssignment = true;
			}
			
			Long gradebookId = thisGradebook.getId();
			CourseGrade courseGrade = getCourseGrade(gradebookId);

			Map enrollmentMap;
			String userUid = authn.getUserUid();
			
			Map viewableEnrollmentsMap = authz.findMatchingEnrollmentsForViewableCourseGrade(gradebookUid, null, null);
			enrollmentMap = new HashMap();

			Map enrollmentMapUid = new HashMap();
			for (Iterator iter = viewableEnrollmentsMap.keySet().iterator(); iter.hasNext(); ) 
			{
				EnrollmentRecord enr = (EnrollmentRecord)iter.next();
				enrollmentMap.put(enr.getUser().getUserUid(), enr);
				enrollmentMapUid.put(enr.getUser().getUserUid(), enr);
			}
			List gradeRecords = getPointsEarnedCourseGradeRecords(courseGrade, enrollmentMap.keySet());
			ArrayList grades = new ArrayList();
			for (Iterator iter = gradeRecords.iterator(); iter.hasNext(); ) 
			{
				CourseGradeRecord gradeRecord = (CourseGradeRecord)iter.next();

				GradeMapping gradeMap= thisGradebook.getSelectedGradeMapping();

				EnrollmentRecord enr = (EnrollmentRecord)enrollmentMapUid.get(gradeRecord.getStudentId());
				if(enr != null)
				{
					if(gradeRecord.getEnteredGrade() != null && !gradeRecord.getEnteredGrade().equalsIgnoreCase(""))
					{
						returnMap.put(enr.getUser().getDisplayId(), gradeRecord.getEnteredGrade());
					}
					else
					{
						if(!nonAssignment)
							returnMap.put(enr.getUser().getDisplayId(), (String)gradeMap.getGrade(gradeRecord.getNonNullAutoCalculatedGrade()));
					}
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return returnMap;
	}

	public CourseGrade getCourseGrade(Long gradebookId) {
		return (CourseGrade)getHibernateTemplate().find(
				"from CourseGrade as cg where cg.gradebook.id=?",
				gradebookId).get(0);
	}

	public List getPointsEarnedCourseGradeRecords(final CourseGrade courseGrade, final Collection studentUids) {
		HibernateCallback hc = new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				if(studentUids == null || studentUids.size() == 0) {
					if(log.isInfoEnabled()) log.info("Returning no grade records for an empty collection of student UIDs");
					return new ArrayList();
				}

				Query q = session.createQuery("from CourseGradeRecord as cgr where cgr.gradableObject.id=:gradableObjectId");
				q.setLong("gradableObjectId", courseGrade.getId().longValue());
				List records = filterAndPopulateCourseGradeRecordsByStudents(courseGrade, q.list(), studentUids);

				Long gradebookId = courseGrade.getGradebook().getId();
				Gradebook gradebook = getGradebook(gradebookId);
				List cates = getCategories(gradebookId);
				//double totalPointsPossible = getTotalPointsInternal(gradebookId, session);
				//if(log.isDebugEnabled()) log.debug("Total points = " + totalPointsPossible);

				for(Iterator iter = records.iterator(); iter.hasNext();) {
					CourseGradeRecord cgr = (CourseGradeRecord)iter.next();
					//double totalPointsEarned = getTotalPointsEarnedInternal(gradebookId, cgr.getStudentId(), session);
					List totalEarned = getTotalPointsEarnedInternal(gradebookId, cgr.getStudentId(), session, gradebook, cates);
					double totalPointsEarned = ((Double)totalEarned.get(0)).doubleValue();
					double literalTotalPointsEarned = ((Double)totalEarned.get(1)).doubleValue();
					double totalPointsPossible = getTotalPointsInternal(gradebookId, session, gradebook, cates, cgr.getStudentId());
					cgr.initNonpersistentFields(totalPointsPossible, totalPointsEarned, literalTotalPointsEarned);
					if(log.isDebugEnabled()) log.debug("Points earned = " + cgr.getPointsEarned());
				}

				return records;
			}
		};
		return (List)getHibernateTemplate().execute(hc);
	}


	private List filterAndPopulateCourseGradeRecordsByStudents(CourseGrade courseGrade, Collection gradeRecords, Collection studentUids) {
		List filteredRecords = new ArrayList();
		Set missingStudents = new HashSet(studentUids);
		for (Iterator iter = gradeRecords.iterator(); iter.hasNext(); ) {
			CourseGradeRecord cgr = (CourseGradeRecord)iter.next();
			if (studentUids.contains(cgr.getStudentId())) {
				filteredRecords.add(cgr);
				missingStudents.remove(cgr.getStudentId());
			}
		}
		for (Iterator iter = missingStudents.iterator(); iter.hasNext(); ) {
			String studentUid = (String)iter.next();
			CourseGradeRecord cgr = new CourseGradeRecord(courseGrade, studentUid);
			filteredRecords.add(cgr);
		}
		return filteredRecords;
	}

	private double getTotalPointsInternal(final Long gradebookId, Session session, final Gradebook gradebook, final List categories, final String studentId)
	{
  	double totalPointsPossible = 0;
  	List assgnsList = session.createQuery(
  			"select asn from Assignment asn where asn.gradebook.id=:gbid and asn.removed=false and asn.notCounted=false").
  			setParameter("gbid", gradebookId).
  			list();

  	Iterator scoresIter = session.createQuery(
  	"select agr.pointsEarned, asn from AssignmentGradeRecord agr, Assignment asn where agr.gradableObject=asn and agr.studentId=:student and asn.gradebook.id=:gbid and asn.removed=false and asn.notCounted=false").
  	setParameter("student", studentId).
  	setParameter("gbid", gradebookId).
  	list().iterator();

  	Set assignmentsTaken = new HashSet();
  	Set categoryTaken = new HashSet();
  	while (scoresIter.hasNext()) {
  		Object[] returned = (Object[])scoresIter.next();
  		Double pointsEarned = (Double)returned[0];
  		Assignment go = (Assignment) returned[1];
  		if (pointsEarned != null) {
  			if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_NO_CATEGORY)
  			{
  				assignmentsTaken.add(go.getId());
  			}
  			else if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_ONLY_CATEGORY && go != null)
  			{
  				assignmentsTaken.add(go.getId());
  			}
  			else if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY && go != null && categories != null)
  			{
  				for(int i=0; i<categories.size(); i++)
  				{
  					Category cate = (Category) categories.get(i);
  					if(cate != null && !cate.isRemoved() && go.getCategory() != null && cate.getId().equals(go.getCategory().getId()))
  					{
  						assignmentsTaken.add(go.getId());
  						categoryTaken.add(cate.getId());
  						break;
  					}
  				}
  			}
  		}
  	}

  	if(!assignmentsTaken.isEmpty())
  	{
  		if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY)
  		{
    		for(int i=0; i<categories.size(); i++)
    		{
    			Category cate = (Category) categories.get(i);
    			if(cate != null && !cate.isRemoved() && categoryTaken.contains(cate.getId()) )
    			{
    				totalPointsPossible += cate.getWeight().doubleValue();
    			}
    		}
    		return totalPointsPossible;
  		}
  		Iterator assignmentIter = assgnsList.iterator();
  		while (assignmentIter.hasNext()) {
  			Assignment asn = (Assignment) assignmentIter.next();
  			if(asn != null)
  			{
  				Double pointsPossible = asn.getPointsPossible();

  				if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_NO_CATEGORY && assignmentsTaken.contains(asn.getId()))
  				{
  					totalPointsPossible += pointsPossible.doubleValue();
  				}
  				else if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_ONLY_CATEGORY && assignmentsTaken.contains(asn.getId()))
  				{
  					totalPointsPossible += pointsPossible.doubleValue();
  				}
  			}
  		}
  	}
  	else
  		totalPointsPossible = -1;

  	return totalPointsPossible;
	}

	private List getTotalPointsEarnedInternal(final Long gradebookId, final String studentId, final Session session, final Gradebook gradebook, final List categories) 
	{
  	double totalPointsEarned = 0;
  	double literalTotalPointsEarned = 0;
  	Iterator scoresIter = session.createQuery(
  			"select agr.pointsEarned, asn from AssignmentGradeRecord agr, Assignment asn where agr.gradableObject=asn and agr.studentId=:student and asn.gradebook.id=:gbid and asn.removed=false and asn.notCounted=false").
  			setParameter("student", studentId).
  			setParameter("gbid", gradebookId).
  			list().iterator();

  	List assgnsList = session.createQuery(
  	"from Assignment as asn where asn.gradebook.id=:gbid and asn.removed=false and asn.notCounted=false").
  	setParameter("gbid", gradebookId).
  	list();

  	Map cateScoreMap = new HashMap();
  	Map cateTotalScoreMap = new HashMap();

  	Set assignmentsTaken = new HashSet();
  	while (scoresIter.hasNext()) {
  		Object[] returned = (Object[])scoresIter.next();
  		Double pointsEarned = (Double)returned[0];
  		Assignment go = (Assignment) returned[1];
  		if (pointsEarned != null) {
  			if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_NO_CATEGORY)
  			{
  				totalPointsEarned += pointsEarned.doubleValue();
  				literalTotalPointsEarned += pointsEarned.doubleValue();
  				assignmentsTaken.add(go.getId());
  			}
  			else if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_ONLY_CATEGORY && go != null && categories != null)
  			{
  				totalPointsEarned += pointsEarned.doubleValue();
  				literalTotalPointsEarned += pointsEarned.doubleValue();
  				assignmentsTaken.add(go.getId());
  			}
  			else if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY && go != null && categories != null)
  			{
  				for(int i=0; i<categories.size(); i++)
  				{
  					Category cate = (Category) categories.get(i);
  					if(cate != null && !cate.isRemoved() && go.getCategory() != null && cate.getId().equals(go.getCategory().getId()))
  					{
  						assignmentsTaken.add(go.getId());
  						literalTotalPointsEarned += pointsEarned.doubleValue();
  						if(cateScoreMap.get(cate.getId()) != null)
  						{
  							cateScoreMap.put(cate.getId(), new Double(((Double)cateScoreMap.get(cate.getId())).doubleValue() + pointsEarned.doubleValue()));
  						}
  						else
  						{
  							cateScoreMap.put(cate.getId(), new Double(pointsEarned));
  						}
  						break;
  					}
  				}
  			}
  		}
  	}

  	if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY && categories != null)
  	{
  		Iterator assgnsIter = assgnsList.iterator();
  		while (assgnsIter.hasNext()) 
  		{
  			Assignment asgn = (Assignment)assgnsIter.next();
  			if(assignmentsTaken.contains(asgn.getId()))
  			{
  				for(int i=0; i<categories.size(); i++)
  				{
  					Category cate = (Category) categories.get(i);
  					if(cate != null && !cate.isRemoved() && asgn.getCategory() != null && cate.getId().equals(asgn.getCategory().getId()))
  					{
  						if(cateTotalScoreMap.get(cate.getId()) == null)
  						{
  							cateTotalScoreMap.put(cate.getId(), asgn.getPointsPossible());
  						}
  						else
  						{
  							cateTotalScoreMap.put(cate.getId(), new Double(((Double)cateTotalScoreMap.get(cate.getId())).doubleValue() + asgn.getPointsPossible().doubleValue()));
  						}
  					}
  				}
  			}
  		}
  	}

  	if(assignmentsTaken.isEmpty())
  		totalPointsEarned = -1;

  	if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY)
  	{
  		for(int i=0; i<categories.size(); i++)
  		{
  			Category cate = (Category) categories.get(i);
  			if(cate != null && !cate.isRemoved() && cateScoreMap.get(cate.getId()) != null && cateTotalScoreMap.get(cate.getId()) != null)
  			{
  				totalPointsEarned += ((Double)cateScoreMap.get(cate.getId())).doubleValue() * cate.getWeight().doubleValue() / ((Double)cateTotalScoreMap.get(cate.getId())).doubleValue();
  			}
  		}
  	}

  	if (log.isDebugEnabled()) log.debug("getTotalPointsEarnedInternal for studentId=" + studentId + " returning " + totalPointsEarned);
  	List returnList = new ArrayList();
  	returnList.add(new Double(totalPointsEarned));
  	returnList.add(new Double(literalTotalPointsEarned));
  	return returnList;
	}

	public Gradebook getGradebook(Long id) {
		return (Gradebook)getHibernateTemplate().load(Gradebook.class, id);
	}

	protected List getAssignmentsCounted(final Long gradebookId) throws HibernateException 
	{
		HibernateCallback hc = new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				List assignments = session.createQuery(
						"from Assignment as asn where asn.gradebook.id=? and asn.removed=false and asn.notCounted=false").
						setLong(0, gradebookId.longValue()).
						list();
				return assignments;
			}
		};
		return (List)getHibernateTemplate().execute(hc);
	}
	
  public boolean checkStuendsNotSubmitted(String gradebookUid)
  {
  	Gradebook gradebook = getGradebook(gradebookUid);
  	Set studentUids = getAllStudentUids(getGradebookUid(gradebook.getId()));
  	if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_NO_CATEGORY || gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_ONLY_CATEGORY)
  	{
  		List records = getAllAssignmentGradeRecords(gradebook.getId(), studentUids);
  		List assigns = getAssignments(gradebook.getId(), Assignment.DEFAULT_SORT, true);
  		List filteredAssigns = new ArrayList();
  		for(Iterator iter = assigns.iterator(); iter.hasNext();)
  		{
  			Assignment assignment = (Assignment)iter.next();
  			if(assignment.isCounted() && !assignment.isUngraded())
  				filteredAssigns.add(assignment);
  		}
  		List filteredRecords = new ArrayList();
  		for(Iterator iter = records.iterator(); iter.hasNext();)
  		{
  			AssignmentGradeRecord agr = (AssignmentGradeRecord)iter.next();
  			if(!agr.isCourseGradeRecord() && agr.getAssignment().isCounted() && !agr.getAssignment().isUngraded())
  			{
  				if(agr.getPointsEarned() == null)
  					return true;
  				filteredRecords.add(agr);
  			}
  		}

  		if(filteredRecords.size() < (filteredAssigns.size() * studentUids.size()))
  			return true;
  		
  		return false;
  	}
  	else
  	{
    	List assigns = getAssignments(gradebook.getId(), Assignment.DEFAULT_SORT, true);
    	List records = getAllAssignmentGradeRecords(gradebook.getId(), studentUids);
    	Set filteredAssigns = new HashSet();
    	for (Iterator iter = assigns.iterator(); iter.hasNext(); )
    	{
    		Assignment assign = (Assignment) iter.next();
    		if(assign != null && assign.isCounted() && !assign.isUngraded())
    		{
    			if(assign.getCategory() != null && !assign.getCategory().isRemoved())
    			{
    				filteredAssigns.add(assign.getId());
    			}
    		}
    	}
    	
  		List filteredRecords = new ArrayList();
  		for(Iterator iter = records.iterator(); iter.hasNext();)
  		{
  			AssignmentGradeRecord agr = (AssignmentGradeRecord)iter.next();
  			if(filteredAssigns.contains(agr.getAssignment().getId()) && !agr.isCourseGradeRecord())
  			{
  				if(agr.getPointsEarned() == null)
  					return true;
  				filteredRecords.add(agr);
  			}
  		}

  		if(filteredRecords.size() < filteredAssigns.size() * studentUids.size())
  			return true;

  		return false;
  	}
  }

  public List getAllAssignmentGradeRecords(final Long gradebookId, final Collection studentUids) {
  	HibernateCallback hc = new HibernateCallback() {
  		public Object doInHibernate(Session session) throws HibernateException {
  			if(studentUids.size() == 0) {
  				// If there are no enrollments, no need to execute the query.
  				if(log.isInfoEnabled()) log.info("No enrollments were specified.  Returning an empty List of grade records");
  				return new ArrayList();
  			} else {
  				Query q = session.createQuery("from AssignmentGradeRecord as agr where agr.gradableObject.removed=false and " +
  				"agr.gradableObject.gradebook.id=:gradebookId order by agr.pointsEarned");
  				q.setLong("gradebookId", gradebookId.longValue());
  				return filterGradeRecordsByStudents(q.list(), studentUids);
  			}
  		}
  	};
  	return (List)getHibernateTemplate().execute(hc);
  }

  public List getAssignments(final Long gradebookId, final String sortBy, final boolean ascending) {
  	return (List)getHibernateTemplate().execute(new HibernateCallback() {
  		public Object doInHibernate(Session session) throws HibernateException {
  			List assignments = getAssignments(gradebookId, session);

  			sortAssignments(assignments, sortBy, ascending);
  			return assignments;
  		}
  	});
  }
  private void sortAssignments(List assignments, String sortBy, boolean ascending) {
  	Comparator comp;
  	if(Assignment.SORT_BY_NAME.equals(sortBy)) {
  		comp = Assignment.nameComparator;
  	} else if(Assignment.SORT_BY_MEAN.equals(sortBy)) {
  		comp = Assignment.meanComparator;
  	} else if(Assignment.SORT_BY_POINTS.equals(sortBy)) {
  		comp = Assignment.pointsComparator;
  	}else if(Assignment.SORT_BY_RELEASED.equals(sortBy)){
  		comp = Assignment.releasedComparator;
  	} else if(Assignment.SORT_BY_COUNTED.equals(sortBy)){
  		comp = Assignment.countedComparator;
  	} else if(Assignment.SORT_BY_EDITOR.equals(sortBy)){
  		comp = Assignment.gradeEditorComparator;
  	} else {
  		comp = Assignment.dateComparator;
  	}
  	Collections.sort(assignments, comp);
  	if(!ascending) {
  		Collections.reverse(assignments);
  	}
  }
}