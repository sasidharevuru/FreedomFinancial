package com.freedom.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.freedom.model.Employee;
import com.freedom.util.HibernateUtil;
import com.freedom.util.Status;
import com.freedom.util.TestMemcached;

 
@RestController
@RequestMapping("/ems/employee")
public class EmployeeController {
	private static final Logger logger = Logger.getLogger(EmployeeController.class);
	
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	@ResponseBody
	public static List<Employee> getEmployees() {
		List<Employee> employeeList = (List<Employee>) TestMemcached.getMemcachedclinet().get("employeeList"); // getting the cached value from the memcache.
		if(employeeList != null){
			logger.info("CACHE HIT - Returning data from Cache ");
			return employeeList;
		}
		logger.info("CACHE MISS - Returning data from Databse ");
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();
		Query query = session.createQuery("from Employee");
		employeeList = (List<Employee>) query.list(); // getting the employee list from the db.
		session.getTransaction().commit();
		TestMemcached.getMemcachedclinet().add("employeeList", employeeList);
		return employeeList;
	}
	
	@RequestMapping(value = "/addEmployee", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Status addEmployee(@RequestBody Employee employee) {
		Status currentStatus = new Status();
		try {
			Session session = HibernateUtil.getSessionFactory().openSession();
			Query query = session.createQuery(
					"select count(*) from Employee objemployee where objemployee.empId=:empId and objemployee.ssn=:ssn");
			query.setInteger("empId", employee.getEmpId());
			query.setString("ssn", employee.getSsn());
			Long count = (Long) query.uniqueResult();
			if (count == 0) {
				session.beginTransaction();
				session.save(employee); // adding the employee to the database
				session.getTransaction().commit();
				List<Employee> employeeList = (List<Employee>) TestMemcached.getMemcachedclinet().get("employeeList"); // updating the cache.
				if(employeeList!= null)
				{
					logger.info("Adding the newly added employee to cache");
					employeeList.add(employee);
					TestMemcached.getMemcachedclinet().replace("employeeList", employeeList);
				}
				currentStatus.setStatus("SUCCESS");
			} else {
				currentStatus.setStatus("FAILURE");
				currentStatus.setErrorCode(10);
				currentStatus.setErrorMsg("The employee with the given Id or SSN already exist in the system");
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
	
	@RequestMapping(value = "/deleteEmployee", method = RequestMethod.DELETE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Status deletEmployee(@RequestBody Employee employee) {
		Status currentStatus = new Status();
		try {
			Session session = HibernateUtil.getSessionFactory().openSession();
				session.beginTransaction();
				session.delete(employee);
				session.getTransaction().commit();
				removefromcache(employee);// deleting the employee from cache
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
	
	
	@RequestMapping(value = "/updateEmployee", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Status updateEmployee(@RequestBody Employee employee) {
		Status currentStatus = new Status();
		try {
			Session session = HibernateUtil.getSessionFactory().openSession();
				session.beginTransaction();
				session.update(employee);
				session.getTransaction().commit();
				removefromcache(employee);
				List<Employee> employeeList = (ArrayList<Employee>) TestMemcached.getMemcachedclinet().get("employeeList");
				employeeList.add(employee);
				TestMemcached.getMemcachedclinet().replace("employeeList", employeeList);
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
	
	@RequestMapping(value = "/patchEmployee/{id}", method = RequestMethod.PATCH)
	@ResponseBody
	public Status pathEmployee(@PathVariable int id, @RequestBody Object employee) {
		List<Object> temp = (List<Object>) employee;
		Status currentStatus = new Status();
		try {
			
			for(int index = 0; index < temp.size(); index++)
			{
				Session session = HibernateUtil.getSessionFactory().openSession();
				session.beginTransaction();
				Criteria criteria = session.createCriteria(Employee.class);
				
				Query query = session.createQuery(
						"select count(*) from Employee objemployee where objemployee.empId=:empId");
				query.setInteger("empId", id);
				Long count = (Long) query.uniqueResult();
				if(count == 1){
					
						LinkedHashMap<String, String> map= (LinkedHashMap<String, String>)(temp.get(index));
						String querystr = "update Employee set " +map.get("path")+"= :value where empId=:empId";
						query = session.createQuery(querystr);
						query.setInteger("empId", id);
						query.setString("value", map.get("value"));
						query.executeUpdate();
						session.getTransaction().commit();
						currentStatus.setStatus("SUCCESS");
				}
				else {
					currentStatus.setStatus("FAILURE");
					currentStatus.setErrorCode(11);
					currentStatus.setErrorMsg("The employee with the given Id deosnt exist in the system");
					break;
				}
			}
			updatethecache();	
		} catch (Exception e) {
			currentStatus.setStatus("FAILURE");
			currentStatus.setErrorCode(14);
			currentStatus.setErrorMsg(e.getMessage());
			logger.error(e.getMessage());
		}
		logger.info("Returned Status details [Status : "+currentStatus.getStatus() +"\n ErrorCode : "+ currentStatus.getErrorCode()+" ]");
		return currentStatus;
	}
	
	
	private void updatethecache() {
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();
		Query query = session.createQuery("from Employee");
		List<Employee> employeeList = (List<Employee>) query.list(); // getting the employee list from the db.
		session.getTransaction().commit();
		TestMemcached.getMemcachedclinet().replace("employeeList", employeeList);
	}

	private void removefromcache(Employee employee) {
	
				List<Employee> employeeList = (List<Employee>) TestMemcached.getMemcachedclinet().get("employeeList");
				if (employeeList != null) {
					for (int empindex = 0; empindex < employeeList.size(); empindex++) {
						Employee objEmployee = employeeList.get(empindex);
						if (objEmployee.getSsn().equals(employee.getSsn())) {
							employeeList.remove(empindex);
							TestMemcached.getMemcachedclinet().replace("employeeList", employeeList);
							logger.info("employeeList  key has been updated in cache");
							break;
						}
					}
				}
			
	}
	
	private void removefromcacheWithId(int id) {
		
		List<Employee> employeeList = (List<Employee>) TestMemcached.getMemcachedclinet().get("employeeList");
		if (employeeList != null) {
			for (int empindex = 0; empindex < employeeList.size(); empindex++) {
				Employee objEmployee = employeeList.get(empindex);
				if (objEmployee.getEmpId()==id) {
					employeeList.remove(empindex);
					TestMemcached.getMemcachedclinet().replace("employeeList", employeeList);
					logger.info("employeeList  key has been updated in cache");
					break;
				}
			}
		}
	
}
	
	

}
