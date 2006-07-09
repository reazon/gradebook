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

package org.sakaiproject.tool.gradebook.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.sakaiproject.component.section.support.IntegrationSupport;
import org.sakaiproject.component.section.support.UserManager;
import org.sakaiproject.tool.gradebook.Gradebook;

/**
 * Create a Gradebook for each site context in the database.
 */
public class TestGradebookLoader extends GradebookLoaderBase {
	private static final Log log = LogFactory.getLog(TestGradebookLoader.class);

    public static String GRADEBOOK_WITH_GRADES = "QA_6";

    static String ASN_BASE_NAME = "Homework #";
    static String EXTERNAL_ASN_NAME1 = "External Assessment #1";
    static String EXTERNAL_ASN_NAME2 = "External Assessment #2";
    static String ASN_NO_DUE_DATE_NAME = "Fl\u00F8ating Assignment (Due Whenever)";
    static String ASN_NOT_COUNTED_NAME = "Self-Assessment";

	protected IntegrationSupport integrationSupport;
	protected UserManager userManager;

	public TestGradebookLoader() {
    	// Don't roll these tests back, since they are intended to load data
		setDefaultRollback(false);
	}

	public IntegrationSupport getIntegrationSupport() {
		return integrationSupport;
	}
	public void setIntegrationSupport(IntegrationSupport integrationSupport) {
		this.integrationSupport = integrationSupport;
	}

	public void testLoadGradebooks() throws Exception {
        List gradebooks = new ArrayList();
        List gradebookUids = new ArrayList();

        // Create some gradebooks
        for(int i = 0; i < StandaloneSectionsDataLoader.SITE_UIDS.length; i++) {
        	String gradebookUid = StandaloneSectionsDataLoader.SITE_UIDS[i];
        	gradebookService.addGradebook(gradebookUid, StandaloneSectionsDataLoader.SITE_NAMES[i]);
            gradebookUids.add(gradebookUid);
        }

        // Fetch the gradebooks
        for(int i=0; i < StandaloneSectionsDataLoader.SITE_UIDS.length; i++) {
            gradebooks.add(gradebookManager.getGradebook((String)gradebookUids.get(i)));
        }

        // Add assignments for gradebook #6
        Gradebook gb = (Gradebook)gradebooks.get(5);
        for(int i = 0; i < 7; i++) {
        	int pts = (i + 1) * 10;
        	Date date = new Date();
            date.setTime(date.getTime() - ((6 - i) * 86400000));
            gradebookManager.createAssignment(gb.getId(), ASN_BASE_NAME + i, new Double(pts), date, Boolean.FALSE);
        }

        // Add an assignment without a due date.
        gradebookManager.createAssignment(gb.getId(), ASN_NO_DUE_DATE_NAME, new Double(50), null, Boolean.FALSE);

        // Add external assessments
        gradebookService.addExternalAssessment(gb.getUid(), EXTERNAL_ASN_NAME1, "samigo://external1", EXTERNAL_ASN_NAME1, 10, new Date(), "Test and Quiz");
        gradebookService.addExternalAssessment(gb.getUid(), EXTERNAL_ASN_NAME2, null, EXTERNAL_ASN_NAME2, 10, new Date(), "Test and Quiz");

        // Add an assignment which won't count towards the final grade.
        gradebookManager.createAssignment(gb.getId(), ASN_NOT_COUNTED_NAME, new Double(100), new Date(), Boolean.TRUE);

        // Ensure that this is actually saved to the database
        setComplete();
	}
}


