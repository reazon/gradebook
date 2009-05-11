<link href="dhtmlpopup/dhtmlPopup.css" rel="stylesheet" type="text/css" />
<script src="dhtmlpopup/dhtmlPopup.js" type="text/javascript"></script>
<f:view>
	<div class="portletBody">
	  <h:form id="gbForm">

			<t:aliasBean alias="#{bean}" value="#{gradebookSetupBean}">
				<%@include file="/inc/appMenu.jspf"%>
			</t:aliasBean>
	
			<sakai:flowState bean="#{gradebookSetupBean}" />
	
			<h2><h:outputText value="#{msgs.gb_setup_title}"/></h2>
			
			<%@include file="/inc/globalMessages.jspf"%>

			<p class="instruction">
				<h:outputText value="* " styleClass="reqStarInline"/>
				<h:outputText value="#{msgs.flag_required}"/>
			</p>
			
		<%-- /*Per SAK-15061, Error message doesn't need to show up. */
		 	<h:panelGroup rendered="#{!gradebookSetupBean.isExistingConflictScale}" styleClass="validation">
			  <h:outputText value="#{msgs.feedback_options_existing_conflict1}" rendered="#{!gradebookSetupBean.isExistingConflictScale}"/> 
		  	<h:outputLink value="http://kb.iu.edu/data/aitz.html" rendered="#{!gradebookSetupBean.isExistingConflictScale}" target="support_window1">
		  		<h:outputText value="#{msgs.feedback_options_existing_conflict2}" rendered="#{!gradebookSetupBean.isExistingConflictScale}"/>
			  </h:outputLink>
			  <h:outputText value=" " rendered="#{!gradebookSetupBean.isExistingConflictScale}"/>
			  <h:outputText value="#{msgs.feedback_options_existing_conflict3}" rendered="#{!gradebookSetupBean.isExistingConflictScale}"/>
			</h:panelGroup>
			<h:panelGroup rendered="#{!gradebookSetupBean.isValidWithCourseGrade}" styleClass="validation">
			  <h:outputText value="#{msgs.feedback_options_cannot_change_percentage1}" rendered="#{!gradebookSetupBean.isValidWithCourseGrade}"/>
		  	<h:outputLink value="http://kb.iu.edu/data/aitz.html" rendered="#{!gradebookSetupBean.isValidWithCourseGrade}" target="support_window2">
		  		<h:outputText value="#{msgs.feedback_options_cannot_change_percentage2}" rendered="#{!gradebookSetupBean.isValidWithCourseGrade}"/>
			  </h:outputLink>
			  <h:outputText value=" " rendered="#{!gradebookSetupBean.isValidWithCourseGrade}"/>
			  <h:outputText value="#{msgs.feedback_options_cannot_change_percentage3}" rendered="#{!gradebookSetupBean.isValidWithCourseGrade}"/>
			</h:panelGroup>
			
			--%>
	
			<h4><h:outputText value="#{msgs.grade_entry_heading}"/></h4>
			
			<div class="indnt1">
				<div class="instruction"><h:outputText value="#{msgs.grade_entry_info}" escape="false" rendered="#{!gradebookSetupBean.isExistingGrades}"/></div>
			
				<h:selectOneRadio value="#{gradebookSetupBean.gradeEntryMethod}" id="gradeEntryMethod1" layout="pageDirection"  
				rendered="#{gradebookSetupBean.enableLetterGrade && !gradebookSetupBean.isExistingGrades}"
				valueChangeListener="#{gradebookSetupBean.processGradeEntryMethodChange}" onclick="this.form.submit();">
					<f:selectItem itemValue="points" itemLabel="#{msgs.entry_opt_points}" />
	        <f:selectItem itemValue="percent" itemLabel="#{msgs.entry_opt_percent}" /> 
	        <f:selectItem itemValue="letterGrade" itemLabel="#{msgs.entry_opt_letters}"/>
				</h:selectOneRadio>

				<h:selectOneRadio value="#{gradebookSetupBean.gradeEntryMethod}" id="gradeEntryMethod2" layout="pageDirection"  
				rendered="#{!gradebookSetupBean.enableLetterGrade && !gradebookSetupBean.isExistingGrades}"
				valueChangeListener="#{gradebookSetupBean.processGradeEntryMethodChange}" onclick="this.form.submit();">
					<f:selectItem itemValue="points" itemLabel="#{msgs.entry_opt_points}" />
	        <f:selectItem itemValue="percent" itemLabel="#{msgs.entry_opt_percent}" /> 
	        <f:selectItem itemValue="letterGrade" itemLabel="#{msgs.entry_opt_letters}"/>
				</h:selectOneRadio>

			</div>
			
			<% /*Per SAK-10879, no longer allow user to customize letter grading scale. set rendered="false" and removed js call from select radio button */
				/*onclick="javascript:displayHideElement(this.form, 'gradeEntryScale', 'gradeEntryMethod', 'letterGrade'); resize();"*/ %>
			<h:panelGroup id="gradeEntryScale" style="#{gradebookSetupBean.displayGradeEntryScaleStyle}" rendered="false">
				<f:verbatim><h4 class="indnt3"></f:verbatim>
					<h:outputText value="#{msgs.grade_entry_scale}" />
				<f:verbatim></h4></f:verbatim>
				
				<t:dataTable cellpadding="0" cellspacing="0"
					id="gradingScaleTable"
					value="#{gradebookSetupBean.letterGradeRows}"
					var="gradeRow"
		      columnClasses="bogus,bogus,specialLink"
					styleClass="listHier narrowerTable indnt3"
					headerClass="center">
					<h:column>
						<f:facet name="header">
							<h:outputText value="#{msgs.grade_scale_grade}"/>
						</f:facet>
						<h:outputText id="letterGrade" value="#{gradeRow.grade}"/>
					</h:column>
					<h:column>
						<f:facet name="header">
							<h:outputText value="#{msgs.grade_scale_percent}"/>
						</f:facet>
						<h:inputText id="percentGrade" value="#{gradeRow.mappingValue}" rendered="#{gradeRow.editable}" style="text-align:right;" size="6"/>
						<h:message for="percentGrade" styleClass="alertMessageInline" />
					</h:column>
				</t:dataTable>
				
			</h:panelGroup>
			
			<div class="indnt1">
		<h:outputText styleClass="instruction" value="#{msgs.gradebook_setup_message1}" rendered="#{gradebookSetupBean.isExistingGrades && gradebookSetupBean.isPointGrade}"/>
		<h:outputText styleClass="instruction" value="#{msgs.gradebook_setup_message5}" rendered="#{gradebookSetupBean.isExistingGrades && gradebookSetupBean.isPercentageGrade}"/>
		<h:outputText styleClass="instruction" value="#{msgs.gradebook_setup_message3}" rendered="#{gradebookSetupBean.isExistingGrades && gradebookSetupBean.isLetterGrade}"/> 
		 <h:commandLink	action="#{gradebookSetupBean.navigateToDeleteAllGrades}" immediate="true" rendered="#{gradebookSetupBean.isExistingGrades}">
			<h:outputText value="#{msgs.gradebook_setup_message2}" />
		</h:commandLink> 
		<h:outputText value="#{msgs.gradebook_setup_message4}" rendered="#{gradebookSetupBean.isExistingGrades}"/>
		</div>
				
			<h4><h:outputText value="#{msgs.gb_setup_items_display}"/></h4>
			
			<div class="indnt1">
				<div class="gbSection">
					<h:selectBooleanCheckbox id="releaseItems" value="#{gradebookSetupBean.localGradebook.assignmentsDisplayed}"	/>
					<h:outputLabel for="releaseItems" value="#{msgs.display_released_items}" />
					<div class="indnt2">
						<h:outputText styleClass="instruction" value="#{msgs.display_released_items_info}" />
					</div>
				</div>
			</div>
			<%--
			<h4><h:outputText value="#{msgs.gb_setup_grader_perms_title}"/></h4>
			<div class="indnt1 gbSection">
				<h:commandLink action="graderRules">
					<h:outputText value="#{msgs.gb_setup_modify_grader_perms}"/>
				</h:commandLink>
			</div>
			--%>
	 
		  <t:aliasBean alias="#{bean}" value="#{gradebookSetupBean}">
				<%@include file="/inc/categoryEdit.jspf"%>
			</t:aliasBean>
			
			<div class="act calendarPadding">
				<h:commandButton
					id="saveButton"
					styleClass="active"
					value="#{msgs.gb_setup_save}"
					action="#{gradebookSetupBean.processSaveGradebookSetup}"/>
				<h:commandButton
					value="#{msgs.gb_setup_cancel}"
					action="#{gradebookSetupBean.processCancelGradebookSetup}" immediate="true"/>
			</div>
			
			<%
			  String thisId = request.getParameter("panel");
			  if (thisId == null) 
			  {
			    thisId = "Main" + org.sakaiproject.tool.cover.ToolManager.getCurrentPlacement().getId();
			  }
			%>
			<script type="text/javascript">
				function resize(){
	  				mySetMainFrameHeight('<%= org.sakaiproject.util.Web.escapeJavascript(thisId)%>');
	  		}
			</script> 
			
	  </h:form>
	</div>
</f:view>