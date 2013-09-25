/*Copyright (C) Reazon Systems, Inc.  All rights reserved.*/
package com.reazon.tool.irubricpatch.ui;

import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.authz.api.Member;
import org.sakaiproject.component.api.ServerConfigurationService;
import org.sakaiproject.coursemanagement.api.CourseManagementService;
import org.sakaiproject.coursemanagement.api.Enrollment;
import org.sakaiproject.coursemanagement.api.EnrollmentSet;
import org.sakaiproject.coursemanagement.api.Section;
import org.sakaiproject.exception.IdUnusedException;
import org.sakaiproject.portal.util.URLUtils;
import org.sakaiproject.service.gradebook.shared.GradebookService;
import org.sakaiproject.site.api.Site;
import org.sakaiproject.site.api.SiteService;
import org.sakaiproject.tool.api.Placement;
import org.sakaiproject.tool.api.Session;
import org.sakaiproject.tool.api.ToolManager;
import org.sakaiproject.tool.cover.SessionManager;
import org.sakaiproject.tool.gradebook.Assignment;
import org.sakaiproject.tool.gradebook.iRubric.GradableObjectRubric;
import org.sakaiproject.tool.gradebook.Gradebook;
import org.sakaiproject.tool.gradebook.business.GradebookManager;
import org.sakaiproject.user.api.User;
import org.sakaiproject.user.api.UserDirectoryService;
//DN 2012-06-07:add class
import org.sakaiproject.tool.gradebook.AssignmentGradeRecord;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import com.reazon.tool.irubric.IRubricManager;
import org.sakaiproject.authz.cover.SecurityService;
/**
 * iRubric bean - a class working with iRubric server
 * 
 * @author CD
 */
public class IrubricBean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3177647771610921706L;
	private static Log LOG = LogFactory.getLog(IrubricBean.class);
	/**
	 * Declare URL parameter names. Abbreviations: CMD - command, SCH - school,
	 * GDB - gradebook, ROS - roster student, USR - user, DEST - destination
	 */
	private static final String PURPOSE = "purpose";
	private static final String CMD_VIEW = "v";
	private static final String CMD_ATTACH = "a";
	private static final String CMD_GRADE = "g";
	private static final String CMD_GRADE_ALL = "ga";
	private static final String CMD_GET_GRADE = "gg";
	private static final String CMD_GET_GRADES_BY_GDB = "gag";
	private static final String CMD_GET_GRADES_BY_ROS = "gas";
	
	//DN 2012-11-29: create string summary report(use set gradebookitem to irubric site) 
	private static final String CMD_SUMMARY_REPORT = "srpt";

	private static final String NULL_STRING = "null";
	private static final String EMPTY_STRING = "";

	private static final String P_CERTIFICATE_ID = "certID";
	private static final String P_SITE_ID = "siteId";
	private static final String P_SITE_TITLE = "siteTitle";
	private static final String P_GDB_ITEM_NAME = "gradebookItemName";
	private static final String P_GDB_ITEM_ID = "gradebookItemId";
	private static final String P_GDB_ITEM_ENTRY_TYPE = "gradeEntryType";
	private static final String P_GDB_ITEM_CAL = "cw";
	private static final String P_POINTS_POSSIBLE = "pointsPossible";
	private static final String P_ACADEMIC_ID = "academicSessionId";
	private static final String P_PROP_SITE_SECTION_EID = "site.cm.requested";
	private static final String P_USR_ID = "userId";
	private static final String P_USR_FNAME = "userFirstName";
	private static final String P_USR_LNAME = "userLastName";
	private static final String P_USR_ROLE = "userRole";
	private static final String DATA_STUDENTS = "dataStudents";
	private static final String P_ROS_ID = "rosterStudentId";
	private static final String P_ROS_FNAME = "rosterStudentFirstName";
	private static final String P_ROS_LNAME = "rosterStudentLastName";
	private static final String P_ENS = "enrollmentstatus";
	private static final String P_EN_SET = "enrollmentset";
	private static final String P_ROLE_TYPE = "userroletype";
	
	//DN 2013-08-13:param gradepublished
	private static final String P_GRADE_PUBLISHED = "gradepublished";

	//add param rubricid from table GB_GRADABLE_OBJECT_IRUBRIC_T
	private static final String P_RUB_ID = "rubricId";
	
	private static final String CATEGORY_OPT_NONE = "noCategories";
	private static final String CATEGORY_OPT_CAT_ONLY = "onlyCategories";
	private static final String CATEGORY_OPT_CAT_AND_WEIGHT = "categoriesAndWeighting";
	private static final String ENTRY_OPT_POINTS = "points";
	private static final String ENTRY_OPT_PERCENT = "percent";
	private static final String ENTRY_OPT_LETTER = "letterGrade";
	private static final String ROLE_TYPE_EVALUATOR = "evaluator";
	private static final String ROLE_TYPE_EVALUATEE = "evaluatee";

	private static final String ERR_MSG = "<br/><br/><div align=center>Error {errorcode}. Please contact your system administator.</div>";

	private int timeout;
	private boolean isSSL;
	private boolean isAnonymousStudents;
	
	private int irubricSwitch = 0;

	private String irubricRootUrl;
	private String irubricInitReqUrl;
	private String irubricRedirectUrl;
	private String certID;
	private String xtoken;
	private String termPropertyName;
	private String[] evaluator;
	private String[] evaluatee;

	private String academicSessionId;
	private String sslPort;

	private CourseManagementService courseManagementService;
	private UserDirectoryService userDirectoryService;
	private SiteService siteService;
	private GradebookManager gradebookManager;
	private IRubricManager rubricManager;
	private ToolManager toolManager;
	private ServerConfigurationService serverConfigurationService;
	
	private Double pointsPossible;

	/**
	 * sets the serverConfigurationService this property is set by
	 * spring-beans.xml
	 * 
	 * @param serverConfigurationService
	 *            the serverConfigurationService to set
	 */

	public void setServerConfigurationService(
			ServerConfigurationService serverConfigurationService) {
		this.serverConfigurationService = serverConfigurationService;
	}

	/**
	 * get the xToken property
	 * 
	 * @return the xToken
	 */
	public String getXtoken() {
		return xtoken;
	}

	/**
	 * set the xToken property
	 * 
	 * @param xtoken
	 *            the xToken to set
	 */
	public void setXtoken(String xtoken) {
		this.xtoken = xtoken;
	}

	/**
	 * gets session of the current user
	 * 
	 * @return the session
	 */
	public Session getSession() {
		return SessionManager.getCurrentSession();
	}

	/**
	 * sets the courseManagementService this property is set by spring-beans.xml
	 * 
	 * @param courseManagementService
	 *            the courseManagementService to set
	 */
	public void setCourseManagementService(
			CourseManagementService courseManagementService) {
		this.courseManagementService = courseManagementService;
	}

	/**
	 * sets the rubricManager service this property is set by
	 * irubric-beans.xml
	 * 
	 * @param rubricManager
	 */
	public void setRubricManager(IRubricManager rubricManager) {
		this.rubricManager = rubricManager;
	}

	/**
	 * sets the gradebookManager service this property is set by
	 * spring-beans.xml
	 * 
	 * @param gradebookManager
	 */
	public void setGradebookManager(GradebookManager gradebookManager) {
		this.gradebookManager = gradebookManager;
	}


	/**
	 * sets the site service property this property is set by spring-beans.xml
	 * 
	 * @param siteService
	 */
	public void setSiteService(SiteService siteService) {
		this.siteService = siteService;
	}

	/**
	 * set the userDirectoryService property this property is set by
	 * spring-beans.xml
	 * 
	 * @param userDirectoryService
	 */
	public void setUserDirectoryService(
			UserDirectoryService userDirectoryService) {
		this.userDirectoryService = userDirectoryService;
	}

	/**
	 * set the toolManager property this property is set by spring-beans.xml
	 * 
	 * @param toolManager
	 */
	public void setToolManager(ToolManager toolManager) {
		this.toolManager = toolManager;
	}

	/**
	 * get the courseManagementService property
	 * 
	 * @return the courseManagementService
	 */
	public CourseManagementService getCourseManagementService() {
		return courseManagementService;
	}

	/**
	 * get the gradebookManager property
	 * 
	 * @return the gradebookManager
	 */
	public GradebookManager getGradebookManager() {
		return gradebookManager;
	}

	/**
	 * get the rubricManager property
	 * 
	 * @return the rubricManager
	 */
	public IRubricManager getRubricManager() {
		return rubricManager;
	}

	/**
	 * get the userDirectoryService property
	 * 
	 * @return the userDirectoryService
	 */
	public UserDirectoryService getUserDirectoryService() {
		return userDirectoryService;
	}

	/**
	 * get the siteService property
	 * 
	 * @return the siteService
	 */
	public SiteService getSiteService() {
		return siteService;
	}

	/**
	 * get the toolManager property
	 * 
	 * @return the toolManager
	 */
	public ToolManager getToolManager() {
		return toolManager;
	}
	
	/**
	 * get the timeout property
	 * 
	 * @return the timeout
	 */
	public int getTimeout() {
		return timeout;
	}

	/**
	 * get the irubricRootUrl property
	 * 
	 * @return the irubricRootUrl
	 */
	public String getIrubricRootUrl() {
		StringBuilder url = new StringBuilder();
		if (this.isSSL) {
			url.append("https://");
			url.append(irubricRootUrl);
			url.append(":");
			url.append(this.sslPort);
		} else {
			url.append("http://");
			url.append(irubricRootUrl);
		}
		return url.toString();
	}

	/**
	 * Retrieve the irubricInitReqUrl property which directs to a URL on iRubric
	 * system. iRubric will return an xToken with dispatch token (starts with
	 * "T") or error code (starts with "E") ...
	 * 
	 * @return the irubricInitReqUrl
	 */
	public String getIrubricInitReqUrl() {
		return irubricInitReqUrl;
	}

	/**
	 * get the irubricRedirectUrl property
	 * 
	 * @return the irubricRedirectUrl
	 */
	public String getIrubricRedirectUrl() {
		StringBuilder str = new StringBuilder(getIrubricRootUrl());
		str.append("/");
		str.append(this.irubricRedirectUrl);

		return str.toString();
	}
	
	/**
	 * set the irubricSwitch property
	 * 
	 * @param irubricSwitch
	 *            the irubricSwitch to set
	 */
	public void setIrubricSwitch(int irubricSwitch) {
		this.irubricSwitch = irubricSwitch;
	}

	/**
	 * get the irubricSwitch property
	 * 
	 * @return the irubricSwitch
	 */
	public int getIrubricSwitch() {
		return Integer.parseInt(serverConfigurationService.getString("irubric.switch")); 
	
	}

	/**
	 * Constructor of this class
	 */
	public IrubricBean() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.sakaiproject.tool.gradebook.ui.InitializableBean#init()
	 */
	public void init() {
		this.irubricRootUrl = serverConfigurationService
				.getString("irubric.rootUrl");

		this.irubricRedirectUrl = serverConfigurationService
				.getString("irubric.redirectUrl");

		this.irubricInitReqUrl = serverConfigurationService
				.getString("irubric.initReqUrl");

		this.sslPort = serverConfigurationService.getString("irubric.sslPort");

		this.timeout = Integer.parseInt(serverConfigurationService
				.getString("irubric.timeout"));

		this.certID = serverConfigurationService.getString("irubric.certID");

		this.isAnonymousStudents = Boolean
				.parseBoolean(serverConfigurationService
						.getString("irubric.anonymousStudents"));
		this.isSSL = Boolean.parseBoolean(serverConfigurationService
				.getString("irubric.isSSL"));

		this.termPropertyName = serverConfigurationService
				.getString("irubric.termPropertyName");

		this.evaluator = serverConfigurationService
				.getStrings("irubric.evaluator");
		this.evaluatee = serverConfigurationService
				.getStrings("irubric.evaluatee");

	}

	/**
	 * get AcademicSessionId by site id
	 * 
	 * @param siteId
	 * @return string
	 * @author CD
	 */
	private String getAcademicSessionId(String siteId) throws IdUnusedException {
			Site site = siteService.getSite(siteId);
			academicSessionId = site.getProperties().getProperty(
					this.termPropertyName);
			if ((academicSessionId == "") || (academicSessionId == null))
			{									
				academicSessionId = "OTHER";
			}
		return academicSessionId;
	}

	/**
	 * Get role of current user
	 * 
	 * @return string
	 * @author CD
	 */
	private String getCurrentUserRole(String siteId) throws IdUnusedException, Exception {
		String roleName = null;

		// Get the current user
		String userId = userDirectoryService.getCurrentUser().getId();

		// Get the current site
		if (siteId == null) {
			siteId = toolManager.getCurrentPlacement().getContext();
		}
		
		Site site = siteService.getSite(siteId);

		// query teacher of this class
		Set<Member> members = site.getMembers();

		for (Member member : members) {
			if (member.getUserId().equals(userId)) {
				roleName = member.getRole().getId();
			}
		}

		// If user does not have a role in current site
		if (roleName == null) {
			//check if user is an admin (superuser)
			if(siteService.allowAccessSite(siteId) && SecurityService.isSuperUser()){
				//pass role "instructor" to iRubric
				roleName = "instructor";
			} else {
				throw new Exception("Cannot retrieve the role of current user.");
			}
		}
		
		return roleName;
	}

	/**
	 * Retrieve student's enrollment data
	 * 
	 * @param userEid
	 * @return String
	 */
	public String[] getStudentEnrollment(String userEid, String siteId) throws Exception {
		String[] studentEnrollment = { NULL_STRING, NULL_STRING };

		// Get the current site
		if (siteId == null) {
			siteId = toolManager.getCurrentPlacement().getContext();
		}
		Site site = siteService.getSite(siteId);
		if (site != null) {
			String sectionId = site.getProperties().getProperty(
					P_PROP_SITE_SECTION_EID);

			// RS: this condition is needed by ONC as sectionId can be null.
			if (sectionId != null) {
				Section section = courseManagementService.getSection(sectionId);
				if (section != null) {
					EnrollmentSet enrollmentSet = section.getEnrollmentSet();
					if (enrollmentSet != null) {
						Enrollment enr = courseManagementService
								.findEnrollment(userEid, enrollmentSet.getEid());
						// Only add the enrollment if it's not dropped and it
						// has an
						// enrollment role mapping
						if (enr != null && !enr.isDropped()) {
							studentEnrollment[0] = enrollmentSet.getEid();
							studentEnrollment[1] = enr.getEnrollmentStatus();
							return studentEnrollment;
						}
					}
				}
			}
		}
		return studentEnrollment;
	}

	/**
	 * Construct a string containing default post data
	 * 
	 * @return String
	 * @throws Exception
	 */
	private String buildDefaultPostData(String siteId) throws Exception {
		StringBuilder dataBuilder = new StringBuilder();

		Helper.addUrlParam(dataBuilder, "postFile", URLUtils
				.encodeUrl(this.irubricInitReqUrl));

		Helper.addUrlParam(dataBuilder, P_CERTIFICATE_ID, URLUtils
				.encodeUrl(this.certID));

		String currentSiteId = null;
		if (siteId != null) {
			currentSiteId = siteId;
		} else {
			Placement placement = getToolManager().getCurrentPlacement();
			if (placement != null)
				currentSiteId = placement.getContext();
		}
		
		String academicId = getAcademicSessionId(currentSiteId);
		Helper.addUrlParam(dataBuilder, P_ACADEMIC_ID, URLUtils
				.encodeUrl(academicId));

		Helper.addUrlParam(dataBuilder, P_SITE_ID, URLUtils
				.encodeUrl(currentSiteId));

		String siteTitle = null;
		Site currentSite = getSiteService().getSite(currentSiteId);
		if (currentSite != null)
			siteTitle = currentSite.getTitle();
		Helper.addUrlParam(dataBuilder, P_SITE_TITLE, URLUtils
				.encodeUrl(siteTitle));

		// add user parameter
		dataBuilder.append("&");
		dataBuilder.append(teacherParameters(siteId));
		return dataBuilder.toString();
	}

	/**
	 * @param request
	 * @param dataBuilder
	 */
	private void addGradebookParams(Long gradebookItemId,
			StringBuilder dataBuilder) {

		Assignment gradebookItem = gradebookManager
				.getAssignment(gradebookItemId);

		if (gradebookItem != null) {
			Helper.addUrlParam(dataBuilder, P_GDB_ITEM_NAME, URLUtils
					.encodeUrl(gradebookItem.getName()));

			Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ID, URLUtils
					.encodeUrl(gradebookItemId.toString()));
			
			setPointsPossible(gradebookItem.getPointsPossible());
			
			Gradebook gradebook = gradebookItem.getGradebook();
			if (gradebook != null) {
				int assignmentCategory = gradebook.getCategory_type();
				if (assignmentCategory == GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY)
					Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
							.encodeUrl(CATEGORY_OPT_CAT_AND_WEIGHT));
				else if (assignmentCategory == GradebookService.CATEGORY_TYPE_ONLY_CATEGORY)
					Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
							.encodeUrl(CATEGORY_OPT_CAT_ONLY));
				else
					Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
							.encodeUrl(CATEGORY_OPT_NONE));

				int gradeEntry = gradebook.getGrade_type();
				if (gradeEntry == GradebookService.GRADE_TYPE_PERCENTAGE)
					Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE,
							URLUtils.encodeUrl(ENTRY_OPT_PERCENT));
				else if (gradeEntry == GradebookService.GRADE_TYPE_POINTS)
					Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE,
							URLUtils.encodeUrl(ENTRY_OPT_POINTS));
				else
					Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE,
							URLUtils.encodeUrl(ENTRY_OPT_LETTER));
			}
		}
	}

	/**
	 * Add some pieces of a teacher profile to URL parameters string
	 * 
	 * @return string
	 */
	private String teacherParameters(String siteId) throws Exception {
		// Create data with String builder
		StringBuilder dataBuilder = new StringBuilder();

		String userFirstName = null;
		String userLastName = null;
		String userDisplayName = null;
		String userId = getSession().getUserId();

		// teacher is current user
		User teacher = getUserDirectoryService().getCurrentUser();
		if (teacher != null) {
			userFirstName = teacher.getFirstName();
			userLastName = teacher.getLastName();
			userDisplayName = teacher.getDisplayName();
		}

		Helper.addUrlParam(dataBuilder, P_USR_ID, URLUtils.encodeUrl(userId));

		if ((userFirstName == null || "".equals(userFirstName))
				&& (userLastName == null || "".equals(userLastName))) {
			Helper.addUrlParam(dataBuilder, P_USR_FNAME, URLUtils
					.encodeUrl(userDisplayName));

		} else {
			Helper.addUrlParam(dataBuilder, P_USR_FNAME, URLUtils
					.encodeUrl(userFirstName));

			Helper.addUrlParam(dataBuilder, P_USR_LNAME, URLUtils
					.encodeUrl(userLastName));
		}

		String userRole = getCurrentUserRole(siteId);
		Helper.addUrlParam(dataBuilder, P_USR_ROLE, URLUtils
				.encodeUrl(userRole));

		String userRoleType = getUserRoleType(userRole);
		Helper.addUrlParam(dataBuilder, P_ROLE_TYPE, URLUtils
				.encodeUrl(userRoleType));


		return dataBuilder.toString();
	}

	/**
	 * @param request
	 * @param purpose
	 * @return
	 * @throws Exception
	 */
	public String buildPostData(HttpServletRequest request, String purpose, String siteId)
			throws Exception {
		StringBuilder dataBuilder = new StringBuilder(buildDefaultPostData(siteId));
		Helper.addUrlParam(dataBuilder, PURPOSE, purpose);

		if (purpose.equals(CMD_ATTACH)) {
			String urlParam = request.getParameter(P_GDB_ITEM_ID);
			Long gradebookItemId = new Long(urlParam);
			buildPostDataForAttach(dataBuilder, gradebookItemId, siteId);
			
			//DN 2012-08-08:add param rubId
			String rubId = getRubricId(gradebookItemId);
			if(rubId != "") {
				Helper.addUrlParam(dataBuilder, P_RUB_ID, rubId);
			}
			
		} else if (purpose.equals(CMD_GRADE) || purpose.equals(CMD_VIEW) || purpose.equals(CMD_GRADE_ALL)) { 
			
			String studentId = "";
			if(!purpose.equals(CMD_GRADE_ALL)){
				studentId = request.getParameter(P_ROS_ID);
			}
			
			String urlParam = request.getParameter(P_GDB_ITEM_ID);

			Long gradebookItemId = new Long(urlParam);
			
			//DN 2012-05-28:change get studentUids from database (do not pass param)
			//used for gradeAll
			if(purpose.equals(CMD_GRADE_ALL) && gradebookItemId != 0) {
				
				//get studentUids by assignmentid(gradebookItemid) from database
				studentId = rubricManager.getStudentUIdsByGradebookItemId(gradebookItemId);
			    		
			}

			buildPostDataForGrade(dataBuilder, gradebookItemId, studentId, siteId);
			
			// DN 2012-08-08:add param rubId
			String rubId = getRubricId(gradebookItemId);
			//LOG.error("rubric id form table:"+ rubId);
			if(rubId != "") {
				Helper.addUrlParam(dataBuilder, P_RUB_ID, rubId);
			}

			//DN 2013-08-13: grade published
			if(purpose.equals(CMD_VIEW)) {
				Assignment gradebookItem = gradebookManager.getAssignment(gradebookItemId);
				boolean gradePublished = gradebookItem.isReleased();
				Helper.addUrlParam(dataBuilder, P_GRADE_PUBLISHED, String.valueOf(gradePublished));
			}

		} else if (purpose.equals(CMD_GET_GRADE)) {
			
			
			String urlParam = request.getParameter(P_GDB_ITEM_ID);

			Long gradebookItemId = new Long(urlParam);
			addGradebookParams(gradebookItemId, dataBuilder);
			

			String studentId = request.getParameter(P_ROS_ID);
			addRosterParams(studentId, dataBuilder, siteId);

		//DN 2012-11-29: iRubric Tool: view summary report	
		} else if (purpose.equals(CMD_GET_GRADES_BY_GDB) || purpose.equals(CMD_SUMMARY_REPORT)) {
			String gradebookItemId = request.getParameter(P_GDB_ITEM_ID);
			Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ID, URLUtils
					.encodeUrl(gradebookItemId));

			//DN 2012-11-29: iRubric Tool: if is get grade by gradable then set point possible
			if(purpose.equals(CMD_GET_GRADES_BY_GDB)) {
				Assignment gradebookItem = gradebookManager.getAssignment(Long.parseLong(gradebookItemId));
				if (gradebookItem != null) {
					setPointsPossible(gradebookItem.getPointsPossible());				
				}
			}
		} else if (purpose.equals(CMD_GET_GRADES_BY_ROS)) {
			String rosterStudentId = request.getParameter(P_ROS_ID);
			Helper.addUrlParam(dataBuilder, P_ROS_ID, URLUtils
					.encodeUrl(rosterStudentId));
		} 

		return dataBuilder.toString();
	}

	/**
	 * @param request
	 * @param dataBuilder
	 * @throws Exception
	 */
	private void addRosterParams(String rosterStudentId,
			StringBuilder dataBuilder, String siteId) throws Exception {
		// get value of purpose in dataBuilder
		String data = dataBuilder.toString();				
		String[] params = data.split("&");		
		String purpose = Helper.EMPTY_STRING;		
		for (String param : params) {						
			if (param.split("=")[0].equals("purpose")) {
				purpose = param.split("=")[1];
				break;
			}			
		}
		if (purpose.equals("ga")) {
			String[] rosterStudentIds = rosterStudentId.split(",");
			StringBuilder studentData = new StringBuilder();
			studentData.append("<students>");
			for (String rStudentId: rosterStudentIds)
			{				
				CreateXmlStudent(rStudentId,studentData,siteId);
			}
			studentData.append("</students>");			
			Helper.addUrlParam(dataBuilder, DATA_STUDENTS,
					URLUtils.encodeUrl(studentData.toString()));
		} else {
			Helper.addUrlParam(dataBuilder, P_ROS_ID, URLUtils
					.encodeUrl(rosterStudentId));
	
			Helper.addUrlParam(dataBuilder, P_EN_SET, URLUtils
					.encodeUrl(getStudentEnrollment(rosterStudentId,siteId)[0]));
			Helper.addUrlParam(dataBuilder, P_ENS, URLUtils
					.encodeUrl(getStudentEnrollment(rosterStudentId,siteId)[1]));
	
			if (!this.isAnonymousStudents) {
				User student = getUserDirectoryService().getUser(rosterStudentId);
	
				if (student != null) {
					String rosterStudentFirstName = Helper.EMPTY_STRING;
					if (student.getFirstName() != null) {
						rosterStudentFirstName = student.getFirstName();
					}
					String rosterStudentLastName = Helper.EMPTY_STRING;
					if (student.getLastName() != null) {
						rosterStudentLastName = student.getLastName();
					}
	
					Helper.addUrlParam(dataBuilder, P_ROS_FNAME, URLUtils
							.encodeUrl(rosterStudentFirstName));
	
					Helper.addUrlParam(dataBuilder, P_ROS_LNAME, URLUtils
							.encodeUrl(rosterStudentLastName));
				}
			}
		}
	}
	
	/**
	 * Student's infors are appended to data package builder to send to Irubric
	 * @param rosStudentId String Student ID
	 * @param studentData string data package builder
	 * @throws Exception
	 */
	private void CreateXmlStudent(String rosStudentId,StringBuilder studentData, String siteId) throws Exception
	{
		studentData.append("<student ");
		studentData.append(P_ROS_ID + "=\"" + rosStudentId +"\"");
		studentData.append(" " + P_EN_SET + "=\"" + getStudentEnrollment(rosStudentId,siteId)[0] + "\"");
		studentData.append(" " + P_ENS + "=\"" + getStudentEnrollment(rosStudentId,siteId)[1] + "\"");
				
		User student = getUserDirectoryService().getUser(
				rosStudentId);

		if (student != null) {
			String rosterStudentFirstName = Helper.EMPTY_STRING;
			if (student.getFirstName() != null) {
				rosterStudentFirstName = student.getFirstName();
			}
			String rosterStudentLastName = Helper.EMPTY_STRING;
			if (student.getLastName() != null) {
				rosterStudentLastName = student.getLastName();
			}
			
			studentData.append(" " + P_ROS_FNAME + "=\"" + rosterStudentFirstName + "\"");
			studentData.append(" " + P_ROS_LNAME + "=\"" + rosterStudentLastName + "\"");				
		}
		
		studentData.append(" />");			
	}
	
	/**
	 * render a error alert when page loaded
	 * 
	 * @param errorMsg
	 *            an error message
	 * @return a string
	 */
	public String renderJSErrorBox(String errorMsg) {
		StringBuilder builder = new StringBuilder(
				"<html><body onload=\"alert('");
		builder.append(errorMsg);
		builder.append("');\"></body></html>");

		return builder.toString();
	}

	/**
	 * render a error message when page loaded
	 * 
	 * @param errorMsg
	 *            an error message
	 * @return a string
	 */
	private String renderErrorMessageBox(String errorMsg) {
		StringBuilder builder = new StringBuilder(
				"<br/><br/><div align=center>");
		builder.append(errorMsg);
		builder.append("</div>");

		return builder.toString();
	}

	/**
	 * render error message by command
	 * 
	 * @param printWriter
	 * @param cmd
	 * @param errorMsg
	 */
	public void renderErrorMessageByCmd(PrintWriter printWriter, String cmd,
			String errorMsg) {

		if (cmd.equals(CMD_GET_GRADES_BY_GDB)
				|| cmd.equals(CMD_GET_GRADES_BY_ROS)) {
			printWriter
					.print("<html><body onload=\"window.parent.alertMsgByCmd('allgrades', '"
							+ errorMsg + "');\"></body></html>");
		} else if (cmd.equals(CMD_GET_GRADE)) {
			printWriter
					.print("<html><body onload=\"window.parent.alertMsgByCmd('getGradeFrame', '"
							+ errorMsg + "');\"></body></html>");
		} else {
			printWriter.print(renderErrorMessageBox(errorMsg));
		}
	}

	/**
	 * dump error message by purpose
	 * 
	 * @param writer
	 * @param cmd
	 *            the purpose is get from iRubricLink.jsp
	 * @param errorCode
	 */
	public void dumpErrorMessage(PrintWriter writer, String cmd,
			String errorCode) {
		if (cmd.equals(CMD_GET_GRADES_BY_GDB)
				|| cmd.equals(CMD_GET_GRADES_BY_ROS)) {
			writer
					.print("<html><body onload=\"window.parent.alertInvalidValue('allgrades', '"
							+ errorCode + "');\"></body></html>");
		} else if (cmd.equals(CMD_GET_GRADE)) {
			writer
					.print("<html><body onload=\"window.parent.alertInvalidValue('getGradeFrame', '"
							+ errorCode + "');\"></body></html>");
		} else {
			writer.print(ERR_MSG.replace("{errorcode}", errorCode));
		}
	}

	/**
	 * Get the URL on iRubric system to initialize a request from Sakai
	 * 
	 * @return A URL
	 */
	public String getInitReqURL() {
		StringBuilder url = new StringBuilder();
		url.append(getIrubricRootUrl());
		url.append("/");
		url.append(getIrubricInitReqUrl());
		return url.toString();
	}

	/**
	 * Sync the attached rubric data from iRubric system
	 * 
	 * @param gradebookItemId
	 * @param iRubricId
	 * @param iRubricTitle
	 * 
	 * @return void
	 */
	public void updateAssignmetByRubric(Long gradebookItemId, String iRubricId,
			String iRubricTitle) {
		GradableObjectRubric gradableObjectRubric = rubricManager
				.getGradableObjectRubric(gradebookItemId);

		if (gradableObjectRubric == null) {
			gradableObjectRubric = new GradableObjectRubric();
			gradableObjectRubric.setGradableObjectId(gradebookItemId);
		}

		if (iRubricId.toLowerCase().equals(NULL_STRING)) {
			gradableObjectRubric.setiRubricId(null);
			gradableObjectRubric.setiRubricTitle(null);
		} else {
			gradableObjectRubric.setiRubricId(iRubricId);
			gradableObjectRubric.setiRubricTitle(iRubricTitle);
		}

		rubricManager.updateGradableObjectRubric(gradableObjectRubric);
	}

	/**
	 * Get the role type name by user role
	 * 
	 * @param roleName
	 * @return
	 */
	public String getUserRoleType(String roleName) {
		if (roleName == null) {
			return EMPTY_STRING;
		}

		for (int i = 0; i < evaluator.length; i++) {
			LOG.info(evaluator[i]);
			if (evaluator[i].toLowerCase()
					.equals(roleName.trim().toLowerCase())) {
				return ROLE_TYPE_EVALUATOR;
			}
		}

		for (int i = 0; i < evaluatee.length; i++) {
			LOG.info(evaluatee[i]);
			if (evaluatee[i].toLowerCase()
					.equals(roleName.trim().toLowerCase())) {
				return ROLE_TYPE_EVALUATEE;
			}
		}

		return EMPTY_STRING;
	}

	/**
	 * build data packet for attach purpose
	 * 
	 * @param dataBuilder
	 * @param gradebookItemId
	 * @throws Exception
	 */
	private void buildPostDataForAttach(StringBuilder dataBuilder,
			Long gradebookItemId, String siteId) throws Exception {

		Assignment gradebookItem = gradebookManager
				.getAssignment(gradebookItemId);
		if (gradebookItem != null) {

			Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ID, URLUtils
					.encodeUrl(gradebookItemId.toString()));

			String gradebookItemName = gradebookItem.getName();
			Helper.addUrlParam(dataBuilder, P_GDB_ITEM_NAME, URLUtils
					.encodeUrl(gradebookItemName));

			// add current user's information
			teacherParameters(siteId);

			int assignmentCategory = gradebookItem.getGradebook()
					.getCategory_type();
			if (assignmentCategory == GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY)
				Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
						.encodeUrl(CATEGORY_OPT_CAT_AND_WEIGHT));
			else if (assignmentCategory == GradebookService.CATEGORY_TYPE_ONLY_CATEGORY)
				Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
						.encodeUrl(CATEGORY_OPT_CAT_ONLY));
			else
				Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
						.encodeUrl(CATEGORY_OPT_NONE));

			int gradeEntry = gradebookItem.getGradebook().getGrade_type();
			if (gradeEntry == GradebookService.GRADE_TYPE_PERCENTAGE)
				Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE, URLUtils
						.encodeUrl(ENTRY_OPT_PERCENT));
			else if (gradeEntry == GradebookService.GRADE_TYPE_POINTS)
				Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE, URLUtils
						.encodeUrl(ENTRY_OPT_POINTS));
			else
				Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE, URLUtils
						.encodeUrl(ENTRY_OPT_LETTER));

			String pointsPossible = null;
			if (gradebookItem.getPointsPossible() == null) {
				pointsPossible = Helper.EMPTY_STRING;
			} else {
				pointsPossible = gradebookItem.getPointsPossible().toString();
			}
			Helper.addUrlParam(dataBuilder, P_POINTS_POSSIBLE, pointsPossible);
		}
	}

	/**
	 * build data packet for get grade purpose
	 * 
	 * @param dataBuilder
	 * @param id
	 * @throws Exception
	 */
	private void buildPostDataForGrade(StringBuilder dataBuilder,
			Long assignmentId, String rosterStudentId, String siteId) throws Exception {

		// add current user's information
		teacherParameters(siteId);

		Assignment gradbookItem = gradebookManager.getAssignment(assignmentId);
		if (gradbookItem != null) {
			String gradebookItemId = gradbookItem.getId().toString();
			Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ID, URLUtils
					.encodeUrl(gradebookItemId));
			String pointsPossible = null;
			if (gradbookItem.getPointsPossible() == null) {
				pointsPossible = "";
			} else {
				pointsPossible = gradbookItem.getPointsPossible().toString();
			}
			Helper.addUrlParam(dataBuilder, P_POINTS_POSSIBLE, pointsPossible);
			
			Helper.addUrlParam(dataBuilder, P_GDB_ITEM_NAME, URLUtils
					.encodeUrl(gradbookItem.getName()));
			int assignmentCategory = gradbookItem.getGradebook()
			.getCategory_type();
			if (assignmentCategory == GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY)
				Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
						.encodeUrl(CATEGORY_OPT_CAT_AND_WEIGHT));
			else if (assignmentCategory == GradebookService.CATEGORY_TYPE_ONLY_CATEGORY)
				Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
						.encodeUrl(CATEGORY_OPT_CAT_ONLY));
			else
				Helper.addUrlParam(dataBuilder, P_GDB_ITEM_CAL, URLUtils
						.encodeUrl(CATEGORY_OPT_NONE));
		
			int gradeEntry = gradbookItem.getGradebook().getGrade_type();
			if (gradeEntry == GradebookService.GRADE_TYPE_PERCENTAGE)
				Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE, URLUtils
						.encodeUrl(ENTRY_OPT_PERCENT));
			else if (gradeEntry == GradebookService.GRADE_TYPE_POINTS)
				Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE, URLUtils
						.encodeUrl(ENTRY_OPT_POINTS));
			else
				Helper.addUrlParam(dataBuilder, P_GDB_ITEM_ENTRY_TYPE, URLUtils
						.encodeUrl(ENTRY_OPT_LETTER));
		}

		addRosterParams(rosterStudentId, dataBuilder,siteId);
	}

	/**
	 * @return the pointsPossible
	 */
	public Double getPointsPossible() {
		return pointsPossible;
	}

	/**
	 * @param pointsPossible the pointsPossible to set
	 */
	public void setPointsPossible(Double pointsPossible) {
		this.pointsPossible = pointsPossible;
	}
	
	/**
	 * Determine whether iRubric is available for a given gradebook item.
	 * 
	 * @param siteId
	 * @param gradebookId
	 */
	public boolean isIRubricAvailable(long gradebookItemId) {
		boolean enabled = false;
		// TODO We may also want to do the check to see if iRubric is available for
		// the site, in the event that iRubric was turned off at a later date
		// for the site.
		GradableObjectRubric rubric = rubricManager.getGradableObjectRubric(gradebookItemId);
		if (rubric != null) {
			enabled = true;
		}
		return enabled;
	}
	
	/**
	 * DN 2012-06-07: function save grade form Irubric system(use for Gradebook2) 
	 * 
	 * @param strScoreStream(grade from irubric system)(datatype: studenUId1,score1|studenUId2,score2|...)
	 * @param gradebookId
	 */
	public void saveGradeFromGB2(long gradebookItemId, String strScoreStream) {
		
		//check grade request from irubric 
		LOG.error("Request Grade form irubric:" + strScoreStream);
		
		//get assignment by gradebookItemId
		Assignment assignment = gradebookManager.getAssignment(gradebookItemId);
		
		//get grade type
		int gradeType = assignment.getGradebook().getGrade_type();
		
		String[] records = strScoreStream.split("\\|");
		int length = records.length;
		//create list gradeRecords  because function updateStudentGradeRecords need param(coolection,gradeType,studentUId)
		List gradeRecords = new ArrayList();
		
		if (length > 0) {
			
			for(int i = 0; i< length; i ++){
				
				//score(grade) when split
				Double score = Double.parseDouble(records[i].split("\\,")[1].trim());
				String studentUId = records[i].split("\\,")[0].trim();
				
				//get AssignmentGradeRecord to update grade
				AssignmentGradeRecord agr = gradebookManager.getAssignmentGradeRecordForAssignmentForStudent(assignment,studentUId);
				
				if(agr != null && (agr.getPointsEarned() == null || !agr.getPointsEarned().equals(score))){

						//set grade from Irubric system
						agr.setPointsEarned(score);
						
						//DN 2012-08-10: if grade is not saved in AssignmentGradeRecord
						//need to set value to save AssignmentGradeRecord
						if(agr.getStudentId() == null){
							//set student id(value needs to be saved in table GB_GRADE_RECORD_T)
							agr.setStudentId(studentUId);
							//set assignment(value 'GRADABLE_OBJECT_ID' needs to be saved in table GB_GRADE_RECORD_T)
							agr.setGradableObject(assignment);
						}
						
						gradeRecords.add(agr);
						
						//update grade
						gradebookManager.updateStudentGradeRecords((Collection)gradeRecords, gradeType, studentUId);
						
						gradeRecords.clear();
				}
				
			}
			
		}
		
	}
	
	//DN 2012-08-08: function get rubricid
	private String getRubricId(long gradebookItemId) {
		GradableObjectRubric rubric = rubricManager.getGradableObjectRubric(gradebookItemId);
		if (rubric != null) {
			return rubric.getiRubricId();
		} else {
			return "";
			
		}
	}
}

