package com.kizuna.features.admin.service;

import com.kizuna.features.admin.dto.OperationsAccountSummaryResponse;
import com.kizuna.features.admin.repository.OperationsAccountSummaryRepository;
import com.kizuna.core.exception.BusinessException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.time.Clock;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class OperationsAccountSummaryService {
    private final OperationsActorResolver actors;
    private final OperationsAccountSummaryRepository accounts;
    private final Clock operationsClock;

    public OperationsAccountSummaryResponse read() {
        actors.requireAdmin();
        try { return accounts.read(OffsetDateTime.now(operationsClock)); }
        catch (DataAccessException exception) {
            throw new BusinessException(503, "Tạm thời chưa tải được thống kê tài khoản. Vui lòng thử lại sau.");
        }
    }
}
