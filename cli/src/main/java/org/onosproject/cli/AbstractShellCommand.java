/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Annotations;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.osgi.ServiceNotFoundException;

/**
 * Base abstraction of Karaf shell commands.
 */
public abstract class AbstractShellCommand extends OsgiCommandSupport {

    @Option(name = "-j", aliases = "--json", description = "Output JSON",
            required = false, multiValued = false)
    private boolean json = false;

    /**
     * Returns the reference to the implementation of the specified service.
     *
     * @param serviceClass service class
     * @param <T>          type of service
     * @return service implementation
     * @throws org.onlab.osgi.ServiceNotFoundException if service is unavailable
     */
    public static <T> T get(Class<T> serviceClass) {
        return DefaultServiceDirectory.getService(serviceClass);
    }

    /**
     * Returns application ID for the CLI.
     *
     * @return command-line application identifier
     */
    protected ApplicationId appId() {
        return get(CoreService.class).registerApplication("org.onosproject.cli");
    }

    /**
     * Prints the arguments using the specified format.
     *
     * @param format format string; see {@link String#format}
     * @param args   arguments
     */
    public void print(String format, Object... args) {
        System.out.println(String.format(format, args));
    }

    /**
     * Prints the arguments using the specified format to error stream.
     *
     * @param format format string; see {@link String#format}
     * @param args   arguments
     */
    public void error(String format, Object... args) {
        System.err.println(String.format(format, args));
    }

    /**
     * Produces a string image of the specified key/value annotations.
     *
     * @param annotations key/value annotations
     * @return string image with ", k1=v1, k2=v2, ..." pairs
     */
    public static String annotations(Annotations annotations) {
        StringBuilder sb = new StringBuilder();
        for (String key : annotations.keys()) {
            sb.append(", ").append(key).append('=').append(annotations.value(key));
        }
        return sb.toString();
    }

    /**
     * Produces a JSON object from the specified key/value annotations.
     *
     * @param mapper ObjectMapper to use while converting to JSON
     * @param annotations key/value annotations
     * @return JSON object
     */
    public static ObjectNode annotations(ObjectMapper mapper, Annotations annotations) {
        ObjectNode result = mapper.createObjectNode();
        for (String key : annotations.keys()) {
            result.put(key, annotations.value(key));
        }
        return result;
    }

    /**
     * Executes this command.
     */
    protected abstract void execute();

    /**
     * Indicates whether JSON format should be output.
     *
     * @return true if JSON is requested
     */
    protected boolean outputJson() {
        return json;
    }

    @Override
    protected Object doExecute() throws Exception {
        try {
            execute();
        } catch (ServiceNotFoundException e) {
            error(e.getMessage());
        }
        return null;
    }

}
