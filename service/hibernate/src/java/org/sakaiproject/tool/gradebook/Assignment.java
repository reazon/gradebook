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

import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;

/**
 * An Assignment is the basic unit that composes a gradebook.  It represents a
 * single unit that, when aggregated in a gradebook, can be used as the
 * denomenator in calculating a CourseGradeRecord.
 *
 * @author <a href="mailto:jholtzman@berkeley.edu">Josh Holtzman</a>
 */
public class Assignment extends GradableObject {

    public static String SORT_BY_DATE = "dueDate";
    public static String SORT_BY_NAME = "name";
    public static String SORT_BY_MEAN = "mean";
    public static String SORT_BY_POINTS = "pointsPossible";
    public static String SORT_BY_RELEASED ="released";
    public static String DEFAULT_SORT = SORT_BY_DATE;

    public static Comparator dateComparator;
    public static Comparator nameComparator;
    public static Comparator pointsComparator;
    public static Comparator meanComparator;
    public static Comparator releasedComparator;

    private Double pointsPossible;
    private Date dueDate;
    private boolean notCounted;
    private boolean externallyMaintained;
    private String externalStudentLink;
    private String externalInstructorLink;
    private String externalId;
    private String externalAppName;
    private boolean released;

    static {
        dateComparator = new Comparator() {
            public int compare(Object o1, Object o2) {
                if(log.isDebugEnabled()) log.debug("Comparing assignment + " + o1 + " to " + o2 + " by date");
                Assignment one = (Assignment)o1;
                Assignment two = (Assignment)o2;

                // Sort by name if no date on either
                if(one.getDueDate() == null && two.getDueDate() == null) {
                    return one.getName().compareTo(two.getName());
                }

                // Null dates are last
                if(one.getDueDate() == null) {
                    return 1;
                }
                if(two.getDueDate() == null) {
                    return -1;
                }

                // Sort by name if both assignments have the same date
                int comp = (one.getDueDate().compareTo(two.getDueDate()));
                if(comp == 0) {
                    return one.getName().compareTo(two.getName());
                } else {
                    return comp;
                }
            }
        };
        nameComparator = new Comparator() {
			public int compare(Object o1, Object o2) {
                return ((Assignment)o1).getName().toLowerCase().compareTo(((Assignment)o2).getName().toLowerCase());
			}
        };
        pointsComparator = new Comparator() {
            public int compare(Object o1, Object o2) {
                if(log.isDebugEnabled()) log.debug("Comparing assignment + " + o1 + " to " + o2 + " by points");
                Assignment one = (Assignment)o1;
                Assignment two = (Assignment)o2;

                int comp = one.getPointsPossible().compareTo(two.getPointsPossible());
                if(comp == 0) {
                    return one.getName().compareTo(two.getName());
                } else {
                    return comp;
                }
            }
        };
        meanComparator = new Comparator() {
            public int compare(Object o1, Object o2) {
                if(log.isDebugEnabled()) log.debug("Comparing assignment + " + o1 + " to " + o2 + " by mean");
                Assignment one = (Assignment)o1;
                Assignment two = (Assignment)o2;

                Double mean1 = one.getMean();
                Double mean2 = two.getMean();
                if(mean1 == null && mean2 == null) {
                    return 0;
                }
                if(mean1 != null && mean2 == null) {
                    return 1;
                }
                if(mean1 == null && mean2 != null) {
                    return -1;
                }
                int comp = mean1.compareTo(mean2);
                if(comp == 0) {
                    return one.getName().compareTo(two.getName());
                } else {
                    return comp;
                }
            }
        };

        releasedComparator = new Comparator() {
            public int compare(Object o1, Object o2) {
                if(log.isDebugEnabled()) log.debug("Comparing assignment + " + o1 + " to " + o2 + " by release");
                Assignment one = (Assignment)o1;
                Assignment two = (Assignment)o2;

                int comp = String.valueOf(one.isReleased()).compareTo(String.valueOf(two.isReleased()));
                if(comp == 0) {
                    return one.getName().compareTo(two.getName());
                } else {
                    return comp;
                }
            }
        };
    }

    public Assignment(Gradebook gradebook, String name, Double pointsPossible, Date dueDate) {
        this.gradebook = gradebook;
        this.name = name;
        this.pointsPossible = pointsPossible;
        this.dueDate = dueDate;
        this.released = true;
    }


    /**
     * constructor to support selective release
     * @param gradebook
     * @param name
     * @param pointsPossible
     * @param dueDate
     * @param released
     */
    public Assignment(Gradebook gradebook, String name, Double pointsPossible, Date dueDate, boolean released) {
        this.gradebook = gradebook;
        this.name = name;
        this.pointsPossible = pointsPossible;
        this.dueDate = dueDate;
        this.released = released;
    }

    public Assignment() {
    	super();
    }

	/**
     */
    public boolean isCourseGrade() {
        return false;
    }

    /**
	 */
	public Double getPointsForDisplay() {
		return pointsPossible;
	}

	/**
	 */
	public Date getDateForDisplay() {
        return dueDate;
	}

	/**
	 * @return Returns the dueDate.
	 */
	public Date getDueDate() {
		return dueDate;
	}
	/**
	 * @param dueDate The dueDate to set.
	 */
	public void setDueDate(Date dueDate) {
		this.dueDate = dueDate;
	}
	/**
	 */
	public boolean isNotCounted() {
		return notCounted;
	}
	/**
	 */
	public void setNotCounted(boolean notCounted) {
		this.notCounted = notCounted;
	}
	/**
	 */
	public boolean isCounted() {
		return !isNotCounted();
	}
	/**
	 * This cover is for the benefit of JSF checkboxes.
	 */
	public void setCounted(boolean counted) {
		setNotCounted(!counted);
	}
	/**
	 * @return Returns the externalInstructorLink.
	 */
	public String getExternalInstructorLink() {
		return externalInstructorLink;
	}
	/**
	 * @param externalInstructorLink The externalInstructorLink to set.
	 */
	public void setExternalInstructorLink(String externalInstructorLink) {
		this.externalInstructorLink = externalInstructorLink;
	}
	/**
	 * @return Returns the externallyMaintained.
	 */
	public boolean isExternallyMaintained() {
		return externallyMaintained;
	}
	/**
	 * @param externallyMaintained The externallyMaintained to set.
	 */
	public void setExternallyMaintained(boolean externallyMaintained) {
		this.externallyMaintained = externallyMaintained;
	}
	/**
	 * @return Returns the externalStudentLink.
	 */
	public String getExternalStudentLink() {
		return externalStudentLink;
	}
	/**
	 * @param externalStudentLink The externalStudentLink to set.
	 */
	public void setExternalStudentLink(String externalStudentLink) {
		this.externalStudentLink = externalStudentLink;
	}
	/**
	 * @return Returns the pointsPossible.
	 */
	public Double getPointsPossible() {
		return pointsPossible;
	}
	/**
	 * @param pointsPossible The pointsPossible to set.
	 */
	public void setPointsPossible(Double pointsPossible) {
		this.pointsPossible = pointsPossible;
	}
	/**
	 * @return Returns the externalId.
	 */
	public String getExternalId() {
		return externalId;
	}
	/**
	 * @param externalId The externalId to set.
	 */
	public void setExternalId(String externalId) {
		this.externalId = externalId;
	}
	/**
	 * @return Returns the externalAppName.
	 */
	public String getExternalAppName() {
		return externalAppName;
	}
	/**
	 * @param externalAppName The externalAppName to set.
	 */
	public void setExternalAppName(String externalAppName) {
		this.externalAppName = externalAppName;
	}


    /**
     *
     * @return selective release true or false
     */

    public boolean isReleased() {
        return released;
    }

    /**
     *
     * @param released returns wther the assignment has been released to users
     */
    public void setReleased(boolean released) {
        this.released = released;
    }

    /**
     * returns the mean score for students with entered grades.
	 */
    protected Double calculateMean(Collection grades, int numEnrollments) {
        if(grades == null || grades.size() == 0) {
            return null;
        }

        // The numEnrollments argument is unused in this method.
        // Only actually entered grades count towards the calculation.
        int numScored = 0;
        double total = 0;
        for(Iterator iter = grades.iterator(); iter.hasNext();) {
            Double grade = (Double)iter.next();
            if(grade == null) {
                continue;
            }
            numScored++;
            total += grade.doubleValue();
        }
        if (numScored == 0) {
        	return null;
        }
        return new Double(total / numScored);
    }
}



