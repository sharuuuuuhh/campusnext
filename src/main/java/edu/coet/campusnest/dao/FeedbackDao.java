package edu.coet.campusnest.dao;

import edu.coet.campusnest.exception.DatabaseOperationException;
import edu.coet.campusnest.model.Feedback;
import java.util.List;

/**
 * Data Access Object interface for user feedback and suggestions.
 */
public interface FeedbackDao extends GenericDao<Feedback, Long> {
    List<Feedback> findAllRecent() throws DatabaseOperationException;
    long countNewFeedback() throws DatabaseOperationException;
}
