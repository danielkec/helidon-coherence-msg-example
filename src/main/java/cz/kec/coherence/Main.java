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

import io.helidon.microprofile.server.Server;

/**
 * Explicit example.
 */
public class Main {
    private Main() {
    }

    /**
     * Starts server and initializes CDI container manually.
     *
     * @param args command line arguments (ignored)
     */
    public static void main(String[] args) {
        Server server = Server.builder()
                .addApplication(CoherenceMessagingApplication.class)
                .host("localhost")
                .build();

        server.start();
        String endpoint = "http://" + server.host() + ":" + server.port();
        System.out.println("Send topic message " + endpoint + "/example/send/HelloWorld");
    }
}
