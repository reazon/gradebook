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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.sakaiproject.service.gradebook.shared.AssessmentNotFoundException;
import org.sakaiproject.service.gradebook.shared.AssignmentHasIllegalPointsException;
import org.sakaiproject.service.gradebook.shared.ConflictingAssignmentNameException;
import org.sakaiproject.service.gradebook.shared.ConflictingExternalIdException;
import org.sakaiproject.service.gradebook.shared.GradebookExistsException;
import org.sakaiproject.service.gradebook.shared.GradebookNotFoundException;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.service.gradebook.shared.GradingScaleDefinition;
import org.sakaiproject.tool.gradebook.Assignment;
import org.sakaiproject.tool.gradebook.AssignmentGradeRecord;
import org.sakaiproject.tool.gradebook.CourseGrade;
import org.sakaiproject.tool.gradebook.GradeMapping;
import org.sakaiproject.tool.gradebook.Gradebook;
import org.sakaiproject.tool.gradebook.GradingScale;
import org.sakaiproject.tool.gradebook.LetterGradeMapping;
import org.sakaiproject.tool.gradebook.LetterGradePlusMinusMapping;
import org.sakaiproject.tool.gradebook.PassNotPassMapping;
import org.sakaiproject.tool.gradebook.facades.Authz;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;

/**
 * A Hibernate implementation of GradebookService.
 */
public class GradebookServiceHibernateImpl extends BaseHibernateManager implements GradebookService {
    private static final Log log = LogFactory.getLog(GradebookServiceHibernateImpl.class);
    // Special logger for data contention analysis.
    private static final Log logData = LogFactory.getLog(GradebookServiceHibernateImpl.class.getName() + ".GB_DATA");

	public static final String UID_OF_DEFAULT_GRADING_SCALE_PROPERTY = "uidOfDefaultGradingScale";

    private Authz authz;

	public void addGradebook(final String uid, final String name) {
        if(isGradebookDefined(uid)) {
            log.warn("You can not add a gradebook with uid=" + uid + ".  That gradebook already exists.");
            throw new GradebookExistsException("You can not add a gradebook with uid=" + uid + ".  That gradebook already exists.");
        }
        if (log.isInfoEnabled()) log.info("Adding gradebook uid=" + uid + " by userUid=" + getUserUid());

        getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				// Get available grade mapping templates.
				List gradingScales = session.createQuery("from GradingScale as gradingScale where gradingScale.unavailable=false").list();

				// The application won't be able to run without grade mapping
				// templates, so if for some reason none have been defined yet,
				// do that now.
				if (gradingScales.isEmpty()) {
					if (log.isInfoEnabled()) log.info("No Grading Scale defined yet. This is probably because you have upgraded or you are working with a new database. Default grading scales will be created. Any customized system-wide grade mappings you may have defined in previous versions will have to be reconfigured.");
					gradingScales = GradebookServiceHibernateImpl.this.addDefaultGradingScales(session);
				}

				// Create and save the gradebook
				Gradebook gradebook = new Gradebook(name);
				gradebook.setUid(uid);
				session.save(gradebook);

				// Create the course grade for the gradebook
				CourseGrade cg = new CourseGrade();
				cg.setGradebook(gradebook);
				session.save(cg);

				// According to the specification, Display Assignment Grades is
				// on by default, and Display course grade is off.
				gradebook.setAssignmentsDisplayed(true);
				gradebook.setCourseGradeDisplayed(false);

				String defaultScaleUid = GradebookServiceHibernateImpl.this.getPropertyValue(UID_OF_DEFAULT_GRADING_SCALE_PROPERTY);

				// Add and save grade mappings based on the templates.
				GradeMapping defaultGradeMapping = null;
				Set gradeMappings = new HashSet();
				for (Iterator iter = gradingScales.iterator(); iter.hasNext();) {
					GradingScale gradingScale = (GradingScale)iter.next();
					GradeMapping gradeMapping = new GradeMapping(gradingScale);
					gradeMapping.setGradebook(gradebook);
					session.save(gradeMapping);
					gradeMappings.add(gradeMapping);
					if (gradingScale.getUid().equals(defaultScaleUid)) {
						defaultGradeMapping = gradeMapping;
					}
				}

				// Check for null default.
				if (defaultGradeMapping == null) {
					defaultGradeMapping = (GradeMapping)gradeMappings.iterator().next();
					if (log.isWarnEnabled()) log.warn("No default GradeMapping found for new Gradebook=" + gradebook.getUid() + "; will set default to " + defaultGradeMapping.getName());
				}
				gradebook.setSelectedGradeMapping(defaultGradeMapping);

				// The Hibernate mapping as of Sakai 2.2 makes this next
				// call meaningless when it comes to persisting changes at
				// the end of the transaction. It is, however, needed for
				// the mappings to be seen while the transaction remains
				// uncommitted.
				gradebook.setGradeMappings(gradeMappings);

				// Update the gradebook with the new selected grade mapping
				session.update(gradebook);

				return null;

			}
		});
	}

    private List addDefaultGradingScales(Session session) throws HibernateException {
    	List gradingScales = new ArrayList();

    	// Base the default set of templates on the old
    	// statically defined GradeMapping classes.
    	GradeMapping[] oldGradeMappings = {
    		new LetterGradeMapping(),
    		new LetterGradePlusMinusMapping(),
    		new PassNotPassMapping()
    	};

    	for (int i = 0; i < oldGradeMappings.length; i++) {
    		GradeMapping sampleMapping = oldGradeMappings[i];
    		sampleMapping.setDefaultValues();
			GradingScale gradingScale = new GradingScale();
			String uid = sampleMapping.getClass().getName();
			uid = uid.substring(uid.lastIndexOf('.') + 1);
			gradingScale.setUid(uid);
			gradingScale.setUnavailable(false);
			gradingScale.setName(sampleMapping.getName());
			gradingScale.setGrades(new ArrayList(sampleMapping.getGrades()));
			gradingScale.setDefaultBottomPercents(new HashMap(sampleMapping.getGradeMap()));
			session.save(gradingScale);
			if (log.isInfoEnabled()) log.info("Added Grade Mapping " + gradingScale.getUid());
			gradingScales.add(gradingScale);
		}
		setDefaultGradingScale("LetterGradePlusMinusMapping");
		session.flush();
		return gradingScales;
	}

	public void setAvailableGradingScales(final Collection gradingScaleDefinitions) {
        getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				mergeGradeMappings(gradingScaleDefinitions, session);
				return null;
			}
		});
	}

	public void setDefaultGradingScale(String uid) {
		setPropertyValue(UID_OF_DEFAULT_GRADING_SCALE_PROPERTY, uid);
	}

	private void copyDefinitionToScale(GradingScaleDefinition bean, GradingScale gradingScale) {
		gradingScale.setUnavailable(false);
		gradingScale.setName(bean.getName());
		gradingScale.setGrades(bean.getGrades());
		Map defaultBottomPercents = new HashMap();
		Iterator gradesIter = bean.getGrades().iterator();
		Iterator defaultBottomPercentsIter = bean.getDefaultBottomPercents().iterator();
		while (gradesIter.hasNext() && defaultBottomPercentsIter.hasNext()) {
			String grade = (String)gradesIter.next();
			Double value = (Double)defaultBottomPercentsIter.next();
			defaultBottomPercents.put(grade, value);
		}
		gradingScale.setDefaultBottomPercents(defaultBottomPercents);
	}

	private void mergeGradeMappings(Collection gradingScaleDefinitions, Session session) throws HibernateException {
		Map newMappingDefinitionsMap = new HashMap();
		HashSet uidsToSet = new HashSet();
		for (Iterator iter = gradingScaleDefinitions.iterator(); iter.hasNext(); ) {
			GradingScaleDefinition bean = (GradingScaleDefinition)iter.next();
			newMappingDefinitionsMap.put(bean.getUid(), bean);
			uidsToSet.add(bean.getUid());
		}

		// Until we move to Hibernate 3 syntax, we need to update one record at a time.
		Query q;
		List gmtList;

		// Toggle any scales that are no longer specified.
		q = session.createQuery("from GradingScale as gradingScale where gradingScale.uid not in (:uidList) and gradingScale.unavailable=false");
		q.setParameterList("uidList", uidsToSet);
		gmtList = q.list();
		for (Iterator iter = gmtList.iterator(); iter.hasNext(); ) {
			GradingScale gradingScale = (GradingScale)iter.next();
			gradingScale.setUnavailable(true);
			session.update(gradingScale);
			if (log.isInfoEnabled()) log.info("Set Grading Scale " + gradingScale.getUid() + " unavailable");
		}

		// Modify any specified scales that already exist.
		q = session.createQuery("from GradingScale as gradingScale where gradingScale.uid in (:uidList)");
		q.setParameterList("uidList", uidsToSet);
		gmtList = q.list();
		for (Iterator iter = gmtList.iterator(); iter.hasNext(); ) {
			GradingScale gradingScale = (GradingScale)iter.next();
			copyDefinitionToScale((GradingScaleDefinition)newMappingDefinitionsMap.get(gradingScale.getUid()), gradingScale);
			uidsToSet.remove(gradingScale.getUid());
			session.update(gradingScale);
			if (log.isInfoEnabled()) log.info("Updated Grading Scale " + gradingScale.getUid());
		}

		// Add any new scales.
		for (Iterator iter = uidsToSet.iterator(); iter.hasNext(); ) {
			String uid = (String)iter.next();
			GradingScale gradingScale = new GradingScale();
			gradingScale.setUid(uid);
			GradingScaleDefinition bean = (GradingScaleDefinition)newMappingDefinitionsMap.get(uid);
			copyDefinitionToScale(bean, gradingScale);
			session.save(gradingScale);
			if (log.isInfoEnabled()) log.info("Added Grading Scale " + gradingScale.getUid());
		}
		session.flush();
	}


	public void deleteGradebook(final String uid)
		throws GradebookNotFoundException {
        if (log.isInfoEnabled()) log.info("Deleting gradebook uid=" + uid + " by userUid=" + getUserUid());
        final Long gradebookId = getGradebook(uid).getId();

        // Worse of both worlds code ahead. We've been quick-marched
        // into Hibernate 3 sessions, but we're also having to use classic query
        // parsing -- which keeps us from being able to use either Hibernate's new-style
        // bulk delete queries or Hibernate's old-style session.delete method.
        // Instead, we're stuck with going through the Spring template for each
        // deletion one at a time.
        HibernateTemplate hibTempl = getHibernateTemplate();
        // int numberDeleted = hibTempl.bulkUpdate("delete GradingEvent as ge where ge.gradableObject.gradebook.id=?", gradebookId);
        // log.warn("GradingEvent numberDeleted=" + numberDeleted);

        List toBeDeleted;
        int numberDeleted;

        toBeDeleted = hibTempl.find("from GradingEvent as ge where ge.gradableObject.gradebook.id=?", gradebookId);
        numberDeleted = toBeDeleted.size();
        hibTempl.deleteAll(toBeDeleted);
        if (log.isDebugEnabled()) log.debug("Deleted " + numberDeleted + " grading events");

        toBeDeleted = hibTempl.find("from AbstractGradeRecord as gr where gr.gradableObject.gradebook.id=?", gradebookId);
        numberDeleted = toBeDeleted.size();
        hibTempl.deleteAll(toBeDeleted);
        if (log.isDebugEnabled()) log.debug("Deleted " + numberDeleted + " grade records");

        toBeDeleted = hibTempl.find("from GradableObject as go where go.gradebook.id=?", gradebookId);
        numberDeleted = toBeDeleted.size();
        hibTempl.deleteAll(toBeDeleted);
        if (log.isDebugEnabled()) log.debug("Deleted " + numberDeleted + " gradable objects");

        Gradebook gradebook = (Gradebook)hibTempl.load(Gradebook.class, gradebookId);
        gradebook.setSelectedGradeMapping(null);

        toBeDeleted = hibTempl.find("from GradeMapping as gm where gm.gradebook.id=?", gradebookId);
        numberDeleted = toBeDeleted.size();
        hibTempl.deleteAll(toBeDeleted);
        if (log.isDebugEnabled()) log.debug("Deleted " + numberDeleted + " grade mappings");

        hibTempl.delete(gradebook);
        hibTempl.flush();
        hibTempl.clear();
	}

    /**
     * @see org.sakaiproject.service.gradebook.shared.GradebookService#isGradebookDefined(java.lang.String)
     */
    public boolean isGradebookDefined(String gradebookUid) {
        String hql = "from Gradebook as gb where gb.uid=?";
        return getHibernateTemplate().find(hql, gradebookUid).size() == 1;
    }

    public boolean gradebookExists(String gradebookUid) {
        return isGradebookDefined(gradebookUid);
    }

    /**
	 * @see org.sakaiproject.service.gradebook.shared.GradebookService#addExternalAssessment(java.lang.String, java.lang.String, java.lang.String, java.lang.String, long, java.util.Date, java.lang.String)
	 */
	public void addExternalAssessment(final String gradebookUid, final String externalId, final String externalUrl,
			final String title, final double points, final Date dueDate, final String externalServiceDescription)
            throws ConflictingAssignmentNameException, ConflictingExternalIdException, GradebookNotFoundException {

        // Ensure that the required strings are not empty
        if(StringUtils.trimToNull(externalServiceDescription) == null ||
                StringUtils.trimToNull(externalId) == null ||
                StringUtils.trimToNull(title) == null) {
            throw new RuntimeException("External service description, externalId, and title must not be empty");
        }

        // Ensure that points is > zero
        if(points <= 0) {
            throw new AssignmentHasIllegalPointsException("Points must be > 0");
        }

        // Ensure that the assessment name is unique within this gradebook
		if (isAssignmentDefined(gradebookUid, title)) {
            throw new ConflictingAssignmentNameException("An assignment with that name already exists in gradebook uid=" + gradebookUid);
        }

		getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				// Ensure that the externalId is unique within this gradebook
				Integer externalIdConflicts = (Integer)session.createQuery(
					"select count(asn) from Assignment as asn where asn.externalId=? and asn.gradebook.uid=?").
					setString(0, externalId).
					setString(1, gradebookUid).
					uniqueResult();
				if (externalIdConflicts.intValue() > 0) {
					throw new ConflictingExternalIdException("An external assessment with that ID already exists in gradebook uid=" + gradebookUid);
				}

				// Get the gradebook
				Gradebook gradebook = getGradebook(gradebookUid);

				// Create the external assignment
				Assignment asn = new Assignment(gradebook, title, new Double(points), dueDate);
				asn.setExternallyMaintained(true);
				asn.setExternalId(externalId);
				asn.setExternalInstructorLink(externalUrl);
				asn.setExternalStudentLink(externalUrl);
				asn.setExternalAppName(externalServiceDescription);
                //set released to be true to support selective release
                asn.setReleased(true);

                session.save(asn);
				return null;
			}
		});
        if (log.isInfoEnabled()) log.info("External assessment added to gradebookUid=" + gradebookUid + ", externalId=" + externalId + " by userUid=" + getUserUid() + " from externalApp=" + externalServiceDescription);
	}

	/**
	 * @see org.sakaiproject.service.gradebook.shared.GradebookService#updateExternalAssessment(java.lang.String, java.lang.String, java.lang.String, java.lang.String, long, java.util.Date)
     */
    public void updateExternalAssessment(final String gradebookUid, final String externalId, final String externalUrl,
                                         final String title, final double points, final Date dueDate) throws GradebookNotFoundException, AssessmentNotFoundException,AssignmentHasIllegalPointsException {
        final Assignment asn = getExternalAssignment(gradebookUid, externalId);

        if(asn == null) {
            throw new AssessmentNotFoundException("There is no assessment id=" + externalId + " in gradebook uid=" + gradebookUid);
        }

        // Ensure that points is > zero
        if(points <= 0) {
            throw new AssignmentHasIllegalPointsException("Points must be > 0");
        }

        // Ensure that the required strings are not empty
        if( StringUtils.trimToNull(externalId) == null ||
                StringUtils.trimToNull(title) == null) {
            throw new RuntimeException("ExternalId, and title must not be empty");
        }

        HibernateCallback hc = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                asn.setExternalInstructorLink(externalUrl);
                asn.setExternalStudentLink(externalUrl);
                asn.setName(title);
                asn.setDueDate(dueDate);
                //support selective release
                asn.setReleased(true);
                asn.setPointsPossible(new Double(points));
                session.update(asn);
                if (log.isInfoEnabled()) log.info("External assessment updated in gradebookUid=" + gradebookUid + ", externalId=" + externalId + " by userUid=" + getUserUid());
                return null;

            }
        };
        getHibernateTemplate().execute(hc);
	}

	/**
	 * @see org.sakaiproject.service.gradebook.shared.GradebookService#removeExternalAssessment(java.lang.String, java.lang.String)
	 */
	public void removeExternalAssessment(final String gradebookUid,
            final String externalId) throws GradebookNotFoundException, AssessmentNotFoundException {
        // Get the external assignment
        final Assignment asn = getExternalAssignment(gradebookUid, externalId);
        if(asn == null) {
            throw new AssessmentNotFoundException("There is no external assessment id=" + externalId + " in gradebook uid=" + gradebookUid);
        }

        // We need to go through Spring's HibernateTemplate to do
        // any deletions at present. See the comments to deleteGradebook
        // for the details.
        HibernateTemplate hibTempl = getHibernateTemplate();

        final List studentsWithExternalScores = hibTempl.find("select agr.studentId from AssignmentGradeRecord as agr where agr.gradableObject=?", asn);

        List toBeDeleted = hibTempl.find("from AssignmentGradeRecord as agr where agr.gradableObject=?", asn);
        int numberDeleted = toBeDeleted.size();
        hibTempl.deleteAll(toBeDeleted);
        if (log.isInfoEnabled()) log.info("Deleted " + numberDeleted + " externally defined scores");

        // Delete the assessment.
		hibTempl.flush();
		hibTempl.clear();
		hibTempl.delete(asn);

        if (log.isInfoEnabled()) log.info("External assessment removed from gradebookUid=" + gradebookUid + ", externalId=" + externalId + " by userUid=" + getUserUid());
	}

    private Assignment getExternalAssignment(final String gradebookUid, final String externalId) throws GradebookNotFoundException {
        final Gradebook gradebook = getGradebook(gradebookUid);

        HibernateCallback hc = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
				return session.createQuery(
					"from Assignment as asn where asn.gradebook=? and asn.externalId=?").
					setEntity(0, gradebook).
					setString(1, externalId).
					uniqueResult();
            }
        };
        return (Assignment)getHibernateTemplate().execute(hc);
    }

	/**
	 * @see org.sakaiproject.service.gradebook.shared.GradebookService#updateExternalAssessmentScore(java.lang.String, java.lang.String, java.lang.String, Double)
	 */
	public void updateExternalAssessmentScore(final String gradebookUid, final String externalId,
			final String studentUid, final Double points) throws GradebookNotFoundException, AssessmentNotFoundException {

        final Assignment asn = getExternalAssignment(gradebookUid, externalId);

        if(asn == null) {
            throw new AssessmentNotFoundException("There is no assessment id=" + externalId + " in gradebook uid=" + gradebookUid);
        }
        
        if (logData.isDebugEnabled()) logData.debug("BEGIN: Update 1 score for gradebookUid=" + gradebookUid + ", external assessment=" + externalId + " from " + asn.getExternalAppName());

        HibernateCallback hc = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                Date now = new Date();

                AssignmentGradeRecord agr = getAssignmentGradeRecord(asn, studentUid, session);

                // Try to reduce data contention by only updating when the
                // score has actually changed.
                Double oldPointsEarned = (agr == null) ? null : agr.getPointsEarned();
                if ( ((points != null) && (!points.equals(oldPointsEarned))) ||
					((points == null) && (oldPointsEarned != null)) ) {
					if (agr == null) {
						agr = new AssignmentGradeRecord(asn, studentUid, points);
					} else {
						agr.setPointsEarned(points);
					}

					agr.setDateRecorded(now);
					agr.setGraderId(getUserUid());
					if (log.isDebugEnabled()) log.debug("About to save AssignmentGradeRecord id=" + agr.getId() + ", version=" + agr.getVersion() + ", studenttId=" + agr.getStudentId() + ", pointsEarned=" + agr.getPointsEarned());
					session.saveOrUpdate(agr);

					Gradebook gradebook = asn.getGradebook();
					Set set = new HashSet();
					set.add(studentUid);

					// Sync database.
					session.flush();
					session.clear();
				} else {
					if(log.isDebugEnabled()) log.debug("Ignoring updateExternalAssessmentScore, since the new points value is the same as the old");
				}
                return null;
            }
        };
        getHibernateTemplate().execute(hc);
        if (logData.isDebugEnabled()) logData.debug("END: Update 1 score for gradebookUid=" + gradebookUid + ", external assessment=" + externalId + " from " + asn.getExternalAppName());
		if (log.isDebugEnabled()) log.debug("External assessment score updated in gradebookUid=" + gradebookUid + ", externalId=" + externalId + " by userUid=" + getUserUid() + ", new score=" + points);
	}

	public void updateExternalAssessmentScores(final String gradebookUid, final String externalId, final Map studentUidsToScores)
		throws GradebookNotFoundException, AssessmentNotFoundException {

        final Assignment assignment = getExternalAssignment(gradebookUid, externalId);
        if (assignment == null) {
            throw new AssessmentNotFoundException("There is no assessment id=" + externalId + " in gradebook uid=" + gradebookUid);
        }
		final Set studentIds = studentUidsToScores.keySet();
		if (studentIds.isEmpty()) {
			return;
		}
		final Date now = new Date();
		final String graderId = getUserUid();

		getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				List existingScores;
				if (studentIds.size() <= MAX_NUMBER_OF_SQL_PARAMETERS_IN_LIST) {
					Query q = session.createQuery("from AssignmentGradeRecord as gr where gr.gradableObject=:go and gr.studentId in (:studentIds)");
					q.setParameter("go", assignment);
					q.setParameterList("studentIds", studentIds);
					existingScores = q.list();
				} else {
					Query q = session.createQuery("from AssignmentGradeRecord as gr where gr.gradableObject=:go");
					q.setParameter("go", assignment);
					existingScores = filterGradeRecordsByStudents(q.list(), studentIds);
				}

				Set previouslyUnscoredStudents = new HashSet(studentIds);
				Set changedStudents = new HashSet();
				for (Iterator iter = existingScores.iterator(); iter.hasNext(); ) {
					AssignmentGradeRecord agr = (AssignmentGradeRecord)iter.next();
					String studentUid = agr.getStudentId();
					previouslyUnscoredStudents.remove(studentUid);

					// Try to reduce data contention by only updating when a score
					// has changed.
					Double oldPointsEarned = agr.getPointsEarned();
					Double newPointsEarned = (Double)studentUidsToScores.get(studentUid);
					if ( ((newPointsEarned != null) && (!newPointsEarned.equals(oldPointsEarned))) || ((newPointsEarned == null) && (oldPointsEarned != null)) ) {
						agr.setDateRecorded(now);
						agr.setGraderId(graderId);
						agr.setPointsEarned(newPointsEarned);
						session.update(agr);
						changedStudents.add(studentUid);
					}
				}
				for (Iterator iter = previouslyUnscoredStudents.iterator(); iter.hasNext(); ) {
					String studentUid = (String)iter.next();

					// Don't save unnecessary null scores.
					Double newPointsEarned = (Double)studentUidsToScores.get(studentUid);
					if (newPointsEarned != null) {
						AssignmentGradeRecord agr = new AssignmentGradeRecord(assignment, studentUid, newPointsEarned);
						agr.setDateRecorded(now);
						agr.setGraderId(graderId);
						session.save(agr);
						changedStudents.add(studentUid);
					}
				}

				if (log.isDebugEnabled()) log.debug("updateExternalAssessmentScores sent " + studentIds.size() + " records, actually changed " + changedStudents.size());

				// Sync database.
				session.flush();
				session.clear();
                return null;
            }
        });
	}


	public boolean isAssignmentDefined(final String gradebookUid, final String assignmentName)
        throws GradebookNotFoundException {
        Assignment assignment = (Assignment)getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				return getAssignmentWithoutStats(gradebookUid, assignmentName, session);
			}
		});
        return (assignment != null);
    }

	public boolean isExternalAssignmentDefined(String gradebookUid, String externalId) throws GradebookNotFoundException {
        Assignment assignment = getExternalAssignment(gradebookUid, externalId);
        return (assignment != null);
	}

	public boolean isUserAbleToGradeStudent(String gradebookUid, String studentUid) {
		return getAuthz().isUserAbleToGradeStudent(gradebookUid, studentUid);
	}

	public List getAssignments(String gradebookUid)
		throws GradebookNotFoundException {
		final Long gradebookId = getGradebook(gradebookUid).getId();

        List internalAssignments = (List)getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                return getAssignments(gradebookId, session);
            }
        });

		List assignments = new ArrayList();
		for (Iterator iter = internalAssignments.iterator(); iter.hasNext(); ) {
			Assignment assignment = (Assignment)iter.next();
			assignments.add(new AssignmentImpl(assignment));
		}
		return assignments;
	}

	public Double getAssignmentScore(final String gradebookUid, final String assignmentName, final String studentUid)
		throws GradebookNotFoundException, AssessmentNotFoundException {
		if (!isUserAbleToGradeStudent(gradebookUid, studentUid)) {
			log.error("AUTHORIZATION FAILURE: User " + getUserUid() + " in gradebook " + gradebookUid + " attempted to retrieve grade for student " + studentUid);
			throw new SecurityException("You do not have permission to perform this operation");
		}

		Double assignmentScore = (Double)getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				Assignment assignment = getAssignmentWithoutStats(gradebookUid, assignmentName, session);
				if (assignment == null) {
					throw new AssessmentNotFoundException("There is no assignment named " + assignmentName + " in gradebook " + gradebookUid);
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
		if (!isUserAbleToGradeStudent(gradebookUid, studentUid)) {
			log.error("AUTHORIZATION FAILURE: User " + getUserUid() + " in gradebook " + gradebookUid + " attempted to grade student " + studentUid + " from " + clientServiceDescription);
			throw new SecurityException("You do not have permission to perform this operation");
		}

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
				// Sync database.
				session.flush();
				session.clear();
				return null;
			}
		});

		if (log.isInfoEnabled()) log.info("Score updated in gradebookUid=" + gradebookUid + ", assignmentName=" + assignmentName + " by userUid=" + getUserUid() + " from client=" + clientServiceDescription + ", new score=" + score);
	}

    public Authz getAuthz() {
        return authz;
    }
    public void setAuthz(Authz authz) {
        this.authz = authz;
    }

	private Assignment getAssignmentWithoutStats(String gradebookUid, String assignmentName, Session session) throws HibernateException {
		return (Assignment)session.createQuery(
			"from Assignment as asn where asn.name=? and asn.gradebook.uid=? and asn.removed=false").
			setString(0, assignmentName).
			setString(1, gradebookUid).
			uniqueResult();
	}

	private AssignmentGradeRecord getAssignmentGradeRecord(Assignment assignment, String studentUid, Session session) throws HibernateException {
		return (AssignmentGradeRecord)session.createQuery(
			"from AssignmentGradeRecord as agr where agr.studentId=? and agr.gradableObject.id=?").
			setString(0, studentUid).
			setLong(1, assignment.getId().longValue()).
			uniqueResult();
	}

}

