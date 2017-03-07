package com.freedom.model;

import java.io.Serializable;

public class Department implements Serializable {

	private static final long serialVersionUID = 1L;
	private String departmentName;
	private int departmentNumer;
	private String managerSSN;
	public String getDepartmentName() {
		return departmentName;
	}
	public void setDepartmentName(String departmentName) {
		this.departmentName = departmentName;
	}
	
	public String getManagerSSN() {
		return managerSSN;
	}
	public void setManagerSSN(String managerSSN) {
		this.managerSSN = managerSSN;
	}
	public int getDepartmentNumer() {
		return departmentNumer;
	}
	public void setDepartmentNumer(int departmentNumer) {
		this.departmentNumer = departmentNumer;
	}
	

}