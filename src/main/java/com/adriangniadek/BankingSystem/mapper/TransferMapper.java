package com.adriangniadek.BankingSystem.mapper;

import com.adriangniadek.BankingSystem.dto.TransferDTO;
import com.adriangniadek.BankingSystem.model.Transfer;
import org.springframework.stereotype.Component;

@Component
public class TransferMapper {

    public TransferDTO toDto(Transfer transfer) {
        return new TransferDTO(
                transfer.getId(),
                transfer.getSourceAccount().getId(),
                transfer.getTargetAccount().getId(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getDescription(),
                transfer.getStatus(),
                transfer.getCreatedAt()
        );
    }

    public Transfer toEntity(TransferDTO transferDTO) {
        Transfer transfer = new Transfer();
        transfer.setId(transferDTO.id());
        transfer.setAmount(transferDTO.amount());
        transfer.setCurrency(transferDTO.currency());
        transfer.setDescription(transferDTO.description());
        transfer.setStatus(transferDTO.status());
        transfer.setCreatedAt(transferDTO.createdAt());

        return transfer;
    }
}
