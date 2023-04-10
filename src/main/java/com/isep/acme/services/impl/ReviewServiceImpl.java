package com.isep.acme.services.impl;

import com.isep.acme.constants.Constants;
import com.isep.acme.controllers.ResourceNotFoundException;
import com.isep.acme.model.Product;
import com.isep.acme.model.Review;
import com.isep.acme.model.dtos.CreateReviewDTO;
import com.isep.acme.model.dtos.ReviewDTO;
import com.isep.acme.model.mappers.ReviewMapper;
import com.isep.acme.repositories.ProductRepository;
import com.isep.acme.repositories.ReviewRepository;
import com.isep.acme.services.RestService;
import com.isep.acme.services.ReviewService;
import com.isep.acme.services.UserService;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    ReviewRepository repository;
    @Autowired
    ProductRepository pRepository;
    @Autowired
    UserService userService;
    @Autowired
    RestService restService;
    @Autowired
    RabbitTemplate rabbitTemplate;

    @Override
    public ReviewDTO create(final CreateReviewDTO createReviewDTO, String sku) {
        final Optional<Product> product = pRepository.findBySku(sku);
        final var user = userService.getUserId(createReviewDTO.getUserID());
        if(!product.isEmpty() && !user.isEmpty()){
            LocalDate date = LocalDate.now();
            String funfact = restService.getFunFact(date);
            if (funfact != null){
                Review review = new Review(createReviewDTO.getReviewText(), date,product.get(), funfact, user.get());
                Review reviewReturned = repository.save(review);
                this.rabbitTemplate.convertAndSend(Constants.EXCHANGE, "", reviewReturned, createMessageProcessor(Constants.CREATED_REVIEW_HEADER));
                return ReviewMapper.toDto(review);
            }
        }
        return null;
    }

    @Override
    public Boolean DeleteReview(Long reviewId)  {
        Optional<Review> rev = repository.findById(reviewId);
        if (!rev.isEmpty()){
            Review r = rev.get();
            if (r.getUpVote().isEmpty() && r.getDownVote().isEmpty()) {
                repository.delete(r);
                this.rabbitTemplate.convertAndSend(Constants.EXCHANGE, "", r, createMessageProcessor(Constants.DELETED_REVIEW_HEADER));
                return true;
            }
        }
        return false;
    }

    @Override
    public ReviewDTO moderateReview(Long reviewID, String approved) throws ResourceNotFoundException, IllegalArgumentException {
        Optional<Review> r = repository.findById(reviewID);
        if(r.isEmpty()){throw new ResourceNotFoundException("Review not found");}
        Boolean ap = r.get().setApprovalStatus(approved);
        if(!ap) {throw new IllegalArgumentException("Invalid status value");}
        Review review = repository.save(r.get());
        this.rabbitTemplate.convertAndSend(Constants.EXCHANGE, "", review, createMessageProcessor(Constants.MODERATED_REVIEW_HEADER));
        return ReviewMapper.toDto(review);
    }

    @Override
    public MessagePostProcessor createMessageProcessor(String header) {
        return message -> {
            message.getMessageProperties().setHeader("action", header);
            return message;
        };
    }
    /*
    @Override
    public boolean addVoteToReview(Long reviewID, VoteReviewDTO voteReviewDTO) {

        Optional<Review> review = this.repository.findById(reviewID);

        if (review.isEmpty()) return false;

        Vote vote = new Vote(voteReviewDTO.getVote(), voteReviewDTO.getUserID());
        if (voteReviewDTO.getVote().equalsIgnoreCase("upVote")) {
            boolean added = review.get().addUpVote(vote);

            if (added) {
                Review reviewUpdated = this.repository.save(review.get());
                return reviewUpdated != null;
            }
        } else if (voteReviewDTO.getVote().equalsIgnoreCase("downVote")) {
            boolean added = review.get().addDownVote(vote);
            if (added) {
                Review reviewUpdated = this.repository.save(review.get());
                return reviewUpdated != null;
            }
        }
        return false;
    }*/
}