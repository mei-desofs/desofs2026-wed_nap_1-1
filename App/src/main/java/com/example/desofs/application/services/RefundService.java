package com.example.desofs.application.services;

import com.example.desofs.domain.entities.RefundRequest;
import com.example.desofs.domain.repositories.RefundRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RefundService {
    private final RefundRequestRepository refundRequestRepository;

    public RefundService(RefundRequestRepository refundRequestRepository) {
        this.refundRequestRepository = refundRequestRepository;
    }

    public List<RefundRequest> listAll() {
        return refundRequestRepository.findAll();
    }

    public Optional<RefundRequest> get(Long id) {
        return refundRequestRepository.findById(id);
    }

    public RefundRequest create(RefundRequest refund) {
        return refundRequestRepository.save(refund);
    }

    public RefundRequest approve(Long id) {
        return refundRequestRepository.findById(id).map(refund -> {
            refund.setStatus(RefundRequest.RefundStatus.APPROVED);
            return refundRequestRepository.save(refund);
        }).orElse(null);
    }

    public RefundRequest reject(Long id, String reason) {
        return refundRequestRepository.findById(id).map(refund -> {
            refund.setStatus(RefundRequest.RefundStatus.REJECTED);
            refund.setReason(reason);
            return refundRequestRepository.save(refund);
        }).orElse(null);
    }

    public RefundRequest complete(Long id) {
        return refundRequestRepository.findById(id).map(refund -> {
            //TEM ERRO AQUIIIIIII!
            //refund.setStatus(RefundRequest.RefundStatus.COMPLETED);
            return refundRequestRepository.save(refund);
        }).orElse(null);
    }
}
