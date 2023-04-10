package com.isep.acme.rabbit;

import com.isep.acme.constants.Constants;
import com.isep.acme.model.Review;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class RMQListener {

    @RabbitListener(queues = "#{queue.name}")
    public void listener(Message message){

    }
}