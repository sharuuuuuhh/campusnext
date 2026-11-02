package edu.coet.campusnest.service;

import edu.coet.campusnest.exception.CampusNestException;
import edu.coet.campusnest.exception.ResourceNotFoundException;
import edu.coet.campusnest.exception.ValidationException;
import edu.coet.campusnest.model.Inquiry;
import java.util.List;

/**
 * Service interface for Student Inquiries.
 */
public interface InquiryService {
    void submitInquiry(Inquiry inquiry) throws ValidationException, ResourceNotFoundException, CampusNestException;
    List<Inquiry> getInquiriesForOwner(long ownerId) throws CampusNestException;
}
