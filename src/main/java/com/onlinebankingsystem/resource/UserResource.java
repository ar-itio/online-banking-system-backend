package com.onlinebankingsystem.resource;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinebankingsystem.config.CustomUserDetailsService;
import com.onlinebankingsystem.dto.CommonApiResponse;
import com.onlinebankingsystem.dto.RegisterUserRequestDto;
import com.onlinebankingsystem.dto.UserListResponseDto;
import com.onlinebankingsystem.dto.UserLoginRequest;
import com.onlinebankingsystem.dto.UserLoginResponse;
import com.onlinebankingsystem.dto.UserStatusUpdateRequestDto;
import com.onlinebankingsystem.entity.Bank;
import com.onlinebankingsystem.entity.User;
import com.onlinebankingsystem.service.BankService;
import com.onlinebankingsystem.service.EmailService;
import com.onlinebankingsystem.service.JwtService;
import com.onlinebankingsystem.service.UserService;
import com.onlinebankingsystem.utility.Constants.IsAccountLinked;
import com.onlinebankingsystem.utility.Constants.UserRole;
import com.onlinebankingsystem.utility.Constants.UserStatus;
import com.onlinebankingsystem.utility.TransactionIdGenerator;

import io.micrometer.common.util.StringUtils;

@Component
public class UserResource {

	private final Logger LOG = LoggerFactory.getLogger(UserResource.class);

	@Autowired
	private UserService userService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private AuthenticationManager authenticationManager;

	@Autowired
	private CustomUserDetailsService customUserDetailsService;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private BankService bankService;

	@Autowired
	private EmailService emailService;

	private ObjectMapper objectMapper = new ObjectMapper();

	public ResponseEntity<CommonApiResponse> registerUser(RegisterUserRequestDto request) {

		LOG.info("Received request for register user");

		CommonApiResponse response = new CommonApiResponse();

		if (request == null) {
			response.setResponseMessage("user is null");
			response.setSuccess(true);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		User existingUser = this.userService.getUserByEmail(request.getEmail());

		if (existingUser != null) {
			response.setResponseMessage("User with this Email Id already resgistered!!!");
			response.setSuccess(true);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		if (request.getRoles() == null) {
			response.setResponseMessage("bad request ,Role is missing");
			response.setSuccess(true);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		Bank bank = null;

		User user = RegisterUserRequestDto.toUserEntity(request);

		String encodedPassword = "";

		String rawPassword = "";
		String accountId = "";
		user.setAccountBalance(BigDecimal.ZERO);

		if (request.getRoles().equals(UserRole.ROLE_CUSTOMER.value())) {
			user.setStatus(UserStatus.PENDING.value());
			user.setIsAccountLinked(IsAccountLinked.NO.value());

			accountId = TransactionIdGenerator.generateAccountId();
			rawPassword = TransactionIdGenerator.generatePassword();

			user.setAccountId(accountId);

			encodedPassword = passwordEncoder.encode(rawPassword);

		}

		// in case of Bank, password will come from UI
		else {
			user.setStatus(UserStatus.ACTIVE.value());
			encodedPassword = passwordEncoder.encode(user.getPassword());
		}

		user.setPassword(encodedPassword);

		existingUser = this.userService.registerUser(user);

		if (existingUser == null) {
			response.setResponseMessage("failed to register user");
			response.setSuccess(true);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		if (request.getRoles().equals(UserRole.ROLE_CUSTOMER.value())) {

			String subject = " Your Temporary Password for Bank Registration";

			sendPasswordGenerationMail(user, accountId, rawPassword, subject);
		}

		response.setResponseMessage("User registered Successfully");
		response.setSuccess(true);

		// Convert the object to a JSON string
		String jsonString = null;
		try {
			jsonString = objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new ResponseEntity<CommonApiResponse>(response, HttpStatus.OK);
	}

	public ResponseEntity<CommonApiResponse> registerAdmin(RegisterUserRequestDto registerRequest) {

		CommonApiResponse response = new CommonApiResponse();

		if (registerRequest == null) {
			response.setResponseMessage("user is null");
			response.setSuccess(true);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		if (registerRequest.getEmail() == null || registerRequest.getPassword() == null) {
			response.setResponseMessage("missing input");
			response.setSuccess(true);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		User existingUser = this.userService.getUserByEmail(registerRequest.getEmail());

		if (existingUser != null) {
			response.setResponseMessage("User already register with this Email");
			response.setSuccess(true);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		User user = new User();
		user.setEmail(registerRequest.getEmail());
		user.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
		user.setRoles(UserRole.ROLE_ADMIN.value());
		user.setStatus(UserStatus.ACTIVE.value());
		existingUser = this.userService.registerUser(user);

		if (existingUser == null) {
			response.setResponseMessage("failed to register admin");
			response.setSuccess(true);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		response.setResponseMessage("Admin registered Successfully");
		response.setSuccess(true);

		// Convert the object to a JSON string
		String jsonString = null;
		try {
			jsonString = objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println(jsonString);

		return new ResponseEntity<CommonApiResponse>(response, HttpStatus.OK);
	}

	public ResponseEntity<UserLoginResponse> login(UserLoginRequest loginRequest) {

		UserLoginResponse response = new UserLoginResponse();

		if (loginRequest == null) {
			response.setResponseMessage("Missing Input");
			response.setSuccess(true);

			return new ResponseEntity<UserLoginResponse>(response, HttpStatus.BAD_REQUEST);
		}

		String jwtToken = null;
		User user = null;

		try {
			authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(loginRequest.getEmailId(), loginRequest.getPassword()));
		} catch (Exception ex) {
			response.setResponseMessage("Invalid email or password.");
			response.setSuccess(true);
			return new ResponseEntity<UserLoginResponse>(response, HttpStatus.BAD_REQUEST);
		}

		UserDetails userDetails = customUserDetailsService.loadUserByUsername(loginRequest.getEmailId());

		user = userService.getUserByEmail(loginRequest.getEmailId());

		if (!user.getStatus().equals(UserStatus.ACTIVE.value())) {
			response.setResponseMessage("Failed to login");
			response.setSuccess(true);
			return new ResponseEntity<UserLoginResponse>(response, HttpStatus.BAD_REQUEST);
		}

		for (GrantedAuthority grantedAuthory : userDetails.getAuthorities()) {
			if (grantedAuthory.getAuthority().equals(loginRequest.getRole())) {
				jwtToken = jwtService.generateToken(userDetails.getUsername());
			}
		}

		// user is authenticated
		if (jwtToken != null) {
			response.setUser(user);
			response.setResponseMessage("Logged in sucessful");
			response.setSuccess(true);
			response.setJwtToken(jwtToken);
			return new ResponseEntity<UserLoginResponse>(response, HttpStatus.OK);
		}

		else {
			response.setResponseMessage("Failed to login");
			response.setSuccess(true);
			return new ResponseEntity<UserLoginResponse>(response, HttpStatus.BAD_REQUEST);
		}

	}

	public ResponseEntity<UserListResponseDto> getUsersByRole(String role) {

		UserListResponseDto response = new UserListResponseDto();

		List<User> users = new ArrayList<>();
		users = this.userService.getUserByRoles(role);

		if (!users.isEmpty()) {
			response.setUsers(users);
		}

		response.setResponseMessage("User Fetched Successfully");
		response.setSuccess(true);

		// Convert the object to a JSON string
		String jsonString = null;
		try {
			jsonString = objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println(jsonString);

		return new ResponseEntity<UserListResponseDto>(response, HttpStatus.OK);
	}

	public ResponseEntity<UserListResponseDto> fetchBankManagers() {

		UserListResponseDto response = new UserListResponseDto();

		List<User> users = new ArrayList<>();
		users = this.userService.getUsersByRolesAndStatus(UserRole.ROLE_BANK.value(), UserStatus.ACTIVE.value());

		if (!users.isEmpty()) {
			response.setUsers(users);
		}

		response.setResponseMessage("User Fetched Successfully");
		response.setSuccess(true);

		// Convert the object to a JSON string
		String jsonString = null;
		try {
			jsonString = objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println(jsonString);

		return new ResponseEntity<UserListResponseDto>(response, HttpStatus.OK);
	}

	public ResponseEntity<CommonApiResponse> updateUserStatus(UserStatusUpdateRequestDto request) {

		LOG.info("Received request for updating the user status");

		CommonApiResponse response = new CommonApiResponse();

		if (request == null) {
			response.setResponseMessage("bad request, missing data");
			response.setSuccess(true);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		if (request.getUserId() == 0) {
			response.setResponseMessage("bad request, user id is missing");
			response.setSuccess(true);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		User user = null;
		user = this.userService.getUserById(request.getUserId());

		user.setStatus(request.getStatus());

		User updatedUser = this.userService.updateUser(user);

		if (updatedUser != null) {
			response.setResponseMessage("User " + request.getStatus() + " Successfully!!!");
			response.setSuccess(true);
			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.OK);
		} else {
			response.setResponseMessage("Failed to " + request.getStatus() + " the user");
			response.setSuccess(true);
			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.INTERNAL_SERVER_ERROR);
		}

	}

	public ResponseEntity<UserListResponseDto> fetchBankCustomerByBankId(int bankId) {

		UserListResponseDto response = new UserListResponseDto();

		List<User> users = new ArrayList<>();

		users = this.userService.getUsersByRolesAndStatus(UserRole.ROLE_CUSTOMER.value(), UserStatus.ACTIVE.value());

		if (!users.isEmpty()) {
			response.setUsers(users);
		}

		response.setResponseMessage("User Fetched Successfully");
		response.setSuccess(true);

		// Convert the object to a JSON string
		String jsonString = null;
		try {
			jsonString = objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println(jsonString);

		return new ResponseEntity<UserListResponseDto>(response, HttpStatus.OK);
	}

//	public ResponseEntity<UserListResponseDto> searchBankCustomer(int bankId, String customerName) {
//
//		UserListResponseDto response = new UserListResponseDto();
//
//		List<User> users = new ArrayList<>();
//		
//		users = this.userService.searchBankCustomerByNameAndRole(customerName, bankId, UserRole.ROLE_CUSTOMER.value());
//		
//		if(!users.isEmpty()) {
//			response.setUsers(users);
//		}
//		
//		response.setResponseMessage("User Fetched Successfully");
//		response.setSuccess(true);
//
//		// Convert the object to a JSON string
//		String jsonString = null;
//		try {
//			jsonString = objectMapper.writeValueAsString(response);
//		} catch (JsonProcessingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//
//		System.out.println(jsonString);
//
//		return new ResponseEntity<UserListResponseDto>(response, HttpStatus.OK);
//	}

	public ResponseEntity<UserListResponseDto> searchBankCustomer(String customerName) {

		UserListResponseDto response = new UserListResponseDto();

		List<User> users = new ArrayList<>();

		users = this.userService.searchBankCustomerByNameAndRole(customerName, UserRole.ROLE_CUSTOMER.value());

		if (!users.isEmpty()) {
			response.setUsers(users);
		}

		response.setResponseMessage("User Fetched Successfully");
		response.setSuccess(true);

		// Convert the object to a JSON string
		String jsonString = null;
		try {
			jsonString = objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println(jsonString);

		return new ResponseEntity<UserListResponseDto>(response, HttpStatus.OK);
	}

	private void sendPasswordGenerationMail(User customer, String accountId, String rawPassord, String subject) {

		StringBuilder emailBody = new StringBuilder();
		emailBody.append("<html><body>");
		emailBody.append("<h3>Dear " + customer.getName() + ",</h3>");
		emailBody.append("<p>Welcome aboard! We've generated a temporary password for you.</p>");
		emailBody.append("</br> Your Account Id is:<span><b>" + accountId + "</b><span></p>");
		emailBody.append("</br> Your Password is:<span><b>" + rawPassord + "</b><span></p>");

		emailBody.append("<p>Please use generated Password for login.</p>");
		emailBody.append("<p>And use Account Id for KYC.</p>");

		emailBody.append("<p>Best Regards,<br/>Bank</p>");

		emailBody.append("</body></html>");

		this.emailService.sendEmail(customer.getEmail(), subject, emailBody.toString());
	}

	public ResponseEntity<UserListResponseDto> fetchPendingCustomers() {

		UserListResponseDto response = new UserListResponseDto();

		List<User> users = new ArrayList<>();
		users = this.userService.getUsersByRolesAndStatus(UserRole.ROLE_CUSTOMER.value(), UserStatus.PENDING.value());

		if (!users.isEmpty()) {
			response.setUsers(users);
		}

		response.setResponseMessage("Pending Customers Fetched Successful!!!");
		response.setSuccess(true);

		// Convert the object to a JSON string
		String jsonString = null;
		try {
			jsonString = objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println(jsonString);

		return new ResponseEntity<UserListResponseDto>(response, HttpStatus.OK);
	}

	public ResponseEntity<UserListResponseDto> fetchById(int id) {

		UserListResponseDto response = new UserListResponseDto();

		User user = this.userService.getUserById(id);

		response.setUsers(Arrays.asList(user));

		response.setResponseMessage("User Fetched Successfully");
		response.setSuccess(true);

		// Convert the object to a JSON string
		String jsonString = null;
		try {
			jsonString = objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println(jsonString);

		return new ResponseEntity<UserListResponseDto>(response, HttpStatus.OK);
	}

	public ResponseEntity<CommonApiResponse> updateUserprofile(RegisterUserRequestDto request) {

		LOG.info("Received request for update user profile");

		CommonApiResponse response = new CommonApiResponse();

		if (request == null || request.getId() == 0) {
			response.setResponseMessage("bad request - missing input");
			response.setSuccess(true);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		User existingUser = this.userService.getUserByEmail(request.getEmail());

		if (existingUser == null) {
			response.setResponseMessage("Customer Profile not found!!!");
			response.setSuccess(true);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		existingUser.setCity(request.getCity());
		existingUser.setContact(request.getContact());
		
		if(org.apache.commons.lang3.StringUtils.isNotEmpty(request.getGender())) {
			existingUser.setGender(request.getGender());
		}
		
		existingUser.setName(request.getName());
		existingUser.setPincode(request.getPincode());
		existingUser.setStreet(request.getStreet());

		User updatedUser = this.userService.updateUser(existingUser);

		if (updatedUser == null) {
			response.setResponseMessage("failed to update the user profile");
			response.setSuccess(true);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		response.setResponseMessage("User Profile Updated Successful!!!");
		response.setSuccess(true);

		// Convert the object to a JSON string
		String jsonString = null;
		try {
			jsonString = objectMapper.writeValueAsString(response);
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new ResponseEntity<CommonApiResponse>(response, HttpStatus.OK);
	}
	
	
	

}
