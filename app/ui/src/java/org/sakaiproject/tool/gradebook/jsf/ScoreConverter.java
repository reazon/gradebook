/**********************************************************************************
 *
 * $Id$
 *
 ***********************************************************************************
 *
 * Copyright (c) 2005, 2006, 2007 The Regents of the University of California, The MIT Corporation
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

package org.sakaiproject.tool.gradebook.jsf;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.tool.gradebook.ui.AssignmentGradeRow;
import org.sakaiproject.tool.gradebook.Gradebook;
import org.sakaiproject.service.gradebook.shared.GradebookService;

/**
 * This formatting-only converver consolidates the rather complex formatting
 * logic for the display of a student's score. The display
 * changes based upon the grade entry method.
 */
public class ScoreConverter extends PointsConverter {
	private static final Log log = LogFactory.getLog(ScoreConverter.class);

	public String getAsString(FacesContext context, UIComponent component, Object value) {
		if (log.isDebugEnabled()) log.debug("getAsString(" + context + ", " + component + ", " + value + ")");

		String formattedPtsPossible;
		String formattedScore;
		String gradeEntryMethod=null;
		final String POINTS = "points";
		final String PERCENT = "percent";
		final String LETTER = "letter";
		Object score = null;
		Object pointsPossible = null;
		Gradebook gradebook;
		boolean notCounted = true;
		boolean ungraded = false;
		boolean extraCredit = false;

		if (value != null) {
			if (value instanceof AssignmentGradeRow) {
				AssignmentGradeRow gradeRow = (AssignmentGradeRow) value;
				if (gradeRow.getAssociatedAssignment().isCounted())
					notCounted=false;
				if(gradeRow.getAssociatedAssignment().getUngraded()){
					ungraded = true;
				}
				if (gradeRow.getAssociatedAssignment().getIsExtraCredit()!=null)
					if (gradeRow.getAssociatedAssignment().getIsExtraCredit())
						extraCredit = true;
				gradebook = gradeRow.getGradebook();
				score = gradeRow.getScore();
				if (gradebook.getGrade_type() == GradebookService.GRADE_TYPE_POINTS) {
					gradeEntryMethod = POINTS;
					pointsPossible = gradeRow.getAssociatedAssignment().getPointsPossible();
				} else if (gradebook.getGrade_type() == GradebookService.GRADE_TYPE_PERCENTAGE) {
					gradeEntryMethod = PERCENT;
				} else if (gradebook.getGrade_type() == GradebookService.GRADE_TYPE_LETTER) {
					gradeEntryMethod = LETTER;
					score = gradeRow.getLetterScore();
				}
				// if weighting enabled, item is not counted if not assigned
				// a category
				if (!notCounted && gradebook.getCategory_type() == GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY) {
					notCounted = gradeRow.getAssociatedAssignment().getCategory() == null;
				}
			}
		}
		
		formattedScore = getFormattedValue(context, component, score);
		formattedPtsPossible = getFormattedValue(context, component, pointsPossible);
		
		if (score != null) {
			if (gradeEntryMethod.equals(POINTS)) {
				if (extraCredit)
					formattedScore = FacesUtil.getLocalizedString("overview_avg_display_points_adjustment", new String[] {formattedScore} );
				else
				formattedScore = FacesUtil.getLocalizedString("overview_avg_display_points", new String[] {formattedScore, formattedPtsPossible} );
			} else if (gradeEntryMethod.equals(PERCENT) && !ungraded) {
				formattedScore = FacesUtil.getLocalizedString("overview_avg_display_percent", new String[] {formattedScore} );
			}
			
			if (notCounted) {
				formattedScore = FacesUtil.getLocalizedString("score_not_counted",
						new String[] {formattedScore, FacesUtil.getLocalizedString("score_not_counted_tooltip")});
			}
		}
		return formattedScore;
	}
	
	private String getFormattedValue(FacesContext context, UIComponent component, Object value) {
		String formattedValue;
		if (value == null) {
			formattedValue = FacesUtil.getLocalizedString("score_null_placeholder");
		} else {
			if (value instanceof Number) {
				// Truncate to 2 decimal places.
				value = new Double(FacesUtil.getRoundDown(((Number)value).doubleValue(), 2));
				formattedValue = super.getAsString(context, component, value);
			} else {
				formattedValue = value.toString();
			}
		}
		
		return formattedValue;
	}
}
