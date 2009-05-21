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

package org.sakaiproject.tool.gradebook;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import org.sakaiproject.service.gradebook.shared.GradebookService;

/**
 * A CourseGradeRecord is a grade record that can be associated with a CourseGrade.
 *
 * @author <a href="mailto:jholtzman@berkeley.edu">Josh Holtzman</a>
 */
public class CourseGradeRecord extends AbstractGradeRecord {
    private String enteredGrade;
    private Double autoCalculatedGrade;  // Not persisted
    private String calculatedPointsEarned;	// Not persisted
    private Double adjustmentScore;

	public static Comparator<CourseGradeRecord> calcComparator;
    public static Comparator<CourseGradeRecord> calcComparatorIgnoreEnteredGrade;

    static {
        calcComparator = new Comparator<CourseGradeRecord>() {
            public int compare(CourseGradeRecord cgr1, CourseGradeRecord cgr2) {
                if((cgr1 == null || cgr2 == null) || (cgr1.getGradeAsPercentage() == null && cgr2.getGradeAsPercentage() == null)) {
                    return 0;
                }
                if(cgr1 == null || cgr1.getGradeAsPercentage() == null) {
                    return -1;
                }
                if(cgr2 == null || cgr2.getGradeAsPercentage() == null) {
                    return 1;
                }
                //SAK-12017 - Commented out as getPointsEarned is no longer an accurate comparator
                // due to nulls no longer being calculated in to the Course Grade
                //return cgr1.getPointsEarned().compareTo(cgr2.getPointsEarned());
                //   Better to use getGradeAsPercentage
                return cgr1.getGradeAsPercentage().compareTo(cgr2.getGradeAsPercentage());
            }
        };
    }
    
    static {
        calcComparatorIgnoreEnteredGrade = new Comparator<CourseGradeRecord>() {
            public int compare(CourseGradeRecord cgr1, CourseGradeRecord cgr2) {
                if((cgr1 == null || cgr2 == null) || (cgr1.getAutoCalculatedGrade() == null && cgr2.getAutoCalculatedGrade() == null)) {
                    return 0;
                }
                if(cgr1 == null || cgr1.getAutoCalculatedGrade() == null) {
                    return -1;
                }
                if(cgr2 == null || cgr2.getAutoCalculatedGrade() == null) {
                    return 1;
                }

                return cgr1.getAutoCalculatedGrade().compareTo(cgr2.getAutoCalculatedGrade());
            }
        };
    }

    public static Comparator<CourseGradeRecord> getOverrideComparator(final GradeMapping mapping) {
        return new Comparator<CourseGradeRecord>() {
            public int compare(CourseGradeRecord cgr1, CourseGradeRecord cgr2) {

                if(cgr1 == null && cgr2 == null) {
                    return 0;
                }
                if(cgr1 == null) {
                    return -1;
                }
                if(cgr2 == null) {
                    return 1;
                }

                String enteredGrade1 = StringUtils.trimToEmpty(cgr1.getEnteredGrade());
                String enteredGrade2 = StringUtils.trimToEmpty(cgr2.getEnteredGrade());
                
                // Grading scales are always defined in descending order.
                List<String> grades = mapping.getGradingScale().getGrades();
                int gradePos1 = -1;
                int gradePos2 = -1;
                for (int i = 0; (i < grades.size()) && ((gradePos1 == -1) || (gradePos2 == -1)); i++) {
                	String grade = grades.get(i);
                	if (grade.equals(enteredGrade1)) gradePos1 = i;
                	if (grade.equals(enteredGrade2)) gradePos2 = i;
                }
                return gradePos2 - gradePos1;
            }
        };

    }
    /**
     * The graderId and dateRecorded properties will be set explicitly by the
     * grade manager before the database is updated.
	 * @param courseGrade
	 * @param studentId
	 */
	public CourseGradeRecord(CourseGrade courseGrade, String studentId) {
        this.gradableObject = courseGrade;
        this.studentId = studentId;
	}

    /**
     * Default no-arg constructor
     */
    public CourseGradeRecord() {
        super();
    }

	/**
     * This method will fail unless this course grade was fetched "with statistics",
     * since it relies on having the total number of points possible available to
     * calculate the percentage.
     *
     * @see org.sakaiproject.tool.gradebook.AbstractGradeRecord#getGradeAsPercentage()
     */
    public Double getGradeAsPercentage() {
        if(enteredGrade == null) {
            return autoCalculatedGrade;
        } else {
            return getCourseGrade().getGradebook().getSelectedGradeMapping().getValue(enteredGrade);
        }
    }

    /**
     * Convenience method to get the correctly cast CourseGrade that this
     * CourseGradeRecord references.
     *
     * @return CourseGrade referenced by this GradableObject
     */
    public CourseGrade getCourseGrade() {
    	return (CourseGrade)super.getGradableObject();
    }
    /**
	 * @return Returns the enteredGrade.
	 */
	public String getEnteredGrade() {
		return enteredGrade;
	}
	/**
	 * @param enteredGrade The enteredGrade to set.
	 */
	public void setEnteredGrade(String enteredGrade) {
		this.enteredGrade = enteredGrade;
	}
	/**
	 * @return Returns the autoCalculatedGrade.
	 */
	public Double getAutoCalculatedGrade() {
		return autoCalculatedGrade;
	}

	public String getPointsEarned() {
		return calculatedPointsEarned;
	}

    /**
	 * @return Returns the displayGrade.
	 */
	public String getDisplayGrade() {
        if(enteredGrade != null) {
            return enteredGrade;
        } else {
            return getCourseGrade().getGradebook().getSelectedGradeMapping().getGrade(autoCalculatedGrade);
        }
	}

	/**
	 * @see org.sakaiproject.tool.gradebook.AbstractGradeRecord#isCourseGradeRecord()
	 */
	public boolean isCourseGradeRecord() {
		return true;
	}
	
    public Double getAdjustmentScore() {
		return adjustmentScore;
	}
	public void setAdjustmentScore(Double adjustmentScore) {
		this.adjustmentScore = adjustmentScore;
	}

	/**
	 * For use by the Course Grade UI.
	 */
	public Double getNonNullAutoCalculatedGrade() {
		Double percent = getAutoCalculatedGrade();
		if (percent == null) {
			percent = new Double(0);
		}
		return percent;
	}

	public void initNonpersistentFields(double totalPointsPossible, double totalPointsEarned, double courseGradePointsAdjustment) {
		Double percentageEarned;
		calculatedPointsEarned = new Double(totalPointsEarned).toString();
		BigDecimal bdTotalPointsPossible = new BigDecimal(totalPointsPossible);
		BigDecimal bdTotalPointsEarned = new BigDecimal(totalPointsEarned);
		BigDecimal bdCourseGradePointsAdjustment = new BigDecimal(courseGradePointsAdjustment);
		// this adds in the Course Grade Adjustment Score
		bdTotalPointsEarned = bdTotalPointsEarned.add(bdCourseGradePointsAdjustment);
		if (totalPointsPossible == 0.0) {
			percentageEarned = null;
		} else {
			percentageEarned = new Double(bdTotalPointsEarned.divide(bdTotalPointsPossible, GradebookService.MATH_CONTEXT).multiply(new BigDecimal("100")).doubleValue());
		}
		autoCalculatedGrade = percentageEarned;
	}

	public void initNonpersistentFields(double totalPointsPossible, double totalPointsEarned, double literalTotalPointsEarned, double courseGradePointsAdjustment, double adjustmentPointsEarned) {
		Double percentageEarned;
		//calculatedPointsEarned = totalPointsEarned;
		calculatedPointsEarned = new Double(literalTotalPointsEarned).toString();
		BigDecimal bdTotalPointsPossible = new BigDecimal(totalPointsPossible);
		BigDecimal bdTotalPointsEarned = new BigDecimal(totalPointsEarned);
		BigDecimal bdAdjustmentPointsEarned = new BigDecimal(adjustmentPointsEarned);
		BigDecimal bdCourseGradePointsAdjustment = new BigDecimal(courseGradePointsAdjustment);
		if (totalPointsPossible <= 0.0) {
			percentageEarned = null;
		} else {
			if (getCourseGrade().getGradebook().getGrade_type() == GradebookService.GRADE_TYPE_PERCENTAGE)
			{
				percentageEarned = new Double(bdTotalPointsEarned.divide(bdTotalPointsPossible, GradebookService.MATH_CONTEXT).multiply(new BigDecimal("100")).doubleValue());
				if (getCourseGrade().getGradebook().getCategory_type() != GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY)
				{
					percentageEarned += bdAdjustmentPointsEarned.multiply(new BigDecimal("100")).doubleValue();
				}
				percentageEarned += courseGradePointsAdjustment;
			}
			else
			{
				// if its a weighted category gradebook, divide by 100 to go in the pointsEarned correctly
				if (getCourseGrade().getGradebook().getCategory_type() == GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY)
					bdCourseGradePointsAdjustment = bdCourseGradePointsAdjustment.divide(new BigDecimal("100"));
				// this adds in the Course Grade Adjustment Score
				bdTotalPointsEarned = bdTotalPointsEarned.add(bdCourseGradePointsAdjustment);
				percentageEarned = new Double(bdTotalPointsEarned.divide(bdTotalPointsPossible, GradebookService.MATH_CONTEXT).multiply(new BigDecimal("100")).doubleValue());
			}
		}
		autoCalculatedGrade = percentageEarned;
	}
}
