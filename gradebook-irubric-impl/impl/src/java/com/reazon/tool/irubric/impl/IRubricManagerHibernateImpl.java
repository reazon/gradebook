package com.reazon.tool.irubric.impl;

import java.util.*;

import java.util.List;
import java.util.Set;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

import org.sakaiproject.component.gradebook.BaseHibernateManager;
import org.sakaiproject.service.gradebook.shared.ConflictingAssignmentNameException;
import org.sakaiproject.service.gradebook.shared.StaleObjectModificationException;
import org.sakaiproject.service.gradebook.shared.GradebookNotFoundException;
import org.sakaiproject.tool.gradebook.LetterGradePercentMapping;
import org.sakaiproject.tool.gradebook.GradableObject;
import org.sakaiproject.tool.gradebook.iRubric.GradableObjectRubric;
import org.sakaiproject.tool.gradebook.Assignment;

import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateOptimisticLockingFailureException;

import com.reazon.tool.irubric.IRubricManager;
import org.sakaiproject.tool.cover.ToolManager;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.site.cover.SiteService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.component.cover.ServerConfigurationService;
import org.sakaiproject.tool.gradebook.Gradebook;
/**
 * Manages Rubric persistence via hibernate.
 */
public class IRubricManagerHibernateImpl extends BaseHibernateManager
      implements IRubricManager {
      public static final String IRUBRIC_SITE_KEY = "iRubricSite";
    /**
     * Update a GradableObjectRubric object
     *
     * @param gradableObjectRubric
	 * @return void
     */
	public void updateGradableObjectRubric(final GradableObjectRubric gradableObjectRubric){
    	HibernateCallback hc = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
            	session.evict(gradableObjectRubric);
        		session.saveOrUpdate(gradableObjectRubric);
                return null;
            }
        };
        getHibernateTemplate().execute(hc);
    }
	
	/**
     * Get a GradableObjectRubric object by assignment
     *
     * @param assignmentId
	 * @return GradableObjectRubric
     */
    public GradableObjectRubric getGradableObjectRubric(Long gradableObjectId) {
    	//logger.info(gradableObjectId);
    	String hql = "from GradableObjectRubric as gr where gr.gradableObjectId=?";
    	if (getHibernateTemplate().find(hql, gradableObjectId).size()>0)
    		return (GradableObjectRubric)(getHibernateTemplate().find(hql, gradableObjectId)).get(0);
    	return null;
    }
    
    /**
     * Get a GradableObjectRubric objects by a list of assignment parameters
     *
     * @param gradableObjectIds
    * @return List of gradableObjectRubrics
     */
    public List getGradableObjectRubrics(final List<Long> gradableObjectIds) {
        HibernateCallback hc = new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException ,SQLException {
                List gradableObjectRubrics = new ArrayList();

                if (gradableObjectIds != null && !gradableObjectIds.isEmpty()) {
                    String hql = "from GradableObjectRubric as gr where (gr.gradableObjectId in (:gradableObjectIdList) and (gr.iRubricId <> null))";
                    Query query = session.createQuery(hql);

                    gradableObjectRubrics = queryWithParameterList(query, "gradableObjectIdList", gradableObjectIds);
                }

                return gradableObjectRubrics;
            }
        };
        return (List)getHibernateTemplate().execute(hc);

    }
    
    /**
     * 
     * @param query - your query with all other parameters already defined
     * @param queryParamName - the name of the list parameter referenced in the query
     * @param fullList - the list that you are using as a parameter
     * @return the resulting list from a query that takes in a list as a parameter;
     * this will cycle through with sublists if the size of the list exceeds the
     * allowed size for an sql query
     */
    private List queryWithParameterList(Query query, String queryParamName, List fullList) {
        // sql has a limit for the size of a parameter list, so we may need to cycle
        // through with sublists
        List queryResultList = new ArrayList();

        if (fullList.size() < MAX_NUMBER_OF_SQL_PARAMETERS_IN_LIST) {
            query.setParameterList(queryParamName, fullList);
            queryResultList = query.list();

        } else {
            // if there are more than MAX_NUMBER_OF_SQL_PARAMETERS_IN_LIST, we need to do multiple queries
            int begIndex = 0;
            int endIndex = 0;

            while (begIndex < fullList.size()) {
                endIndex = begIndex + MAX_NUMBER_OF_SQL_PARAMETERS_IN_LIST;
                if (endIndex > fullList.size()) {
                    endIndex = fullList.size();
                }
                List tempSubList = new ArrayList();
                tempSubList.addAll(fullList.subList(begIndex, endIndex));

                query.setParameterList(queryParamName, tempSubList);

                queryResultList.addAll(query.list());
                begIndex = endIndex;
            }
        }

        return queryResultList;
    }
    
    /**
     * convert point to letter grade
     * @param gradebook
     * @param pointsPossible
     * @param point
	 * @return String
	     * 			the letter grade is converted from point
	     */
    public String convertPointToLetterGrade(LetterGradePercentMapping lgpm , Double pointsPossible, Double point){
    	String letterGrade = lgpm.getGrade(calculateEquivalentPercent(pointsPossible, point));
    	return letterGrade;
    }	
    
    /**
     * DN 2012-05-28: defined function get studentUIds by gradebookItemId/assignemntId
     * @param gradebookItemId
     * @return String: studentUIds("studentUId1,studentUId2,...")
	 */
    public String getStudentUIdsByGradebookItemId(Long gradebookItemId){
    	
    	String studentIds = "";
    	
    	Assignment assignment = getAssignment(gradebookItemId);
    	//get gradebookId
    	Long gradebookId = assignment.getGradebook().getId();
        
    	//get studentUids
    	Set studentUids = getAllStudentUids(getGradebookUid(gradebookId));
    	
    	if(studentUids.size() > 0){//if value is in util.set
    		
    		//join string studentId
	    	for (Iterator iter = studentUids.iterator(); iter.hasNext(); ) {
				String strUid = (String)iter.next();

				if(strUid != ""){
					studentIds += strUid +","; 
				}
			}
	    	
    	}
    	return studentIds;
    }
    
    //DN 2012-09-21:get gradeobject id by name assignment(gradeobject) and gradebookUid
    //use for assignment and site-manage(copy site)
	public Long getGradableObjectId(final String name,final String gradebookUid) {
		
		HibernateCallback hcbObj = new HibernateCallback() 
		{
			public Object doInHibernate(Session session) throws HibernateException {
		    	Query q = session.createQuery("select g.id from GradableObject as g where g.gradebook.uid=? and g.name = ? and g.removed=false");
		    	q.setParameter(0, gradebookUid, Hibernate.STRING);
		    	q.setParameter(1, name, Hibernate.STRING);
		    	return q.uniqueResult();
		    }
		};
		Long gradeObjectId = (Long) getHibernateTemplate().execute(hcbObj);	
		
		return gradeObjectId;
	}
	
	//DN 2012-09-25:get gradableobject id by externalId and gradebookUId
	public Long getGradableObjectIdByExternalId(final String name,final String gradebookUid) {
		
		HibernateCallback hcbObj = new HibernateCallback() 
		{
			public Object doInHibernate(Session session) throws HibernateException {
		    	Query q = session.createQuery("select g.id from GradableObject as g where g.gradebook.uid=? and g.externalId = ? and g.removed=false and g.externallyMaintained=true and g.externalAppName= ?");
		    	q.setParameter(0, gradebookUid, Hibernate.STRING);
		    	q.setParameter(1, name, Hibernate.STRING);
		    	q.setParameter(2, "Assignments", Hibernate.STRING);
		    	return q.uniqueResult();
		    }
		};
		Long gradeObjectId = (Long) getHibernateTemplate().execute(hcbObj);	
		
		return gradeObjectId;
	}
 
//DN 2013-04-17: get irubric site from database
    public boolean getIsiRubricSite() {
        boolean isIRubricSite = false;
        
        String currentSiteId = null;
        Placement placement = ToolManager.getCurrentPlacement();
        if (placement != null) {
            currentSiteId = placement.getContext();
        }
        String iRubricSiteValue = null;
        try {
            Site site = SiteService.getSite(currentSiteId);
            iRubricSiteValue = site.getProperties().getProperty(IRUBRIC_SITE_KEY);          
        } catch(IdUnusedException e) {
            iRubricSiteValue = null;
        }
        
        if (iRubricSiteValue != null && "true".equalsIgnoreCase(iRubricSiteValue)) {
            isIRubricSite = true;
        }

        return isIRubricSite;
    }

    public boolean isShowiRubricLink(){
       
        //private int iRubricSwitch = 0;
        int iRubricSwitch = Integer.parseInt(ServerConfigurationService.getString("irubric.switch"));     
        if(iRubricSwitch != 0 && (iRubricSwitch == 1 || getIsiRubricSite())) {
            return true;
        }
        return false;
    }
   //use for irubric tool
    private List getAssignments(final Long gradebookId) {
        return (List)getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException {
                List assignments = getAssignments(gradebookId, session);
                return assignments;
            }
        });
    }
    //use for irubric tool
    private Long getGradebookId(String gradebookUId){
        Gradebook gradebook = null;
        try {
            gradebook = getGradebook(gradebookUId);
        } catch (GradebookNotFoundException e) {
            //gradebookUId = null;
        }

        if(gradebook == null)
            throw new IllegalStateException("Gradebook is null!");
        
        return gradebook.getId();
    }
    
    //use for irubric tool
    public List getAssignmentsByGradebookUId(String gradebookUId){
        Long gradebookId = getGradebookId(gradebookUId);
        
        List assignments = getAssignments(gradebookId);
        
        //for each object, set value of attached rubric
        for (Iterator iter = assignments.iterator(); iter.hasNext(); ) {
            Assignment assignment = (Assignment)iter.next();
            Long assignmentId = assignment.getId();
            
            //if gradebook item has attached rubric, then set isRemoved=false, else isRemoved=true
            assignment.setRemoved(!isHaveAttach(assignmentId));
        }
        return assignments;
    }

    /*
    *   check have attach irubric in gradebook item
    *   @param assignmentid - gradebookitem id (Long)
    *   return true if have attach irubric, false is otherwise
    */
    private boolean isHaveAttach(Long assignmentId) {

        if(getGradableObjectRubric(assignmentId) != null)
            return true;

        return false;
    }
}
