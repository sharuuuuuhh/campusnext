package edu.coet.campusnest.dao;

import edu.coet.campusnest.exception.DatabaseOperationException;
import edu.coet.campusnest.model.Inquiry;
import java.util.List;

/**
 * Data Access Object interface for Student Inquiries.
 */
public interface InquiryDao extends GenericDao<Inquiry, Long> {
    List<Inquiry> findByOwnerId(long ownerId) throws DatabaseOperationException;
    List<Inquiry> findByStudentUserId(long studentUserId) throws DatabaseOperationException;
    long countNewInquiries() throws DatabaseOperationException;
}
