package com.onlinebankingsystem.resource;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.onlinebankingsystem.dto.BankTransactionRequestDto;
import com.onlinebankingsystem.dto.BankTransactionResponse;
import com.onlinebankingsystem.dto.CommonApiResponse;
import com.onlinebankingsystem.dto.UserStatusUpdateRequestDto;
import com.onlinebankingsystem.entity.BankTransaction;
import com.onlinebankingsystem.entity.Beneficiary;
import com.onlinebankingsystem.entity.User;
import com.onlinebankingsystem.service.BankTransactionService;
import com.onlinebankingsystem.service.BeneficiaryService;
import com.onlinebankingsystem.service.UserService;
import com.onlinebankingsystem.utility.Constants.BankTransactionStatus;
import com.onlinebankingsystem.utility.Constants.TransactionType;
import com.onlinebankingsystem.utility.TransactionIdGenerator;

@Component
public class BankTransactionResource {

	@Autowired
	private BankTransactionService bankTransactionService;

	@Autowired
	private UserService userService;
	
	@Autowired
	private BeneficiaryService beneficiaryService;

	public ResponseEntity<CommonApiResponse> addMoney(BankTransactionRequestDto request) {

		CommonApiResponse response = new CommonApiResponse();

		if (request == null || request.getAmount() == null || request.getUserId() == 0) {
			response.setResponseMessage("bad request, missing data");
			response.setSuccess(false);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		if (request.getUserId() == 0) {
			response.setResponseMessage("bad request, Bank user not selected");
			response.setSuccess(false);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		User customer = this.userService.getUserById(request.getUserId());

		if (customer == null) {
			response.setResponseMessage("bad request, customer not found");
			response.setSuccess(false);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		BankTransaction transaction = new BankTransaction();
		transaction.setAmount(request.getAmount());
		transaction.setSenderName(request.getSenderName());
		transaction.setSenderAddress(request.getSenderAddress());
		transaction.setDescription(request.getDescription());
		transaction.setUser(customer);
		transaction.setType(TransactionType.DEPOSIT.value());
		transaction.setStatus(BankTransactionStatus.PENDING.value());
		transaction.setTransactionRefId(TransactionIdGenerator.generateUniqueTransactionRefId());

		bankTransactionService.addTransaction(transaction);

		response.setResponseMessage("Add Money Request Intiated!!!");
		response.setSuccess(true);

		return new ResponseEntity<CommonApiResponse>(response, HttpStatus.OK);
	}

	public ResponseEntity<CommonApiResponse> accountTransfer(BankTransactionRequestDto request) {

		CommonApiResponse response = new CommonApiResponse();

		if (request == null || request.getAmount() == null || request.getUserId() == 0
				|| request.getAccountNumber() == null || request.getBankName() == null
				|| request.getSwiftCode() == null) {
			response.setResponseMessage("bad request, missing data");
			response.setSuccess(false);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		if (request.getUserId() == 0) {
			response.setResponseMessage("bad request, Bank user not selected");
			response.setSuccess(false);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		User customer = this.userService.getUserById(request.getUserId());

		if (customer == null) {
			response.setResponseMessage("bad request, customer not found");
			response.setSuccess(false);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		if (customer.getAccountBalance().compareTo(request.getAmount()) < 0) {
			response.setResponseMessage("Insufficient Balance, Failed to transfer amount!!!");
			response.setSuccess(false);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.OK);
		}

//		private String beneficiaryName; // Transfer Money
//
//		private String accountNumber; // Transfer Money
//
//		private String swiftCode; // Transfer Money
//
//		private String bankName; // Transfer Money
//
//		private String bankAddress; // Transfer Money
//
//		private String purpose; // Transfer Money
//		
		BankTransaction transaction = new BankTransaction();
		transaction.setAmount(request.getAmount());
		transaction.setBeneficiaryName(request.getBeneficiaryName());
		transaction.setAccountNumber(request.getAccountNumber());
		transaction.setSwiftCode(request.getSwiftCode());
		transaction.setBankName(request.getBankName());
		transaction.setBankAddress(request.getBankAddress());
		transaction.setPurpose(request.getPurpose());
		transaction.setUser(customer);
		transaction.setType(TransactionType.ACCOUNT_TRANSFER.value());
		transaction.setStatus(BankTransactionStatus.PENDING.value());
		transaction.setTransactionRefId(TransactionIdGenerator.generateUniqueTransactionRefId());

		bankTransactionService.addTransaction(transaction);

		response.setResponseMessage("Account Transfer Request Intiated!!!");
		response.setSuccess(true);

		return new ResponseEntity<CommonApiResponse>(response, HttpStatus.OK);
	}

	public ResponseEntity<BankTransactionResponse> fetchPendingTransaction() {

		BankTransactionResponse response = new BankTransactionResponse();

		List<BankTransaction> transactions = this.bankTransactionService
				.getTransactionByStatusIn(Arrays.asList(BankTransactionStatus.PENDING.value()));

		if (CollectionUtils.isEmpty(transactions)) {
			response.setResponseMessage("No Pending Transactions found!!!");
			response.setSuccess(false);

			return new ResponseEntity<BankTransactionResponse>(response, HttpStatus.OK);
		}

		response.setTransactions(transactions);
		response.setResponseMessage("Account Transfer Request Intiated!!!");
		response.setSuccess(true);

		return new ResponseEntity<BankTransactionResponse>(response, HttpStatus.OK);
	}

	public ResponseEntity<CommonApiResponse> updateTransactionStatus(UserStatusUpdateRequestDto request) {

		CommonApiResponse response = new CommonApiResponse();

		if (request == null || request.getUserId() == 0 || request.getStatus() == null) {
			response.setResponseMessage("bad request, missing data");
			response.setSuccess(false);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		if (request.getUserId() == 0) {
			response.setResponseMessage("bad request, Bank user not selected");
			response.setSuccess(false);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		BankTransaction transaction = this.bankTransactionService.getTransactionId(request.getUserId());

		User customer = transaction.getUser();

		if (request.getStatus().equals(BankTransactionStatus.REJECT.value())) {
			transaction.setStatus(BankTransactionStatus.REJECT.value());
			bankTransactionService.addTransaction(transaction);

			response.setResponseMessage("Transaction Rejected Successful");
			response.setSuccess(true);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.OK);
		}

		else {
			transaction.setStatus(BankTransactionStatus.SUCCESS.value());
			bankTransactionService.addTransaction(transaction);

			if (transaction.getType().equals(TransactionType.DEPOSIT.value())) {
				customer.setAccountBalance(customer.getAccountBalance().add(transaction.getAmount()));
			} else if (transaction.getType().equals(TransactionType.ACCOUNT_TRANSFER.value())) {
				customer.setAccountBalance(customer.getAccountBalance().subtract(transaction.getAmount()));
			}

			userService.updateUser(customer);

			response.setResponseMessage("Transaction Approved Successful");
			response.setSuccess(true);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.OK);
		}

	}

	public ResponseEntity<BankTransactionResponse> fetchSuccessTransaction() {

		BankTransactionResponse response = new BankTransactionResponse();

		List<BankTransaction> transactions = this.bankTransactionService
				.getTransactionByStatusIn(Arrays.asList(BankTransactionStatus.SUCCESS.value()));

		if (CollectionUtils.isEmpty(transactions)) {
			response.setResponseMessage("No Pending Transactions found!!!");
			response.setSuccess(false);

			return new ResponseEntity<BankTransactionResponse>(response, HttpStatus.OK);
		}

		response.setTransactions(transactions);
		response.setResponseMessage("Account Transfer Request Intiated!!!");
		response.setSuccess(true);

		return new ResponseEntity<BankTransactionResponse>(response, HttpStatus.OK);
	}

	public ResponseEntity<BankTransactionResponse> fetchCustomerTransactions(int customerId) {

		BankTransactionResponse response = new BankTransactionResponse();

		if (customerId == 0) {
			response.setResponseMessage("Customer Id not found");
			response.setSuccess(false);

			return new ResponseEntity<BankTransactionResponse>(response, HttpStatus.OK);
		}

		User customer = this.userService.getUserById(customerId);

		if (customer == null) {
			response.setResponseMessage("bad request, customer not found");
			response.setSuccess(false);

			return new ResponseEntity<BankTransactionResponse>(response, HttpStatus.BAD_REQUEST);
		}

		List<BankTransaction> transactions = this.bankTransactionService
				.getTransactionByStatusInAndUser(Arrays.asList(BankTransactionStatus.PENDING.value(),
						BankTransactionStatus.SUCCESS.value(), BankTransactionStatus.REJECT.value()), customer);

		if (CollectionUtils.isEmpty(transactions)) {
			response.setResponseMessage("No Pending Transactions found!!!");
			response.setSuccess(false);

			return new ResponseEntity<BankTransactionResponse>(response, HttpStatus.OK);
		}

		response.setTransactions(transactions);
		response.setResponseMessage("Account Transfer Request Intiated!!!");
		response.setSuccess(true);

		return new ResponseEntity<BankTransactionResponse>(response, HttpStatus.OK);
	}

	public ResponseEntity<BankTransactionResponse> fetchCustomerTransactionsByTransactionRefId(String transactionRefId,
			int customerId) {

		BankTransactionResponse response = new BankTransactionResponse();

		if (customerId == 0) {
			response.setResponseMessage("Customer Id not found");
			response.setSuccess(false);

			return new ResponseEntity<BankTransactionResponse>(response, HttpStatus.OK);
		}

		User customer = this.userService.getUserById(customerId);

		if (customer == null) {
			response.setResponseMessage("bad request, customer not found");
			response.setSuccess(false);

			return new ResponseEntity<BankTransactionResponse>(response, HttpStatus.BAD_REQUEST);
		}

		List<BankTransaction> transactions = this.bankTransactionService
				.getTransactionByTransactionRedIdInAndUser(transactionRefId, customer);

		if (CollectionUtils.isEmpty(transactions)) {
			response.setResponseMessage("No Pending Transactions found!!!");
			response.setSuccess(false);

			return new ResponseEntity<BankTransactionResponse>(response, HttpStatus.OK);
		}

		response.setTransactions(transactions);
		response.setResponseMessage("Account Transfer Request Intiated!!!");
		response.setSuccess(true);

		return new ResponseEntity<BankTransactionResponse>(response, HttpStatus.OK);
	}

	public ResponseEntity<CommonApiResponse> quickAccountTransfer(BankTransactionRequestDto request) {

		CommonApiResponse response = new CommonApiResponse();

		if (request == null || request.getAmount() == null || request.getUserId() == 0) {
			response.setResponseMessage("bad request, missing data");
			response.setSuccess(false);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}
		
		if (request.getBeneficiaryId() ==null) {
			response.setResponseMessage("bad request, beneficary not selected");
			response.setSuccess(false);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		if (request.getUserId() == 0) {
			response.setResponseMessage("bad request, user not selected");
			response.setSuccess(false);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		User customer = this.userService.getUserById(request.getUserId());

		if (customer == null) {
			response.setResponseMessage("bad request, customer not found");
			response.setSuccess(false);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.BAD_REQUEST);
		}

		if (customer.getAccountBalance().compareTo(request.getAmount()) < 0) {
			response.setResponseMessage("Insufficient Balance, Failed to transfer amount!!!");
			response.setSuccess(false);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.OK);
		}
		
		Beneficiary beneficiary = this.beneficiaryService.getById(request.getBeneficiaryId());
		
		if(beneficiary == null) {
			response.setResponseMessage("bad request - beneficiart not found!!!");
			response.setSuccess(false);

			return new ResponseEntity<CommonApiResponse>(response, HttpStatus.OK);
		}

//		private String beneficiaryName; // Transfer Money
//
//		private String accountNumber; // Transfer Money
//
//		private String swiftCode; // Transfer Money
//
//		private String bankName; // Transfer Money
//
//		private String bankAddress; // Transfer Money
//
//		private String purpose; // Transfer Money
//		
		BankTransaction transaction = new BankTransaction();
		transaction.setAmount(request.getAmount());
		transaction.setBeneficiaryName(beneficiary.getBeneficiaryName());
		transaction.setAccountNumber(beneficiary.getAccountNumber());
		transaction.setSwiftCode(beneficiary.getSwiftCode());
		transaction.setBankName(beneficiary.getBankName());
		transaction.setBankAddress(beneficiary.getBankAddress());
		transaction.setCountry(beneficiary.getCountry());
		transaction.setPurpose(request.getPurpose());
		transaction.setUser(customer);
		transaction.setType(TransactionType.ACCOUNT_TRANSFER.value());
		transaction.setStatus(BankTransactionStatus.PENDING.value());
		transaction.setTransactionRefId(TransactionIdGenerator.generateUniqueTransactionRefId());

		bankTransactionService.addTransaction(transaction);

		response.setResponseMessage("Quick Account Transfer Request Intiated!!!");
		response.setSuccess(true);

		return new ResponseEntity<CommonApiResponse>(response, HttpStatus.OK);
	}

}
