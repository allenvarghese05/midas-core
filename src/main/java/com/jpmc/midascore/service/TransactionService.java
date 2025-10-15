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

    public TransactionService(UserRepository userRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
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

        // All validations passed - process the transaction
        logger.info("Transaction valid - processing: {} -> {} amount: {}",
                transaction.getSenderId(), transaction.getRecipientId(), transaction.getAmount());

        // Step 6 & 7: Update sender balance (deduct)
        sender.setBalance(sender.getBalance() - transaction.getAmount());

        // Step 8: Update recipient balance (add)
        recipient.setBalance(recipient.getBalance() + transaction.getAmount());

        // Step 9 & 10: Save updated user records
        userRepository.save(sender);
        userRepository.save(recipient);

        // Step 11, 12 & 13: Create and save transaction record
        TransactionRecord transactionRecord = new TransactionRecord(sender, recipient, transaction.getAmount());
        transactionRepository.save(transactionRecord);

        logger.info("Transaction processed successfully: {}", transactionRecord);
    }
}