package com.onlinebankingsystem.entity;

import java.math.BigDecimal;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;

	private String name;

	private String email;

	@JsonIgnore
	private String password;

	private String roles;

	private String gender;

	private String contact;

	private String street;

	private String city;

	private String pincode;

	// making it for single bank only
//	@ManyToOne
//	@JoinColumn(name = "bank_id")
//	private Bank bank;

	private String isAccountLinked; // Yes, No

	private String accountId; // to use during KYC

	private BigDecimal accountBalance;

	private String status; // active, deactivated

	
	 private String resetPasswordToken;
	    
	    @Temporal(TemporalType.TIMESTAMP)
	    private Date resetPasswordTokenExpiryDate;
	    
	    
	    
	public String getResetPasswordToken() {
			return resetPasswordToken;
		}

		public void setResetPasswordToken(String resetPasswordToken) {
			this.resetPasswordToken = resetPasswordToken;
		}

		public Date getResetPasswordTokenExpiryDate() {
			return resetPasswordTokenExpiryDate;
		}

		public void setResetPasswordTokenExpiryDate(Date resetPasswordTokenExpiryDate) {
			this.resetPasswordTokenExpiryDate = resetPasswordTokenExpiryDate;
		}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getRoles() {
		return roles;
	}

	public void setRoles(String roles) {
		this.roles = roles;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getContact() {
		return contact;
	}

	public void setContact(String contact) {
		this.contact = contact;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getPincode() {
		return pincode;
	}

	public void setPincode(String pincode) {
		this.pincode = pincode;
	}

	public String getAccountId() {
		return accountId;
	}

	public void setAccountId(String accountId) {
		this.accountId = accountId;
	}

	public String getIsAccountLinked() {
		return isAccountLinked;
	}

	public void setIsAccountLinked(String isAccountLinked) {
		this.isAccountLinked = isAccountLinked;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public BigDecimal getAccountBalance() {
		return accountBalance;
	}

	public void setAccountBalance(BigDecimal accountBalance) {
		this.accountBalance = accountBalance;
	}

}
