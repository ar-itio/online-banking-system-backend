package com.onlinebankingsystem.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.onlinebankingsystem.dao.BankTransactionDao;
import com.onlinebankingsystem.entity.BankTransaction;
import com.onlinebankingsystem.entity.User;

@Service
public class BankTransactionServiceImpl implements BankTransactionService {

	@Autowired
	private BankTransactionDao bankTransactionDao;

	@Override
	public BankTransaction addTransaction(BankTransaction bankTransaction) {
		// TODO Auto-generated method stub
		return bankTransactionDao.save(bankTransaction);
	}

	@Override
	public BankTransaction updateTransaction(BankTransaction bankTransaction) {
		// TODO Auto-generated method stub
		return bankTransactionDao.save(bankTransaction);
	}

	@Override
	public BankTransaction getTransactionId(int transactionId) {

		Optional<BankTransaction> optional = bankTransactionDao.findById(transactionId);

		if (optional.isPresent()) {
			return optional.get();
		}
		
		return null;
	}

	@Override
	public List<BankTransaction> getTransactionByStatusIn(List<String> status) {
		// TODO Auto-generated method stub
		return bankTransactionDao.findByStatusIn(status);
	}

	@Override
	public List<BankTransaction> getTransactionByStatusInAndUser(List<String> status, User user) {
		// TODO Auto-generated method stub
		return bankTransactionDao.findByStatusInAndUser(status, user);
	}

	@Override
	public List<BankTransaction> getTransactionByTransactionRedIdInAndUser(String transactionRefId, User user) {
		// TODO Auto-generated method stub
		return bankTransactionDao.findByTransactionRefIdContainingIgnoreCaseAndUser(transactionRefId, user);
	}

}
