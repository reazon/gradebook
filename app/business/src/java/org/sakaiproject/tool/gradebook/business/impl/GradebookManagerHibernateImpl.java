/**********************************************************************************
*
* $Id$
*
***********************************************************************************
*
* Copyright (c) 2005, 2006 The Regents of the University of California, The MIT Corporation
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

package org.sakaiproject.tool.gradebook.business.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.StaleObjectStateException;
import org.hibernate.TransientObjectException;
import org.sakaiproject.component.gradebook.BaseHibernateManager;
import org.sakaiproject.service.gradebook.shared.ConflictingAssignmentNameException;
import org.sakaiproject.service.gradebook.shared.ConflictingCategoryNameException;
import org.sakaiproject.service.gradebook.shared.ConflictingSpreadsheetNameException;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.service.gradebook.shared.StaleObjectModificationException;
import org.sakaiproject.tool.gradebook.AbstractGradeRecord;
import org.sakaiproject.tool.gradebook.Assignment;
import org.sakaiproject.tool.gradebook.AssignmentGradeRecord;
import org.sakaiproject.tool.gradebook.Category;
import org.sakaiproject.tool.gradebook.Comment;
import org.sakaiproject.tool.gradebook.CourseGrade;
import org.sakaiproject.tool.gradebook.CourseGradeRecord;
import org.sakaiproject.tool.gradebook.GradableObject;
import org.sakaiproject.tool.gradebook.Gradebook;
import org.sakaiproject.tool.gradebook.GradingEvent;
import org.sakaiproject.tool.gradebook.GradingEvents;
import org.sakaiproject.tool.gradebook.LetterGradePercentMapping;
import org.sakaiproject.tool.gradebook.Spreadsheet;
import org.sakaiproject.tool.gradebook.business.GradebookManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException;

/** synchronize from external application*/
import org.sakaiproject.tool.gradebook.business.GbSynchronizer;
import org.sakaiproject.thread_local.cover.ThreadLocalManager;


/**
 * Manages Gradebook persistence via hibernate.
 */
public class GradebookManagerHibernateImpl extends BaseHibernateManager
        implements GradebookManager {

    private static final Log log = LogFactory.getLog(GradebookManagerHibernateImpl.class);
    
    // Special logger for data contention analysis.
    private static final Log logData = LogFactory.getLog(GradebookManagerHibernateImpl.class.getName() + ".GB_DATA");

    /** synchronize from external application*/
    GbSynchronizer synchronizer = null;

    public void removeAssignment(final Long assignmentId) throws StaleObjectModificationException {
        HibernateCallback hc = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                Assignment asn = (Assignment)session.load(Assignment.class, assignmentId);
                Gradebook gradebook = asn.getGradebook();
                asn.setRemoved(true);
                session.update(asn);
                /** synchronize from external application*/
                if ( (synchronizer != null) && (!synchronizer.isProjectSite()))
                {
                	synchronizer.deleteLegacyAssignment(asn.getName());
                }
                if(log.isInfoEnabled()) log.info("Assignment " + asn.getName() + " has been removed from " + gradebook);
                return null;
            }
        };
        getHibernateTemplate().execute(hc);
    }

    public Gradebook getGradebook(Long id) {
        return (Gradebook)getHibernateTemplate().load(Gradebook.class, id);
    }

    public List getAssignmentGradeRecords(final Assignment assignment, final Collection studentUids) {
        HibernateCallback hc = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                if(studentUids == null || studentUids.size() == 0) {
                    if(log.isInfoEnabled()) log.info("Returning no grade records for an empty collection of student UIDs");
                    return new ArrayList();
                } else if (assignment.isRemoved()) {
                    return new ArrayList();                	
                }

                Query q = session.createQuery("from AssignmentGradeRecord as agr where agr.gradableObject.id=:gradableObjectId order by agr.pointsEarned");
                q.setLong("gradableObjectId", assignment.getId().longValue());
                List records = filterGradeRecordsByStudents(q.list(), studentUids);
                return records;
            }
        };
        return (List)getHibernateTemplate().execute(hc);
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

    public List getPointsEarnedCourseGradeRecordsWithStats(final CourseGrade courseGrade, final Collection studentUids) {
    	// Get good class-wide statistics by including all students, whether
    	// the caller is specifically interested in their grade records or not.
    	Long gradebookId = courseGrade.getGradebook().getId();
    	Set allStudentUids = getAllStudentUids(getGradebookUid(gradebookId));
    	List courseGradeRecords = getPointsEarnedCourseGradeRecords(courseGrade, allStudentUids);
    	courseGrade.calculateStatistics(courseGradeRecords, allStudentUids.size());

    	// Filter out the grade records which weren't specified.
    	courseGradeRecords = filterGradeRecordsByStudents(courseGradeRecords, studentUids);

    	return courseGradeRecords;
    }

    public void addToGradeRecordMap(Map gradeRecordMap, List gradeRecords) {
		for (Iterator iter = gradeRecords.iterator(); iter.hasNext(); ) {
			AbstractGradeRecord gradeRecord = (AbstractGradeRecord)iter.next();
			String studentUid = gradeRecord.getStudentId();
			Map studentMap = (Map)gradeRecordMap.get(studentUid);
			if (studentMap == null) {
				studentMap = new HashMap();
				gradeRecordMap.put(studentUid, studentMap);
			}
			studentMap.put(gradeRecord.getGradableObject().getId(), gradeRecord);
		}
    }
    
    public void addToCategoryResultMap(Map categoryResultMap, List categories, Map gradeRecordMap, Map enrollmentMap) {
    	
    	for (Iterator stuIter = enrollmentMap.keySet().iterator(); stuIter.hasNext(); ){
    		String studentUid = (String) stuIter.next();
    		Map studentMap = (Map) gradeRecordMap.get(studentUid);
    		
    		for (Iterator iter = categories.iterator(); iter.hasNext(); ){
    			Object obj = iter.next();
    			if(!(obj instanceof Category)){
    				continue;
    			}
    			Category category = (Category) obj; 		
	    		
	    		List categoryAssignments = category.getAssignmentList();
	    		if (categoryAssignments == null){
	    			continue;
	    		}
	    		
	    		List gradeRecords = new ArrayList();
								
	    		for (Iterator assignmentsIter = categoryAssignments.iterator(); assignmentsIter.hasNext(); ){
	    			Assignment assignment = (Assignment) assignmentsIter.next();
	    			AbstractGradeRecord gradeRecord = (AbstractGradeRecord) studentMap.get(assignment.getId());
	    			gradeRecords.add(gradeRecord);
			
	    		}
	    		category.calculateStatisticsPerStudent(gradeRecords, studentUid);

	    		Map studentCategoryMap = (Map) categoryResultMap.get(studentUid);
		    	if (studentCategoryMap == null) {
		    		studentCategoryMap = new HashMap();
		    		categoryResultMap.put(studentUid, studentCategoryMap);
		    	}
		    	Map stats = new HashMap();
		    	stats.put("studentAverageScore", category.getAverageScore());
		    	stats.put("studentAverageTotalPoints", category.getAverageTotalPoints());
		    	stats.put("studentMean", category.getMean());
		    	
		    	stats.put("category", category);

		    	studentCategoryMap.put(category.getId(), stats);
	    	}
    	}
    	
    }
    
//    public List getPointsEarnedCourseGradeRecords(final CourseGrade courseGrade, final Collection studentUids, final Collection assignments, final Map gradeRecordMap) {
//    	HibernateCallback hc = new HibernateCallback() {
//    		public Object doInHibernate(Session session) throws HibernateException {
//    			if(studentUids == null || studentUids.size() == 0) {
//    				if(log.isInfoEnabled()) log.info("Returning no grade records for an empty collection of student UIDs");
//    				return new ArrayList();
//    			}
//
//    			Query q = session.createQuery("from CourseGradeRecord as cgr where cgr.gradableObject.id=:gradableObjectId");
//    			q.setLong("gradableObjectId", courseGrade.getId().longValue());
//    			List records = filterAndPopulateCourseGradeRecordsByStudents(courseGrade, q.list(), studentUids);
//
//     			Set assignmentsNotCounted = new HashSet();
//    			double totalPointsPossible = 0;
//     			for (Iterator iter = assignments.iterator(); iter.hasNext(); ) {
//     				Assignment assignment = (Assignment)iter.next();
//     				if (assignment.isCounted()) {
//     					totalPointsPossible += assignment.getPointsPossible();
//     				} else {
//     					assignmentsNotCounted.add(assignment.getId());
//     				}
//     			}
//    			if(log.isDebugEnabled()) log.debug("Total points = " + totalPointsPossible);
//
//    			for(Iterator iter = records.iterator(); iter.hasNext();) {
//    				CourseGradeRecord cgr = (CourseGradeRecord)iter.next();
//    				double totalPointsEarned = 0;
//    				Map studentMap = (Map)gradeRecordMap.get(cgr.getStudentId());
//    				if (studentMap != null) {
//        				Collection studentGradeRecords = studentMap.values();
//    					for (Iterator gradeRecordIter = studentGradeRecords.iterator(); gradeRecordIter.hasNext(); ) {
//    						AssignmentGradeRecord agr = (AssignmentGradeRecord)gradeRecordIter.next();
//    						if (!assignmentsNotCounted.contains(agr.getGradableObject().getId())) {
//    							Double pointsEarned = agr.getPointsEarned();
//    							if (pointsEarned != null) {
//    								totalPointsEarned += pointsEarned.doubleValue();
//    							}    						
//    						}
//    					}
//    				}
//   					cgr.initNonpersistentFields(totalPointsPossible, totalPointsEarned);
//    				if(log.isDebugEnabled()) log.debug("Points earned = " + cgr.getPointsEarned());
//    			}
//
//    			return records;
//    		}
//    	};
//    	return (List)getHibernateTemplate().execute(hc);
//    }
    public List getPointsEarnedCourseGradeRecords(final CourseGrade courseGrade, final Collection studentUids, final Collection assignments, final Map gradeRecordMap) {
    	HibernateCallback hc = new HibernateCallback() {
    		public Object doInHibernate(Session session) throws HibernateException {
    			if(studentUids == null || studentUids.size() == 0) {
    				if(log.isInfoEnabled()) log.info("Returning no grade records for an empty collection of student UIDs");
    				return new ArrayList();
    			}

    			Query q = session.createQuery("from CourseGradeRecord as cgr where cgr.gradableObject.id=:gradableObjectId");
    			q.setLong("gradableObjectId", courseGrade.getId().longValue());
    			List records = filterAndPopulateCourseGradeRecordsByStudents(courseGrade, q.list(), studentUids);

    			Gradebook gradebook = getGradebook(courseGrade.getGradebook().getId());
    			List categories = getCategories(courseGrade.getGradebook().getId());
    			
     			Set assignmentsNotCounted = new HashSet();
    			double totalPointsPossible = 0;
					Map cateTotalScoreMap = new HashMap();

					for (Iterator iter = assignments.iterator(); iter.hasNext(); ) {
						Assignment assignment = (Assignment)iter.next();
						if (!assignment.isCounted() || assignment.isUngraded() || assignment.getPointsPossible().doubleValue() <= 0.0) {
   						assignmentsNotCounted.add(assignment.getId());
   					}
    			}
    			if(log.isDebugEnabled()) log.debug("Total points = " + totalPointsPossible);

    			for(Iterator iter = records.iterator(); iter.hasNext();) {
    				CourseGradeRecord cgr = (CourseGradeRecord)iter.next();
    				double totalPointsEarned = 0;
    	    	double literalTotalPointsEarned = 0;
  					Map cateScoreMap = new HashMap();
    				Map studentMap = (Map)gradeRecordMap.get(cgr.getStudentId());
    				Set assignmentsTaken = new HashSet();
    				if (studentMap != null) {
    					Collection studentGradeRecords = studentMap.values();
    					for (Iterator gradeRecordIter = studentGradeRecords.iterator(); gradeRecordIter.hasNext(); ) {
    						AssignmentGradeRecord agr = (AssignmentGradeRecord)gradeRecordIter.next();
    						if (!assignmentsNotCounted.contains(agr.getGradableObject().getId())) {
    							Double pointsEarned = agr.getPointsEarned();
    							if (pointsEarned != null) {
    								if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_NO_CATEGORY)
    								{
    									totalPointsEarned += pointsEarned.doubleValue();
    						    	literalTotalPointsEarned += pointsEarned.doubleValue();
              				assignmentsTaken.add(agr.getAssignment().getId());
    								}
    								else if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_ONLY_CATEGORY && categories != null)
    	     					{
    									totalPointsEarned += pointsEarned.doubleValue();
    									literalTotalPointsEarned += pointsEarned.doubleValue();
    									assignmentsTaken.add(agr.getAssignment().getId());
    	     					}
    			    			else if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY && categories != null)
    			    			{
    			    				for(int i=0; i<categories.size(); i++)
    			    				{
    			    					Category cate = (Category) categories.get(i);
    			    					if(cate != null && !cate.isRemoved() && agr.getAssignment().getCategory() != null && cate.getId().equals(agr.getAssignment().getCategory().getId()))
    			    					{
    		          				assignmentsTaken.add(agr.getAssignment().getId());
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
    					}

    					cateTotalScoreMap.clear();
    		    	if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY && categories != null)
    		    	{
    		    		Iterator assignIter = assignments.iterator();
    		    		while (assignIter.hasNext()) 
    		    		{
    		    			Assignment asgn = (Assignment)assignIter.next();
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
    				}

    				totalPointsPossible = 0;
    				if(!assignmentsTaken.isEmpty())
    	    	{
    	    		if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY)
    	    		{
    		    		for(int i=0; i<categories.size(); i++)
    		    		{
    		    			Category cate = (Category) categories.get(i);
    		    			if(cate != null && !cate.isRemoved() && cateScoreMap.get(cate.getId()) != null && cateTotalScoreMap.get(cate.getId()) != null)
    		    			{
    		    				totalPointsPossible += cate.getWeight().doubleValue();
    		    			}
    		    		}
    	    		}
    	    		Iterator assignIter = assignments.iterator();
  		    		while (assignIter.hasNext()) 
  		    		{
    						Assignment assignment = (Assignment)assignIter.next();
    						if(assignment != null)
    						{
    							Double pointsPossible = assignment.getPointsPossible();
    							if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_NO_CATEGORY && assignmentsTaken.contains(assignment.getId()))
    							{
    	    					totalPointsPossible += pointsPossible.doubleValue();
    							}
    							else if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_ONLY_CATEGORY && assignmentsTaken.contains(assignment.getId()))
    							{
    								totalPointsPossible += pointsPossible.doubleValue();
    							}
    						}
    					}
    	    	}
    				cgr.initNonpersistentFields(totalPointsPossible, totalPointsEarned, literalTotalPointsEarned);
    				if(log.isDebugEnabled()) log.debug("Points earned = " + cgr.getPointsEarned());
    			}

    			return records;
    		}
    	};
    	return (List)getHibernateTemplate().execute(hc);
    }

    /**
     */
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
    
    public List getAllAssignmentGradeRecordsConverted(Long gradebookId, Collection studentUids)
    {
    	List allAssignRecordsFromDB = getAllAssignmentGradeRecords(gradebookId, studentUids);
    	Gradebook gradebook = getGradebook(gradebookId);
    	if(gradebook.getGrade_type() == GradebookService.GRADE_TYPE_POINTS)
    		return allAssignRecordsFromDB;
    	else if(gradebook.getGrade_type() == GradebookService.GRADE_TYPE_PERCENTAGE)
    	{
    		return convertPointsToPercentage(gradebook, allAssignRecordsFromDB);
    	}
    	else if(gradebook.getGrade_type() == GradebookService.GRADE_TYPE_LETTER)
    	{
    		return convertPointsToLetterGrade(gradebook, allAssignRecordsFromDB);
    	}
    	return null;
    }

    /**
     * @return Returns set of student UIDs who were given scores higher than the assignment's value.
     */
    public Set updateAssignmentGradeRecords(final Assignment assignment, final Collection gradeRecordsFromCall)
            throws StaleObjectModificationException {
        // If no grade records are sent, don't bother doing anything with the db
        if(gradeRecordsFromCall.size() == 0) {
            log.debug("updateAssignmentGradeRecords called for zero grade records");
            return new HashSet();
        }

        if (logData.isDebugEnabled()) logData.debug("BEGIN: Update " + gradeRecordsFromCall.size() + " scores for gradebook=" + assignment.getGradebook().getUid() + ", assignment=" + assignment.getName());

        HibernateCallback hc = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                Date now = new Date();
                String graderId = authn.getUserUid();

                Set studentsWithUpdatedAssignmentGradeRecords = new HashSet();
                Set studentsWithExcessiveScores = new HashSet();
                
                /** synchronize from external application*/
                if(synchronizer != null)
                {
                	boolean isUpdateAll = Boolean.TRUE.equals(ThreadLocalManager.get("iquiz_update_all"));
                	boolean isIquizCall = Boolean.TRUE.equals(ThreadLocalManager.get("iquiz_call"));
                	boolean isStudentView = Boolean.TRUE.equals(ThreadLocalManager.get("iquiz_student_view"));

                	Map iquizAssignmentMap = new HashMap();            
                	List legacyUpdates = new ArrayList();            
                	Map convertedEidUidRecordMap = new HashMap();

                	convertedEidUidRecordMap = synchronizer.convertEidUid(gradeRecordsFromCall);
                	if (!isUpdateAll && synchronizer !=null && !synchronizer.isProjectSite()){
                		iquizAssignmentMap = synchronizer.getLegacyAssignmentWithStats(assignment.getName());
                	}
                	Map recordsFromCLDb = null;
                	if(synchronizer != null && isIquizCall && isUpdateAll)
                	{
                		recordsFromCLDb = synchronizer.getPersistentRecords(assignment.getId());
                	}

                	for(Iterator iter = gradeRecordsFromCall.iterator(); iter.hasNext();) {
                		AssignmentGradeRecord gradeRecordFromCall = (AssignmentGradeRecord)iter.next();

                		boolean updated = false;
                		if(isIquizCall && synchronizer != null)
                		{
                			gradeRecordFromCall = synchronizer.convertIquizRecordToUid(gradeRecordFromCall, convertedEidUidRecordMap, isUpdateAll, graderId);
                		}
                		else
                		{
                			gradeRecordFromCall.setGraderId(graderId);
                			gradeRecordFromCall.setDateRecorded(now);
                		}
                		try {
                			/** sychronize - add condition for null value */
                			if(gradeRecordFromCall != null)
                			{
                				if(gradeRecordFromCall.getId() == null && isIquizCall && isUpdateAll && recordsFromCLDb != null)
                				{
                					AssignmentGradeRecord returnedPersistentItem = (AssignmentGradeRecord) recordsFromCLDb.get(gradeRecordFromCall.getStudentId());
                					if(returnedPersistentItem != null && returnedPersistentItem.getPointsEarned() != null && gradeRecordFromCall.getPointsEarned() != null
                							&& !returnedPersistentItem.getPointsEarned().equals(gradeRecordFromCall.getPointsEarned()))
                					{
                						graderId = gradeRecordFromCall.getGraderId();
                						updated = true;
                						returnedPersistentItem.setGraderId(gradeRecordFromCall.getGraderId());
                						returnedPersistentItem.setPointsEarned(gradeRecordFromCall.getPointsEarned());
                						returnedPersistentItem.setDateRecorded(gradeRecordFromCall.getDateRecorded());
                						session.saveOrUpdate(returnedPersistentItem);
                					}
                					else if(returnedPersistentItem == null)
                					{
                						graderId = gradeRecordFromCall.getGraderId();
                						updated = true;
                						session.saveOrUpdate(gradeRecordFromCall);
                					}
                				}
                				else
                				{
                					updated = true;
                					session.saveOrUpdate(gradeRecordFromCall);
                				}
                			}
                			if (!isUpdateAll && !isStudentView && synchronizer != null && !synchronizer.isProjectSite())
                			{
                				Object updateIquizRecord = synchronizer.getNeededUpdateIquizRecord(assignment, gradeRecordFromCall);
                				if(updateIquizRecord != null)
                					legacyUpdates.add(updateIquizRecord);
                			}
                		} catch (TransientObjectException e) {
                			// It's possible that a previously unscored student
                			// was scored behind the current user's back before
                			// the user saved the new score. This translates
                			// that case into an optimistic locking failure.
                			if(log.isInfoEnabled()) log.info("An optimistic locking failure occurred while attempting to add a new assignment grade record");
                			throw new StaleObjectModificationException(e);
                		}

                		// Check for excessive (AKA extra credit) scoring.
                		/** synchronize - add condition for null value*/
                		if(gradeRecordFromCall != null && updated == true)
                		{
                			if (gradeRecordFromCall.getPointsEarned() != null &&
                					!assignment.isUngraded() && 
                					gradeRecordFromCall.getPointsEarned().compareTo(assignment.getPointsPossible()) > 0) {
                				studentsWithExcessiveScores.add(gradeRecordFromCall.getStudentId());
                			}

                			// Log the grading event, and keep track of the students with saved/updated grades
                			session.save(new GradingEvent(assignment, graderId, gradeRecordFromCall.getStudentId(), gradeRecordFromCall.getPointsEarned()));
                			studentsWithUpdatedAssignmentGradeRecords.add(gradeRecordFromCall.getStudentId());
                		}

                		/** synchronize external records */
                		if (legacyUpdates.size() > 0 && synchronizer != null)
                		{
                			synchronizer.updateLegacyGradeRecords(assignment.getName(), legacyUpdates);
                		}
                	}

                }
                else
                {
                	for(Iterator iter = gradeRecordsFromCall.iterator(); iter.hasNext();) {
                		AssignmentGradeRecord gradeRecordFromCall = (AssignmentGradeRecord)iter.next();
                		gradeRecordFromCall.setGraderId(graderId);
                		gradeRecordFromCall.setDateRecorded(now);
                		try {
                			session.saveOrUpdate(gradeRecordFromCall);
                		} catch (TransientObjectException e) {
                			// It's possible that a previously unscored student
                			// was scored behind the current user's back before
                			// the user saved the new score. This translates
                			// that case into an optimistic locking failure.
                			if(log.isInfoEnabled()) log.info("An optimistic locking failure occurred while attempting to add a new assignment grade record");
                			throw new StaleObjectModificationException(e);
                		}

                		// Check for excessive (AKA extra credit) scoring.
                		if (gradeRecordFromCall.getPointsEarned() != null &&
                				!assignment.isUngraded() && 
                				gradeRecordFromCall.getPointsEarned().compareTo(assignment.getPointsPossible()) > 0) {
                			studentsWithExcessiveScores.add(gradeRecordFromCall.getStudentId());
                		}

                		// Log the grading event, and keep track of the students with saved/updated grades
                		session.save(new GradingEvent(assignment, graderId, gradeRecordFromCall.getStudentId(), gradeRecordFromCall.getPointsEarned()));
                		studentsWithUpdatedAssignmentGradeRecords.add(gradeRecordFromCall.getStudentId());
                	}
                }
                if (logData.isDebugEnabled()) logData.debug("Updated " + studentsWithUpdatedAssignmentGradeRecords.size() + " assignment score records");

                return studentsWithExcessiveScores;
            }
        };

        Set studentsWithExcessiveScores = (Set)getHibernateTemplate().execute(hc);
        if (logData.isDebugEnabled()) logData.debug("END: Update " + gradeRecordsFromCall.size() + " scores for gradebook=" + assignment.getGradebook().getUid() + ", assignment=" + assignment.getName());
        return studentsWithExcessiveScores;
    }
    
    /**
     * 
     * @return Returns set of Assignments given scores higher than the assignment's value.
     */
    private Set updateStudentGradeRecords(final Collection gradeRecordsFromCall)
            throws StaleObjectModificationException {
        // If no grade records are sent, don't bother doing anything with the db
        if(gradeRecordsFromCall.size() == 0) {
            log.debug("updateStudentGradeRecords called for zero grade records");
            return new HashSet();
        }

        if (logData.isDebugEnabled()) logData.debug("BEGIN: Update " + gradeRecordsFromCall.size());

        HibernateCallback hc = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                Date now = new Date();
                String graderId = authn.getUserUid();

                Set studentsWithUpdatedAssignmentGradeRecords = new HashSet();
                Set assignmentsWithExcessiveScores = new HashSet();

                for(Iterator iter = gradeRecordsFromCall.iterator(); iter.hasNext();) {
                	AssignmentGradeRecord gradeRecordFromCall = (AssignmentGradeRecord)iter.next();
                	Assignment assignment = gradeRecordFromCall.getAssignment();
                	Double pointsPossible = assignment.getPointsPossible();
                	
                	gradeRecordFromCall.setGraderId(graderId);
                	gradeRecordFromCall.setDateRecorded(now);
                	try {
                		session.saveOrUpdate(gradeRecordFromCall);
                	} catch (TransientObjectException e) {
                		// It's possible that a previously unscored student
                		// was scored behind the current user's back before
                		// the user saved the new score. This translates
                		// that case into an optimistic locking failure.
                		if(log.isInfoEnabled()) log.info("An optimistic locking failure occurred while attempting to add a new assignment grade record");
                		throw new StaleObjectModificationException(e);
                	}

                	// Check for excessive (AKA extra credit) scoring.
                	if (gradeRecordFromCall.getPointsEarned() != null &&
                			!assignment.isUngraded() && 
                			gradeRecordFromCall.getPointsEarned().compareTo(pointsPossible) > 0) {
                		assignmentsWithExcessiveScores.add(assignment);
                	}

                	// Log the grading event, and keep track of the students with saved/updated grades
                	session.save(new GradingEvent(assignment, graderId, gradeRecordFromCall.getStudentId(), gradeRecordFromCall.getPointsEarned()));
                	studentsWithUpdatedAssignmentGradeRecords.add(gradeRecordFromCall.getStudentId());
                }
				if (logData.isDebugEnabled()) logData.debug("Updated " + studentsWithUpdatedAssignmentGradeRecords.size() + " assignment score records");

                return assignmentsWithExcessiveScores;
            }
        };

        Set assignmentsWithExcessiveScores = (Set)getHibernateTemplate().execute(hc);
        if (logData.isDebugEnabled()) logData.debug("END: Update " + gradeRecordsFromCall.size());
        return assignmentsWithExcessiveScores;
    }

	public Set updateAssignmentGradesAndComments(Assignment assignment, Collection gradeRecords, Collection comments) throws StaleObjectModificationException {
		//Set studentsWithExcessiveScores = updateAssignmentGradeRecords(assignment, gradeRecords);
		Gradebook gradebook = getGradebook(assignment.getGradebook().getId());
		Set studentsWithExcessiveScores = updateAssignmentGradeRecords(assignment, gradeRecords, gradebook.getGrade_type());
		
		updateComments(comments);
		
		return studentsWithExcessiveScores;
	}
	
	public void updateComments(final Collection comments) throws StaleObjectModificationException {
        final Date now = new Date();
        final String graderId = authn.getUserUid();

        // Unlike the complex grade update logic, this method assumes that
		// the client has done the work of filtering out any unchanged records
		// and isn't interested in throwing an optimistic locking exception for untouched records
		// which were changed by other sessions.
		HibernateCallback hc = new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				for (Iterator iter = comments.iterator(); iter.hasNext();) {
					Comment comment = (Comment)iter.next();
					comment.setGraderId(graderId);
					comment.setDateRecorded(now);
					session.saveOrUpdate(comment);
				}
				return null;
			}
		};
		try {
			getHibernateTemplate().execute(hc);
		} catch (DataIntegrityViolationException e) {
			// If a student hasn't yet received a comment for this
			// assignment, and two graders try to save a new comment record at the
			// same time, the database should report a unique constraint violation.
			// Since that's similar to the conflict between two graders who
			// are trying to update an existing comment record at the same
			// same time, this method translates the exception into an
			// optimistic locking failure.
			if(log.isInfoEnabled()) log.info("An optimistic locking failure occurred while attempting to update comments");
			throw new StaleObjectModificationException(e);
		}
	}

	/**
     */
    public void updateCourseGradeRecords(final CourseGrade courseGrade, final Collection gradeRecordsFromCall)
            throws StaleObjectModificationException {

        if(gradeRecordsFromCall.size() == 0) {
            log.debug("updateCourseGradeRecords called with zero grade records to update");
            return;
        }
        
        if (logData.isDebugEnabled()) logData.debug("BEGIN: Update " + gradeRecordsFromCall.size() + " course grades for gradebook=" + courseGrade.getGradebook().getUid());

        HibernateCallback hc = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                for(Iterator iter = gradeRecordsFromCall.iterator(); iter.hasNext();) {
                    session.evict(iter.next());
                }

                Date now = new Date();
                String graderId = authn.getUserUid();
                int numberOfUpdatedGrades = 0;

                for(Iterator iter = gradeRecordsFromCall.iterator(); iter.hasNext();) {
                    // The modified course grade record
                    CourseGradeRecord gradeRecordFromCall = (CourseGradeRecord)iter.next();
                    gradeRecordFromCall.setGraderId(graderId);
                    gradeRecordFromCall.setDateRecorded(now);
                    try {
                        session.saveOrUpdate(gradeRecordFromCall);
                        session.flush();
                    } catch (StaleObjectStateException sose) {
                        if(log.isInfoEnabled()) log.info("An optimistic locking failure occurred while attempting to update course grade records");
                        throw new StaleObjectModificationException(sose);
                    }

                    // Log the grading event
                    session.save(new GradingEvent(courseGrade, graderId, gradeRecordFromCall.getStudentId(), gradeRecordFromCall.getEnteredGrade()));
                    
                    numberOfUpdatedGrades++;
                }
                if (logData.isDebugEnabled()) logData.debug("Changed " + numberOfUpdatedGrades + " course grades for gradebook=" + courseGrade.getGradebook().getUid());
                return null;
            }
        };
        try {
	        getHibernateTemplate().execute(hc);
	        if (logData.isDebugEnabled()) logData.debug("END: Update " + gradeRecordsFromCall.size() + " course grades for gradebook=" + courseGrade.getGradebook().getUid());
		} catch (DataIntegrityViolationException e) {
			// It's possible that a previously ungraded student
			// was graded behind the current user's back before
			// the user saved the new grade. This translates
			// that case into an optimistic locking failure.
			if(log.isInfoEnabled()) log.info("An optimistic locking failure occurred while attempting to update course grade records");
			throw new StaleObjectModificationException(e);
		}
    }

    public boolean isEnteredAssignmentScores(final Long assignmentId) {
        HibernateCallback hc = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                Integer total = (Integer)session.createQuery(
                        "select count(agr) from AssignmentGradeRecord as agr where agr.gradableObject.id=? and agr.pointsEarned is not null").
                        setLong(0, assignmentId.longValue()).
                        uniqueResult();
                if (log.isInfoEnabled()) log.info("assignment " + assignmentId + " has " + total + " entered scores");
                return total;
            }
        };
        return ((Integer)getHibernateTemplate().execute(hc)).intValue() > 0;
    }

    /**
     */
    public List getStudentGradeRecords(final Long gradebookId, final String studentId) {
        HibernateCallback hc = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                return session.createQuery(
                        "from AssignmentGradeRecord as agr where agr.studentId=? and agr.gradableObject.removed=false and agr.gradableObject.gradebook.id=?").
                        setString(0, studentId).
                        setLong(1, gradebookId.longValue()).
                        list();
            }
        };
        return (List)getHibernateTemplate().execute(hc);
    }
    
    public List getStudentGradeRecordsConverted(final Long gradebookId, final String studentId) {
    	List studentGradeRecsFromDB = getStudentGradeRecords(gradebookId, studentId);
    	Gradebook gradebook = getGradebook(gradebookId);
    	if(gradebook.getGrade_type() == GradebookService.GRADE_TYPE_POINTS)
    		return studentGradeRecsFromDB;
    	else if(gradebook.getGrade_type() == GradebookService.GRADE_TYPE_PERCENTAGE)
    	{
    		return convertPointsToPercentage(gradebook, studentGradeRecsFromDB);
    	}
    	else if(gradebook.getGrade_type() == GradebookService.GRADE_TYPE_LETTER)
    	{
    		return convertPointsToLetterGrade(gradebook, studentGradeRecsFromDB);
    	}
    	
    	return null;
    }
    
    private double getTotalPointsEarnedInternal(final Long gradebookId, final String studentId, final Session session) {
        double totalPointsEarned = 0;
        Iterator scoresIter = session.createQuery(
        		"select agr.pointsEarned from AssignmentGradeRecord agr, Assignment asn where agr.gradableObject=asn and agr.studentId=:student and asn.gradebook.id=:gbid and asn.removed=false and asn.notCounted=false and asn.ungraded=false and asn.pointsPossible > 0").
        		setParameter("student", studentId).
        		setParameter("gbid", gradebookId).
        		list().iterator();
       	while (scoresIter.hasNext()) {
       		Double pointsEarned = (Double)scoresIter.next();
       		if (pointsEarned != null) {
       			totalPointsEarned += pointsEarned.doubleValue();
       		}
       	}
       	if (log.isDebugEnabled()) log.debug("getTotalPointsEarnedInternal for studentId=" + studentId + " returning " + totalPointsEarned);
       	return totalPointsEarned;
    }

    private List getTotalPointsEarnedInternal(final Long gradebookId, final String studentId, final Session session, final Gradebook gradebook, final List categories) 
    {
    	double totalPointsEarned = 0;
    	double literalTotalPointsEarned = 0;
    	Iterator scoresIter = session.createQuery(
    			"select agr.pointsEarned, asn from AssignmentGradeRecord agr, Assignment asn where agr.gradableObject=asn and agr.studentId=:student and asn.gradebook.id=:gbid and asn.removed=false and asn.pointsPossible > 0").
    			setParameter("student", studentId).
    			setParameter("gbid", gradebookId).
    			list().iterator();

    	List assgnsList = session.createQuery(
    	"from Assignment as asn where asn.gradebook.id=:gbid and asn.removed=false and asn.notCounted=false and asn.ungraded=false and asn.pointsPossible > 0").
    	setParameter("gbid", gradebookId).
    	list();

    	Map cateScoreMap = new HashMap();
    	Map cateTotalScoreMap = new HashMap();

    	Set assignmentsTaken = new HashSet();
    	while (scoresIter.hasNext()) {
    		Object[] returned = (Object[])scoresIter.next();
    		Double pointsEarned = (Double)returned[0];
    		Assignment go = (Assignment) returned[1];
    		if (go.isCounted() && pointsEarned != null) {
    			if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_NO_CATEGORY)
    			{
    				totalPointsEarned += pointsEarned.doubleValue();
    				literalTotalPointsEarned += pointsEarned.doubleValue();
    				assignmentsTaken.add(go.getId());
    			}
    			else if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_ONLY_CATEGORY && go != null)
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

    //for testing
    public double getTotalPointsEarnedInternal(final Long gradebookId, final String studentId, final Gradebook gradebook, final List categories) 
    {
    	HibernateCallback hc = new HibernateCallback() {
    		public Object doInHibernate(Session session) throws HibernateException {
    			double totalPointsEarned = 0;
    			Iterator scoresIter = session.createQuery(
    			"select agr.pointsEarned, asn from AssignmentGradeRecord agr, Assignment asn where agr.gradableObject=asn and agr.studentId=:student and asn.gradebook.id=:gbid and asn.removed=false and asn.notCounted=false and asn.ungraded=false and asn.pointsPossible > 0").
    			setParameter("student", studentId).
    			setParameter("gbid", gradebookId).
    			list().iterator();

    			List assgnsList = session.createQuery(
    			"from Assignment as asn where asn.gradebook.id=:gbid and asn.removed=false and asn.notCounted=false and asn.ungraded=false and asn.pointsPossible > 0").
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
        				assignmentsTaken.add(go.getId());
    					}
    					else if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_ONLY_CATEGORY && go != null)
    					{
    						totalPointsEarned += pointsEarned.doubleValue();
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
    			return totalPointsEarned;
    		}
    	};
    	return (Double)getHibernateTemplate().execute(hc);
    }

    public CourseGradeRecord getStudentCourseGradeRecord(final Gradebook gradebook, final String studentId) {
    	if (logData.isDebugEnabled()) logData.debug("About to read student course grade for gradebook=" + gradebook.getUid());
    	return (CourseGradeRecord)getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                CourseGradeRecord courseGradeRecord = getCourseGradeRecord(gradebook, studentId, session);
                if (courseGradeRecord == null) {
                	courseGradeRecord = new CourseGradeRecord(getCourseGrade(gradebook.getId()), studentId);
                }
                
                // Only take the hit of autocalculating the course grade if no explicit
                // grade has been entered.
                if (courseGradeRecord.getEnteredGrade() == null) {
                    // TODO We could easily get everything we need in a single query by using an outer join if we
                    // weren't mapping the different classes together into single sparsely populated
                    // tables. When we finally break up the current mungings of Assignment with CourseGrade
                    // and AssignmentGradeRecord with CourseGradeRecord, redo this section.
                	List cates = getCategories(gradebook.getId());
                	//double totalPointsPossible = getTotalPointsInternal(gradebook.getId(), session);
                	//double totalPointsEarned = getTotalPointsEarnedInternal(gradebook.getId(), studentId, session);
                	double totalPointsPossible = getTotalPointsInternal(gradebook.getId(), session, gradebook, cates, studentId);
                	List totalEarned = getTotalPointsEarnedInternal(gradebook.getId(), studentId, session, gradebook, cates);
                	double totalPointsEarned = ((Double)totalEarned.get(0)).doubleValue();
                	double literalTotalPointsEarned = ((Double)totalEarned.get(1)).doubleValue();
                	courseGradeRecord.initNonpersistentFields(totalPointsPossible, totalPointsEarned, literalTotalPointsEarned);
                }             
                return courseGradeRecord;
            }
        });
    }

    public GradingEvents getGradingEvents(final GradableObject gradableObject, final Collection studentIds) {

        // Don't attempt to run the query if there are no enrollments
        if(studentIds == null || studentIds.size() == 0) {
            log.debug("No enrollments were specified.  Returning an empty GradingEvents object");
            return new GradingEvents();
        }

        HibernateCallback hc = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                List eventsList;
                if (studentIds.size() <= MAX_NUMBER_OF_SQL_PARAMETERS_IN_LIST) {
                    Query q = session.createQuery("from GradingEvent as ge where ge.gradableObject=:go and ge.studentId in (:students)");
                    q.setParameter("go", gradableObject, Hibernate.entity(GradableObject.class));
                    q.setParameterList("students", studentIds);
                    eventsList = q.list();
                } else {
                    Query q = session.createQuery("from GradingEvent as ge where ge.gradableObject=:go");
                    q.setParameter("go", gradableObject, Hibernate.entity(GradableObject.class));
                    eventsList = new ArrayList();
                    for (Iterator iter = q.list().iterator(); iter.hasNext(); ) {
                        GradingEvent event = (GradingEvent)iter.next();
                        if (studentIds.contains(event.getStudentId())) {
                            eventsList.add(event);
                        }
                    }
                }
                return eventsList;
            }
        };

        List list = (List)getHibernateTemplate().execute(hc);

        GradingEvents events = new GradingEvents();

        for(Iterator iter = list.iterator(); iter.hasNext();) {
            GradingEvent event = (GradingEvent)iter.next();
            events.addEvent(event);
        }
        return events;
    }


    /**
     */
    public List getAssignments(final Long gradebookId, final String sortBy, final boolean ascending) {
        return (List)getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                List assignments = getAssignments(gradebookId, session);
                
                /** synchronize from external application*/
                if (synchronizer != null)
                {
                	synchronizer.synchrornizeAssignments(assignments);

                    assignments = getAssignments(gradebookId, session);
                }
                /** end synchronize from external application*/

                sortAssignments(assignments, sortBy, ascending);
                return assignments;
            }
        });
    }

    /**
     */
    public List getAssignmentsWithStats(final Long gradebookId, final String sortBy, final boolean ascending) {
        Set studentUids = getAllStudentUids(getGradebookUid(gradebookId));
        List assignments = getAssignments(gradebookId);
        List<AssignmentGradeRecord> gradeRecords = getAllAssignmentGradeRecords(gradebookId, studentUids);
        for (Iterator iter = assignments.iterator(); iter.hasNext(); ) {
        	Assignment assignment = (Assignment)iter.next();
        	assignment.calculateStatistics(gradeRecords);
        }
        sortAssignments(assignments, sortBy, ascending);
        return assignments;
    }

    public List getAssignmentsAndCourseGradeWithStats(final Long gradebookId, final String sortBy, final boolean ascending) {
        Set studentUids = getAllStudentUids(getGradebookUid(gradebookId));
        List assignments = getAssignments(gradebookId);
        CourseGrade courseGrade = getCourseGrade(gradebookId);
        Map gradeRecordMap = new HashMap();
        List<AssignmentGradeRecord> gradeRecords = getAllAssignmentGradeRecords(gradebookId, studentUids);
        addToGradeRecordMap(gradeRecordMap, gradeRecords);
        
        for (Iterator iter = assignments.iterator(); iter.hasNext(); ) {
        	Assignment assignment = (Assignment)iter.next();
        	assignment.calculateStatistics(gradeRecords);
        }
        
        List<CourseGradeRecord> courseGradeRecords = getPointsEarnedCourseGradeRecords(courseGrade, studentUids, assignments, gradeRecordMap);
        courseGrade.calculateStatistics(courseGradeRecords, studentUids.size());
        
        sortAssignments(assignments, sortBy, ascending);
        
        // Always put the Course Grade at the end.
        assignments.add(courseGrade);

        return assignments;
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

    /**
     * TODO Remove this method in favor of doing database sorting.
     *
     * @param assignments
     * @param sortBy
     * @param ascending
     */
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

    /**
     */
    public List getAssignments(Long gradebookId) {
        return getAssignments(gradebookId, Assignment.DEFAULT_SORT, true);
    }

    /**
     */
    public Assignment getAssignment(Long assignmentId) {
        return (Assignment)getHibernateTemplate().load(Assignment.class, assignmentId);
    }

    /**
     */
    public Assignment getAssignmentWithStats(Long assignmentId) {
    	Assignment assignment = getAssignment(assignmentId);
    	Long gradebookId = assignment.getGradebook().getId();
        Set studentUids = getAllStudentUids(getGradebookUid(gradebookId));
        List<AssignmentGradeRecord> gradeRecords = getAssignmentGradeRecords(assignment, studentUids);
        assignment.calculateStatistics(gradeRecords);
        return assignment;
    }

    /**
     */
    public void updateAssignment(final Assignment assignment)
        throws ConflictingAssignmentNameException, StaleObjectModificationException {
        HibernateCallback hc = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
            	updateAssignment(assignment, session);
                return null;
            }
        };
        try {
        	/** synchronize from external application*/
        	String oldTitle = null;
        	if(synchronizer != null)
        	{
        		Assignment assign = getAssignment(assignment.getId());
        		oldTitle = assign.getName();
        	}
            getHibernateTemplate().execute(hc);
        	/** synchronize from external application*/
        	if(synchronizer != null && oldTitle != null)
        	{
        		synchronizer.updateAssignment(oldTitle, assignment.getName());
        	}
        } catch (HibernateOptimisticLockingFailureException holfe) {
            if(log.isInfoEnabled()) log.info("An optimistic locking failure occurred while attempting to update an assignment");
            throw new StaleObjectModificationException(holfe);
        }
    }

    /**
     * Gets the total number of points possible in a gradebook.
     */
    public double getTotalPoints(final Long gradebookId) {
    	Double totalPoints = (Double)getHibernateTemplate().execute(new HibernateCallback() {
    		public Object doInHibernate(Session session) throws HibernateException {
    			Gradebook gradebook = getGradebook(gradebookId);
    			List cates = getCategories(gradebookId);
    			return new Double(getLiteralTotalPointsInternal(gradebookId, session, gradebook, cates));
    			//return new Double(getTotalPointsInternal(gradebookId, session));
    		}
    	});
    	return totalPoints.doubleValue();
    }
 
    private double getTotalPointsInternal(Long gradebookId, Session session) {
        double totalPointsPossible = 0;
    	Iterator assignmentPointsIter = session.createQuery(
        		"select asn.pointsPossible from Assignment asn where asn.gradebook.id=:gbid and asn.removed=false and asn.notCounted=false and asn.ungraded=false").
        		setParameter("gbid", gradebookId).
        		list().iterator();
        while (assignmentPointsIter.hasNext()) {
        	Double pointsPossible = (Double)assignmentPointsIter.next();
        	totalPointsPossible += pointsPossible.doubleValue();
        }
        return totalPointsPossible;
    }

    //for testing
    public double getTotalPointsInternal(final Long gradebookId, final Gradebook gradebook, final List categories, final String studentId) 
    {
    	HibernateCallback hc = new HibernateCallback() {
    		public Object doInHibernate(Session session) throws HibernateException {
    			double totalPointsPossible = 0;
    			List assgnsList = session.createQuery(
    			"select asn from Assignment asn where asn.gradebook.id=:gbid and asn.removed=false and asn.notCounted=false and asn.ungraded=false and asn.pointsPossible > 0").
    			setParameter("gbid", gradebookId).
    			list();
    			
    			Iterator scoresIter = session.createQuery(
    			"select agr.pointsEarned, asn from AssignmentGradeRecord agr, Assignment asn where agr.gradableObject=asn and agr.studentId=:student and asn.gradebook.id=:gbid and asn.removed=false and asn.notCounted=false and asn.ungraded=false and asn.pointsPossible > 0").
    			setParameter("student", studentId).
    			setParameter("gbid", gradebookId).
    			list().iterator();

    			Set categoryTaken = new HashSet();
    			Set assignmentsTaken = new HashSet();
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
    	};
    	return (Double)getHibernateTemplate().execute(hc);    	
    }
    
    private double getTotalPointsInternal(final Long gradebookId, Session session, final Gradebook gradebook, final List categories, final String studentId)
    {
    	double totalPointsPossible = 0;
    	List assgnsList = session.createQuery(
    			"select asn from Assignment asn where asn.gradebook.id=:gbid and asn.removed=false and asn.notCounted=false and asn.ungraded=false and asn.pointsPossible > 0").
    			setParameter("gbid", gradebookId).
    			list();

    	Iterator scoresIter = session.createQuery(
    	"select agr.pointsEarned, asn from AssignmentGradeRecord agr, Assignment asn where agr.gradableObject=asn and agr.studentId=:student and asn.gradebook.id=:gbid and asn.removed=false and asn.notCounted=false and asn.ungraded=false and asn.pointsPossible > 0").
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

    //for test
    public double getLiteralTotalPointsInternal(final Long gradebookId, final Gradebook gradebook, final List categories)
    {
    	HibernateCallback hc = new HibernateCallback() {
    		public Object doInHibernate(Session session) throws HibernateException {
    			double totalPointsPossible = 0;
    			Iterator assignmentIter = session.createQuery(
    			"select asn from Assignment asn where asn.gradebook.id=:gbid and asn.removed=false and asn.notCounted=false and asn.ungraded=false").
    			setParameter("gbid", gradebookId).
    			list().iterator();
    			while (assignmentIter.hasNext()) {
    				Assignment asn = (Assignment) assignmentIter.next();
    				if(asn != null)
    				{
    					Double pointsPossible = asn.getPointsPossible();

    					if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_NO_CATEGORY)
    					{
    						totalPointsPossible += pointsPossible.doubleValue();
    					}
    					else if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_ONLY_CATEGORY )
    					{
    						totalPointsPossible += pointsPossible.doubleValue();    						
    					}
    					else if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY && categories != null)
    					{
    						for(int i=0; i<categories.size(); i++)
    						{
    							Category cate = (Category) categories.get(i);
    							if(cate != null && !cate.isRemoved() && asn.getCategory() != null && cate.getId().equals(asn.getCategory().getId()))
    							{
    								totalPointsPossible += pointsPossible.doubleValue();
    								break;
    							}
    						}
    					}
    				}
    			}
    			return totalPointsPossible;
    		}
    	};
    	return (Double)getHibernateTemplate().execute(hc);    	
    }

    private double getLiteralTotalPointsInternal(final Long gradebookId, Session session, final Gradebook gradebook, final List categories)
    {
    	double totalPointsPossible = 0;
    	Iterator assignmentIter = session.createQuery(
    			"select asn from Assignment asn where asn.gradebook.id=:gbid and asn.removed=false and asn.notCounted=false and asn.ungraded=false").
    			setParameter("gbid", gradebookId).
    			list().iterator();
    	while (assignmentIter.hasNext()) {
    		Assignment asn = (Assignment) assignmentIter.next();
    		if(asn != null)
    		{
    			Double pointsPossible = asn.getPointsPossible();

    			if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_NO_CATEGORY)
    			{
    				totalPointsPossible += pointsPossible.doubleValue();
    			}
    			else if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_ONLY_CATEGORY)
 					{
    				totalPointsPossible += pointsPossible.doubleValue();
 					}
    			else if(gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY && categories != null)
    			{
    				for(int i=0; i<categories.size(); i++)
    				{
    					Category cate = (Category) categories.get(i);
    					if(cate != null && !cate.isRemoved() && asn.getCategory() != null && cate.getId().equals(asn.getCategory().getId()))
    					{
    						totalPointsPossible += pointsPossible.doubleValue();
    						break;
    					}
    				}
    			}
    		}
    	}
    	return totalPointsPossible;
    }

    public Gradebook getGradebookWithGradeMappings(final Long id) {
		return (Gradebook)getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) throws HibernateException {
				Gradebook gradebook = (Gradebook)session.load(Gradebook.class, id);
				Hibernate.initialize(gradebook.getGradeMappings());
				return gradebook;
			}
		});
	}


    /**
     *
     * @param spreadsheetId
     * @return
     */
    public Spreadsheet getSpreadsheet(final Long spreadsheetId) {
        return (Spreadsheet)getHibernateTemplate().load(Spreadsheet.class, spreadsheetId);
    }

    /**
     *
     * @param gradebookId
     * @return
     */
    public List getSpreadsheets(final Long gradebookId) {
        return (List)getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                List spreadsheets = getSpreadsheets(gradebookId, session);
                return spreadsheets;
            }
        });
    }

    /**
     *
     * @param spreadsheetId
     */
    public void removeSpreadsheet(final Long spreadsheetId)throws StaleObjectModificationException {

        HibernateCallback hc = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                Spreadsheet spt = (Spreadsheet)session.load(Spreadsheet.class, spreadsheetId);
                session.delete(spt);
                if(log.isInfoEnabled()) log.info("Spreadsheet " + spt.getName() + " has been removed from gradebook" );

                return null;
            }
        };
        getHibernateTemplate().execute(hc);

    }

    /**
     *
     * @param spreadsheet
     */
    public void updateSpreadsheet(final Spreadsheet spreadsheet)throws ConflictingAssignmentNameException, StaleObjectModificationException  {
            HibernateCallback hc = new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException {
                    // Ensure that we don't have the assignment in the session, since
                    // we need to compare the existing one in the db to our edited assignment
                    session.evict(spreadsheet);

                    Spreadsheet sptFromDb = (Spreadsheet)session.load(Spreadsheet.class, spreadsheet.getId());
                    int numNameConflicts = ((Integer)session.createQuery(
                            "select count(spt) from Spreadsheet as spt where spt.name = ? and spt.gradebook = ? and spt.id != ?").
                            setString(0, spreadsheet.getName()).
                            setEntity(1, spreadsheet.getGradebook()).
                            setLong(2, spreadsheet.getId().longValue()).
                            uniqueResult()).intValue();
                    if(numNameConflicts > 0) {
                        throw new ConflictingAssignmentNameException("You can not save multiple spreadsheets in a gradebook with the same name");
                    }

                    session.evict(sptFromDb);
                    session.update(spreadsheet);

                    return null;
                }
            };
            try {
                getHibernateTemplate().execute(hc);
            } catch (HibernateOptimisticLockingFailureException holfe) {
                if(log.isInfoEnabled()) log.info("An optimistic locking failure occurred while attempting to update a spreadsheet");
                throw new StaleObjectModificationException(holfe);
            }
    }


    public Long createSpreadsheet(final Long gradebookId, final String name, final String creator, Date dateCreated, final String content) throws ConflictingSpreadsheetNameException,StaleObjectModificationException {

        HibernateCallback hc = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                Gradebook gb = (Gradebook)session.load(Gradebook.class, gradebookId);
                int numNameConflicts = ((Integer)session.createQuery(
                        "select count(spt) from Spreadsheet as spt where spt.name = ? and spt.gradebook = ? ").
                        setString(0, name).
                        setEntity(1, gb).
                        uniqueResult()).intValue();
                if(numNameConflicts > 0) {
                    throw new ConflictingSpreadsheetNameException("You can not save multiple spreadsheets in a gradebook with the same name");
                }

                Spreadsheet spt = new Spreadsheet();
                spt.setGradebook(gb);
                spt.setName(name);
                spt.setCreator(creator);
                spt.setDateCreated(new Date());
                spt.setContent(content);

                // Save the new assignment
                Long id = (Long)session.save(spt);
                return id;
            }
        };

        return (Long)getHibernateTemplate().execute(hc);

    }

    protected List getSpreadsheets(Long gradebookId, Session session) throws HibernateException {
        List spreadsheets = session.createQuery(
                "from Spreadsheet as spt where spt.gradebook.id=? ").
                setLong(0, gradebookId.longValue()).
                list();
        return spreadsheets;
    }

    public List getComments(final Assignment assignment, final Collection studentIds) {
    	if (studentIds.isEmpty()) {
    		return new ArrayList();
    	}
        return (List)getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
            	List comments;
            	if (studentIds.size() <= MAX_NUMBER_OF_SQL_PARAMETERS_IN_LIST) {
            		Query q = session.createQuery(
            			"from Comment as c where c.gradableObject=:go and c.studentId in (:studentIds)");
                    q.setParameter("go", assignment);
                    q.setParameterList("studentIds", studentIds);
                    comments = q.list();
            	} else {
            		comments = new ArrayList();
            		Query q = session.createQuery("from Comment as c where c.gradableObject=:go");
            		q.setParameter("go", assignment);
            		List allComments = q.list();
            		for (Iterator iter = allComments.iterator(); iter.hasNext(); ) {
            			Comment comment = (Comment)iter.next();
            			if (studentIds.contains(comment.getStudentId())) {
            				comments.add(comment);
            			}
            		}
            	}
                return comments;
            }
        });
    }


    public List getStudentAssignmentComments(final String studentId, final Long gradebookId) {
        return (List)getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                List comments;
                comments = new ArrayList();
                Query q = session.createQuery("from Comment as c where c.studentId=:studentId and c.gradableObject.gradebook.id=:gradebookId");
                q.setParameter("studentId", studentId);
                q.setParameter("gradebookId",gradebookId);
                List allComments = q.list();
                for (Iterator iter = allComments.iterator(); iter.hasNext(); ) {
                    Comment comment = (Comment)iter.next();
                    comments.add(comment);
                }
                return comments;
            }
        });
    }
    
    public boolean validateCategoryWeighting(Long gradebookId)
    {
    	Gradebook gradebook = getGradebook(gradebookId);
    	if(gradebook.getCategory_type() != GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY)
    		return true;
    	List cats = getCategories(gradebookId);
    	double weight = 0.0;
    	for(int i=0; i<cats.size(); i++)
    	{
    		Category cat = (Category) cats.get(i);
    		if(cat != null)
    		{
    			weight += cat.getWeight().doubleValue();
    		}
    	}
    	if(Math.rint(weight) == 1)
    		return true;
    	else
    		return false;
    }
    
    public Set updateAssignmentGradeRecords(Assignment assignment, Collection gradeRecords, int grade_type)
    {
    	if(grade_type == GradebookService.GRADE_TYPE_POINTS)
    		return updateAssignmentGradeRecords(assignment, gradeRecords);
    	else if(grade_type == GradebookService.GRADE_TYPE_PERCENTAGE)
    	{
    		Collection convertList = new ArrayList();
    		for(Iterator iter = gradeRecords.iterator(); iter.hasNext();) 
    		{
    			AssignmentGradeRecord agr = (AssignmentGradeRecord) iter.next();
    			Double doubleValue = calculateDoublePointForRecord(agr);
    			if(agr != null && doubleValue != null)
    			{
    				agr.setPointsEarned(doubleValue);
    				convertList.add(agr);
    			}
    			else if(agr != null)
    			{
    				agr.setPointsEarned(null);
    				convertList.add(agr);
    			}
    		}
    		return updateAssignmentGradeRecords(assignment, convertList);
    	}
    	else if(grade_type == GradebookService.GRADE_TYPE_LETTER)
    	{
    		Collection convertList = new ArrayList();
    		for(Iterator iter = gradeRecords.iterator(); iter.hasNext();) 
    		{
    			AssignmentGradeRecord agr = (AssignmentGradeRecord) iter.next();
    			Double doubleValue = calculateDoublePointForLetterGradeRecord(agr);
    			if(agr != null && doubleValue != null)
    			{
        		agr.setPointsEarned(doubleValue);
        		convertList.add(agr);
        	}
        	else if(agr != null)
        	{
        		agr.setPointsEarned(null);
        		convertList.add(agr);
        	}
        }
        return updateAssignmentGradeRecords(assignment, convertList);
    	}

    	else
    		return null;
    }
    
    /**
     * Updates student grade records based upon the grade entry type -
     * grade will be converted appropriately before update
     * 
     * @param studentUid
     * @param gradeRecords
     * @param grade_type
     * @return
     */
    public Set updateStudentGradeRecords(Collection gradeRecords, int grade_type)
    {
    	if(grade_type == GradebookService.GRADE_TYPE_POINTS)
    		return updateStudentGradeRecords(gradeRecords);
    	else if(grade_type == GradebookService.GRADE_TYPE_PERCENTAGE)
    	{
    		Collection convertList = new ArrayList();
    		for(Iterator iter = gradeRecords.iterator(); iter.hasNext();) 
    		{
    			AssignmentGradeRecord agr = (AssignmentGradeRecord) iter.next();
    			Double doubleValue = calculateDoublePointForRecord(agr);
    			if(agr != null && doubleValue != null)
    			{
    				agr.setPointsEarned(doubleValue);
    				convertList.add(agr);
    			}
    			else if(agr != null)
    			{
    				agr.setPointsEarned(null);
    				convertList.add(agr);
    			}
    		}
    		return updateStudentGradeRecords(convertList);
    	}
    	else if(grade_type == GradebookService.GRADE_TYPE_LETTER)
    	{
    		Collection convertList = new ArrayList();
    		for(Iterator iter = gradeRecords.iterator(); iter.hasNext();) 
    		{
    			AssignmentGradeRecord agr = (AssignmentGradeRecord) iter.next();
    			Double doubleValue = calculateDoublePointForLetterGrade(agr);
    			if(agr != null && doubleValue != null)
    			{
    				agr.setPointsEarned(doubleValue);
    				convertList.add(agr);
    			}
    			else if(agr != null)
    			{
    				agr.setPointsEarned(null);
    				convertList.add(agr);
    			}
    		}
    		return updateStudentGradeRecords(convertList);
    	}
    	else
    		return null;
    }

    private Double calculateDoublePointForRecord(AssignmentGradeRecord gradeRecordFromCall)
    {
    	Assignment assign = getAssignment(gradeRecordFromCall.getAssignment().getId()); 
    	Gradebook gradebook = getGradebook(assign.getGradebook().getId());
    	if(gradeRecordFromCall.getPercentEarned() != null)
    	{
    		if(gradeRecordFromCall.getPercentEarned().doubleValue() / 100.0 < 0)
    		{
    			throw new IllegalArgumentException("percent for record is less than 0 for percentage points in GradebookManagerHibernateImpl.calculateDoublePointForRecord");
    		}
    		return new Double(assign.getPointsPossible().doubleValue() * (gradeRecordFromCall.getPercentEarned().doubleValue() / 100.0));
    	}
    	else
    		return null;
    }
    
    private Double calculateDoublePointForLetterGradeRecord(AssignmentGradeRecord gradeRecordFromCall)
    {
    	Assignment assign = getAssignment(gradeRecordFromCall.getAssignment().getId()); 
    	Gradebook gradebook = getGradebook(assign.getGradebook().getId());
    	if(gradeRecordFromCall.getLetterEarned() != null)
    	{
    		LetterGradePercentMapping lgpm = getLetterGradePercentMapping(gradebook);
    		if(lgpm != null && lgpm.getGradeMap() != null)
    		{
    			Double doublePercentage = lgpm.getValue(gradeRecordFromCall.getLetterEarned());
    			if(doublePercentage == null)
    			{
    				log.error("percentage for " + gradeRecordFromCall.getLetterEarned() + " is not found in letter grade mapping in GradebookManagerHibernateImpl.calculateDoublePointForLetterGradeRecord");
    				return null;
    			}
    			return new Double(assign.getPointsPossible().doubleValue() * (doublePercentage.doubleValue()/100));
    		}
    		return null;
    	}
    	else
    		return null;
    }

    private Double calculateDoublePointForLetterGrade(AssignmentGradeRecord gradeRecordFromCall)
    {
    	Assignment assign = getAssignment(gradeRecordFromCall.getAssignment().getId()); 
    	Gradebook gradebook = getGradebook(assign.getGradebook().getId());
    	if(gradeRecordFromCall.getLetterEarned() != null)
    	{
    		LetterGradePercentMapping lgpm = getLetterGradePercentMapping(assign.getGradebook());
    		if(lgpm != null && lgpm.getGradeMap() != null)
    		{
    			Double doublePercentage = lgpm.getValue(gradeRecordFromCall.getLetterEarned());
    			if(doublePercentage == null)
    			{
    				log.error("percentage for " + gradeRecordFromCall.getLetterEarned() + " is not found in letter grade mapping in GradebookManagerHibernateImpl.calculateDoublePointForLetterGrade");
    				return null;
    			}
    			return new Double(assign.getPointsPossible().doubleValue() * (doublePercentage.doubleValue()/100));
    		}
    		return null;
    	}
    	else
    		return null;
    }
    
    public List getAssignmentGradeRecordsConverted(Assignment assignment, Collection studentUids)
    {
    	List assignRecordsFromDB = getAssignmentGradeRecords(assignment, studentUids);
    	Gradebook gradebook = getGradebook(assignment.getGradebook().getId());
    	if(gradebook.getGrade_type() == GradebookService.GRADE_TYPE_POINTS)
    		return assignRecordsFromDB;
    	else if(gradebook.getGrade_type() == GradebookService.GRADE_TYPE_PERCENTAGE)
    	{
    		return convertPointsToPercentage(assignment, gradebook, assignRecordsFromDB);
    	}
    	else if(gradebook.getGrade_type() == GradebookService.GRADE_TYPE_LETTER)
    	{
    		return convertPointsToLetterGrade(assignment, gradebook, assignRecordsFromDB);
    	}    	
    	return null;
    }

    private List convertPointsToPercentage(Assignment assignment, Gradebook gradebook, List assignRecordsFromDB)
    {
    	double pointPossible = assignment.getPointsPossible().doubleValue();
    	List percentageList = new ArrayList();
    	if(pointPossible > 0)
    	{

    		for(int i=0; i<assignRecordsFromDB.size(); i++)
    		{
    			AssignmentGradeRecord agr = (AssignmentGradeRecord) assignRecordsFromDB.get(i);
    			agr.setDateRecorded(agr.getDateRecorded());
    			agr.setGraderId(agr.getGraderId());
    			if(agr != null && agr.getPointsEarned() != null)
    			{
    				agr.setPercentEarned(new Double((agr.getPointsEarned().doubleValue() * 100.0)/pointPossible));
    				percentageList.add(agr);
    			}
    			else if(agr != null)
    			{
    				agr.setPercentEarned(null);
    				percentageList.add(agr);
    			}
    		}
    	}
    	return percentageList;
    }
    
    private List convertPointsToLetterGrade(Assignment assignment, Gradebook gradebook, List assignRecordsFromDB)
    {
    	double pointPossible = assignment.getPointsPossible().doubleValue();
    	if(pointPossible > 0)
    	{
    		List letterGradeList = new ArrayList();
    		LetterGradePercentMapping lgpm = getLetterGradePercentMapping(assignment.getGradebook());
    		for(int i=0; i<assignRecordsFromDB.size(); i++)
    		{
    			AssignmentGradeRecord agr = (AssignmentGradeRecord) assignRecordsFromDB.get(i);
      		agr.setDateRecorded(agr.getDateRecorded());
      		agr.setGraderId(agr.getGraderId());
      		if(agr != null && agr.getPointsEarned() != null )
      		{
        		String letterGrade = lgpm.getGrade(
        				new Double (agr.getPointsEarned().doubleValue() / agr.getAssignment().getPointsPossible().doubleValue() * 100));
      			agr.setLetterEarned(letterGrade);
      			letterGradeList.add(agr);
      		}
      		else if(agr != null)
      		{
      			agr.setLetterEarned(null);
      			letterGradeList.add(agr);
      		}
    		}
    		return letterGradeList;
    	}
    	return null;
    }

    
    /**
     * Converts points to percentage for all assignments for a single student
     * @param gradebook
     * @param studentRecordsFromDB
     * @return
     */
    private List convertPointsToPercentage(Gradebook gradebook, List studentRecordsFromDB)
    {
    	List percentageList = new ArrayList();
    	for(int i=0; i < studentRecordsFromDB.size(); i++)
    	{
    		AssignmentGradeRecord agr = (AssignmentGradeRecord) studentRecordsFromDB.get(i);
    		if(agr != null && agr.getPointsEarned() != null)
    		{
    			AssignmentGradeRecord newAgr = agr.clone();
    			double pointsPossible = agr.getAssignment().getPointsPossible().doubleValue();
    			newAgr.setDateRecorded(agr.getDateRecorded());
    			newAgr.setGraderId(agr.getGraderId());
    			newAgr.setPercentEarned(new Double((agr.getPointsEarned().doubleValue() * 100.0)/pointsPossible));
    			percentageList.add(newAgr);
    		}
    		else if(agr != null)
    		{
    			AssignmentGradeRecord newAgr = agr.clone();
    			newAgr.setPercentEarned(null);
    			percentageList.add(newAgr);
    		}
    	}
    	return percentageList;
    }
    
    /**
     * Converts points to letter grade for all assignments for a single student
     * @param gradebook
     * @param studentRecordsFromDB
     * @return
     */
    private List convertPointsToLetterGrade(Gradebook gradebook, List studentRecordsFromDB)
    {
    	List letterGradeList = new ArrayList();
    	LetterGradePercentMapping lgpm = getLetterGradePercentMapping(gradebook);
    	for(int i=0; i < studentRecordsFromDB.size(); i++)
    	{
    		AssignmentGradeRecord agr = (AssignmentGradeRecord) studentRecordsFromDB.get(i);
    		double pointsPossible = agr.getAssignment().getPointsPossible().doubleValue();
    		agr.setDateRecorded(agr.getDateRecorded());
    		agr.setGraderId(agr.getGraderId());
    		if(agr != null && agr.getPointsEarned() != null )
    		{
      		String letterGrade = lgpm.getGrade(
      				new Double (agr.getPointsEarned().doubleValue() / agr.getAssignment().getPointsPossible().doubleValue() * 100) );
    			agr.setLetterEarned(letterGrade);
    			letterGradeList.add(agr);
    		}
    		else if(agr != null)
    		{
    			agr.setLetterEarned(null);
    			letterGradeList.add(agr);
    		}
    	}
    	return letterGradeList;
    }
    
    public List getCategoriesWithStats(Long gradebookId, String assignmentSort, boolean assignAscending, String categorySort, boolean categoryAscending) {
    	List categories = getCategories(gradebookId);
    	Set allStudentUids = getAllStudentUids(getGradebookUid(gradebookId));
    	List allAssignments;
    	if(assignmentSort != null)
    		allAssignments = getAssignmentsWithStats(gradebookId, assignmentSort, assignAscending);
    	else
    		allAssignments = getAssignmentsWithStats(gradebookId, Assignment.DEFAULT_SORT, assignAscending);
    	
//    	List releasedAssignments = new ArrayList();
    	List gradeRecords = getAllAssignmentGradeRecords(gradebookId, allStudentUids);
    	Map cateMap = new HashMap();
    	for (Iterator iter = allAssignments.iterator(); iter.hasNext(); )
    	{
    		Assignment assign = (Assignment) iter.next();
    		if(assign != null)
    		{
//    			if(assign.isReleased())
//    			{
    				assign.calculateStatistics(gradeRecords);
//    				releasedAssignments.add(assign);
//    			}
    			if(assign.getCategory() != null && cateMap.get(assign.getCategory().getId()) == null)
    			{
    				List assignList = new ArrayList();
    				assignList.add(assign);
    				cateMap.put(assign.getCategory().getId(), assignList);
    			}
    			else
    			{
    				if(assign.getCategory() != null)
    				{
    					List assignList = (List) cateMap.get(assign.getCategory().getId());
    					assignList.add(assign);
    					cateMap.put(assign.getCategory().getId(),assignList);
    				}
    			}
    		}
    	}
    	
  		for (Iterator iter = categories.iterator(); iter.hasNext(); )
    	{
    		Category cate = (Category) iter.next();
    		if(cate != null && cateMap.get(cate.getId()) != null)
    		{
    			cate.calculateStatistics((List) cateMap.get(cate.getId()));
    			cate.setAssignmentList((List)cateMap.get(cate.getId()));
    		}
    	}
  		
  		if(categorySort != null)
  			sortCategories(categories, categorySort, categoryAscending);
  		else
  			sortCategories(categories, Category.SORT_BY_NAME, categoryAscending);
  			
      Set studentUids = getAllStudentUids(getGradebookUid(gradebookId));
      CourseGrade courseGrade = getCourseGrade(gradebookId);
      Map gradeRecordMap = new HashMap();
      addToGradeRecordMap(gradeRecordMap, gradeRecords);
//      List<CourseGradeRecord> courseGradeRecords = getPointsEarnedCourseGradeRecords(courseGrade, studentUids, releasedAssignments, gradeRecordMap);
      List<CourseGradeRecord> courseGradeRecords = getPointsEarnedCourseGradeRecords(courseGrade, studentUids, allAssignments, gradeRecordMap);
      courseGrade.calculateStatistics(courseGradeRecords, studentUids.size());

      categories.add(courseGrade);
  		
  		return categories;
    }

    private void sortCategories(List categories, String sortBy, boolean ascending) 
    {
    	Comparator comp;
    	if(Category.SORT_BY_NAME.equals(sortBy)) 
    	{
    		comp = Category.nameComparator;
    	}
    	else if(Category.SORT_BY_AVERAGE_SCORE.equals(sortBy))
    	{
    		comp = Category.averageScoreComparator;
    	}
    	else if(Category.SORT_BY_WEIGHT.equals(sortBy))
    	{
    		comp = Category.weightComparator;
    	}
    	else
    	{
    		comp = Category.nameComparator;
    	}
    	Collections.sort(categories, comp);
    	if(!ascending) 
    	{
    		Collections.reverse(categories);
    	}
    }

    public List getAssignmentsWithNoCategory(final Long gradebookId, String assignmentSort, boolean assignAscending)
    {
    	HibernateCallback hc = new HibernateCallback() {
    		public Object doInHibernate(Session session) throws HibernateException {
    			List assignments = session.createQuery(
    					"from Assignment as asn where asn.gradebook.id=? and asn.removed=false and asn.category is null").
    					setLong(0, gradebookId.longValue()).
    					list();
    			return assignments;
    		}
    	};
    	
    	List assignList = (List)getHibernateTemplate().execute(hc);
    	if(assignmentSort != null)
    		sortAssignments(assignList, assignmentSort, assignAscending);
    	else
    		sortAssignments(assignList, Assignment.DEFAULT_SORT, assignAscending);
    	
    	return assignList;
    }

    public List getAssignmentsWithNoCategoryWithStats(Long gradebookId, String assignmentSort, boolean assignAscending)
    {
    	Set studentUids = getAllStudentUids(getGradebookUid(gradebookId));
    	List assignments = getAssignmentsWithNoCategory(gradebookId, assignmentSort, assignAscending);
    	List<AssignmentGradeRecord> gradeRecords = getAllAssignmentGradeRecords(gradebookId, studentUids);
    	for (Iterator iter = assignments.iterator(); iter.hasNext(); ) {
    		Assignment assignment = (Assignment)iter.next();
    		assignment.calculateStatistics(gradeRecords);
    	}

    	return assignments;
    }

    public void convertGradingEventsConverted(Assignment assign, GradingEvents events, List studentUids, int grade_type)
    {
    	LetterGradePercentMapping lgpm = new LetterGradePercentMapping();
    	if (grade_type == GradebookService.GRADE_TYPE_LETTER) {
    		lgpm = getLetterGradePercentMapping(assign.getGradebook());
    	}
    	
    	for(Iterator iter = studentUids.iterator(); iter.hasNext();)
    	{
    		List gradingEvents = events.getEvents((String)iter.next());
    		for(Iterator eventIter = gradingEvents.iterator(); eventIter.hasNext();)
    		{
    			GradingEvent ge = (GradingEvent) eventIter.next();
    			if (ge.getGrade() != null) {
	    			if(grade_type == GradebookService.GRADE_TYPE_PERCENTAGE)
	    			{
	    				ge.setGrade(new Double((new Double(ge.getGrade()).doubleValue()  * 100.0) / assign.getPointsPossible().doubleValue()).toString());
	    			} else if(grade_type == GradebookService.GRADE_TYPE_LETTER) {
	    				String letterGrade = null;
	    				if (lgpm != null) {
	    					letterGrade = lgpm.getGrade(
	    	      				new Double (new Double(ge.getGrade()).doubleValue() / assign.getPointsPossible().doubleValue() * 100));
	    				}
	    				ge.setGrade(letterGrade);	
	    			}
    			}
    		}
    	}
    }
    
    public boolean checkStuendsNotSubmitted(Gradebook gradebook)
    {
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
    
    public void fillInZeroForNullGradeRecords(Gradebook gradebook)
    {
    	Set studentUids = getAllStudentUids(getGradebookUid(gradebook.getId()));
    	List assigns = getAssignments(gradebook.getId(), Assignment.DEFAULT_SORT, true);
    	for(Iterator iter = assigns.iterator(); iter.hasNext();)
    	{
    		Assignment assignment = (Assignment)iter.next();
    		if(assignment.isCounted() && !assignment.isUngraded())
    		{
    			List records = getAssignmentGradeRecords(assignment, studentUids);
    			Map recordsMap = new HashMap();
    			for(Iterator recordsIter = records.iterator(); recordsIter.hasNext();)
    			{
    				AssignmentGradeRecord agr = (AssignmentGradeRecord)recordsIter.next();
    				recordsMap.put(agr.getStudentId(), agr);
    			}
    			List updateAssignmentRecord = new ArrayList();
    			for(Iterator studentsIter = studentUids.iterator(); studentsIter.hasNext();)
    			{
    				String studentUid = (String)studentsIter.next();
    				if(recordsMap.get(studentUid) != null && 
    						((AssignmentGradeRecord)recordsMap.get(studentUid)).getPointsEarned() == null)
    				{
    					AssignmentGradeRecord agr = (AssignmentGradeRecord)recordsMap.get(studentUid);
    					agr.setPointsEarned(new Double(0));
    					updateAssignmentRecord.add(agr);
    				}
    				else if(recordsMap.get(studentUid) == null)
    				{
    					AssignmentGradeRecord gradeRecord = new AssignmentGradeRecord(assignment, studentUid, new Double(0));
    					updateAssignmentRecord.add(gradeRecord);
    				}
    			}
    			updateAssignmentGradeRecords(assignment, updateAssignmentRecord);
    		}
    	}
    }

    public void convertGradePointsForUpdatedTotalPoints(Gradebook gradebook, Assignment assignment, Double newTotal, List studentUids)
    {
  		if(newTotal == null || assignment == null || gradebook == null)
  		{
  			throw new IllegalArgumentException("null values found in convertGradePointsForUpdatedTotalPoints.");
  		}
    	if(gradebook.getGrade_type() == GradebookService.GRADE_TYPE_PERCENTAGE && assignment.getPointsPossible() != null)
    	{
    		List records = getAssignmentGradeRecordsConverted(assignment, studentUids);
    		for(Iterator iter = records.iterator(); iter.hasNext(); )
    		{
    			AssignmentGradeRecord agr = (AssignmentGradeRecord) iter.next();
    			if(agr != null && agr.getPercentEarned() != null)
    			{
    				agr.setPointsEarned(new Double(agr.getPercentEarned().doubleValue() * newTotal.doubleValue() / 100.0));
    			}
    		}
    		updateAssignmentGradeRecords(assignment, records);
    	}
    	else if(gradebook.getGrade_type() == GradebookService.GRADE_TYPE_LETTER && assignment.getPointsPossible() != null)
    	{
    		List records = getAssignmentGradeRecordsConverted(assignment, studentUids);
    		LetterGradePercentMapping lgpm = getLetterGradePercentMapping(gradebook);
    		for(Iterator iter = records.iterator(); iter.hasNext(); )
    		{
    			AssignmentGradeRecord agr = (AssignmentGradeRecord) iter.next();
    			if(agr != null && agr.getLetterEarned() != null)
    			{
    				agr.setPointsEarned(new Double(lgpm.getValue(agr.getLetterEarned()).doubleValue() * newTotal.doubleValue()/100));
    			}
    		}
    		updateAssignmentGradeRecords(assignment, records);
    	}
    }
    
    /** synchronize from external application - override createAssignment method in BaseHibernateManager.*/
    public Long createAssignment(final Long gradebookId, final String name, final Double points, final Date dueDate, final Boolean isNotCounted, final Boolean isReleased) throws ConflictingAssignmentNameException, StaleObjectModificationException {

    	HibernateCallback hc = new HibernateCallback() {
    		public Object doInHibernate(Session session) throws HibernateException {
    			Gradebook gb = (Gradebook)session.load(Gradebook.class, gradebookId);
    			int numNameConflicts = ((Integer)session.createQuery(
    					"select count(go) from GradableObject as go where go.name = ? and go.gradebook = ? and go.removed=false").
    					setString(0, name).
    					setEntity(1, gb).
    					uniqueResult()).intValue();
    			if(numNameConflicts > 0) {
    				throw new ConflictingAssignmentNameException("You can not save multiple assignments in a gradebook with the same name");
    			}

    			Assignment asn = new Assignment();
    			asn.setGradebook(gb);
    			asn.setName(name);
    			asn.setPointsPossible(points);
    			asn.setDueDate(dueDate);
    			asn.setUngraded(false);
    			if (isNotCounted != null) {
    				asn.setNotCounted(isNotCounted.booleanValue());
    			}

    			if(isReleased!=null){
    				asn.setReleased(isReleased.booleanValue());
    			}

    			/** synchronize from external application */
    			if (synchronizer != null && !synchronizer.isProjectSite())
    			{
    				synchronizer.addLegacyAssignment(name);
    			}  

    			// Save the new assignment
    			Long id = (Long)session.save(asn);

    			return id;
    		}
    	};
    	return (Long)getHibernateTemplate().execute(hc);
    }

    /** synchronize from external application - override createAssignmentForCategory method in BaseHibernateManager.*/
    public Long createAssignmentForCategory(final Long gradebookId, final Long categoryId, final String name, final Double points, final Date dueDate, final Boolean isNotCounted, final Boolean isReleased)
    throws ConflictingAssignmentNameException, StaleObjectModificationException, IllegalArgumentException
    {
    	if(gradebookId == null || categoryId == null)
    	{
    		throw new IllegalArgumentException("gradebookId or categoryId is null in BaseHibernateManager.createAssignmentForCategory");
    	}

    	HibernateCallback hc = new HibernateCallback() {
    		public Object doInHibernate(Session session) throws HibernateException {
    			Gradebook gb = (Gradebook)session.load(Gradebook.class, gradebookId);
    			Category cat = (Category)session.load(Category.class, categoryId);
    			int numNameConflicts = ((Integer)session.createQuery(
    			"select count(go) from GradableObject as go where go.name = ? and go.gradebook = ? and go.removed=false").
    			setString(0, name).
    			setEntity(1, gb).
    			uniqueResult()).intValue();
    			if(numNameConflicts > 0) {
    				throw new ConflictingAssignmentNameException("You can not save multiple assignments in a gradebook with the same name");
    			}

    			Assignment asn = new Assignment();
    			asn.setGradebook(gb);
    			asn.setCategory(cat);
    			asn.setName(name);
    			asn.setPointsPossible(points);
    			asn.setDueDate(dueDate);
    			asn.setUngraded(false);
    			if (isNotCounted != null) {
    				asn.setNotCounted(isNotCounted.booleanValue());
    			}

    			if(isReleased!=null){
    				asn.setReleased(isReleased.booleanValue());
    			}

    			/** synchronize from external application */
    			if (synchronizer != null && !synchronizer.isProjectSite())
    			{
    				synchronizer.addLegacyAssignment(name);
    			}  

    			Long id = (Long)session.save(asn);

    			return id;
    		}
    	};

    	return (Long)getHibernateTemplate().execute(hc);
    }

    /** synchronize from external application */
    public void setSynchronizer(GbSynchronizer synchronizer) 
    {
    	this.synchronizer = synchronizer;
    }
}
