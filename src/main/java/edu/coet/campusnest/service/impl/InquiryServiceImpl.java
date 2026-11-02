package edu.coet.campusnest.service.impl;

import edu.coet.campusnest.dao.InquiryDao;
import edu.coet.campusnest.exception.CampusNestException;
import edu.coet.campusnest.exception.ResourceNotFoundException;
import edu.coet.campusnest.exception.ValidationException;
import edu.coet.campusnest.model.Inquiry;
import edu.coet.campusnest.service.InquiryService;
import java.util.List;

/**
 * Implementation of InquiryService.
 */
public class InquiryServiceImpl implements InquiryService {

    private final InquiryDao inquiryDao;

    public InquiryServiceImpl(InquiryDao inquiryDao) {
        this.inquiryDao = inquiryDao;
    }

    @Override
    public void submitInquiry(Inquiry inquiry) throws ValidationException, ResourceNotFoundException, CampusNestException {
        inquiry.validate();
        Inquiry saved = inquiryDao.save(inquiry);
        if (saved == null) {
            throw new ResourceNotFoundException("This PG listing is no longer available for inquiries.");
        }
    }

    @Override
    public List<Inquiry> getInquiriesForOwner(long ownerId) throws CampusNestException {
        return inquiryDao.findByOwnerId(ownerId);
    }
}
