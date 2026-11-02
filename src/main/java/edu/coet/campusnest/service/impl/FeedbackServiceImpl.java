package edu.coet.campusnest.service.impl;

import edu.coet.campusnest.dao.FeedbackDao;
import edu.coet.campusnest.exception.CampusNestException;
import edu.coet.campusnest.exception.ValidationException;
import edu.coet.campusnest.model.Feedback;
import edu.coet.campusnest.service.FeedbackService;
import java.util.List;

/**
 * Implementation of FeedbackService.
 */
public class FeedbackServiceImpl implements FeedbackService {

    private final FeedbackDao feedbackDao;

    public FeedbackServiceImpl(FeedbackDao feedbackDao) {
        this.feedbackDao = feedbackDao;
    }

    @Override
    public void submitFeedback(Feedback feedback) throws ValidationException, CampusNestException {
        feedback.validate();
        feedbackDao.save(feedback);
    }

    @Override
    public List<Feedback> getAllFeedback() throws CampusNestException {
        return feedbackDao.findAllRecent();
    }
}
