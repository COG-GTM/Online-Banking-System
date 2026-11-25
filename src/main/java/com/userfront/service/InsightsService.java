package com.userfront.service;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.SavingsAccount;
import com.userfront.dto.Insights;

public interface InsightsService {
    Insights buildInsights(PrimaryAccount primaryAccount, SavingsAccount savingsAccount);
}
