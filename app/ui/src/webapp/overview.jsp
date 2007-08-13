<f:view>
  <div class="portletBody">
	<h:form id="gbForm">
	  <t:aliasBean alias="#{bean}" value="#{overviewBean}">
		<%@include file="/inc/appMenu.jspf"%>
	  </t:aliasBean>

	  <sakai:flowState bean="#{overviewBean}" />

		<h2><h:outputText value="#{msgs.appmenu_overview}"/></h2>

		<div class="instruction">
			<h:outputText value="#{msgs.overview_instruction}" escape="false"/>
			<h:panelGroup rendered="#{overviewBean.userAbleToEditAssessments}">
				<f:verbatim><p></f:verbatim>
				<h:outputText value="#{overviewBean.gradeOptionSummary} "/>
				<h:commandLink action="feedbackOptions" immediate="true">
					<h:outputText value="#{msgs.overview_grade_option_change}"/>
				</h:commandLink>
				<f:verbatim></p></f:verbatim>
			</h:panelGroup>
		</div>

		<%@include file="/inc/globalMessages.jspf"%>

		<h4><h:outputText value="#{msgs.overview_assignments_title}"/></h4>
		<t:dataTable cellpadding="0" cellspacing="0"
			id="assignmentsTable"
			value="#{overviewBean.gradableObjects}"
			var="gradableObject"
			sortColumn="#{overviewBean.assignmentSortColumn}"
            sortAscending="#{overviewBean.assignmentSortAscending}"
            columnClasses="left,left,center,rightpadded,rightpadded,external"
            rowClasses="#{overviewBean.rowStyles}"
			styleClass="listHier narrowTable">
			<h:column>
				<f:facet name="header">
		            <t:commandSortHeader columnName="name" immediate="true" arrow="true">
		                <h:outputText value="#{msgs.overview_assignments_header_name}" />
		            </t:commandSortHeader>
		        </f:facet>

				<!-- Assignment / Assessment link -->
				<h:commandLink action="assignmentDetails" rendered="#{!gradableObject.courseGrade}">
					<h:outputText value="#{gradableObject.name}" />
					<f:param name="assignmentId" value="#{gradableObject.id}"/>
				</h:commandLink>

				<!-- Course grade link -->
				<h:commandLink action="courseGradeDetails" rendered="#{gradableObject.courseGrade}"  styleClass="courseGrade">
					<h:outputText value="#{msgs.course_grade_name}" />
				</h:commandLink>
			</h:column>
			<h:column>
				<f:facet name="header">
		            <t:commandSortHeader columnName="dueDate" immediate="true" arrow="true"
		            	propertyName="dueDate">
						<h:outputText value="#{msgs.overview_assignments_header_due_date}"/>
		            </t:commandSortHeader>
		        </f:facet>

				<h:outputText value="#{gradableObject.dueDate}" rendered="#{! gradableObject.courseGrade && gradableObject.dueDate != null}">
                    <gbx:convertDateTime/>
                </h:outputText>
				<h:outputText value="#{msgs.score_null_placeholder}" rendered="#{! gradableObject.courseGrade && gradableObject.dueDate == null}"/>
			</h:column>

            <h:column>
				<f:facet name="header">
                    <t:commandSortHeader columnName="released" immediate="true" arrow="true"
                    	propertyName="released">
                        <h:outputText value="#{msgs.overview_released}"/>
                    </t:commandSortHeader>
                </f:facet>
				<h:outputText value="#{msgs.overview_released_true}" rendered="#{!gradableObject.courseGrade && gradableObject.released == true }"/>
				<h:outputText value="#{msgs.overview_released_false}" rendered="#{!gradableObject.courseGrade && gradableObject.released == false}"/>

			</h:column>

            <h:column rendered="#{overviewBean.userAbleToGradeAll}">
				<f:facet name="header">
		            <t:commandSortHeader columnName="mean" immediate="true" arrow="true">
						<h:outputText value="#{msgs.overview_assignments_header_average}"/>
		            </t:commandSortHeader>
		        </f:facet>

				<h:outputText value="#{gradableObject.formattedMean}">
					<f:convertNumber type="percent" integerOnly="true" />
				</h:outputText>
			</h:column>
			<h:column>
				<f:facet name="header">
		            <t:commandSortHeader columnName="pointsPossible" immediate="true" arrow="true"
		            	propertyName="pointsPossible">
						<h:outputText value="#{msgs.overview_assignments_header_points}"/>
		            </t:commandSortHeader>
		        </f:facet>
				<h:outputText value="#{gradableObject}" escape="false" rendered="#{!gradableObject.courseGrade}">
					<f:converter converterId="org.sakaiproject.gradebook.jsf.converter.ASSIGNMENT_POINTS"/>
				</h:outputText>
				<h:outputText value="#{overviewBean.totalPoints}" escape="false" rendered="#{gradableObject.courseGrade}">
					<f:converter converterId="org.sakaiproject.gradebook.jsf.converter.POINTS"/>
				</h:outputText>
			</h:column>
			<h:column>
				<h:outputText value="from #{gradableObject.externalAppName}" rendered="#{! gradableObject.courseGrade && ! empty gradableObject.externalAppName}"/>
			</h:column>
		</t:dataTable>

	  </h:form>
	</div>
</f:view>
