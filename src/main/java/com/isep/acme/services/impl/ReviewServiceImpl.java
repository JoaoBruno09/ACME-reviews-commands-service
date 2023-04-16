package com.isep.acme.services.impl;

import com.isep.acme.constants.Constants;
import com.isep.acme.controllers.ResourceNotFoundException;
import com.isep.acme.model.Product;
import com.isep.acme.model.Review;
import com.isep.acme.model.Vote;
import com.isep.acme.model.dtos.CreateReviewDTO;
import com.isep.acme.model.dtos.ReviewDTO;
import com.isep.acme.model.dtos.VoteReviewDTO;
import com.isep.acme.model.mappers.ReviewMapper;
import com.isep.acme.model.mappers.VoteMapper;
import com.isep.acme.repositories.ProductRepository;
import com.isep.acme.repositories.ReviewRepository;
import com.isep.acme.repositories.VoteRepository;
import com.isep.acme.services.RestService;
import com.isep.acme.services.ReviewService;
import com.isep.acme.services.UserService;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDate;
import java.util.Optional;

@Service
public class ReviewServiceImpl implements ReviewService {

    @Autowired
    ReviewRepository repository;
    @Autowired
    ProductRepository pRepository;

    @Autowired
    VoteRepository voteRepository;
    @Autowired
    UserService userService;
    @Autowired
    RestService restService;
    @Autowired
    RabbitTemplate rabbitTemplate;

    private static final VoteMapper VOTE_MAPPER = VoteMapper.INSTANCE;

    @Override
    public ReviewDTO create(final CreateReviewDTO createReviewDTO, String sku) {
        final Optional<Product> product = pRepository.findBySku(sku);
        final var user = userService.getUserId(createReviewDTO.getUserID());
        if(!product.isEmpty() && !user.isEmpty()){
            LocalDate date = LocalDate.now();
            String funfact = restService.getFunFact(date);
            if (funfact != null){
                Review review = new Review(createReviewDTO.getReviewText(), date,product.get(), funfact, user.get());
                ReviewDTO reviewDTO = ReviewMapper.toDto(repository.save(review));
                this.rabbitTemplate.convertAndSend(Constants.EXCHANGE, "", reviewDTO, createMessageProcessor(Constants.CREATED_REVIEW_HEADER));
                return reviewDTO;
            }
        }
        return null;
    }

    @Override
    public Boolean DeleteReview(String RID)  {
        Optional<Review> rev = repository.findByRID(RID);
        if (!rev.isEmpty()){
            Review r = rev.get();
            if (r.getUpVote().isEmpty() && r.getDownVote().isEmpty()) {
                repository.delete(r);
                this.rabbitTemplate.convertAndSend(Constants.EXCHANGE, "", ReviewMapper.toDto(repository.save(r)), createMessageProcessor(Constants.DELETED_REVIEW_HEADER));
                return true;
            }
        }
        return false;
    }

    @Override
    public ReviewDTO moderateReview(String RID, String approved) throws ResourceNotFoundException, IllegalArgumentException {
        Optional<Review> r = repository.findByRID(RID);
        if(r.isEmpty()){throw new ResourceNotFoundException("Review not found");}
        Boolean ap = r.get().setApprovalStatus(approved);
        if(!ap) {throw new IllegalArgumentException("Invalid status value");}
        ReviewDTO reviewDTO = ReviewMapper.toDto(repository.save(r.get()));
        this.rabbitTemplate.convertAndSend(Constants.EXCHANGE, "", reviewDTO, createMessageProcessor(Constants.MODERATED_REVIEW_HEADER));
        return reviewDTO;
    }

    @Override
    public MessagePostProcessor createMessageProcessor(String header) {
        return message -> {
            message.getMessageProperties().setHeader("action", header);
            return message;
        };
    }

    @Transactional
    @Override
    public boolean addVoteToReview(VoteReviewDTO voteReviewDTO, Vote vote) {

        Optional<Review> review = this.repository.findByRID(voteReviewDTO.getRID());

        if (review.isEmpty()) return false;

        if (voteReviewDTO.getVote().equalsIgnoreCase("upVote") && review.get().addUpVote(vote)
                || voteReviewDTO.getVote().equalsIgnoreCase("downVote") && review.get().addDownVote(vote)) {

            repository.save(review.get());
            return true;

        }
        return false;
    }

}