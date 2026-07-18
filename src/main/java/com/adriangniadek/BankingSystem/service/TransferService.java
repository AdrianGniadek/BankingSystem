package com.adriangniadek.BankingSystem.service;

import com.adriangniadek.BankingSystem.dto.CreateTransferRequest;
import com.adriangniadek.BankingSystem.dto.TransferDTO;
import java.util.List;

public interface TransferService {
    TransferDTO createTransfer(CreateTransferRequest request);
    List<TransferDTO> getTransfersForAccount(Long accountId);
}
