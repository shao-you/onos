package org.onlab.onos.cluster.impl;

import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.onos.cluster.ClusterEventListener;
import org.onlab.onos.cluster.ClusterService;
import org.onlab.onos.cluster.ControllerNode;
import org.onlab.onos.cluster.ControllerNode.State;
import org.onlab.onos.cluster.DefaultControllerNode;
import org.onlab.onos.cluster.MastershipService;
import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.event.impl.TestEventDispatcher;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.trivial.impl.SimpleMastershipStore;
import org.onlab.packet.IpPrefix;

import static org.junit.Assert.assertEquals;
import static org.onlab.onos.net.MastershipRole.*;

/**
 * Test codifying the mastership service contracts.
 */
public class MastershipManagerTest {

    private static final NodeId NID_LOCAL = new NodeId("local");
    private static final NodeId NID_OTHER = new NodeId("foo");
    private static final IpPrefix LOCALHOST = IpPrefix.valueOf("127.0.0.1");
    private static final DeviceId DEV_MASTER = DeviceId.deviceId("of:1");
    private static final DeviceId DEV_OTHER = DeviceId.deviceId("of:2");

    private MastershipManager mgr;
    protected MastershipService service;

    @Before
    public void setUp() {
        mgr = new MastershipManager();
        service = mgr;
        mgr.store = new SimpleMastershipStore();
        mgr.eventDispatcher = new TestEventDispatcher();
        mgr.clusterService = new TestClusterService();
        mgr.activate();
    }

    @After
    public void tearDown() {
        mgr.deactivate();
        mgr.clusterService = null;
        mgr.eventDispatcher = null;
        mgr.store = null;
    }

    @Test
    public void setRole() {
        mgr.setRole(NID_OTHER, DEV_MASTER, MASTER);
        assertEquals("wrong local role:", STANDBY, mgr.getLocalRole(DEV_MASTER));

        //set to master
        mgr.setRole(NID_LOCAL, DEV_MASTER, MASTER);
        assertEquals("wrong local role:", MASTER, mgr.getLocalRole(DEV_MASTER));
    }

    @Test
    public void relinquishMastership() {
        //TODO
    }

    @Test
    public void requestRoleFor() {
        mgr.setRole(NID_LOCAL, DEV_MASTER, MASTER);
        mgr.setRole(NID_OTHER, DEV_OTHER, MASTER);

        //local should be master for one but standby for other
        assertEquals("wrong role:", MASTER, mgr.requestRoleFor(DEV_MASTER));
        assertEquals("wrong role:", STANDBY, mgr.requestRoleFor(DEV_OTHER));
    }

    @Test
    public void getMasterFor() {
        mgr.setRole(NID_LOCAL, DEV_MASTER, MASTER);
        mgr.setRole(NID_OTHER, DEV_OTHER, MASTER);
        assertEquals("wrong master:", NID_LOCAL, mgr.getMasterFor(DEV_MASTER));
        assertEquals("wrong master:", NID_OTHER, mgr.getMasterFor(DEV_OTHER));

        //have NID_OTHER hand over DEV_OTHER to NID_LOCAL
        mgr.setRole(NID_LOCAL, DEV_OTHER, MASTER);
        assertEquals("wrong master:", NID_LOCAL, mgr.getMasterFor(DEV_OTHER));
    }

    @Test
    public void getDevicesOf() {
        mgr.setRole(NID_LOCAL, DEV_MASTER, MASTER);
        mgr.setRole(NID_LOCAL, DEV_OTHER, STANDBY);
        assertEquals("should be one device:", 1, mgr.getDevicesOf(NID_LOCAL).size());

        //hand both devices to NID_LOCAL
        mgr.setRole(NID_LOCAL, DEV_OTHER, MASTER);
        assertEquals("should be two devices:", 2, mgr.getDevicesOf(NID_LOCAL).size());
    }

    private final class TestClusterService implements ClusterService {

        ControllerNode local = new DefaultControllerNode(NID_LOCAL, LOCALHOST);

        @Override
        public ControllerNode getLocalNode() {
            return local;
        }

        @Override
        public Set<ControllerNode> getNodes() {
            return null;
        }

        @Override
        public ControllerNode getNode(NodeId nodeId) {
            return null;
        }

        @Override
        public State getState(NodeId nodeId) {
            return null;
        }

        @Override
        public void addListener(ClusterEventListener listener) {
        }

        @Override
        public void removeListener(ClusterEventListener listener) {
        }

    }
}