/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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
 */
package cz.kec.coherence;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.BeforeDestroyed;
import javax.enterprise.event.Observes;

import io.helidon.microprofile.reactive.hybrid.HybridPublisher;

import com.tangosol.net.Session;
import com.tangosol.net.options.WithConfiguration;
import com.tangosol.net.topic.NamedTopic;
import com.tangosol.net.topic.Publisher;
import com.tangosol.net.topic.Subscriber;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;
import org.eclipse.microprofile.reactive.messaging.spi.ConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.IncomingConnectorFactory;
import org.eclipse.microprofile.reactive.messaging.spi.OutgoingConnectorFactory;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.SubscriberBuilder;

@ApplicationScoped
@Connector("coherence")
public class CoherenceConnector implements IncomingConnectorFactory, OutgoingConnectorFactory {

    private static final Logger LOGGER = Logger.getLogger(CoherenceConnector.class.getName());

    private final ExecutorService executorService = Executors.newWorkStealingPool();
    private final List<Runnable> closeables = new ArrayList<>();


    @Override
    public PublisherBuilder<? extends Message<?>> getPublisherBuilder(final Config config) {

        String channelName = config.getValue(ConnectorFactory.CHANNEL_NAME_ATTRIBUTE, String.class);

        // Topic name from channel context
        String topicName = config.getValue("coherence-topic-name", String.class);

        LOGGER.info("Preparing publisher for channel " + channelName);

        // coherence-settings from connector context
        ((io.helidon.config.Config) config)
                .get("coherence-settings")
                .detach()
                .asMap()
                .get()
                .forEach((k, v) -> {
                    LOGGER.info("Setting system prop: " + k + "=" + v);
                    System.setProperty(k, v);
                });

        Session session = Session.create(WithConfiguration.autoDetect());
        NamedTopic<String> topic = session.getTopic(topicName);
        Subscriber<String> subscriber = topic.createSubscriber();

        SubmissionPublisher<Message<String>> publisher = new SubmissionPublisher<>();

        closeables.add(() -> {
            LOGGER.log(Level.INFO, "Closing connection to topic: " + topicName);
            try {
                session.close();
                topic.close();
                subscriber.close();
                publisher.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error when closing coherence connection to topic " + topicName, e);
            }
        });

        executorService.submit(() -> {
            while (subscriber.isActive()) {
                Subscriber.Element<String> e = subscriber.receive().join();
                publisher.submit(Message.of(e.getValue()));
            }
        });

        return ReactiveStreams.fromPublisher(HybridPublisher.from(publisher));
    }

    @Override
    public SubscriberBuilder<? extends Message<?>, Void> getSubscriberBuilder(final Config config) {

        String channelName = config.getValue(ConnectorFactory.CHANNEL_NAME_ATTRIBUTE, String.class);

        // Topic name from channel context
        String topicName = config.getValue("coherence-topic-name", String.class);

        LOGGER.info("Preparing subscriber for channel " + channelName);

        // coherence-settings from connector context
        ((io.helidon.config.Config) config)
                .get("coherence-settings")
                .detach()
                .asMap()
                .get()
                .forEach((k, v) -> {
                    LOGGER.info("Setting system prop: " + k + "=" + v);
                    System.setProperty(k, v);
                });

        Session session = Session.create();
        NamedTopic<String> topic = session.getTopic(topicName);
        Publisher<String> publisher = topic.createPublisher();

        closeables.add(() -> {
            LOGGER.log(Level.INFO, "Closing connection to topic: " + topicName);
            try {
                session.close();
                topic.close();
                publisher.close();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error when closing coherence connection to topic " + topicName, e);
            }
        });

        return ReactiveStreams.<Message<String>>builder()
                .map(Message::getPayload)
                .forEach(publisher::send);
    }

    private void onShutdown(@Observes @BeforeDestroyed(ApplicationScoped.class) final Object event) {
        closeables.forEach(Runnable::run);
        executorService.shutdown();
        try {
            executorService.awaitTermination(200, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Error when closing coherence connector executor service.", e);
        }
    }
}
