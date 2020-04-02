/*
 * Copyright (c)  2020 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package cz.kec.coherence;

import java.util.concurrent.SubmissionPublisher;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.FlowAdapters;

@ApplicationScoped
public class MsgProcessingBean {

    private final SubmissionPublisher<String> topicSubmitter = new SubmissionPublisher<>();

    @Incoming("channel1-from-connector")
    public void printTopicMessageChannel1(String msg) {
        System.out.println("Coherence channel 1 topic message: " + msg);
    }

    @Incoming("channel2-from-connector")
    public void printTopicMessageChannel2(String msg) {
        System.out.println("Coherence channel 2 topic message: " + msg);
    }

    @Incoming("channel3-from-connector")
    public void printTopicMessageChannel3(String msg) {
        System.out.println("Coherence channel 3 topic message: " + msg);
    }

    @Outgoing("channel-to-connector")
    public PublisherBuilder<Message<String>> sendTopicMessage() {
        return ReactiveStreams.fromPublisher(FlowAdapters.toPublisher(topicSubmitter))
                .map(Message::of);
    }

    public void broadcast(String message){
        topicSubmitter.submit(message);
    }
}
