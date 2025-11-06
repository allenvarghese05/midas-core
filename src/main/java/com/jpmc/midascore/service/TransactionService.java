package com.jpmc.midascore.service;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final IncentiveService incentiveService;

    public TransactionService(UserRepository userRepository,
                              TransactionRepository transactionRepository,
                              IncentiveService incentiveService) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.incentiveService = incentiveService;
    }

    @Transactional
    public void processTransaction(Transaction transaction) {
        logger.debug("Processing transaction: {}", transaction);

        // Step 1 & 2: Fetch and validate sender exists
        UserRecord sender = userRepository.findById(transaction.getSenderId());
        if (sender == null) {
            logger.warn("Transaction rejected: Invalid sender ID {}", transaction.getSenderId());
            return;
        }

        // Step 3 & 4: Fetch and validate recipient exists
        UserRecord recipient = userRepository.findById(transaction.getRecipientId());
        if (recipient == null) {
            logger.warn("Transaction rejected: Invalid recipient ID {}", transaction.getRecipientId());
            return;
        }

        // Step 5: Validate sender has sufficient balance
        if (sender.getBalance() < transaction.getAmount()) {
            logger.warn("Transaction rejected: Insufficient balance. Sender {} has {} but needs {}",
                    transaction.getSenderId(), sender.getBalance(), transaction.getAmount());
            return;
        }

        // All validations passed - get incentive from API
        float incentive = incentiveService.getIncentive(transaction);
        logger.info("Transaction valid - processing: {} -> {} amount: {} incentive: {}",
                transaction.getSenderId(), transaction.getRecipientId(),
                transaction.getAmount(), incentive);

        // Update sender balance (deduct transaction amount only)
        sender.setBalance(sender.getBalance() - transaction.getAmount());

        // Update recipient balance (add transaction amount + incentive)
        recipient.setBalance(recipient.getBalance() + transaction.getAmount() + incentive);

        // Save updated user records
        userRepository.save(sender);
        userRepository.save(recipient);

        // Create and save transaction record with incentive
        TransactionRecord transactionRecord = new TransactionRecord(
                sender,
                recipient,
                transaction.getAmount(),
                incentive
        );
        transactionRepository.save(transactionRecord);

        logger.info("Transaction processed successfully: {}", transactionRecord);
    }
}