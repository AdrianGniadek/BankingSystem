package com.adriangniadek.BankingSystem.service;

import com.adriangniadek.BankingSystem.dto.TransferDTO;
import java.util.List;

public interface TransferService {
    TransferDTO createTransfer(TransferDTO transferDTO);
    List<TransferDTO> getTransfersForAccount(Long accountId);
}
