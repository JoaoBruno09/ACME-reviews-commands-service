package com.isep.acme.services;

import com.isep.acme.model.dtos.CreateReviewDTO;
import com.isep.acme.model.dtos.ReviewDTO;
import com.isep.acme.model.dtos.VoteReviewDTO;
import org.springframework.amqp.core.MessagePostProcessor;

import javax.transaction.Transactional;

public interface ReviewService {

    ReviewDTO create(CreateReviewDTO createReviewDTO, String sku);
    /*boolean addVoteToReview(Long reviewID, VoteReviewDTO voteReviewDTO);*/
    Boolean DeleteReview(String rId);
    ReviewDTO moderateReview(String rId, String approved);
    MessagePostProcessor createMessageProcessor(String header);

    @Transactional
    boolean addVoteToReview(Long reviewID, VoteReviewDTO voteReviewDTO);
}
