package com.adriangniadek.BankingSystem.service;

import com.adriangniadek.BankingSystem.dto.CreateTransferRequest;
import com.adriangniadek.BankingSystem.dto.PageResponse;
import com.adriangniadek.BankingSystem.dto.TransferDTO;

public interface TransferService {
    TransferDTO createTransfer(CreateTransferRequest request, String createdBy);
    PageResponse<TransferDTO> getTransfersForAccount(Long accountId, int page, int size);
}
