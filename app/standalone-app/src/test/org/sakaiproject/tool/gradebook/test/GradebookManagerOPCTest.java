package org.sakaiproject.tool.gradebook.test;

import junit.framework.Assert;

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
import org.sakaiproject.section.api.coursemanagement.EnrollmentRecord;
import org.sakaiproject.section.api.facade.Role;
import org.sakaiproject.service.gradebook.shared.GradebookService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Date;
import java.util.Map;
import java.math.BigDecimal;

public class GradebookManagerOPCTest extends GradebookTestBase {
	private Long assgn1Long;
	private Long assgn3Long;
	private Long cate1Long;
	private Long cate2Long;
	
	protected void onSetUpInTransaction() throws Exception {
		super.onSetUpInTransaction();

		String className = this.getClass().getName();
		gradebookFrameworkService.addGradebook(className, className);
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		cate1Long = gradebookManager.createCategory(persistentGradebook.getId(), "cate 1", new Double(0.40), 0);
		cate2Long = gradebookManager.createCategory(persistentGradebook.getId(), "cate 2", new Double(0.60), 0);

		List list = (List) gradebookManager.getCategories(persistentGradebook.getId());

		for(int i=0; i<list.size(); i++)
		{
			Category cat = (Category) list.get(i);
			if(i == 0)
			{
				assgn1Long = gradebookManager.createAssignmentForCategory(persistentGradebook.getId(), cat.getId(), 
						cat.getName() + "_assignment_1", new Double(10.0), new Date(), new Boolean(false), new Boolean(true));
			}
			if(i == 1)
			{
				assgn3Long = gradebookManager.createAssignmentForCategory(persistentGradebook.getId(), cat.getId(), 
						cat.getName() + "_assignment_1", new Double(10.0), new Date(), new Boolean(false), new Boolean(true));
			}
			Long assign2 = gradebookManager.createAssignmentForCategory(persistentGradebook.getId(), cat.getId(), 
					cat.getName() + "_assignment_2", new Double(10.0), new Date(), new Boolean(false), new Boolean(true));
		}
	}

//change them into onSetUpInTransaction
//	public void testCreateGradebook() throws Exception {
//		// Create a gradebook
//		String className = this.getClass().getName();
//		gradebookFrameworkService.addGradebook(className, className);
//		setComplete();
//	}
//
//public void testCreateCategory() throws Exception{
//Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
//
//Long id1 = gradebookManager.createCategory(persistentGradebook.getId(), "cate 1", new Double(0.40), 0);
////test for name conflict      Long id2 = gradebookManager.createCategory(persistentGradebook.getId(), "cate 1", new Double(0), 0);
//Long id2 = gradebookManager.createCategory(persistentGradebook.getId(), "cate 2", new Double(0.60), 0);
//
////save data for testing getCategories. otherwise, it will be gone for next transaction.
//setComplete();
//
////System.out.println("category id1::" + id1.longValue());
////System.out.println("category id2::" + id2.longValue());
//}
//
//public void testCreateAssignmentForCategory() throws Exception{
//Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
//
//List list = (List) gradebookManager.getCategories(persistentGradebook.getId());
//
//for(int i=0; i<list.size(); i++)
//{
//	Category cat = (Category) list.get(i);
//	if(i == 0)
//	{
//		assgn1Long = gradebookManager.createAssignmentForCategory(persistentGradebook.getId(), cat.getId(), 
//				cat.getName() + "_assignment_1", new Double(10.0), new Date(), new Boolean(false), new Boolean(true));
//	}
//	if(i == 1)
//	{
//		assgn3Long = gradebookManager.createAssignmentForCategory(persistentGradebook.getId(), cat.getId(), 
//				cat.getName() + "_assignment_1", new Double(10.0), new Date(), new Boolean(false), new Boolean(true));
//	}
//	Long assign2 = gradebookManager.createAssignmentForCategory(persistentGradebook.getId(), cat.getId(), 
//			cat.getName() + "_assignment_2", new Double(10.0), new Date(), new Boolean(false), new Boolean(true));
//}
//setComplete();
//}
//

	public void testGetGradebook() throws Exception {
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());

//		System.out.println("grade_type::" + persistentGradebook.getGrade_type());
//		System.out.println("category_type::" + persistentGradebook.getCategory_type());

		Assert.assertTrue(persistentGradebook.getGrade_type() == 1);
		Assert.assertTrue(persistentGradebook.getCategory_type() == 1);
	}

	public void testGetCategories() throws Exception{
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());

		List list = (List) gradebookManager.getCategories(persistentGradebook.getId());

//		for(int i=0; i<list.size(); i++)
//		{
//		Category cat = (Category) list.get(i);
//		System.out.println("category::" + cat.getName());
//		System.out.println("category::" + cat.getId());
//		System.out.println("category::" + cat.getGradebook().getId().longValue());
//		System.out.println("category::" + cat.getWeight().longValue());
//		System.out.println("category::" + cat.getDrop_lowest());
//		}
		Assert.assertTrue(list.size() == 2);
		Assert.assertTrue(((Category)list.get(0)).getName().equals("cate 1"));
		Assert.assertTrue(((Category)list.get(1)).getName().equals("cate 2"));
	}

	public void testGetAssignmentsForCategory() throws Exception{
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());

		List list = (List) gradebookManager.getCategories(persistentGradebook.getId());

		for(int i=0; i<list.size(); i++)
		{
			Category cat = (Category) list.get(i);
			List asslist = (List) gradebookManager.getAssignmentsForCategory(cat.getId());
			for(int j=0; j<asslist.size(); j++)
			{
				Assignment as = (Assignment) asslist.get(j);
//				System.out.println("category::" + cat.getName() + "--assignment::" + as.getName());
				Assert.assertTrue(as.getName().equals(cat.getName() + "_assignment_" + new Integer(j+1).intValue()));
			}
		} 
	}

	public void testGetCategory() throws Exception{
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		Category cat1 = gradebookManager.getCategory(cate1Long);
		Category cat2 = gradebookManager.getCategory(cate2Long);
//		System.out.println(cat1 + "---" + cat1.getName());
//		System.out.println(cat2 + "---" + cat2.getName());
	}

	public void testUpdateCategory() throws Exception{
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		Category cat1 = gradebookManager.getCategory(cate1Long);
//		test for name conflicts with removed category    	Category cat2 = gradebookManager.getCategory(new Long(2));
//		gradebookManager.removeCategory(cat2.getId());
//		cat1.setName("cate 2");
		cat1.setName("cate-rename");
		gradebookManager.updateCategory(cat1);
		Category cat_after = gradebookManager.getCategory(cate1Long);
		Assert.assertTrue(cat_after.getName().equals("cate-rename"));
	}

	public void testRemoveCategory() throws Exception{
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		Category cat1 = gradebookManager.getCategory(cate1Long);
		gradebookManager.removeCategory(cate1Long);
		List list = (List) gradebookManager.getCategories(persistentGradebook.getId());
		List assignList = (List) gradebookManager.getAssignments(persistentGradebook.getId());
		Assert.assertTrue(list.size() == 1);
		Assert.assertTrue(assignList.size() == 4);
//		for(int i=0; i<list.size(); i++)
//		{
//		Category cat = (Category) list.get(i);
//		System.out.println("category::" + cat.getName());
//		System.out.println("category::" + cat.getId());
//		System.out.println("category::" + cat.getGradebook().getId().longValue());
//		System.out.println("category::" + cat.getWeight().longValue());
//		System.out.println("category::" + cat.getDrop_lowest());
//		}
	}

	public void testValidateCategoryWeighting() throws Exception{
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		Assert.assertTrue(gradebookManager.validateCategoryWeighting(persistentGradebook.getId()));

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		gradebookManager.createCategory(persistentGradebook.getId(), "cate 3", new Double(0), 0);
		List list = (List) gradebookManager.getCategories(persistentGradebook.getId());
		for(int i=0; i<list.size(); i++)
		{
			Category cat = (Category) list.get(i);
			cat.setWeight(1.0/list.size());
			gradebookManager.updateCategory(cat);
//			System.out.println(cat.getWeight().doubleValue());
		}
		Assert.assertTrue(gradebookManager.validateCategoryWeighting(persistentGradebook.getId()));
	}

	public void testUpdateAssignmentGradeRecords() throws Exception{
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		Assignment assign = gradebookManager.getAssignment(assgn1Long);

		//for percentage type
		persistentGradebook.setGrade_type(GradebookService.GRADE_TYPE_PERCENTAGE);
		assign.setPointsPossible(new Double(5));
		gradebookManager.updateAssignment(assign);

		List gradeRecords = generateGradeRecords(assign, 5);
		Collection studentUids = new ArrayList();
		studentUids.add("studentId1");
		studentUids.add("studentId2");
		studentUids.add("studentId3");
		studentUids.add("studentId4");
		studentUids.add("studentId5");
		List returnGradeRecords = gradebookManager.getAssignmentGradeRecords(assign, studentUids);
		List convertGradeRecords = new ArrayList();
		for(int i=0; i<returnGradeRecords.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord)returnGradeRecords.get(i);
//			System.out.println("student::" + agr.getStudentId() + "--assign::" + agr.getAssignment() + "--grade::" + agr.getPointsEarned());
			//agr.setPointsEarned(new Double((agr.getPointsEarned().doubleValue() * 0.9) / assign.getPointsPossible()));
			agr.setPointsEarned(new Double(90.0));
			convertGradeRecords.add(agr);
		}
		gradebookManager.updateAssignmentGradeRecords(assign, convertGradeRecords, GradebookService.GRADE_TYPE_PERCENTAGE);
//		System.out.println("after convert===============");
		returnGradeRecords = gradebookManager.getAssignmentGradeRecords(assign, studentUids);
		for(int i=0; i<returnGradeRecords.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord)returnGradeRecords.get(i);
			Assert.assertTrue((new BigDecimal(agr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP)).doubleValue() == (0.9 * (assign.getPointsPossible())));
//			System.out.println("student::" + agr.getStudentId() + "--assign::" + agr.getAssignment() + "--grade::" + agr.getPointsEarned());
//			System.out.println(new BigDecimal(agr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP));			
		}		
	}

	private List generateGradeRecords(Assignment go, int gradeRecordsToGenerate) {
		List records = new ArrayList();
		AssignmentGradeRecord record;
		for(int i = 1; i <= gradeRecordsToGenerate; i++) {
			record = new AssignmentGradeRecord();

			record.setPointsEarned(new Double(i));
			record.setGradableObject(go);
			record.setStudentId("studentId" + i);
			records.add(record);
		}
		gradebookManager.updateAssignmentGradeRecords(go, records, GradebookService.GRADE_TYPE_POINTS);

		return records;
	}

	public void testGetAssignmentGradeRecordsConverted() throws Exception{
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		Assignment assign = gradebookManager.getAssignment(assgn1Long);

		//for percentage type
		persistentGradebook.setGrade_type(GradebookService.GRADE_TYPE_PERCENTAGE);
		gradebookManager.updateGradebook(persistentGradebook);
		assign.setPointsPossible(new Double(5));
		gradebookManager.updateAssignment(assign);

		List gradeRecords = generateGradeRecords(assign, 5);
		Collection studentUids = new ArrayList();
		studentUids.add("studentId1");
		studentUids.add("studentId2");
		studentUids.add("studentId3");
		studentUids.add("studentId4");
		studentUids.add("studentId5");

		List returnGradeRecords = gradebookManager.getAssignmentGradeRecordsConverted(assign, studentUids);
		for(int i=0; i<returnGradeRecords.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord)returnGradeRecords.get(i);
			Assert.assertTrue((new BigDecimal(agr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP)).doubleValue() == new BigDecimal(((double)(i+1))/assign.getPointsPossible().doubleValue() * 100.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//			System.out.println(new BigDecimal(((double)(i+1))/assign.getPointsPossible().doubleValue()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//			System.out.println("student::" + agr.getStudentId() + "--assign::" + agr.getAssignment() + "--point possible::" + agr.getAssignment().getPointsPossible() + "--grade::" + agr.getPointsEarned());
		}
	}

//for testing internal calculation
//need add  public double getTotalPointsEarnedInternal(final Long gradebookId, final String studentId, Gradebook gradebook, List categories);
//and public double getTotalPointsInternal(final Long gradebookId, final Gradebook gradebook, final List categories, final String studentId);
//and public double getLiteralTotalPointsInternal(final Long gradebookId, final Gradebook gradebook, final List categories);
//into GradebookManager API.
//	public void testGetTotalPointsEarnedInternal() throws Exception{
//		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
//		Assignment assign = gradebookManager.getAssignment(assgn1Long);
//		Assignment assign2 = gradebookManager.getAssignment(assgn3Long);
//
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		assign.setPointsPossible(new Double(5));
//		gradebookManager.updateAssignment(assign);
//		List categories = gradebookManager.getCategories(persistentGradebook.getId());
//		Category cate = gradebookManager.getCategory(assign.getCategory().getId());
//
//		List gradeRecords = generateGradeRecords(assign, 5);
//		List graderRecords2 = generateGradeRecords(assign2, 5);
//
////		Double piontsEearned = gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId1", persistentGradebook, categories);
////		System.out.println(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId1", persistentGradebook, categories));
////		System.out.println(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId2", persistentGradebook, categories));
////		System.out.println(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId3", persistentGradebook, categories));
////		System.out.println(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId4", persistentGradebook, categories));
////		System.out.println(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId5", persistentGradebook, categories));
//		
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId1", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal( (1.0) / 5.0 * cate.getWeight().doubleValue() + (1.0) / 10.0 * 0.6)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId2", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal( (2.0) / 5.0 * cate.getWeight().doubleValue() + (2.0) / 10 * 0.6)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId3", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal( (3.0) / 5.0 * cate.getWeight().doubleValue() + (3.0) / 10 * 0.6)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId4", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal( (4.0) / 5.0 * cate.getWeight().doubleValue() + (4.0) / 10 * 0.6)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId5", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal( (5.0) / 5.0 * cate.getWeight().doubleValue() + (5.0) / 10 * 0.6)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId1", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(1.0+1.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId2", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(2.0+2.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId3", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(3.0+3.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId4", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(4.0+4.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId5", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(5.0+5.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId1", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(1.0+1.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId2", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(2.0+2.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId3", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(3.0+3.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId4", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(4.0+4.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId5", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(5.0+5.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//
//		//test for setting studentId1's assignment1 to null
//		((AssignmentGradeRecord)gradeRecords.get(0)).setPointsEarned(null);
//		gradebookManager.updateAssignmentGradeRecords(assign, gradeRecords, GradebookService.GRADE_TYPE_POINTS);
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId1", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal( (0) / 15 * cate.getWeight().doubleValue() + (1.0) / 10.0 * 0.6)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId1", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal( 1.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId1", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(1.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//
//
//		gradebookManager.removeCategory(cate.getId());
//		categories = gradebookManager.getCategories(persistentGradebook.getId());
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId1", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal( (1.0) / 10 * 0.6)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId2", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal( (2.0) / 10 *0.6)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId1", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(1.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId2", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(4.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId1", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(1.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId2", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(4.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		
//		//test for setting studentId1's assignment2 to null - studentId1 now hasn't taken any assignments now.
//		((AssignmentGradeRecord)graderRecords2.get(0)).setPointsEarned(null);
//		gradebookManager.updateAssignmentGradeRecords(assign, gradeRecords, GradebookService.GRADE_TYPE_POINTS);
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId1", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(-1).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId1", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(-1).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsEarnedInternal(persistentGradebook.getId(), "studentId1", persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(-1).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
//	}
//
//	public void testGetTotalPointsInternal() throws Exception{
//		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
//		Assignment assign = gradebookManager.getAssignment(assgn1Long);
//		Assignment assign2 = gradebookManager.getAssignment(assgn3Long);
//
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		assign.setPointsPossible(new Double(5));
//		gradebookManager.updateAssignment(assign);
//		List categories = gradebookManager.getCategories(persistentGradebook.getId());
//		Category cate = gradebookManager.getCategory(assign.getCategory().getId());
//
//		List gradeRecords = generateGradeRecords(assign, 5);
//		List graderRecords2 = generateGradeRecords(assign2, 5);
//
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId1"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(1.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId2"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(1.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId3"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(1.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId4"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(1.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId5"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(1.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId1"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(15.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId2"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(15.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId3"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(15.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId4"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(15.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId5"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(15.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId1"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(15.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId2"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(15.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId3"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(15.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId4"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(15.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId5"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(15.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//
//		//test for setting studentId1's assignment1 to null
//		((AssignmentGradeRecord)gradeRecords.get(0)).setPointsEarned(null);
//		gradebookManager.updateAssignmentGradeRecords(assign, gradeRecords, GradebookService.GRADE_TYPE_POINTS);
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId1"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(0.6)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId2"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(1.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//
//		gradebookManager.removeCategory(cate.getId());
//		categories = gradebookManager.getCategories(persistentGradebook.getId());
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId1"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(0.6)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId2"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(0.6)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId3"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(0.6)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId4"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(0.6)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId5"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(0.6)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId1"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(10.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());		
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId2"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(15.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId3"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(15.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId4"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(15.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId5"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(15.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId1"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(10.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());		
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId2"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(15.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId3"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(15.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId4"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(15.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId5"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(15.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		
//		//test for setting studentId1's assignment2 to null - studentId1 now hasn't taken any assignments now.
//		((AssignmentGradeRecord)graderRecords2.get(0)).setPointsEarned(null);
//		gradebookManager.updateAssignmentGradeRecords(assign, gradeRecords, GradebookService.GRADE_TYPE_POINTS);
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId1"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(-1)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId1"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(-1).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue()));
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories, "studentId1"))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(-1)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//	}
//
//	public void testGetLiteralTotalPointsInternal() throws Exception {
//		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
//		Assignment assign = gradebookManager.getAssignment(assgn1Long);
//		Assignment assign2 = gradebookManager.getAssignment(assgn3Long);
//
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		assign.setPointsPossible(new Double(5));
//		gradebookManager.updateAssignment(assign);
//		List categories = gradebookManager.getCategories(persistentGradebook.getId());
//		Category cate = gradebookManager.getCategory(assign.getCategory().getId());
//
//		Assert.assertTrue((new BigDecimal(gradebookManager.getLiteralTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(35.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getLiteralTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(35.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getLiteralTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(35.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		
//		gradebookManager.removeCategory(cate.getId());
//		categories = gradebookManager.getCategories(persistentGradebook.getId());
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getLiteralTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(20.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getLiteralTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(35.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getLiteralTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(35.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//		
//		gradebookManager.removeAssignment(assign.getId());
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getLiteralTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(20.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getLiteralTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(30.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//
//		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
//		gradebookManager.updateGradebook(persistentGradebook);
//		Assert.assertTrue((new BigDecimal(gradebookManager.getLiteralTotalPointsInternal(persistentGradebook.getId(), persistentGradebook, categories))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			(new BigDecimal(30.0)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//	}
	
	public void testGetPointsEarnedCourseGradeRecords() throws Exception{
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		Assignment assign = gradebookManager.getAssignment(assgn1Long);
		Assignment assign2 = gradebookManager.getAssignment(assgn3Long);

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		assign.setPointsPossible(new Double(5));
		gradebookManager.updateAssignment(assign);
		List categories = gradebookManager.getCategories(persistentGradebook.getId());
		Category cate = gradebookManager.getCategory(assign.getCategory().getId());
		List assignments = gradebookManager.getAssignments(persistentGradebook.getId());

		List gradeRecords = generateGradeRecords(assign, 5);
		List gradeRecords2 = generateGradeRecords(assign2, 5);
		for(int i=0; i<gradeRecords2.size(); i++)
		{
			gradeRecords.add(gradeRecords.get(i));
		}

		List uid = new ArrayList();
		uid.add("studentId1");
		uid.add("studentId2");
		uid.add("studentId3");
		uid.add("studentId4");
		uid.add("studentId5");
		Map studentIdMap = new HashMap();
		studentIdMap.put("studentId1", new Double(1.0));
		studentIdMap.put("studentId2", new Double(2.0));
		studentIdMap.put("studentId3", new Double(3.0));
		studentIdMap.put("studentId4", new Double(4.0));
		studentIdMap.put("studentId5", new Double(5.0));

		Map filteredGradesMap = new HashMap();
		gradeRecords = gradebookManager.getAllAssignmentGradeRecords(persistentGradebook.getId(), uid);
		gradebookManager.addToGradeRecordMap(filteredGradesMap, gradeRecords);

		CourseGrade courseGrade = gradebookManager.getCourseGrade(persistentGradebook.getId());
		List courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid, assignments, filteredGradesMap);

		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
//			Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
//			new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId()))) * 0.4 / 5.0 + ((Double)studentIdMap.get(cgr.getStudentId())) * 0.6 / 10.0 ).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId()))) * 2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId())) ) * 0.4 * 100 / 5.0 + ((Double)studentIdMap.get(cgr.getStudentId())) * 0.6 * 100 / 10.0 ).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
//System.out.println(cgr.getGradeAsPercentage() + "--" + cgr.getDisplayGrade() + "--" + new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId())) ) * 0.4 * 100 / 5.0 + ((Double)studentIdMap.get(cgr.getStudentId())) * 0.6 * 100 / 10.0 ).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		}

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid, assignments, filteredGradesMap);
		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
			Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId())).doubleValue() * 2 )).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId())).doubleValue() * 2) * 100 / 15.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		}

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid, assignments, filteredGradesMap);
		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
			Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId())).doubleValue() * 2)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal(((((Double)studentIdMap.get(cgr.getStudentId()))) * 2 ) * 100/ 15.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		}

		//test for setting studentId1's assignment1 to null
		for(int i=0; i<gradeRecords.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord)gradeRecords.get(i);
			if(agr.getAssignment().getId().equals(assgn1Long) && agr.getStudentId().equals("studentId1"))
				agr.setPointsEarned(null);
		}
		gradebookManager.updateAssignmentGradeRecords(assign, gradeRecords, GradebookService.GRADE_TYPE_POINTS);
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid, assignments, filteredGradesMap);
		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
			if(cgr.getStudentId().equals("studentId1"))
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((Double)studentIdMap.get(cgr.getStudentId()))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((Double)studentIdMap.get(cgr.getStudentId())) * 0.6 * 100 / 10.0 / 0.60).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
		}
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid, assignments, filteredGradesMap);
		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
			if(cgr.getStudentId().equals("studentId1"))
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId())).doubleValue() )).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId())).doubleValue()) * 100 / 10.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
		}
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid, assignments, filteredGradesMap);
		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
			if(cgr.getStudentId().equals("studentId1"))
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId())).doubleValue())).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId())).doubleValue() ) * 100 / 10.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
		}


		gradebookManager.removeCategory(cate.getId());
		categories = gradebookManager.getCategories(persistentGradebook.getId());

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid, assignments, filteredGradesMap);
		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
			Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal(((Double)studentIdMap.get(cgr.getStudentId()))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal(((Double)studentIdMap.get(cgr.getStudentId())) * 0.6 * 100 / 10.0 / 0.6).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		}

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid, assignments, filteredGradesMap);
		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
			if(!cgr.getStudentId().equals("studentId1"))
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((Double)studentIdMap.get(cgr.getStudentId())).doubleValue() * 2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId())).doubleValue() * 2) * 100 / 15.0 ).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			else
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((Double)studentIdMap.get(cgr.getStudentId())).doubleValue()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId())).doubleValue()) * 100 / 10.0 ).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
		}

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid, assignments, filteredGradesMap);
		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
			if(!cgr.getStudentId().equals("studentId1"))
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((Double)studentIdMap.get(cgr.getStudentId())).doubleValue() * 2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId())).doubleValue() * 2) * 100 / 15.0 ).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			else
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((Double)studentIdMap.get(cgr.getStudentId())).doubleValue()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId())).doubleValue()) * 100 / 10.0 ).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
		}
		
		//test for setting studentId1's assignment2 to null - studentId1 now hasn't taken any assignments now.
		for(int i=0; i<gradeRecords.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord)gradeRecords.get(i);
			if(agr.getAssignment().getId().equals(assgn3Long) && agr.getStudentId().equals("studentId1"))
				agr.setPointsEarned(null);
		}
		gradebookManager.updateAssignmentGradeRecords(assign, gradeRecords, GradebookService.GRADE_TYPE_POINTS);
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid, assignments, filteredGradesMap);
		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
			if(!cgr.getStudentId().equals("studentId1"))
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((Double)studentIdMap.get(cgr.getStudentId())).doubleValue()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId())).doubleValue()) *0.6 * 100 / 10.0 / 0.6).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			else
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(cgr.getGradeAsPercentage() == null);
			}
		}

		gradebookManager.updateAssignmentGradeRecords(assign, gradeRecords, GradebookService.GRADE_TYPE_POINTS);
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid, assignments, filteredGradesMap);
		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
			if(!cgr.getStudentId().equals("studentId1"))
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((Double)studentIdMap.get(cgr.getStudentId())).doubleValue() * 2 ).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId())).doubleValue()) * 2 * 100 / 15.0 ).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			else
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(cgr.getGradeAsPercentage() == null);
			}
		}

		gradebookManager.updateAssignmentGradeRecords(assign, gradeRecords, GradebookService.GRADE_TYPE_POINTS);
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid, assignments, filteredGradesMap);
		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
			if(!cgr.getStudentId().equals("studentId1"))
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((Double)studentIdMap.get(cgr.getStudentId())).doubleValue() * 2 ).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId())).doubleValue()) * 2 * 100 / 15.0 ).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			else
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(cgr.getGradeAsPercentage() == null);
			}
		}
	}

	public void testGetStudentCourseGradeRecord() throws Exception {
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		Assignment assign = gradebookManager.getAssignment(assgn1Long);
		Assignment assign2 = gradebookManager.getAssignment(assgn3Long);

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		assign.setPointsPossible(new Double(5));
		gradebookManager.updateAssignment(assign);
		List categories = gradebookManager.getCategories(persistentGradebook.getId());
		Category cate = gradebookManager.getCategory(assign.getCategory().getId());
		List assignments = gradebookManager.getAssignments(persistentGradebook.getId());

		List gradeRecords = generateGradeRecords(assign, 5);
		List gradeRecords2 = generateGradeRecords(assign2, 5);
		for(int i=0; i<gradeRecords2.size(); i++)
		{
			gradeRecords.add(gradeRecords2.get(i));
		}

		List uid = new ArrayList();
		uid.add("studentId1");
		uid.add("studentId2");
		uid.add("studentId3");
		uid.add("studentId4");
		uid.add("studentId5");
		Map studentIdMap = new HashMap();
		studentIdMap.put("studentId1", new Double(1.0));
		studentIdMap.put("studentId2", new Double(2.0));
		studentIdMap.put("studentId3", new Double(3.0));
		studentIdMap.put("studentId4", new Double(4.0));
		studentIdMap.put("studentId5", new Double(5.0));

		CourseGrade courseGrade = gradebookManager.getCourseGrade(persistentGradebook.getId());
		CourseGradeRecord cgr1 = (CourseGradeRecord) gradebookManager.getStudentCourseGradeRecord(persistentGradebook, "studentId1");
		CourseGradeRecord cgr2 = (CourseGradeRecord) gradebookManager.getStudentCourseGradeRecord(persistentGradebook, "studentId2");
		Assert.assertTrue(new BigDecimal(cgr1.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((((Double)studentIdMap.get(cgr1.getStudentId()))) + (((Double)studentIdMap.get(cgr1.getStudentId())))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cgr1.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((((Double)studentIdMap.get(cgr1.getStudentId()))) * 0.4 * 100 / 5 + (((Double)studentIdMap.get(cgr1.getStudentId()))) * 0.6 * 100 / 10).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cgr2.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((((Double)studentIdMap.get(cgr2.getStudentId()))) + (((Double)studentIdMap.get(cgr2.getStudentId())))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cgr2.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((((Double)studentIdMap.get(cgr2.getStudentId()))) * 0.4 * 100 / 5 + (((Double)studentIdMap.get(cgr2.getStudentId()))) * 0.6 * 100 / 10).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		cgr1 = (CourseGradeRecord) gradebookManager.getStudentCourseGradeRecord(persistentGradebook, "studentId1");
		cgr2 = (CourseGradeRecord) gradebookManager.getStudentCourseGradeRecord(persistentGradebook, "studentId2");
		Assert.assertTrue(new BigDecimal(cgr1.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((((Double)studentIdMap.get(cgr1.getStudentId()))) * 2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cgr1.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((((Double)studentIdMap.get(cgr1.getStudentId()))) * 2) * 100/ 15.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cgr2.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((((Double)studentIdMap.get(cgr2.getStudentId()))) * 2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cgr2.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((((Double)studentIdMap.get(cgr2.getStudentId()))) * 2) * 100/ 15.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		cgr1 = (CourseGradeRecord) gradebookManager.getStudentCourseGradeRecord(persistentGradebook, "studentId1");
		cgr2 = (CourseGradeRecord) gradebookManager.getStudentCourseGradeRecord(persistentGradebook, "studentId2");
		Assert.assertTrue(new BigDecimal(cgr1.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((((Double)studentIdMap.get(cgr1.getStudentId()))) * 2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cgr1.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((((Double)studentIdMap.get(cgr1.getStudentId()))) * 2) * 100/ 15.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cgr2.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((((Double)studentIdMap.get(cgr2.getStudentId()))) * 2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cgr2.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((((Double)studentIdMap.get(cgr2.getStudentId()))) * 2) *100 / 15.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

		//test for setting studentId1's assignment1 to null
		for(int i=0; i<gradeRecords.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord)gradeRecords.get(i);
			if(agr.getAssignment().getId().equals(assgn1Long) && agr.getStudentId().equals("studentId1"))
				agr.setPointsEarned(null);
		}
		gradebookManager.updateAssignmentGradeRecords(assign, gradeRecords, GradebookService.GRADE_TYPE_POINTS);
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		cgr1 = (CourseGradeRecord) gradebookManager.getStudentCourseGradeRecord(persistentGradebook, "studentId1");
		cgr2 = (CourseGradeRecord) gradebookManager.getStudentCourseGradeRecord(persistentGradebook, "studentId2");
		Assert.assertTrue(new BigDecimal(cgr1.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((((Double)studentIdMap.get(cgr1.getStudentId())))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cgr1.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((((Double)studentIdMap.get(cgr1.getStudentId()))) * 0.6 * 100 / 10 / 0.6).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cgr2.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((((Double)studentIdMap.get(cgr2.getStudentId()))) + (((Double)studentIdMap.get(cgr2.getStudentId())))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cgr2.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((((Double)studentIdMap.get(cgr2.getStudentId()))) * 0.4 * 100 / 5 + (((Double)studentIdMap.get(cgr2.getStudentId()))) * 0.6 * 100 / 10).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

		
		gradebookManager.removeCategory(cate.getId());
		categories = gradebookManager.getCategories(persistentGradebook.getId());

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		cgr1 = (CourseGradeRecord) gradebookManager.getStudentCourseGradeRecord(persistentGradebook, "studentId1");
		cgr2 = (CourseGradeRecord) gradebookManager.getStudentCourseGradeRecord(persistentGradebook, "studentId2");
		Assert.assertTrue(new BigDecimal(cgr1.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((((Double)studentIdMap.get(cgr1.getStudentId())) )).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cgr1.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((((Double)studentIdMap.get(cgr1.getStudentId()))) * 0.6 * 100 / 10 / 0.6).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cgr2.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((((Double)studentIdMap.get(cgr2.getStudentId())))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cgr2.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((((Double)studentIdMap.get(cgr2.getStudentId()))) * 0.6 * 100 / 10 / 0.6).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		cgr1 = (CourseGradeRecord) gradebookManager.getStudentCourseGradeRecord(persistentGradebook, "studentId1");
		cgr2 = (CourseGradeRecord) gradebookManager.getStudentCourseGradeRecord(persistentGradebook, "studentId2");
		Assert.assertTrue(new BigDecimal(cgr1.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((((Double)studentIdMap.get(cgr1.getStudentId())))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cgr1.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((((Double)studentIdMap.get(cgr1.getStudentId()))) * 100 / 10).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cgr2.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((((Double)studentIdMap.get(cgr2.getStudentId()))) * 2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cgr2.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((((Double)studentIdMap.get(cgr2.getStudentId()))) * 2.0)* 100.0/ 15.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		cgr1 = (CourseGradeRecord) gradebookManager.getStudentCourseGradeRecord(persistentGradebook, "studentId1");
		cgr2 = (CourseGradeRecord) gradebookManager.getStudentCourseGradeRecord(persistentGradebook, "studentId2");
		Assert.assertTrue(new BigDecimal(cgr1.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((((Double)studentIdMap.get(cgr1.getStudentId())))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cgr1.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((((Double)studentIdMap.get(cgr1.getStudentId())))) * 100.0 / 10.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cgr2.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((((Double)studentIdMap.get(cgr2.getStudentId()))) * 2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cgr2.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((((Double)studentIdMap.get(cgr2.getStudentId()))) * 2.0)* 100.0/ 15.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

	}

	public void testGetPointsEarnedCourseGradeRecords2Params() throws Exception {
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		Assignment assign = gradebookManager.getAssignment(assgn1Long);
		Assignment assign2 = gradebookManager.getAssignment(assgn3Long);

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		assign.setPointsPossible(new Double(5));
		gradebookManager.updateAssignment(assign);
		List categories = gradebookManager.getCategories(persistentGradebook.getId());
		Category cate = gradebookManager.getCategory(assign.getCategory().getId());
		List assignments = gradebookManager.getAssignments(persistentGradebook.getId());

		List gradeRecords = generateGradeRecords(assign, 5);
		List gradeRecords2 = generateGradeRecords(assign2, 5);
		for(int i=0; i<gradeRecords2.size(); i++)
		{
			gradeRecords.add(gradeRecords2.get(i));
		}

		List uid = new ArrayList();
		uid.add("studentId1");
		uid.add("studentId2");
		uid.add("studentId3");
		uid.add("studentId4");
		uid.add("studentId5");
		Map studentIdMap = new HashMap();
		studentIdMap.put("studentId1", new Double(1.0));
		studentIdMap.put("studentId2", new Double(2.0));
		studentIdMap.put("studentId3", new Double(3.0));
		studentIdMap.put("studentId4", new Double(4.0));
		studentIdMap.put("studentId5", new Double(5.0));

		gradeRecords = gradebookManager.getAllAssignmentGradeRecords(persistentGradebook.getId(), uid);

		CourseGrade courseGrade = gradebookManager.getCourseGrade(persistentGradebook.getId());
		List courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid);

		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
			Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId()))) + (((Double)studentIdMap.get(cgr.getStudentId())))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId()))) * 0.4 * 100 / 5 + (((Double)studentIdMap.get(cgr.getStudentId()))) * 0.6 * 100 / 10).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		}

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid);
		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
			Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId()))) * 2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal(((((Double)studentIdMap.get(cgr.getStudentId()))) * 2) * 100 / 15.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		}

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid);
		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
			Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId()))) * 2 ).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal(((((Double)studentIdMap.get(cgr.getStudentId()))) * 2) * 100 / 15.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		}

		//test for setting studentId1's assignment1 to null
		for(int i=0; i<gradeRecords.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord)gradeRecords.get(i);
			if(agr.getAssignment().getId().equals(assgn1Long) && agr.getStudentId().equals("studentId1"))
				agr.setPointsEarned(null);
		}
		gradebookManager.updateAssignmentGradeRecords(assign, gradeRecords, GradebookService.GRADE_TYPE_POINTS);
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid);
		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
			if(!cgr.getStudentId().equals("studentId1"))
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId()))) + (((Double)studentIdMap.get(cgr.getStudentId())))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId()))) * 0.4 * 100 / 5 + (((Double)studentIdMap.get(cgr.getStudentId()))) * 0.6 * 100 / 10).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			else
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId())))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId()))) * 0.6 * 100 / 10 / 0.6).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
		}
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid);
		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
			if(!cgr.getStudentId().equals("studentId1"))
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId()))) * 2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((((Double)studentIdMap.get(cgr.getStudentId()))) * 2) * 100 / 15.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			else
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((Double)studentIdMap.get(cgr.getStudentId())).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((((Double)studentIdMap.get(cgr.getStudentId())))) * 100 / 10.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());				
			}
		}
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid);
		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
			if(!cgr.getStudentId().equals("studentId1"))
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId()))) * 2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((((Double)studentIdMap.get(cgr.getStudentId()))) * 2) * 100 / 15.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			else
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((Double)studentIdMap.get(cgr.getStudentId())).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((((Double)studentIdMap.get(cgr.getStudentId())))) * 100 / 10.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());				
			}
		}

		
		gradebookManager.removeCategory(cate.getId());
		categories = gradebookManager.getCategories(persistentGradebook.getId());

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid);
		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
			Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId())))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId()))) * 0.6  * 100 / 10 / 0.6).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		}

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid);
		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
			if(!cgr.getStudentId().equals("studentId1"))
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((Double)studentIdMap.get(cgr.getStudentId()) * 2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((Double)studentIdMap.get(cgr.getStudentId()) * 2) * 100 / 15.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			else
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((Double)studentIdMap.get(cgr.getStudentId())).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((Double)studentIdMap.get(cgr.getStudentId())) * 100 / 10.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
		}

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid);
		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
			if(!cgr.getStudentId().equals("studentId1"))
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((Double)studentIdMap.get(cgr.getStudentId()) * 2).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((Double)studentIdMap.get(cgr.getStudentId()) * 2) * 100 / 15.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			else
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((Double)studentIdMap.get(cgr.getStudentId())).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((Double)studentIdMap.get(cgr.getStudentId())) * 100 / 10.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
		}
		
		//test for setting studentId1's assignment2 to null - studentId1 now hasn't taken any assignments now.
		for(int i=0; i<gradeRecords.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord)gradeRecords.get(i);
			if(agr.getAssignment().getId().equals(assgn3Long) && agr.getStudentId().equals("studentId1"))
				agr.setPointsEarned(null);
		}
		gradebookManager.updateAssignmentGradeRecords(assign, gradeRecords, GradebookService.GRADE_TYPE_POINTS);
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecords(courseGrade, uid);
		for(int i=0; i<courseGradeRecords.size(); i++)
		{
			CourseGradeRecord cgr = (CourseGradeRecord) courseGradeRecords.get(i);
			if(!cgr.getStudentId().equals("studentId1"))
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId())))).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cgr.getGradeAsPercentage()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((((Double)studentIdMap.get(cgr.getStudentId()))) * 0.6 * 100 / 10 / 0.6).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			else
			{
				Assert.assertTrue(new BigDecimal(cgr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(cgr.getGradeAsPercentage() == null);
			}
		}
	}

	public void testGetTotalPoints() throws Exception {
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		Assignment assign = gradebookManager.getAssignment(assgn1Long);

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		assign.setPointsPossible(new Double(5));
		gradebookManager.updateAssignment(assign);

		Double total = gradebookManager.getTotalPoints(persistentGradebook.getId());
		Assert.assertTrue(new BigDecimal(total).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(35.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		total = gradebookManager.getTotalPoints(persistentGradebook.getId());
		Assert.assertTrue(new BigDecimal(total).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(35.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		total = gradebookManager.getTotalPoints(persistentGradebook.getId());
		Assert.assertTrue(new BigDecimal(total).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(35.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

		gradebookManager.removeCategory(cate1Long);

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		total = gradebookManager.getTotalPoints(persistentGradebook.getId());
		Assert.assertTrue(new BigDecimal(total).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(20.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		total = gradebookManager.getTotalPoints(persistentGradebook.getId());
		Assert.assertTrue(new BigDecimal(total).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(5 + 10 + 10 + 10).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		total = gradebookManager.getTotalPoints(persistentGradebook.getId());
		Assert.assertTrue(new BigDecimal(total).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(5 + 10 + 10 + 10).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
	}

	public void testUpdateAssignmentGradesAndComments() throws Exception{
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		Assignment assign = gradebookManager.getAssignment(assgn1Long);

		//for percentage type
		persistentGradebook.setGrade_type(GradebookService.GRADE_TYPE_PERCENTAGE);
		assign.setPointsPossible(new Double(5));
		gradebookManager.updateAssignment(assign);

		List gradeRecords = generateGradeRecords(assign, 5);
		Collection studentUids = new ArrayList();
		studentUids.add("studentId1");
		studentUids.add("studentId2");
		studentUids.add("studentId3");
		studentUids.add("studentId4");
		studentUids.add("studentId5");
		List returnGradeRecords = gradebookManager.getAssignmentGradeRecords(assign, studentUids);
		List convertGradeRecords = new ArrayList();
		for(int i=0; i<returnGradeRecords.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord)returnGradeRecords.get(i);
			agr.setPointsEarned(90.0);
			convertGradeRecords.add(agr);
		}
		Collection comments = new ArrayList();
		for(int i=0; i<convertGradeRecords.size(); i++)
		{
			Comment cm = new Comment();
			cm.setCommentText(new String("comment--" + (i+1)));
			cm.setDateRecorded(new Date());
			cm.setGradableObject(assign);
			cm.setGraderId("admin_test");
			cm.setStudentId("studentId" + (i+1));
			comments.add(cm);
		}
		gradebookManager.updateAssignmentGradesAndComments(assign, convertGradeRecords, comments);
		returnGradeRecords = gradebookManager.getAssignmentGradeRecords(assign, studentUids);
		List commentReturned = gradebookManager.getComments(assign, studentUids);
		for(int i=0; i<returnGradeRecords.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord)returnGradeRecords.get(i);
			Assert.assertTrue((new BigDecimal(agr.getPointsEarned()).setScale(2, BigDecimal.ROUND_HALF_UP)).doubleValue() == (0.9 * agr.getAssignment().getPointsPossible().doubleValue()));
			Assert.assertTrue((((Comment)commentReturned.get(i)).getCommentText()).equals("comment--" + (i+1)));
//			System.out.println((((Comment)commentReturned.get(i)).getCommentText()));
		}
	}
	
	public void testGetPointsEarnedCourseGradeRecordsWithStats() throws Exception {
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		Assignment assign = gradebookManager.getAssignment(assgn1Long);
		Assignment assign2 = gradebookManager.getAssignment(assgn3Long);

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
  	integrationSupport.createCourse(persistentGradebook.getUid(), persistentGradebook.getUid(), false, false, false);
		gradebookManager.updateGradebook(persistentGradebook);
		assign.setPointsPossible(new Double(5));
		gradebookManager.updateAssignment(assign);
		List categories = gradebookManager.getCategories(persistentGradebook.getId());
		Category cate = gradebookManager.getCategory(assign.getCategory().getId());
		List assignments = gradebookManager.getAssignments(persistentGradebook.getId());

		List gradeRecords = generateGradeRecords(assign, 5);
		List gradeRecords2 = generateGradeRecords(assign2, 5);
		for(int i=0; i<gradeRecords2.size(); i++)
		{
			gradeRecords.add(gradeRecords2.get(i));
		}

		List uid = new ArrayList();
		uid.add("studentId1");
		uid.add("studentId2");
		uid.add("studentId3");
		uid.add("studentId4");
		uid.add("studentId5");
		Map studentIdMap = new HashMap();
		studentIdMap.put("studentId1", new Double(1.0));
		studentIdMap.put("studentId2", new Double(2.0));
		studentIdMap.put("studentId3", new Double(3.0));
		studentIdMap.put("studentId4", new Double(4.0));
		studentIdMap.put("studentId5", new Double(5.0));

		gradeRecords = gradebookManager.getAllAssignmentGradeRecords(persistentGradebook.getId(), uid);

		addUsersEnrollments(persistentGradebook, uid);
		CourseGrade courseGrade = gradebookManager.getCourseGrade(persistentGradebook.getId());
		List courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecordsWithStats(courseGrade, uid);
		Assert.assertTrue(new BigDecimal(courseGrade.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((1 + 2 + 3 + 4 + 5) * 0.4 / 5 / 5+ (1 + 2 + 3 + 4 + 5) * 0.6 / 10 / 5) * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecordsWithStats(courseGrade, uid);
		Assert.assertTrue(new BigDecimal(courseGrade.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((1.0 + 2 + 3 + 4 + 5 + 1 + 2 + 3 + 4 + 5) / 15.0 / 5.0) * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecordsWithStats(courseGrade, uid);
		Assert.assertTrue(new BigDecimal(courseGrade.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((1.0 + 2 + 3 + 4 + 5 + 1 + 2 + 3 + 4 + 5) / 15.0 / 5.0) * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		
		//test for setting studentId1's assignment1 to null
		for(int i=0; i<gradeRecords.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord)gradeRecords.get(i);
			if(agr.getAssignment().getId().equals(assgn1Long) && agr.getStudentId().equals("studentId1"))
				agr.setPointsEarned(null);
		}
		gradebookManager.updateAssignmentGradeRecords(assign, gradeRecords, GradebookService.GRADE_TYPE_POINTS);
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecordsWithStats(courseGrade, uid);
		Assert.assertTrue(new BigDecimal(courseGrade.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((1.0/10.0 ) + (2.0/5.0*0.4 + 2.0/10.0*0.6) + (3.0/5.0*0.4 + 3.0/10.0*0.6) + (4.0/5.0*0.4 + 4.0/10.0*0.6) + (5.0/5.0*0.4 + 5.0/10.0*0.6)) / 5.0 * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		
		gradebookManager.removeCategory(cate.getId());
		categories = gradebookManager.getCategories(persistentGradebook.getId());
		
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecordsWithStats(courseGrade, uid);
		Assert.assertTrue(new BigDecimal(courseGrade.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((1.0/10.0 ) + (2.0/10.0) + (3.0/10.0) + (4.0/10.0) + (5.0/10.0)) / 5.0 * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecordsWithStats(courseGrade, uid);
		Assert.assertTrue(new BigDecimal(courseGrade.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((1.0/10.0) + (4.0/15) + (6.0/15.0) + (8.0/15.0) + (10.0/15.0)) / 5.0 * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecordsWithStats(courseGrade, uid);
		Assert.assertTrue(new BigDecimal(courseGrade.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((1.0/10.0) + (4.0/15) + (6.0/15.0) + (8.0/15.0) + (10.0/15.0)) / 5.0 * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

		//test for setting studentId1's assignment2 to null - studentId1 now hasn't taken any assignments now.
		for(int i=0; i<gradeRecords.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord)gradeRecords.get(i);
			if(agr.getAssignment().getId().equals(assgn3Long) && agr.getStudentId().equals("studentId1"))
				agr.setPointsEarned(null);
		}
		gradebookManager.updateAssignmentGradeRecords(assign, gradeRecords, GradebookService.GRADE_TYPE_POINTS);
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecordsWithStats(courseGrade, uid);
		Assert.assertTrue(new BigDecimal(courseGrade.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((2.0/10.0) + (3.0/10.0) + (4.0/10.0) + (5.0/10.0)) / 4.0 * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecordsWithStats(courseGrade, uid);
		Assert.assertTrue(new BigDecimal(courseGrade.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((4.0/15) + (6.0/15.0) + (8.0/15.0) + (10.0/15.0)) / 4.0 * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		courseGradeRecords = gradebookManager.getPointsEarnedCourseGradeRecordsWithStats(courseGrade, uid);
		Assert.assertTrue(new BigDecimal(courseGrade.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((4.0/15) + (6.0/15.0) + (8.0/15.0) + (10.0/15.0)) / 4.0 * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
	}

	public void testGetAssignmentsWithStats() throws Exception{
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		Assignment assign = gradebookManager.getAssignment(assgn1Long);
		Assignment assign2 = gradebookManager.getAssignment(assgn3Long);

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		integrationSupport.createCourse(persistentGradebook.getUid(), persistentGradebook.getUid(), false, false, false);
		gradebookManager.updateGradebook(persistentGradebook);
		assign.setPointsPossible(new Double(5));
		gradebookManager.updateAssignment(assign);
		List categories = gradebookManager.getCategories(persistentGradebook.getId());
		Category cate = gradebookManager.getCategory(assign.getCategory().getId());
		List assignments = gradebookManager.getAssignments(persistentGradebook.getId());

		List gradeRecords = generateGradeRecords(assign, 5);
		List gradeRecords2 = generateGradeRecords(assign2, 5);
		for(int i=0; i<gradeRecords2.size(); i++)
		{
			gradeRecords.add(gradeRecords2.get(i));
		}

		List uid = new ArrayList();
		uid.add("studentId1");
		uid.add("studentId2");
		uid.add("studentId3");
		uid.add("studentId4");
		uid.add("studentId5");
		Map studentIdMap = new HashMap();
		studentIdMap.put("studentId1", new Double(1.0));
		studentIdMap.put("studentId2", new Double(2.0));
		studentIdMap.put("studentId3", new Double(3.0));
		studentIdMap.put("studentId4", new Double(4.0));
		studentIdMap.put("studentId5", new Double(5.0));

		addUsersEnrollments(persistentGradebook, uid);
		List assgnsWithStats = gradebookManager.getAssignmentsWithStats(persistentGradebook.getId(),  Assignment.DEFAULT_SORT, true);

		for(int i=0; i<assgnsWithStats.size(); i++)
		{
			Assignment as = (Assignment) assgnsWithStats.get(i);
			if(as.getMean() != null)
			{
				Assert.assertTrue(new BigDecimal(as.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((1 + 2 + 3 + 4 + 5) / as.getPointsPossible().doubleValue() / 5) * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(as.getAverageTotal()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((1 + 2 + 3 + 4 + 5) / 5).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
		}
		
		//test for setting studentId1's assignment1 to null
		for(int i=0; i<gradeRecords.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord)gradeRecords.get(i);
			if(agr.getAssignment().getId().equals(assgn1Long) && agr.getStudentId().equals("studentId1"))
				agr.setPointsEarned(null);
		}
		gradebookManager.updateAssignmentGradeRecords(assign, gradeRecords, GradebookService.GRADE_TYPE_POINTS);
		assgnsWithStats = gradebookManager.getAssignmentsWithStats(persistentGradebook.getId(),  Assignment.DEFAULT_SORT, true);
		for(int i=0; i<assgnsWithStats.size(); i++)
		{
			Assignment as = (Assignment) assgnsWithStats.get(i);
			if(as.getMean() != null)
			{
				if(as.getId().equals(assgn1Long))
				{
					Assert.assertTrue(new BigDecimal(as.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
						new BigDecimal(((2.0 + 3 + 4 + 5) / as.getPointsPossible().doubleValue() / 4.0) * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
					Assert.assertTrue(new BigDecimal(as.getAverageTotal()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
						new BigDecimal((2.0 + 3 + 4 + 5) / 4.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				}
				else
				{
					Assert.assertTrue(new BigDecimal(as.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
						new BigDecimal(((1 + 2.0 + 3 + 4 + 5) / as.getPointsPossible().doubleValue() / 5.0) * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
					Assert.assertTrue(new BigDecimal(as.getAverageTotal()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
						new BigDecimal((1 + 2.0 + 3 + 4 + 5) / 5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				}
			}
		}
	}

	public void testGetAssignmentsAndCourseGradeWithStats() throws Exception{
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		Assignment assign = gradebookManager.getAssignment(assgn1Long);
		Assignment assign2 = gradebookManager.getAssignment(assgn3Long);

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		integrationSupport.createCourse(persistentGradebook.getUid(), persistentGradebook.getUid(), false, false, false);
		gradebookManager.updateGradebook(persistentGradebook);
		assign.setPointsPossible(new Double(5));
		gradebookManager.updateAssignment(assign);
		List categories = gradebookManager.getCategories(persistentGradebook.getId());
		Category cate = gradebookManager.getCategory(assign.getCategory().getId());
		List assignments = gradebookManager.getAssignments(persistentGradebook.getId());

		List gradeRecords = generateGradeRecords(assign, 5);
		List gradeRecords2 = generateGradeRecords(assign2, 5);
		for(int i=0; i<gradeRecords2.size(); i++)
		{
			gradeRecords.add(gradeRecords2.get(i));
		}

		List uid = new ArrayList();
		uid.add("studentId1");
		uid.add("studentId2");
		uid.add("studentId3");
		uid.add("studentId4");
		uid.add("studentId5");
		Map studentIdMap = new HashMap();
		studentIdMap.put("studentId1", new Double(1.0));
		studentIdMap.put("studentId2", new Double(2.0));
		studentIdMap.put("studentId3", new Double(3.0));
		studentIdMap.put("studentId4", new Double(4.0));
		studentIdMap.put("studentId5", new Double(5.0));

		addUsersEnrollments(persistentGradebook, uid);
		List assgnsWithStats = gradebookManager.getAssignmentsAndCourseGradeWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true);

		for(int i=0; i<(assgnsWithStats.size() - 1); i++)
		{
			Assignment as = (Assignment) assgnsWithStats.get(i);
			if(as.getMean() != null)
			{
				Assert.assertTrue(new BigDecimal(as.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((1.0 + 2 + 3 + 4 + 5) / as.getPointsPossible().doubleValue() / 5.0) * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(as.getAverageTotal()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((1.0 + 2 + 3 + 4 + 5) / 5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
		}
		CourseGrade cg = (CourseGrade) assgnsWithStats.get(assgnsWithStats.size() - 1);
		Assert.assertTrue(new BigDecimal(cg.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((1.0 + 2 + 3 + 4 + 5) * 0.4 / 5.0 / 5.0 + (1.0 + 2 + 3 + 4 + 5) * 0.6 / 10.0 / 5.0 ) * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cg.getAverageScore()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((1.0 + 2 + 3 + 4 + 5 + 1.0 + 2 + 3 + 4 + 5) / 5.0 )).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		assgnsWithStats = gradebookManager.getAssignmentsAndCourseGradeWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true);
		cg = (CourseGrade) assgnsWithStats.get(assgnsWithStats.size() - 1);
		Assert.assertTrue(new BigDecimal(cg.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((1.0 + 2 + 3 + 4 + 5 + 1.0 + 2 + 3 + 4 + 5) / 15.0 / 5.0 ) * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		assgnsWithStats = gradebookManager.getAssignmentsAndCourseGradeWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true);
		cg = (CourseGrade) assgnsWithStats.get(assgnsWithStats.size() - 1);
		Assert.assertTrue(new BigDecimal(cg.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((1.0 + 2 + 3 + 4 + 5 + 1.0 + 2 + 3 + 4 + 5) / 15.0 / 5.0 ) * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		
		//test for setting studentId1's assignment1 to null
		for(int i=0; i<gradeRecords.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord)gradeRecords.get(i);
			if(agr.getAssignment().getId().equals(assgn1Long) && agr.getStudentId().equals("studentId1"))
				agr.setPointsEarned(null);
		}
		gradebookManager.updateAssignmentGradeRecords(assign, gradeRecords, GradebookService.GRADE_TYPE_POINTS);
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		assgnsWithStats = gradebookManager.getAssignmentsAndCourseGradeWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true);
		for(int i=0; i<(assgnsWithStats.size() - 1); i++)
		{
			Assignment as = (Assignment) assgnsWithStats.get(i);
			if(as.getMean() != null && !as.getId().equals(assgn1Long))
			{
				Assert.assertTrue(new BigDecimal(as.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((1.0 + 2 + 3 + 4 + 5) / as.getPointsPossible().doubleValue() / 5.0) * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(as.getAverageTotal()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((1.0 + 2 + 3 + 4 + 5) / 5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			else if(as.getMean() != null)
			{
				Assert.assertTrue(new BigDecimal(as.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((2.0 + 3 + 4 + 5) / as.getPointsPossible().doubleValue() / 4.0) * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(as.getAverageTotal()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((2.0 + 3 + 4 + 5) / 4.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
		}
		cg = (CourseGrade) assgnsWithStats.get(assgnsWithStats.size() - 1);
		Assert.assertTrue(new BigDecimal(cg.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((1.0/10.0) + (2.0/5.0*0.4 + 2.0/10.0*0.6) + (3.0/5.0*0.4 + 3.0/10.0*0.6) + (4.0/5.0*0.4 + 4.0/10.0*0.6) + (5.0/5.0*0.4 + 5.0/10.0*0.6)) *100.0/ 5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		assgnsWithStats = gradebookManager.getAssignmentsAndCourseGradeWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true);
		cg = (CourseGrade) assgnsWithStats.get(assgnsWithStats.size() - 1);
		Assert.assertTrue(new BigDecimal(cg.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((1.0/10.0) + (4.0/15.0) + (6.0/15.0) + (8.0/15.0) + (10.0/15.0)) *100.0/ 5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cg.getAverageScore()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((2 + 3 + 4 + 5 + 1.0 + 2 + 3 + 4 + 5) / 5.0 )).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		assgnsWithStats = gradebookManager.getAssignmentsAndCourseGradeWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true);
		cg = (CourseGrade) assgnsWithStats.get(assgnsWithStats.size() - 1);
		Assert.assertTrue(new BigDecimal(cg.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((1.0/10.0) + (4.0/15.0) + (6.0/15.0) + (8.0/15.0) + (10.0/15.0)) *100.0/ 5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cg.getAverageScore()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((2 + 3 + 4 + 5 + 1.0 + 2 + 3 + 4 + 5) / 5.0 )).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		
		//test for setting studentId1's assignment2 to null - studentId1 doesn't have any scores now
		for(int i=0; i<gradeRecords.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord)gradeRecords.get(i);
			if(agr.getAssignment().getId().equals(assgn3Long) && agr.getStudentId().equals("studentId1"))
				agr.setPointsEarned(null);
		}
		gradebookManager.updateAssignmentGradeRecords(assign, gradeRecords, GradebookService.GRADE_TYPE_POINTS);
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		assgnsWithStats = gradebookManager.getAssignmentsAndCourseGradeWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true);
		for(int i=0; i<(assgnsWithStats.size() - 1); i++)
		{
			Assignment as = (Assignment) assgnsWithStats.get(i);
			if(as.getMean() != null && !as.getId().equals(assgn1Long) && !as.getId().equals(assgn3Long))
			{
				Assert.assertTrue(new BigDecimal(as.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((1.0 + 2 + 3 + 4 + 5) / as.getPointsPossible().doubleValue() / 5.0) * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(as.getAverageTotal()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((1.0 + 2 + 3 + 4 + 5) / 5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			else if(as.getMean() != null)
			{
				Assert.assertTrue(new BigDecimal(as.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((2.0 + 3 + 4 + 5) / as.getPointsPossible().doubleValue() / 4.0) * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(as.getAverageTotal()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((2.0 + 3 + 4 + 5) / 4.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
		}
		cg = (CourseGrade) assgnsWithStats.get(assgnsWithStats.size() - 1);
		Assert.assertTrue(new BigDecimal(cg.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((2.0/5.0*0.4 + 2.0/10.0*0.6) + (3.0/5.0*0.4 + 3.0/10.0*0.6) + (4.0/5.0*0.4 + 4.0/10.0*0.6) + (5.0/5.0*0.4 + 5.0/10.0*0.6)) *100.0/ 4.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cg.getAverageScore()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((2 + 3 + 4 + 5 + 2.0 + 3 + 4 + 5) / 4.0 )).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		assgnsWithStats = gradebookManager.getAssignmentsAndCourseGradeWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true);
		cg = (CourseGrade) assgnsWithStats.get(assgnsWithStats.size() - 1);
		Assert.assertTrue(new BigDecimal(cg.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((4.0/15.0) + (6.0/15.0) + (8.0/15.0) + (10.0/15.0)) *100.0/ 4.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cg.getAverageScore()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((2 + 3 + 4 + 5 + 2.0 + 3 + 4 + 5) / 4.0 )).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		assgnsWithStats = gradebookManager.getAssignmentsAndCourseGradeWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true);
		cg = (CourseGrade) assgnsWithStats.get(assgnsWithStats.size() - 1);
		Assert.assertTrue(new BigDecimal(cg.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((4.0/15.0) + (6.0/15.0) + (8.0/15.0) + (10.0/15.0)) *100.0/ 4.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cg.getAverageScore()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((2 + 3 + 4 + 5 + 2.0 + 3 + 4 + 5) / 4.0 )).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
	}

	public void testGetAssignmentWithStats() throws Exception{
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		Assignment assign = gradebookManager.getAssignment(assgn1Long);
		Assignment assign2 = gradebookManager.getAssignment(assgn3Long);

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		integrationSupport.createCourse(persistentGradebook.getUid(), persistentGradebook.getUid(), false, false, false);
		gradebookManager.updateGradebook(persistentGradebook);
		assign.setPointsPossible(new Double(5));
		gradebookManager.updateAssignment(assign);
		List categories = gradebookManager.getCategories(persistentGradebook.getId());
		Category cate = gradebookManager.getCategory(assign.getCategory().getId());
		List assignments = gradebookManager.getAssignments(persistentGradebook.getId());

		List gradeRecords = generateGradeRecords(assign, 5);
		List gradeRecords2 = generateGradeRecords(assign2, 5);
		for(int i=0; i<gradeRecords2.size(); i++)
		{
			gradeRecords.add(gradeRecords2.get(i));
		}

		List uid = new ArrayList();
		uid.add("studentId1");
		uid.add("studentId2");
		uid.add("studentId3");
		uid.add("studentId4");
		uid.add("studentId5");
		Map studentIdMap = new HashMap();
		studentIdMap.put("studentId1", new Double(1.0));
		studentIdMap.put("studentId2", new Double(2.0));
		studentIdMap.put("studentId3", new Double(3.0));
		studentIdMap.put("studentId4", new Double(4.0));
		studentIdMap.put("studentId5", new Double(5.0));

		addUsersEnrollments(persistentGradebook, uid);

		Assignment as = gradebookManager.getAssignmentWithStats(assign.getId());
		if(as.getMean() != null)
		{
			Assert.assertTrue(new BigDecimal(as.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal(((1.0 + 2 + 3 + 4 + 5) / as.getPointsPossible().doubleValue() / 5.0) * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			Assert.assertTrue(new BigDecimal(as.getAverageTotal()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal((1.0 + 2 + 3 + 4 + 5) / 5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		}
		as = gradebookManager.getAssignmentWithStats(assign2.getId());
		if(as.getMean() != null)
		{
			Assert.assertTrue(new BigDecimal(as.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal(((1.0 + 2 + 3 + 4 + 5) / as.getPointsPossible().doubleValue() / 5.0) * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			Assert.assertTrue(new BigDecimal(as.getAverageTotal()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal((1.0 + 2 + 3 + 4 + 5) / 5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		}
		
		//test for setting studentId1's assignment1 to null
		for(int i=0; i<gradeRecords.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord)gradeRecords.get(i);
			if(agr.getAssignment().getId().equals(assgn1Long) && agr.getStudentId().equals("studentId1"))
				agr.setPointsEarned(null);
		}
		gradebookManager.updateAssignmentGradeRecords(assign, gradeRecords, GradebookService.GRADE_TYPE_POINTS);
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		as = gradebookManager.getAssignmentWithStats(assign.getId());
		if(as.getMean() != null)
		{
			Assert.assertTrue(new BigDecimal(as.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal(((2.0 + 3 + 4 + 5) / as.getPointsPossible().doubleValue() / 4.0) * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			Assert.assertTrue(new BigDecimal(as.getAverageTotal()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal((2.0 + 3 + 4 + 5) / 4.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		}
		as = gradebookManager.getAssignmentWithStats(assign2.getId());
		if(as.getMean() != null)
		{
			Assert.assertTrue(new BigDecimal(as.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal(((1.0 + 2 + 3 + 4 + 5) / as.getPointsPossible().doubleValue() / 5.0) * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			Assert.assertTrue(new BigDecimal(as.getAverageTotal()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal((1.0 + 2 + 3 + 4 + 5) / 5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		}

	}

	public void testGetCategoriesWithStats() throws Exception{
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		Assignment assign = gradebookManager.getAssignment(assgn1Long);
		Assignment assign2 = gradebookManager.getAssignment(assgn3Long);

		integrationSupport.createCourse(persistentGradebook.getUid(), persistentGradebook.getUid(), false, false, false);
		gradebookManager.updateGradebook(persistentGradebook);
		assign.setPointsPossible(new Double(5));
		gradebookManager.updateAssignment(assign);
		List categories = gradebookManager.getCategories(persistentGradebook.getId());

		List gradeRecords = generateGradeRecords(assign, 5);
		List gradeRecords2 = generateGradeRecords(assign2, 5);
		for(int i=0; i<gradeRecords2.size(); i++)
		{
			gradeRecords.add(gradeRecords2.get(i));
		}

		List uid = new ArrayList();
		uid.add("studentId1");
		uid.add("studentId2");
		uid.add("studentId3");
		uid.add("studentId4");
		uid.add("studentId5");

		addUsersEnrollments(persistentGradebook, uid);

		List cateList = gradebookManager.getCategoriesWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true, Category.SORT_BY_NAME, true);
		List tempList = gradebookManager.getAssignmentsAndCourseGradeWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true);
		CourseGrade cg1 = (CourseGrade) tempList.get(tempList.size() - 1);

		for(int i=0; i<cateList.size(); i++)
		{
			if(i == (cateList.size() -1))
			{
				CourseGrade cg = (CourseGrade) cateList.get(i); 
				Assert.assertTrue(new BigDecimal(cg.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(((2.0 / 15.0 ) + (4.0 / 15.0) + (6.0/15.0) + (8.0/15.0) + (10.0/ 15.0)) * 100 / 5).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			else
			{
				Category cat = (Category) cateList.get(i);
				if(i == 0)
				{
					Assert.assertTrue(new BigDecimal(cat.getAverageTotalPoints()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
						new BigDecimal(5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
					Assert.assertTrue(new BigDecimal(cat.getAverageScore()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
						new BigDecimal((1.0 + 2 + 3 + 4 + 5) / 5.0 ).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
					Assert.assertTrue(new BigDecimal(cat.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
						new BigDecimal((1.0+ 2 + 3+ 4 +5.0) / 5.0 / 5.0 * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				}
				else if(i == 1)
				{
					Assert.assertTrue(new BigDecimal(cat.getAverageTotalPoints()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
						new BigDecimal(10.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
					Assert.assertTrue(new BigDecimal(cat.getAverageScore()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
						new BigDecimal((1.0 + 2 + 3 + 4 + 5) / 5.0 ).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
					Assert.assertTrue(new BigDecimal(cat.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
						new BigDecimal((1.0+ 2 + 3+ 4 +5.0) / 5.0 / 10.0 * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				}

				List assignList = cat.getAssignmentList();
				for(int j=0; j<assignList.size(); j++)
				{
					Assignment assi= (Assignment) assignList.get(j);
					if(assi.getId().equals(assgn1Long))
					{
						Assert.assertTrue(new BigDecimal(assi.getAverageTotal()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
							new BigDecimal((1.0+2+3+4+5) / 5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
						Assert.assertTrue(new BigDecimal(assi.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
							new BigDecimal((1.0+2+3+4+5) / 5.0 / 5.0 * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
					}
					if(assi.getId().equals(assgn3Long))
					{
						Assert.assertTrue(new BigDecimal(assi.getAverageTotal()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
							new BigDecimal((1.0+2+3+4+5) / 5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
						Assert.assertTrue(new BigDecimal(assi.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
							new BigDecimal((1.0+2+3+4+5) / 5.0 / 10.0 * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
					}
					//System.out.println(assi.getAverageTotal() + "==" + assi.getMean());
				}
			}
		}
		
		//test for setting studentId1's assignment1 to null
		for(int i=0; i<gradeRecords.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord)gradeRecords.get(i);
			if(agr.getAssignment().getId().equals(assgn1Long) && agr.getStudentId().equals("studentId1"))
				agr.setPointsEarned(null);
		}
		gradebookManager.updateAssignmentGradeRecords(assign, gradeRecords, GradebookService.GRADE_TYPE_POINTS);
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		cateList = gradebookManager.getCategoriesWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true, Category.SORT_BY_NAME, true);

		CourseGrade cg = (CourseGrade) cateList.get(cateList.size() - 1);
		Assert.assertTrue(new BigDecimal(cg.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((1.0/10.0 ) + (2.0/5.0 * 0.4 + 2.0 / 10.0 * 0.6) + (3.0/5.0 * 0.4 + 3.0 / 10.0 * 0.6) + (4.0/5.0 * 0.4 + 4.0 / 10.0 * 0.6) + (5.0/5.0 * 0.4 + 5.0 / 10.0 * 0.6)) * 100 / 5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		
		for(int i=0; i<(cateList.size() - 1); i++)
		{
			Category cat = (Category) cateList.get(i);
			if(i == 0)
			{
				Assert.assertTrue(new BigDecimal(cat.getAverageTotalPoints()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cat.getAverageScore()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((2.0 + 3 + 4 + 5) / 4.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cat.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((2 + 3+ 4 +5.0) / 5.0 / 4.0 * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			else if(i == 1)
			{
				Assert.assertTrue(new BigDecimal(cat.getAverageTotalPoints()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(10.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cat.getAverageScore()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((1.0 + 2.0 + 3 + 4 + 5) / 5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cat.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((1.0+ 2 + 3+ 4 +5.0) / 5.0 / 10.0 * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			
			List assignList = cat.getAssignmentList();
			for(int j=0; j<assignList.size(); j++)
			{
				Assignment assi= (Assignment) assignList.get(j);
				if(assi.getId().equals(assgn1Long))
				{
					Assert.assertTrue(new BigDecimal(assi.getAverageTotal()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
						new BigDecimal((2+3+4+5) / 4.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
					Assert.assertTrue(new BigDecimal(assi.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
						new BigDecimal((2+3+4+5) / 4.0 / 5.0 * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				}
				if(assi.getId().equals(assgn3Long))
				{
					Assert.assertTrue(new BigDecimal(assi.getAverageTotal()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
						new BigDecimal((1.0+2+3+4+5) / 5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
					Assert.assertTrue(new BigDecimal(assi.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
						new BigDecimal((1.0+2+3+4+5) / 5.0 / 10.0 * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				}
			}
		}
		
		//test for non-released assignment
		Category cateWithNonRleased = (Category)cateList.get(0);
		gradebookManager.createAssignmentForCategory(persistentGradebook.getId(), cateWithNonRleased.getId(), 
				cateWithNonRleased.getName() + "_assignment_non_released", new Double(10.0), new Date(), new Boolean(false), new Boolean(false));
		cateList = gradebookManager.getCategoriesWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true, Category.SORT_BY_NAME, true);
		List assignListWithNonReleased = cateWithNonRleased.getAssignmentList();
		Assert.assertTrue(new BigDecimal(cateWithNonRleased.getAverageTotalPoints()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(new BigDecimal(cateWithNonRleased.getAverageScore()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal((2.0 + 3 + 4 + 5) / 4.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		Assert.assertTrue(assignListWithNonReleased.size() == 3);
		for(int i=0; i<assignListWithNonReleased.size(); i++)
		{
			Assignment testAsignmentNonReleased = (Assignment)assignListWithNonReleased.get(i);
			if(i == 2)
				Assert.assertTrue(testAsignmentNonReleased.getName().equals(cateWithNonRleased.getName() + "_assignment_non_released"));
		}
		
		cg = (CourseGrade) cateList.get(cateList.size() - 1);
		Assert.assertTrue(new BigDecimal(cg.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
			new BigDecimal(((1.0/10.0 ) + (2.0/5.0 * 0.4 + 2.0 / 10.0 * 0.6) + (3.0/5.0 * 0.4 + 3.0 / 10.0 * 0.6) + (4.0/5.0 * 0.4 + 4.0 / 10.0 * 0.6) + (5.0/5.0 * 0.4 + 5.0 / 10.0 * 0.6)) * 100 / 5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		
		//test for setting one assignment to be not included in course grade
		assign.setNotCounted(true);
		gradebookManager.updateAssignment(assign);
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		cateList = gradebookManager.getCategoriesWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true, Category.SORT_BY_NAME, true);
		for(int i=0; i<(cateList.size() - 1); i++)
		{
			Category cat = (Category) cateList.get(i);
			if(i == 0)
			{
				Assert.assertTrue(cat.getAverageTotalPoints() == null);
				Assert.assertTrue(cat.getAverageScore() == null);
				Assert.assertTrue(cat.getMean() == null);
			}
			else if(i == 1)
			{
				Assert.assertTrue(new BigDecimal(cat.getAverageTotalPoints()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(10.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cat.getAverageScore()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((1.0 + 2.0 + 3 + 4 + 5) / 5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cat.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((1.0+ 2 + 3+ 4 +5.0) / 5.0 / 10.0 * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
		}
		//add another assignment to cate_1 for test 
		Category cate1 = (Category)cateList.get(0);
		Long assignAddedId = gradebookManager.createAssignmentForCategory(persistentGradebook.getId(), cate1.getId(), 
				cate1.getName() + "_assignment_added", new Double(10.0), new Date(), new Boolean(false), new Boolean(true));
		generateGradeRecords(gradebookManager.getAssignment(assignAddedId), 5);
		cateList = gradebookManager.getCategoriesWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true, Category.SORT_BY_NAME, true);
		for(int i=0; i<(cateList.size() - 1); i++)
		{
			Category cat = (Category) cateList.get(i);
			if(i == 0)
			{
				Assert.assertTrue(new BigDecimal(cat.getAverageTotalPoints()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(10.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cat.getAverageScore()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((1.0 + 2.0 + 3 + 4 + 5) / 5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cat.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((1.0 + 2 + 3+ 4 +5.0) / 10.0 / 5.0 * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
			else if(i == 1)
			{
				Assert.assertTrue(new BigDecimal(cat.getAverageTotalPoints()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal(10.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cat.getAverageScore()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((1.0 + 2.0 + 3 + 4 + 5) / 5.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
				Assert.assertTrue(new BigDecimal(cat.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((1.0+ 2 + 3+ 4 +5.0) / 5.0 / 10.0 * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			}
		}		
	}
	
	public void testGetAssignmentsWithNoCategory() throws Exception
	{
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		Long assignId1 = gradebookManager.createAssignment(persistentGradebook.getId(), "no_cate_1", new Double(10), new Date(), new Boolean(false), new Boolean(true));
		Long assignId2 = gradebookManager.createAssignment(persistentGradebook.getId(), "no_cate_2", new Double(9), new Date(), new Boolean(false), new Boolean(true));
		List assigns = gradebookManager.getAssignmentsWithNoCategory(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true);
		
		Assert.assertTrue(assigns.size() == 2);
		Assert.assertTrue(assignId1.longValue() == ((Assignment)assigns.get(0)).getId().longValue());
		Assert.assertTrue(assignId2.longValue() == ((Assignment)assigns.get(1)).getId().longValue());

		assigns = gradebookManager.getAssignmentsWithNoCategory(persistentGradebook.getId(), Assignment.SORT_BY_POINTS, true);
		Assert.assertTrue(assignId1.longValue() == ((Assignment)assigns.get(1)).getId().longValue());
		Assert.assertTrue(assignId2.longValue() == ((Assignment)assigns.get(0)).getId().longValue());

		assigns = gradebookManager.getAssignmentsWithNoCategory(persistentGradebook.getId(), Assignment.SORT_BY_POINTS, false);
		Assert.assertTrue(assignId1.longValue() == ((Assignment)assigns.get(0)).getId().longValue());
		Assert.assertTrue(assignId2.longValue() == ((Assignment)assigns.get(1)).getId().longValue());
	}
	
	public void testGetCategoriesWithSorting() throws Exception
	{
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		List cateList = gradebookManager.getCategoriesWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true, Category.SORT_BY_NAME, true);
		for(int i=0; i<(cateList.size() - 1); i++)
		{
			Category cat = (Category) cateList.get(i);
			if(i == 0)
				Assert.assertTrue(cat.getName().equals("cate 1"));
			if(i == 1)
				Assert.assertTrue(cat.getName().equals("cate 2"));
		}

		cateList = gradebookManager.getCategoriesWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true, Category.SORT_BY_NAME, false);
		for(int i=0; i<(cateList.size() - 1); i++)
		{
			Category cat = (Category) cateList.get(i);
			if(i == 0)
				Assert.assertTrue(cat.getName().equals("cate 2"));
			if(i == 1)
				Assert.assertTrue(cat.getName().equals("cate 1"));
		}
		
		Assignment assign = gradebookManager.getAssignment(assgn1Long);
		Assignment assign2 = gradebookManager.getAssignment(assgn3Long);
		integrationSupport.createCourse(persistentGradebook.getUid(), persistentGradebook.getUid(), false, false, false);
		gradebookManager.updateGradebook(persistentGradebook);
		assign.setPointsPossible(new Double(5));
		gradebookManager.updateAssignment(assign);
		List categories = gradebookManager.getCategories(persistentGradebook.getId());
		List gradeRecords = generateGradeRecords(assign, 5);
		List gradeRecords2 = generateGradeRecords(assign2, 5);
		for(int i=0; i<gradeRecords2.size(); i++)
		{
			gradeRecords.add(gradeRecords2.get(i));
		}
		List uid = new ArrayList();
		uid.add("studentId1");
		uid.add("studentId2");
		uid.add("studentId3");
		uid.add("studentId4");
		uid.add("studentId5");
		addUsersEnrollments(persistentGradebook, uid);
		for(int i=0; i<gradeRecords.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord)gradeRecords.get(i);
			if(agr.getAssignment().getId().equals(assgn1Long) && agr.getStudentId().equals("studentId1"))
				agr.setPointsEarned(null);
		}
		gradebookManager.updateAssignmentGradeRecords(assign, gradeRecords, GradebookService.GRADE_TYPE_POINTS);
		cateList = gradebookManager.getCategoriesWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true, Category.SORT_BY_AVERAGE_SCORE, true);
		for(int i=0; i<(cateList.size() - 1); i++)
		{
			Category cat = (Category) cateList.get(i);
			if(i == 0)
				Assert.assertTrue(cat.getAverageScore().equals((double)((1.0+2+3+4+5))/ 5.0));				
			if(i == 1)
				Assert.assertTrue(cat.getAverageScore().equals((double)((2.0+3+4+5)/4.0)));
		}
		
		cateList = gradebookManager.getCategoriesWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true, Category.SORT_BY_AVERAGE_SCORE, false);
		for(int i=0; i<(cateList.size() - 1); i++)
		{
			Category cat = (Category) cateList.get(i);
			if(i == 0)
				Assert.assertTrue(cat.getAverageScore().equals((double)((2.0+3+4+5)/4.0)));
			if(i == 1)
				Assert.assertTrue(cat.getAverageScore().equals((double)((1.0+2+3+4+5))/ 5.0));
		}
		
		cateList = gradebookManager.getCategoriesWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true, Category.SORT_BY_WEIGHT, true);
		for(int i=0; i<(cateList.size() - 1); i++)
		{
			Category cat = (Category) cateList.get(i);
			if(i == 0)
				Assert.assertTrue(cat.getWeight().equals((double)0.4));
			if(i == 1)
				Assert.assertTrue(cat.getWeight().equals((double)0.6));
	
		}
		
		cateList = gradebookManager.getCategoriesWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true, Category.SORT_BY_WEIGHT, false);
		for(int i=0; i<(cateList.size() - 1); i++)
		{
			Category cat = (Category) cateList.get(i);
			if(i == 0)
				Assert.assertTrue(cat.getWeight().equals((double)0.6));
			if(i == 1)
				Assert.assertTrue(cat.getWeight().equals((double)0.4));
		}
	}
	
	public void testGetAssignmentsWithNoCategoryWithStats() throws Exception
	{
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		
		Long assignId1 = gradebookManager.createAssignment(persistentGradebook.getId(), "no_cate_1", new Double(10), new Date(), new Boolean(false), new Boolean(true));
		Long assignId2 = gradebookManager.createAssignment(persistentGradebook.getId(), "no_cate_2", new Double(9), new Date(), new Boolean(false), new Boolean(true));

		Assignment assign = gradebookManager.getAssignment(assgn1Long);
		Assignment assign2 = gradebookManager.getAssignment(assgn3Long);
		integrationSupport.createCourse(persistentGradebook.getUid(), persistentGradebook.getUid(), false, false, false);
		gradebookManager.updateGradebook(persistentGradebook);
		assign.setPointsPossible(new Double(5));
		gradebookManager.updateAssignment(assign);
		List categories = gradebookManager.getCategories(persistentGradebook.getId());
		List gradeRecords = generateGradeRecords(assign, 5);
		List gradeRecords2 = generateGradeRecords(assign2, 5);
		for(int i=0; i<gradeRecords2.size(); i++)
		{
			gradeRecords.add(gradeRecords2.get(i));
		}		
		gradeRecords2 = generateGradeRecords(gradebookManager.getAssignment(assignId1), 5);
		for(int i=0; i<gradeRecords2.size(); i++)
		{
			gradeRecords.add(gradeRecords2.get(i));
		}
		gradeRecords2 = generateGradeRecords(gradebookManager.getAssignment(assignId2), 5);
		for(int i=0; i<gradeRecords2.size(); i++)
		{
			gradeRecords.add(gradeRecords2.get(i));
		}
		List uid = new ArrayList();
		uid.add("studentId1");
		uid.add("studentId2");
		uid.add("studentId3");
		uid.add("studentId4");
		uid.add("studentId5");
		addUsersEnrollments(persistentGradebook, uid);
		
		List assignWithStatsWithNoCategory = gradebookManager.getAssignmentsWithNoCategoryWithStats(persistentGradebook.getId(), Assignment.DEFAULT_SORT, true);
		for(int i=0; i<assignWithStatsWithNoCategory.size(); i++)
		{
			Assignment assginment = (Assignment) assignWithStatsWithNoCategory.get(i);
			Assert.assertTrue(assginment.getId().equals(assignId1) || assginment.getId().equals(assignId2));
			if(assginment.getId().equals(assignId1))
				Assert.assertTrue(new BigDecimal(assginment.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((1.0 + 2.0 +3 +4 +5) / 5.0 / 10.0 * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
			if(assginment.getId().equals(assignId2))
				Assert.assertTrue(new BigDecimal(assginment.getMean()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
					new BigDecimal((1.0 + 2.0 +3 +4 +5) / 5.0 / 9.0 * 100).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		}
	}
	
	public void testConvertGradingEventsConverted() throws Exception
	{
		Assignment assign = gradebookManager.getAssignment(assgn1Long);
		GradingEvent ge1 = new GradingEvent(assign, "admin", "studentId1", new Double(1.0));
		GradingEvent ge2 = new GradingEvent(assign, "admin", "studentId2", new Double(2.0));
		GradingEvents ges = new GradingEvents();
		ges.addEvent(ge1);
		ges.addEvent(ge2);
		List uid = new ArrayList();
		uid.add("studentId1");
		uid.add("studentId2");
		
		gradebookManager.convertGradingEventsConverted(assign, ges, uid, GradebookService.GRADE_TYPE_PERCENTAGE);

		List events = ges.getEvents((String)uid.get(0));
		for(Iterator iter = events.iterator(); iter.hasNext(); )
		{
			GradingEvent converted = (GradingEvent) iter.next();
			Assert.assertTrue(new BigDecimal(new Double(converted.getGrade())).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal(1.0 / assign.getPointsPossible().doubleValue() * 100.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		}
		events = ges.getEvents((String)uid.get(1));
		for(Iterator iter = events.iterator(); iter.hasNext(); )
		{
			GradingEvent converted = (GradingEvent) iter.next();
			Assert.assertTrue(new BigDecimal(new Double(converted.getGrade())).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue() == 
				new BigDecimal(2.0 / assign.getPointsPossible().doubleValue() * 100.0).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue());
		}
	}
	
	public void testCheckStuendsNotSubmitted() throws Exception
	{
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		Assignment assign = gradebookManager.getAssignment(assgn1Long);
		Assignment assign2 = gradebookManager.getAssignment(assgn3Long);

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
  	integrationSupport.createCourse(persistentGradebook.getUid(), persistentGradebook.getUid(), false, false, false);
		gradebookManager.updateGradebook(persistentGradebook);
		assign.setPointsPossible(new Double(5));
		gradebookManager.updateAssignment(assign);
		List categories = gradebookManager.getCategories(persistentGradebook.getId());
		Category cate = gradebookManager.getCategory(assign.getCategory().getId());
		List assignments = gradebookManager.getAssignments(persistentGradebook.getId());

		List gradeRecords = generateGradeRecords(assign, 5);
		List gradeRecords2 = generateGradeRecords(assign2, 5);
		for(int i=0; i<gradeRecords2.size(); i++)
		{
			gradeRecords.add(gradeRecords2.get(i));
		}

		List uid = new ArrayList();
		uid.add("studentId1");
		uid.add("studentId2");
		uid.add("studentId3");
		uid.add("studentId4");
		uid.add("studentId5");
		Map studentIdMap = new HashMap();
		studentIdMap.put("studentId1", new Double(1.0));
		studentIdMap.put("studentId2", new Double(2.0));
		studentIdMap.put("studentId3", new Double(3.0));
		studentIdMap.put("studentId4", new Double(4.0));
		studentIdMap.put("studentId5", new Double(5.0));

		gradeRecords = gradebookManager.getAllAssignmentGradeRecords(persistentGradebook.getId(), uid);

		addUsersEnrollments(persistentGradebook, uid);

		Assert.assertTrue(gradebookManager.checkStuendsNotSubmitted(persistentGradebook));
		
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		Assert.assertTrue(gradebookManager.checkStuendsNotSubmitted(persistentGradebook));
		
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		Assert.assertTrue(gradebookManager.checkStuendsNotSubmitted(persistentGradebook));

		List allAssigns = gradebookManager.getAssignments(persistentGradebook.getId());
		for(Iterator iter = allAssigns.iterator(); iter.hasNext();)
		{
			Assignment assignment = (Assignment) iter.next();
			if(!assignment.getId().equals(assgn1Long))
			{
				assignment.setCounted(false);
				gradebookManager.updateAssignment(assignment);
			}
		}
		
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		Assert.assertTrue(!gradebookManager.checkStuendsNotSubmitted(persistentGradebook));

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		Assert.assertTrue(!gradebookManager.checkStuendsNotSubmitted(persistentGradebook));
		
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		Assert.assertTrue(!gradebookManager.checkStuendsNotSubmitted(persistentGradebook));

		
		//test for setting studentId1's assignment1 to null
		for(int i=0; i<gradeRecords.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord)gradeRecords.get(i);
			if(agr.getAssignment().getId().equals(assgn1Long) && agr.getStudentId().equals("studentId1"))
				agr.setPointsEarned(null);
		}

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		Assert.assertTrue(gradebookManager.checkStuendsNotSubmitted(persistentGradebook));

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_ONLY_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		Assert.assertTrue(gradebookManager.checkStuendsNotSubmitted(persistentGradebook));
		
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_NO_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		Assert.assertTrue(gradebookManager.checkStuendsNotSubmitted(persistentGradebook));
	
	}
	
	public void testfillInZeroForNullGradeRecords() throws Exception
	{
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		Assignment assign = gradebookManager.getAssignment(assgn1Long);
		Assignment assign2 = gradebookManager.getAssignment(assgn3Long);

		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
  	integrationSupport.createCourse(persistentGradebook.getUid(), persistentGradebook.getUid(), false, false, false);
		gradebookManager.updateGradebook(persistentGradebook);
		assign.setPointsPossible(new Double(5));
		gradebookManager.updateAssignment(assign);
		List categories = gradebookManager.getCategories(persistentGradebook.getId());
		Category cate = gradebookManager.getCategory(assign.getCategory().getId());
		List assignments = gradebookManager.getAssignments(persistentGradebook.getId());

		List gradeRecords = generateGradeRecords(assign, 5);
		List gradeRecords2 = generateGradeRecords(assign2, 5);
		for(int i=0; i<gradeRecords2.size(); i++)
		{
			gradeRecords.add(gradeRecords2.get(i));
		}

		List uid = new ArrayList();
		uid.add("studentId1");
		uid.add("studentId2");
		uid.add("studentId3");
		uid.add("studentId4");
		uid.add("studentId5");
		Map studentIdMap = new HashMap();
		studentIdMap.put("studentId1", new Double(1.0));
		studentIdMap.put("studentId2", new Double(2.0));
		studentIdMap.put("studentId3", new Double(3.0));
		studentIdMap.put("studentId4", new Double(4.0));
		studentIdMap.put("studentId5", new Double(5.0));

		gradeRecords = gradebookManager.getAllAssignmentGradeRecords(persistentGradebook.getId(), uid);

		addUsersEnrollments(persistentGradebook, uid);
	
		Assert.assertTrue(gradebookManager.checkStuendsNotSubmitted(persistentGradebook));
		Assert.assertTrue(gradebookManager.getAllAssignmentGradeRecords(persistentGradebook.getId(), uid).size() == 10);
		gradebookManager.fillInZeroForNullGradeRecords(persistentGradebook);
		Assert.assertTrue(!gradebookManager.checkStuendsNotSubmitted(persistentGradebook));
		Assert.assertTrue(gradebookManager.getAllAssignmentGradeRecords(persistentGradebook.getId(), uid).size() == 20);
		
		//test for setting studentId1's assignment1 to null
		for(int i=0; i<gradeRecords.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord)gradeRecords.get(i);
			if(agr.getAssignment().getId().equals(assgn1Long) && agr.getStudentId().equals("studentId1"))
				agr.setPointsEarned(null);
		}
		
		Assert.assertTrue(gradebookManager.checkStuendsNotSubmitted(persistentGradebook));
		gradebookManager.fillInZeroForNullGradeRecords(persistentGradebook);
		Assert.assertTrue(!gradebookManager.checkStuendsNotSubmitted(persistentGradebook));
	}
	
	public void testConvertGradePointsForUpdatedTotalPoints() throws Exception
	{
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		Assignment assign = gradebookManager.getAssignment(assgn1Long);
		Assignment assign2 = gradebookManager.getAssignment(assgn3Long);

		
		persistentGradebook.setGrade_type(GradebookService.GRADE_TYPE_PERCENTAGE);
		gradebookManager.updateGradebook(persistentGradebook);
		assign.setPointsPossible(new Double(5));
		gradebookManager.updateAssignment(assign);

		List studentUids = new ArrayList();
		studentUids.add("studentId1");
		studentUids.add("studentId2");
		studentUids.add("studentId3");
		studentUids.add("studentId4");
		studentUids.add("studentId5");
		Map studentIdMap = new HashMap();
		studentIdMap.put("studentId1", new Double(1.0));
		studentIdMap.put("studentId2", new Double(2.0));
		studentIdMap.put("studentId3", new Double(3.0));
		studentIdMap.put("studentId4", new Double(4.0));
		studentIdMap.put("studentId5", new Double(5.0));

  	integrationSupport.createCourse(persistentGradebook.getUid(), persistentGradebook.getUid(), false, false, false);
		gradebookManager.updateGradebook(persistentGradebook);
		addUsersEnrollments(persistentGradebook, studentUids);

		generateGradeRecords(assign, 5);
		gradebookManager.convertGradePointsForUpdatedTotalPoints(persistentGradebook, assign, new Double(10), studentUids);
		assign.setPointsPossible(new Double(10));
		gradebookManager.updateAssignment(assign);
		List records = gradebookManager.getAllAssignmentGradeRecordsConverted(persistentGradebook.getId(), studentUids);
		for(int i=0; i<records.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord) records.get(i);
			if(agr.getAssignment().getCategory().getName().equals("cate 1"))
				Assert.assertTrue(agr.getPointsEarned().doubleValue() == ((Double)studentIdMap.get(agr.getStudentId())).doubleValue() * 2.0 / 10.0 * 100.0);
			
//				System.out.println(agr.getAssignment().getName() + "-----" + agr.getStudentId() + "---" + agr.getPointsEarned());
		}
	}
	
	public void testCalculateStatisticsPerStudent() throws Exception
	{
		Gradebook persistentGradebook = gradebookManager.getGradebook(this.getClass().getName());
		Assignment assign = gradebookManager.getAssignment(assgn1Long);
		Assignment assign2 = gradebookManager.getAssignment(assgn3Long);
		
		persistentGradebook.setCategory_type(GradebookService.CATEGORY_TYPE_WEIGHTED_CATEGORY);
		gradebookManager.updateGradebook(persistentGradebook);
		assign.setPointsPossible(new Double(5));
		gradebookManager.updateAssignment(assign);

		List gradeRecords = generateGradeRecords(assign, 5);
		List gradeRecords2 = generateGradeRecords(assign2, 5);
		for(int i=0; i<gradeRecords2.size(); i++)
		{
			gradeRecords.add(gradeRecords2.get(i));
		}

		List uid = new ArrayList();
		uid.add("studentId1");
		uid.add("studentId2");
		uid.add("studentId3");
		uid.add("studentId4");
		uid.add("studentId5");
		Map studentIdMap = new HashMap();
		studentIdMap.put("studentId1", new Double(1.0));
		studentIdMap.put("studentId2", new Double(2.0));
		studentIdMap.put("studentId3", new Double(3.0));
		studentIdMap.put("studentId4", new Double(4.0));
		studentIdMap.put("studentId5", new Double(5.0));

		
		Category cate = gradebookManager.getCategory(cate1Long);
		cate.calculateStatisticsPerStudent(gradeRecords, "studentId1");
		Assert.assertTrue(cate.getMean().doubleValue() == 20.0);
		Assert.assertTrue(cate.getAverageScore().doubleValue() == 1.0);
		Assert.assertTrue(cate.getAverageTotalPoints().doubleValue() == 5.0);
		cate = gradebookManager.getCategory(cate2Long);
		cate.calculateStatisticsPerStudent(gradeRecords, "studentId1");
		Assert.assertTrue(cate.getMean().doubleValue() == 10.0);
		Assert.assertTrue(cate.getAverageScore().doubleValue() == 1.0);
		Assert.assertTrue(cate.getAverageTotalPoints().doubleValue() == 10.0);

		//test for setting studentId1's assignment1 to null
		for(int i=0; i<gradeRecords.size(); i++)
		{
			AssignmentGradeRecord agr = (AssignmentGradeRecord)gradeRecords.get(i);
			if(agr.getAssignment().getId().equals(assgn1Long) && agr.getStudentId().equals("studentId1"))
				agr.setPointsEarned(null);
		}
		cate = gradebookManager.getCategory(cate1Long);
		cate.calculateStatisticsPerStudent(gradeRecords, "studentId1");
		Assert.assertTrue(cate.getMean() == null);
		Assert.assertTrue(cate.getAverageScore() == null);
		Assert.assertTrue(cate.getAverageTotalPoints() == null);
		cate = gradebookManager.getCategory(cate2Long);
		cate.calculateStatisticsPerStudent(gradeRecords, "studentId1");
		Assert.assertTrue(cate.getMean().doubleValue() == 10.0);
		Assert.assertTrue(cate.getAverageScore().doubleValue() == 1.0);
		Assert.assertTrue(cate.getAverageTotalPoints().doubleValue() == 10.0);
	}
}