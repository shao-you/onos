package org.onlab.onos.net.intent.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.ElementId;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;
import org.onlab.onos.net.intent.Intent;
import org.onlab.onos.net.intent.IntentId;
import org.onlab.onos.net.intent.IntentTestsMocks;
import org.onlab.onos.net.intent.LinkCollectionIntent;
import org.onlab.onos.net.intent.MultiPointToSinglePointIntent;
import org.onlab.onos.net.topology.LinkWeight;
import org.onlab.onos.net.topology.PathService;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.onlab.onos.net.NetTestTools.connectPoint;
import static org.onlab.onos.net.NetTestTools.createPath;
import static org.onlab.onos.net.intent.LinksHaveEntryWithSourceDestinationPairMatcher.linksHasPath;

/**
 * Unit tests for the MultiPointToSinglePoint intent compiler.
 */
public class TestMultiPointToSinglePointIntentCompiler {

    private TrafficSelector selector = new IntentTestsMocks.MockSelector();
    private TrafficTreatment treatment = new IntentTestsMocks.MockTreatment();

    /**
     * Mock path service for creating paths within the test.
     */
    private static class MockPathService implements PathService {

        final String[] pathHops;

        /**
         * Constructor that provides a set of hops to mock.
         *
         * @param pathHops path hops to mock
         */
        MockPathService(String[] pathHops) {
            this.pathHops = pathHops;
        }

        @Override
        public Set<Path> getPaths(ElementId src, ElementId dst) {
            Set<Path> result = new HashSet<>();

            String[] allHops = new String[pathHops.length + 1];
            allHops[0] = src.toString();
            System.arraycopy(pathHops, 0, allHops, 1, pathHops.length);

            result.add(createPath(allHops));
            return result;
        }

        @Override
        public Set<Path> getPaths(ElementId src, ElementId dst, LinkWeight weight) {
            return null;
        }
    }

    /**
     * Creates a MultiPointToSinglePoint intent for a group of ingress points
     * and an egress point.
     *
     * @param ingressIds array of ingress device ids
     * @param egressId device id of the egress point
     * @return MultiPointToSinglePoint intent
     */
    private MultiPointToSinglePointIntent makeIntent(String[] ingressIds, String egressId) {
        Set<ConnectPoint> ingressPoints = new HashSet<>();
        ConnectPoint egressPoint = connectPoint(egressId, 1);

        for (String ingressId : ingressIds) {
                ingressPoints.add(connectPoint(ingressId, 1));
        }

        return new MultiPointToSinglePointIntent(
                new IntentId(12),
                selector,
                treatment,
                ingressPoints,
                egressPoint);
    }

    /**
     * Creates a compiler for MultiPointToSinglePoint intents.
     *
     * @param hops hops to use while computing paths for this intent
     * @return MultiPointToSinglePoint intent
     */
    private MultiPointToSinglePointIntentCompiler makeCompiler(String[] hops) {
        MultiPointToSinglePointIntentCompiler compiler =
                new MultiPointToSinglePointIntentCompiler();
        compiler.pathService = new MockPathService(hops);
        IdBlockAllocator idBlockAllocator = new DummyIdBlockAllocator();
        compiler.intentIdGenerator =
                new IdBlockAllocatorBasedIntentIdGenerator(idBlockAllocator);
        return compiler;
    }

    /**
     * Tests a single ingress point with 8 hops to its egress point.
     */
    @Test
    public void testSingleLongPathCompilation() {

        String[] ingress = {"ingress"};
        String egress = "egress";

        MultiPointToSinglePointIntent intent = makeIntent(ingress, egress);
        assertThat(intent, is(notNullValue()));

        String[] hops = {"h1", "h2", "h3", "h4", "h5", "h6", "h7", "h8",
                         egress};
        MultiPointToSinglePointIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent);
        assertThat(result, is(Matchers.notNullValue()));
        assertThat(result, hasSize(1));
        Intent resultIntent = result.get(0);
        assertThat(resultIntent instanceof LinkCollectionIntent, is(true));

        if (resultIntent instanceof LinkCollectionIntent) {
            LinkCollectionIntent linkIntent = (LinkCollectionIntent) resultIntent;
            assertThat(linkIntent.links(), hasSize(9));
            assertThat(linkIntent.links(), linksHasPath("ingress", "h1"));
            assertThat(linkIntent.links(), linksHasPath("h1", "h2"));
            assertThat(linkIntent.links(), linksHasPath("h2", "h3"));
            assertThat(linkIntent.links(), linksHasPath("h4", "h5"));
            assertThat(linkIntent.links(), linksHasPath("h5", "h6"));
            assertThat(linkIntent.links(), linksHasPath("h7", "h8"));
            assertThat(linkIntent.links(), linksHasPath("h8", "egress"));
        }
    }

    /**
     * Tests a simple topology where two ingress points share some path segments
     * and some path segments are not shared.
     */
    @Test
    public void testTwoIngressCompilation() {
        String[] ingress = {"ingress1", "ingress2"};
        String egress = "egress";

        MultiPointToSinglePointIntent intent = makeIntent(ingress, egress);
        assertThat(intent, is(notNullValue()));

        final String[] hops = {"inner1", "inner2", egress};
        MultiPointToSinglePointIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent);
        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(1));
        Intent resultIntent = result.get(0);
        assertThat(resultIntent instanceof LinkCollectionIntent, is(true));

        if (resultIntent instanceof LinkCollectionIntent) {
            LinkCollectionIntent linkIntent = (LinkCollectionIntent) resultIntent;
            assertThat(linkIntent.links(), hasSize(4));
            assertThat(linkIntent.links(), linksHasPath("ingress1", "inner1"));
            assertThat(linkIntent.links(), linksHasPath("ingress2", "inner1"));
            assertThat(linkIntent.links(), linksHasPath("inner1", "inner2"));
            assertThat(linkIntent.links(), linksHasPath("inner2", "egress"));
        }
    }

    /**
     * Tests a large number of ingress points that share a common path to the
     * egress point.
     */
    @Test
    public void testMultiIngressCompilation() {
        String[] ingress = {"i1", "i2", "i3", "i4", "i5",
                            "i6", "i7", "i8", "i9", "i10"};
        String egress = "e";

        MultiPointToSinglePointIntent intent = makeIntent(ingress, egress);
        assertThat(intent, is(notNullValue()));

        final String[] hops = {"n1", egress};
        MultiPointToSinglePointIntentCompiler compiler = makeCompiler(hops);
        assertThat(compiler, is(notNullValue()));

        List<Intent> result = compiler.compile(intent);
        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(1));
        Intent resultIntent = result.get(0);
        assertThat(resultIntent instanceof LinkCollectionIntent, is(true));

        if (resultIntent instanceof LinkCollectionIntent) {
            LinkCollectionIntent linkIntent = (LinkCollectionIntent) resultIntent;
            assertThat(linkIntent.links(), hasSize(ingress.length + 1));
            for (String ingressToCheck : ingress) {
                assertThat(linkIntent.links(),
                           linksHasPath(ingressToCheck,
                                        "n1"));
            }
            assertThat(linkIntent.links(), linksHasPath("n1", egress));
        }
    }
}