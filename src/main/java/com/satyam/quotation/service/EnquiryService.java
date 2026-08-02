package com.satyam.quotation.service;

import com.satyam.quotation.model.Enquiry;

import java.util.List;
import java.util.Optional;

public interface EnquiryService {
    Enquiry create(Enquiry enquiry, Long userId, Long companyId);
    List<Enquiry> getByCompany(Long companyId);
    List<Enquiry> getByUser(Long userId);
    List<Enquiry> getAll();
    Optional<Enquiry> getById(Long id);
    Enquiry update(Enquiry enquiry, Long userId);
    void delete(Long id, Long userId);
}
