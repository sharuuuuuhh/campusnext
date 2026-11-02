package edu.coet.campusnest.service;

import edu.coet.campusnest.exception.CampusNestException;
import edu.coet.campusnest.exception.ValidationException;
import edu.coet.campusnest.model.Feedback;
import java.util.List;

/**
 * Service interface for Feedback management.
 */
public interface FeedbackService {
    void submitFeedback(Feedback feedback) throws ValidationException, CampusNestException;
    List<Feedback> getAllFeedback() throws CampusNestException;
}
