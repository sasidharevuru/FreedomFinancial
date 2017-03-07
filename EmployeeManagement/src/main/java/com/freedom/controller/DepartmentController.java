package com.freedom.controller;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.freedom.model.Department;
import com.freedom.util.HibernateUtil;
import com.freedom.util.Status;
import com.freedom.util.TestMemcached;



 
@RestController
@RequestMapping("/ems/department")
public class DepartmentController {
	private static final Logger logger = Logger.getLogger(DepartmentController.class);
	
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	@ResponseBody
	public static List<Department> getDepartments() {
		List<Department> departmentList = (List<Department>) TestMemcached.getMemcachedclinet().get("departmentList"); // Retrieving list from the cache
		if(departmentList != null){
			logger.info("CACHE HIT - Returning data from Cache ");
			return departmentList;
		}
		logger.info("CACHE MISS - Returning data from Databse ");
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();
		Query query = session.createQuery("from Department");
		departmentList = (List<Department>) query.list(); // Retrieving the data from the databse;
		session.getTransaction().commit();
		TestMemcached.getMemcachedclinet().add("departmentList", departmentList);
		return departmentList;
	}
	
	
	@RequestMapping(value = "/addDepartment", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Status addDepartment(@RequestBody Department department) {
		Status currentStatus = new Status();
		try {
			Session session = HibernateUtil.getSessionFactory().openSession();
			Query query = session.createQuery(
					"select count(*) from Department objdepartment where objdepartment.departmentNumer=:departmentNumer");
			query.setInteger("departmentNumer", department.getDepartmentNumer());
			Long count = (Long) query.uniqueResult();
			if (count == 0) {
				session.beginTransaction();
				session.save(department);
				session.getTransaction().commit();
				// adding to cache
				List<Department> departmentList = (List<Department>) TestMemcached.getMemcachedclinet().get("departmentList");
				if(departmentList!= null)
				{
					logger.info("Adding the newly added department to cache");
					departmentList.add(department);
					TestMemcached.getMemcachedclinet().replace("departmentList", departmentList);
				}
				currentStatus.setStatus("SUCCESS");
			} else {
				currentStatus.setStatus("FAILURE");
				currentStatus.setErrorCode(12);
				currentStatus.setErrorMsg("The department with the given Id already exist in the system");
			}
		} catch (Exception e) {
			currentStatus.setStatus("FAILURE");
			currentStatus.setErrorCode(14);
			currentStatus.setErrorMsg(e.getMessage());
			e.printStackTrace();
		}
		logger.info("Returned Status details [Status : "+currentStatus.getStatus() +"\n ErrorCode : "+ currentStatus.getErrorCode()+" ]");
		return currentStatus;
	}
	
	
	@RequestMapping(value = "/deleteDepartment", method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Status deleteDepartment(@RequestBody Department department) {
		Status currentStatus = new Status();
		try {
			Session session = HibernateUtil.getSessionFactory().openSession();
				session.beginTransaction();
				session.delete(department);
				session.getTransaction().commit();
				removefromcache(department);
				currentStatus.setStatus("SUCCESS");
		} catch (Exception e) {
			e.printStackTrace();
			currentStatus.setStatus("FAILURE");
			currentStatus.setErrorCode(14);
			currentStatus.setErrorMsg(e.getMessage());
			logger.error(e.getMessage());
		}
		logger.info("Returned Status details [Status : "+currentStatus.getStatus() +"\n ErrorCode : "+ currentStatus.getErrorCode()+" ]");
		return currentStatus;
	}
	
	
	@RequestMapping(value = "/updateDepartment", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Status updateDepartment(@RequestBody Department department) {
		Status currentStatus = new Status();
		try {
			Session session = HibernateUtil.getSessionFactory().openSession();
				session.beginTransaction();
				session.update(department);
				session.getTransaction().commit();
				removefromcache(department);
				List<Department> departmentList = (ArrayList<Department>) TestMemcached.getMemcachedclinet().get("departmentList");
				departmentList.add(department);
				TestMemcached.getMemcachedclinet().replace("departmentList", departmentList);
				currentStatus.setStatus("SUCCESS");
		} catch (Exception e) {
			currentStatus.setStatus("FAILURE");
			currentStatus.setErrorCode(14);
			currentStatus.setErrorMsg(e.getMessage());
			logger.error(e.getMessage());
		}
		logger.info("Returned Status details [Status : "+currentStatus.getStatus() +"\n ErrorCode : "+ currentStatus.getErrorCode()+" ]");
		return currentStatus;
	}
	
	
	
	
	private void removefromcache(Department department) {
		
		List<Department> departmentList = (List<Department>) TestMemcached.getMemcachedclinet().get("departmentList");
		if (departmentList != null) {
			for (int department_index = 0; department_index < departmentList.size(); department_index++) {
				Department objDepartment = departmentList.get(department_index);
				if (objDepartment.getDepartmentNumer()==department.getDepartmentNumer()) {
					departmentList.remove(department_index);
					TestMemcached.getMemcachedclinet().replace("departmentList", departmentList);
					logger.info("departmentList  key has been updated in cache");
					break;
				}
			}
		}
	
}
	
	
	
	
	

}
