package com.isep.acme.services;

import com.isep.acme.model.dtos.CreateReviewDTO;
import com.isep.acme.model.dtos.ReviewDTO;
import org.springframework.amqp.core.MessagePostProcessor;

public interface ReviewService {

    ReviewDTO create(CreateReviewDTO createReviewDTO, String sku);
    /*boolean addVoteToReview(Long reviewID, VoteReviewDTO voteReviewDTO);*/
    Boolean DeleteReview(String rId);
    ReviewDTO moderateReview(String rId, String approved);
    MessagePostProcessor createMessageProcessor(String header);
}
