package com.freedom.controller;

import java.math.BigInteger;
import java.security.SecureRandom;
import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import com.freedom.model.User;
import com.freedom.util.HibernateUtil;
import com.freedom.util.LoginSessionBean;
import com.freedom.util.Status;


@RestController
@RequestMapping("/user")
public class UserController {
	private static final Logger logger = Logger.getLogger(UserController.class);
	private SecureRandom random = new SecureRandom();
	@Autowired
	private LoginSessionBean loginSessionBean;
	
	public String nextSessionId() {
		return new BigInteger(130, random).toString(32);
	}

	@RequestMapping(value = "/login", method = RequestMethod.POST,consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Status login(@RequestBody User user) {
		Status currentStatus = new Status();
		Session session = HibernateUtil.getSessionFactory().openSession();
		Query query = session.createQuery(
				"select count(*) from User login where login.userName=:userName and login.password=:password");
		query.setString("userName", user.getUserName());
		query.setString("password", user.getPassword());
		Long count = (Long) query.uniqueResult();
		if (count == 1) {
			logger.info("User :"+user.getUserName()+" succesfully logged in");
			String userSessionKey = nextSessionId();
			User user1 = getUser(user.getUserName());
			loginSessionBean.getSessionMap().put(userSessionKey, user1);
			currentStatus.setStatus("SUCCESS");
			currentStatus.setSession(userSessionKey);
		} else {
			logger.error("Unsuccesfull Login, failed to validate credentials");
			currentStatus.setStatus("FAILURE");
			currentStatus.setErrorCode(0);// Invalid user
			currentStatus.setErrorMsg("Invalid Username / Password");
		}
		return currentStatus;
	}
	
	
	@RequestMapping(value = "/logOut", method = RequestMethod.GET)
	@ResponseBody
	public Status logOut( @RequestParam String sessionKey) {
		Status currentStatus = new Status();
		User objuser = (User)loginSessionBean.getSessionMap().remove(sessionKey);
		currentStatus.setStatus("SUCCESS");
		currentStatus.setErrorMsg("User Logged Out Successfully");
		logger.info("User :"+objuser.getUserName()+" succesfully logged out");
		return currentStatus;
	}
	
	private User getUser(String username) {
		Session session = HibernateUtil.getSessionFactory().openSession();
		session.beginTransaction();
		String queryString = "from User where userName = :userName";
		Query query = session.createQuery(queryString);
		query.setString("userName", username);
		Object queryResult = query.uniqueResult();
		User user = (User) queryResult;
		session.getTransaction().commit();
		return user;
	}

}
