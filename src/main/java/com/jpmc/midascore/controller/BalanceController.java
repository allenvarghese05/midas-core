package com.jpmc.midascore.controller;

import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Balance;
import com.jpmc.midascore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class BalanceController {

    private static final Logger logger = LoggerFactory.getLogger(BalanceController.class);

    private final UserRepository userRepository;

    public BalanceController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/balance")
    public Balance getBalance(@RequestParam Long userId) {
        logger.debug("Balance query for userId: {}", userId);

        UserRecord user = userRepository.findById(userId.longValue());

        if (user == null) {
            logger.info("User with id {} not found, returning balance 0", userId);
            return new Balance(0.0f);
        }

        float balance = user.getBalance();
        logger.info("Returning balance {} for user {}", balance, userId);
        return new Balance(balance);
    }
}