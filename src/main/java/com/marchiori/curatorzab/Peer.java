/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marchiori.curatorzab;

import static com.marchiori.curatorzab.configuration.Configuration.DELAY;
import com.marchiori.curatorzab.message.FollowerInfo;
import com.marchiori.curatorzab.message.Message;
import com.marchiori.curatorzab.message.Commit;
import com.marchiori.curatorzab.message.AckNewLeader;
import com.marchiori.curatorzab.message.NewEpoch;
import com.marchiori.curatorzab.message.Propose;
import com.marchiori.curatorzab.message.AckEpoch;
import com.marchiori.curatorzab.message.NewLeader;
import com.marchiori.curatorzab.message.Ack;
import com.marchiori.curatorzab.message.Transaction;
import static com.marchiori.curatorzab.configuration.Configuration.ELECTION_NAMESPACE;
import static com.marchiori.curatorzab.configuration.Configuration.LOCK_NODES;
import static com.marchiori.curatorzab.configuration.Configuration.MAX_RETRIES;
import static com.marchiori.curatorzab.configuration.Configuration.MAX_TIMEOUT;
import static com.marchiori.curatorzab.configuration.Configuration.MILLISECONDS;
import static com.marchiori.curatorzab.configuration.Configuration.MIN_CLUSTER;
import static com.marchiori.curatorzab.configuration.Configuration.MIN_TIMEOUT;
import static com.marchiori.curatorzab.configuration.Configuration.OLDER_NODE;
import static com.marchiori.curatorzab.configuration.Configuration.SESSION_TIMEOUT;
import static com.marchiori.curatorzab.configuration.Configuration.ZOOKEEPER_INSTANCE;
import com.marchiori.curatorzab.message.Clear;
import com.marchiori.curatorzab.message.CommitLeader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.NodeCache;
import org.apache.curator.framework.recipes.cache.NodeCacheListener;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCache.StartMode;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

/**
 * Peer in ZAB algorithm.
 *
 * @author Matteo Marchiori
 */
public final class Peer {

    /**
     * Curator framework to manage easily ZooKeeper.
     */
    private static final CuratorFramework CURATOR
            = CuratorFrameworkFactory.builder()
                    .connectString(ZOOKEEPER_INSTANCE)
                    .sessionTimeoutMs(SESSION_TIMEOUT).retryPolicy(
                    new ExponentialBackoffRetry(
                            SESSION_TIMEOUT,
                            MAX_RETRIES)
            ).build();
    /**
     * Curator cache to observe myself.
     */
    private final NodeCache me;

    /**
     * Curator cache to observe other nodes.
     */
    private final PathChildrenCache leader;

    /**
     * Mutex to read/write on other nodes.
     */
    private final InterProcessSemaphoreMutex mutex;

    /**
     * Used to check if ZAB is finished.
     */
    private boolean zabFinished;

    /**
     * Timer to repeat ZAB if not succeeded.
     */
    private ScheduledExecutorService timer;

    /**
     * State of node in ZAB algorithm.
     */
    private State state;

    /**
     * My phase of ZAB algorithm.
     */
    private Phase phase;

    /**
     * Id of my node in ZooKeeper.
     */
    private final String id;

    /**
     * Prospective leader for ZAB.
     */
    private String prospective;

    /**
     * List used for ACKEPOCHS messages.
     */
    private List<AckEpoch> ackepochs;

    /**
     * List used for ACKNEWLEADER messages.
     */
    private List<AckNewLeader> acksnewleader;

    /**
     * List used for ACKS messages.
     */
    private List<Ack> acks;

    /**
     * Quorum of nodes.
     */
    private List<FollowerInfo> quorum;

    /**
     * History of transactions.
     */
    private List<Transaction> history;

    /**
     * Last NEWEPOCH accepted message.
     */
    private int acceptedEpoch;

    /**
     * Last NEWLEADER message accepted.
     */
    private int currentEpoch;

    /**
     * Last ZXID.
     */
    private Zxid lastZxid;

    /**
     * Contructor for a new Peer in ZAB.
     *
     * @throws Exception curator exceptions.
     */
    public Peer() throws Exception {
        CURATOR.start();
        log("Creating new peer.");
        history = new ArrayList<>();
        acceptedEpoch = 0;
        currentEpoch = 0;
        lastZxid = new Zxid(0, 0);
        prospective = null;

        timer = Executors.newSingleThreadScheduledExecutor();
        ackepochs = new ArrayList<>();
        acksnewleader = new ArrayList<>();
        acks = new ArrayList<>();
        quorum = new ArrayList<>();
        history = new ArrayList<>();

        /*Am I the first peer?*/
        if (CURATOR.checkExists().forPath(ELECTION_NAMESPACE) == null) {
            log("First peer connected.");
            CURATOR.create().creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(ELECTION_NAMESPACE);
            CURATOR.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(ELECTION_NAMESPACE + "/leases");
            CURATOR.create()
                    .creatingParentsIfNeeded()
                    .withMode(CreateMode.PERSISTENT)
                    .forPath(ELECTION_NAMESPACE + "/locks");
        }

        /*Let's create my node*/
        log("Create node on zookeeper...");
        String node = CURATOR.create()
                .withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                .forPath(ELECTION_NAMESPACE + "/c_");
        id = node.replace(ELECTION_NAMESPACE + "/", "");
        history.add(new Transaction(0, lastZxid, id));
        log("Hello, I'm " + id);
        me = new NodeCache(CURATOR, ELECTION_NAMESPACE + "/" + id);
        me.start(true);
        me.getListenable().addListener(new PeerListener());
        leader = new PathChildrenCache(CURATOR, ELECTION_NAMESPACE, false);
        leader.start(StartMode.POST_INITIALIZED_EVENT);
        leader.getListenable().addListener(new LeaderListener());
        mutex = new InterProcessSemaphoreMutex(CURATOR, ELECTION_NAMESPACE);
        electLeader();
    }

    /**
     * Leader election (phase 0) in ZAB.
     */
    private void electLeader() {
        zabFinished = false;
        if (!timer.isShutdown()) {
            timer.shutdown();
        }
        timer = Executors.newSingleThreadScheduledExecutor();
        timer.scheduleAtFixedRate(() -> {
            if (!zabFinished) {
                electLeader();
            }
        },
                MIN_TIMEOUT,
                ThreadLocalRandom.current()
                        .nextInt(MIN_TIMEOUT, MAX_TIMEOUT + 1),
                TimeUnit.SECONDS
        );
        try {
            log("Elect leader...");
            List<String> nodes = CURATOR.getChildren()
                    .forPath(ELECTION_NAMESPACE);
            nodes.sort(String::compareTo);
            prospective = nodes.get(OLDER_NODE);
            if (prospective.equals(id)) {
                log("I'm the prospective leader.");
                state = State.LEADING;
                if (nodes.size() > MIN_CLUSTER) {
                    discoveryLeaderWaitQuorum();
                } else {
                    broadcastLeader();
                }
            } else {
                log("I'm a follower.");
                state = State.FOLLOWING;
                discoveryFollowerInfo();
            }
        } catch (Exception ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Discovery (phase 1) of follower in ZAB algorithm before NEWEPOCH.
     */
    private void discoveryFollowerInfo() {
        log("Phase 1: discovery (follower)");
        phase = Phase.DISCOVERY;
        try {
            FollowerInfo followerInfo = new FollowerInfo(id, acceptedEpoch);
            log("Send FOLLOWERINFO to " + prospective);
            mutex.acquire();
            CURATOR.setData()
                    .forPath(ELECTION_NAMESPACE + "/" + prospective,
                            SerializationUtils.serialize(followerInfo)
                    );
            mutex.release();
            log("Wait NEWEPOCH...");
            waitMessage();
        } catch (Exception ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Discovery (phase 1) of follower in ZAB algorithm before synchronization.
     *
     * @param ne NEWEPOCH message.
     */
    private void discoveryFollowerAckEpoch(final NewEpoch ne) {
        log("NEWEPOCH received from " + ne.getSender());
        try {
            if (ne.getEpoch() >= acceptedEpoch) {
                acceptedEpoch = ne.getEpoch();
                AckEpoch ae = new AckEpoch(currentEpoch, history, lastZxid, id);
                log("Send ACKEPOCH to " + prospective);
                mutex.acquire();

                CURATOR.setData()
                        .forPath(ELECTION_NAMESPACE + "/" + prospective,
                                SerializationUtils.serialize(ae)
                        );

                mutex.release();
                synchronizationFollowerWaitNewLeader();
            } else {
                state = State.ELECTION;
                electLeader();
            }
        } catch (Exception ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Discovery (phase 1) of leader in ZAB algorithm before quorum.
     */
    private void discoveryLeaderWaitQuorum() {
        log("Phase 1: discovery (leader)");
        phase = Phase.DISCOVERY;
        quorum = new ArrayList<>();
        ackepochs = new ArrayList<>();
        waitQuorum();
    }

    /**
     * Discovery (phase 1) of leader in ZAB algorithm before ACKEPOCH.
     *
     * @param ae ACKEPOCH message.
     */
    private void discoveryLeaderWaitAckEpochs(final AckEpoch ae) {
        log("ACKEPOCH received from " + ae.getSender());
        ackepochs.add(ae);
        afterQuorum();
    }

    /**
     * Discovery (phase 1) of leader in ZAB algorithm waiting quorum.
     */
    private void waitQuorum() {
        log("Waiting for quorum...");
        try {
            List<String> nodes = CURATOR.getChildren()
                    .forPath(ELECTION_NAMESPACE);
            /*Do not count myself in quorum...*/
            if (quorum.size() >= (nodes.size() - LOCK_NODES) / 2) {
                log("Quorum ok.");
                int maxEpoch = acceptedEpoch;
                for (FollowerInfo finfo : quorum) {
                    if (finfo.getAcceptedEpoch() > maxEpoch) {
                        maxEpoch = finfo.getAcceptedEpoch();
                    }
                }
                acceptedEpoch = maxEpoch + 1;
                NewEpoch ne = new NewEpoch(acceptedEpoch, id);
                quorum.forEach((FollowerInfo fi) -> {
                    try {
                        if (!fi.getSender().equals(id)) {
                            log("Send NEWEPOCH to " + fi.getSender());
                            CURATOR.setData()
                                    .forPath(
                                            ELECTION_NAMESPACE + "/"
                                            + fi.getSender(),
                                            SerializationUtils.serialize(ne)
                                    );
                        }

                    } catch (Exception ex) {
                        Logger.getLogger(
                                Peer.class.getName()
                        ).log(Level.SEVERE, null, ex);
                    }
                });
                afterQuorum();
            } else {
                log("Wait FOLLOWERINFO...");
                waitMessage();
            }
        } catch (Exception ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Discovery (phase 1) of leader in ZAB algorithm before quorum.
     *
     * @param finfo FOLLOWERINFO message.
     */
    private void receiveFollowerInfo(final FollowerInfo finfo) {
        log("FOLLOWERINFO received from " + finfo.getSender());
        quorum.add(finfo);
        waitQuorum();
    }

    /**
     * Discovery (phase 1) of leader in ZAB algorithm after quorum.
     */
    private void afterQuorum() {
        /*Do not count myself in ACKEPOCHS...*/
        if (ackepochs.size() <= (quorum.size() - 1)) {
            log("Wait ACKEPOCH...");
            waitMessage();
        } else {
            afterAckEpochs();
        }
    }

    /**
     * Discovery (phase 1) of leader in ZAB algorithm after acks.
     */
    private void afterAckEpochs() {
        int maxEpoch = acceptedEpoch;
        Zxid maxZxid = new Zxid(0, 0);
        for (AckEpoch ae : ackepochs) {
            if (ae.getEpoch() > maxEpoch) {
                maxEpoch = ae.getEpoch();
            }
        }
        for (AckEpoch ae : ackepochs) {
            if (ae.getEpoch() == maxEpoch
                    && ae.getZxid().compareTo(maxZxid) >= 0) {
                maxZxid = ae.getZxid();
            }
        }
        for (AckEpoch ae : ackepochs) {
            if (ae.getEpoch() == maxEpoch
                    && ae.getZxid().compareTo(maxZxid) == 0) {
                history = ae.getHistory();
            }
        }
        if (quorum.size() > 0) {
            synchronizationLeaderWaitAckNewLeader(maxEpoch);
        } else {
            broadcastLeader();
        }
    }

    /**
     * Synchronization (phase 2) of follower in ZAB algorithm before NEWLEADER.
     */
    private void synchronizationFollowerWaitNewLeader() {
        log("Phase 2: synchronization (follower)");
        phase = Phase.SYNCHRONIZATION;
        log("Wait NEWLEADER...");
        waitMessage();
    }

    /**
     * Synchronization (phase 2) of follower in ZAB algorithm after NEWLEADER.
     *
     * @param nl NEWLEADER message.
     */
    private void synchronizationFollowerWaitCommitLeader(final NewLeader nl) {
        log("NEWLEADER received from " + nl.getSender());
        try {
            if (acceptedEpoch == nl.getEpoch()) {
                currentEpoch = nl.getEpoch();
                //foreach v,z in order of Zxid update history. Here simplified
                history = nl.getHistory();
                AckNewLeader anl = new AckNewLeader(currentEpoch, history, id);
                log("Send ACKNEWLEADER to " + prospective);
                mutex.acquire();

                CURATOR.setData()
                        .forPath(ELECTION_NAMESPACE + "/" + prospective,
                                SerializationUtils.serialize(anl)
                        );

                mutex.release();
                log("Wait COMMITLEADER...");
                waitMessage();
            } else {
                state = State.ELECTION;
                electLeader();
            }
        } catch (Exception ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Synchronization (phase 2) of follower in ZAB algorithm after COMMIT.
     *
     * @param commit COMMITLEADER message.
     */
    private void synchronizationFollowerToBroadcast(final CommitLeader commit) {
        log("COMMITLEADER received from " + commit.getSender());
        //deliver outstandings transactions in Zxid order.
        broadcastFollowerWaitTransaction();
    }

    /**
     * Synchronization (phase 2) of leader in ZAB algorithm before ACKNEWLEADER.
     *
     * @param epoch epoch from follower.
     */
    private void synchronizationLeaderWaitAckNewLeader(final int epoch) {
        log("Phase 2: synchronization (leader)");
        phase = Phase.SYNCHRONIZATION;
        quorum.forEach((FollowerInfo follower) -> {
            try {
                NewLeader nl = new NewLeader(epoch, history, id);
                log("Send NEWLEADER to " + follower.getSender());
                mutex.acquire();

                CURATOR.setData()
                        .forPath(ELECTION_NAMESPACE + "/"
                                + follower.getSender(),
                                SerializationUtils.serialize(nl)
                        );

                mutex.release();
            } catch (Exception ex) {
                Logger.getLogger(Peer.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        });
        if (quorum.size() > 0) {
            if (acksnewleader.size() < quorum.size()) {
                log("Wait ACKNEWLEADER...");
                waitMessage();
            }
        } else {
            broadcastLeader();
        }
    }

    /**
     * Synchronization (phase 2) of leader in ZAB algorithm after ACKNEWLEADER.
     *
     * @param anl ACKNEWLEADER message.
     */
    private void synchronizationLeaderWaitMoreAcks(final AckNewLeader anl) {
        log("ACKEWLEADER received from " + anl.getSender());
        acksnewleader.add(anl);
        if (acksnewleader.size() < quorum.size()) {
            log("Wait ACKNEWLEADER...");
            waitMessage();
        } else {
            afterAcksNewLeader();
        }
    }

    /**
     * Synchronization (phase 2) of leader in ZAB algorithm after enough
     * ACKNEWLEADER.
     */
    private void afterAcksNewLeader() {
        quorum.forEach((FollowerInfo follower) -> {
            try {
                CommitLeader commit = new CommitLeader(id);
                log("Send COMMIT to " + follower.getSender());
                mutex.acquire();

                CURATOR.setData().forPath(
                        ELECTION_NAMESPACE + "/" + follower.getSender(),
                        SerializationUtils.serialize(commit)
                );

                mutex.release();
            } catch (Exception ex) {
                Logger.getLogger(Peer.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        });
        broadcastLeader();
    }

    /**
     * Broadcast (phase 3) of follower in ZAB algorithm.
     */
    private void broadcastFollowerWaitTransaction() {
        log("Phase 3: broadcast (follower)");
        phase = Phase.BROADCAST;
        if (state == State.LEADING) {
            /*enable transaction broadcast*/
            log("Enable transaction broadcast.");
        }
        log("Wait TRANSACTION...");
        zabFinished = true;
        waitMessage();
    }

    /**
     * Broadcast (phase 3) of follower in ZAB algorithm.
     *
     * @param t TRANSACTION from leader.
     */
    private void broadcastFollowerWaitCommit(final Transaction t) {
        log("TRANSACTION received from " + t.getSender());
        try {
            history.add(t);
            Ack ack = new Ack(id);
            log("Send ACK to " + t.getSender());
            mutex.acquire();

            CURATOR.setData().forPath(ELECTION_NAMESPACE + "/" + t.getSender(),
                    SerializationUtils.serialize(ack));

            mutex.release();
            log("Wait COMMIT...");
            waitMessage();
        } catch (Exception ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Broadcast (phase 3) of follower in ZAB algorithm.
     *
     * @param c COMMIT from leader.
     */
    private void broadcastFollowerAfterCommit(final Commit c) {
        log("COMMIT received from " + c.getSender());
        //while outstanding older transitions, do nothing
        //then deliver transition to the client
        waitMessage();
    }

    /**
     * Broadcast (phase 3) of leader in ZAB algorithm.
     */
    private void broadcastLeader() {
        acksnewleader = new ArrayList<>();
        log("Phase 3: broadcast (leader)");
        phase = Phase.BROADCAST;
        acks = new ArrayList<>();
        log("Wait Message...");
        zabFinished = true;
        waitMessage();
    }

    /**
     * Broadcast (phase 3) of leader in ZAB algorithm.
     *
     * @param fi FOLLOWERINFO from new follower.
     */
    private void broadcastLeaderNewFollower(final FollowerInfo fi) {
        log("FOLLOWERINFO received from " + fi.getSender());
        try {
            NewEpoch ne = new NewEpoch(currentEpoch, id);
            log("Send NEWEPOCH to " + fi.getSender());
            mutex.acquire();
            CURATOR.setData().forPath(ELECTION_NAMESPACE + "/" + fi.getSender(),
                    SerializationUtils.serialize(ne));
            delay();
            NewLeader nl = new NewLeader(currentEpoch, history, id);
            log("Send NEWLEADER to " + fi.getSender());
            CURATOR.setData().forPath(ELECTION_NAMESPACE + "/" + fi.getSender(),
                    SerializationUtils.serialize(nl)
            );
            mutex.release();
            waitMessage();
        } catch (Exception ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Broadcast (phase 3) of leader in ZAB algorithm.
     *
     * @param anl ACKNEWLEADER from follower.
     */
    private void broadcastLeaderAfterAckNewLeader(final AckNewLeader anl) {
        log("ACKNEWLEADER received from " + anl.getSender());
        try {
            CommitLeader cl = new CommitLeader(id);
            log("Send COMMITLEADER to " + anl.getSender());
            mutex.acquire();

            CURATOR.setData()
                    .forPath(ELECTION_NAMESPACE + "/" + anl.getSender(),
                            SerializationUtils.serialize(cl)
                    );
            mutex.release();
            quorum.add(new FollowerInfo(anl.getSender(), anl.getEpoch()));
            zabFinished = true;
            waitMessage();
        } catch (Exception ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Broadcast (phase 3) of leader in ZAB algorithm.
     *
     * @param ack ACK from follower.
     */
    private void broadcastLeaderAfterAck(final Ack ack) {
        log("ACK received from " + ack.getSender());
        while (acks.size() < quorum.size()) {
            acks.add(ack);
            waitMessage();
        }
        Commit c = new Commit(id);
        quorum.forEach(follower -> {
            try {
                log("Send COMMIT to " + follower.getSender());
                mutex.acquire();

                CURATOR.setData()
                        .forPath(ELECTION_NAMESPACE + "/"
                                + follower.getSender(),
                                SerializationUtils.serialize(c)
                        );
                mutex.release();
            } catch (Exception ex) {
                Logger.getLogger(Peer.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        });
        waitMessage();
    }

    /**
     * Broadcast (phase 3) of leader in ZAB algorithm.
     *
     * @param propose PROPOSE from follower.
     */
    private void broadcastLeaderAfterPropose(final Propose propose) {
        log("PROPOSE received from " + propose.getSender());
        Zxid newZxid = new Zxid(lastZxid.getE(), lastZxid.getC() + 1);
        lastZxid = newZxid;
        Transaction t = new Transaction(propose.getV(), newZxid, id);
        history.add(t);
        quorum.forEach((FollowerInfo follower) -> {
            try {
                log("Send TRANSACTION to " + follower.getSender());
                mutex.acquire();

                CURATOR.setData().forPath(
                        ELECTION_NAMESPACE + "/" + follower.getSender(),
                        SerializationUtils.serialize(t)
                );

                mutex.release();
            } catch (Exception ex) {
                Logger.getLogger(Peer.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        });
        waitMessage();
    }

    /**
     * Manage new messages.
     */
    private void waitMessage() {
        try {
            synchronized (CURATOR) {
                CURATOR.wait();
            }
            mutex.acquire();
            Message m = SerializationUtils.deserialize(
                    CURATOR.getData().forPath(
                            ELECTION_NAMESPACE + "/" + id
                    )
            );
            synchronized (CURATOR) {
                CURATOR.setData()
                        .forPath(ELECTION_NAMESPACE + "/" + id,
                                SerializationUtils.serialize(new Clear(id)));
            }
            mutex.release();
            switch (m.getType()) {
                case ACK:
                    broadcastLeaderAfterAck((Ack) m);
                    break;
                case ACKEPOCH:
                    discoveryLeaderWaitAckEpochs((AckEpoch) m);
                    break;
                case ACKNEWLEADER:
                    switch (phase) {
                        case SYNCHRONIZATION:
                            synchronizationLeaderWaitMoreAcks((AckNewLeader) m);
                            break;
                        case BROADCAST:
                            broadcastLeaderAfterAckNewLeader((AckNewLeader) m);
                            break;
                        default:
                            waitMessage();
                            break;
                    }
                    break;
                case CLEAR:
                    waitMessage();
                    break;
                case COMMIT:
                    broadcastFollowerAfterCommit((Commit) m);
                    break;
                case COMMITLEADER:
                    synchronizationFollowerToBroadcast((CommitLeader) m);
                    break;
                case FOLLOWERINFO:
                    switch (phase) {
                        case DISCOVERY:
                            receiveFollowerInfo((FollowerInfo) m);
                            break;
                        case BROADCAST:
                            broadcastLeaderNewFollower((FollowerInfo) m);
                            break;
                        default:
                            waitMessage();
                            break;
                    }
                    break;
                case NEWEPOCH:
                    discoveryFollowerAckEpoch((NewEpoch) m);
                    break;
                case NEWLEADER:
                    synchronizationFollowerWaitCommitLeader((NewLeader) m);
                    break;
                case PROPOSE:
                    broadcastLeaderAfterPropose((Propose) m);
                    break;
                case TRANSACTION:
                    broadcastFollowerWaitCommit((Transaction) m);
                    break;
                default:
                    break;
            }

        } catch (Exception ex) {
            Logger.getLogger(Peer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Add some delay if needed.
     */
    private void delay() {
        try {
            Thread.sleep(DELAY);
        } catch (InterruptedException ex) {
            Logger.getLogger(Peer.class
                    .getName()).log(Level.SEVERE, null, ex);

        }
    }

    /**
     * Listener for leader.
     */
    private class LeaderListener implements PathChildrenCacheListener {

        /**
         * Something happened in the cluster.
         *
         * @param client curator instance.
         * @param event happened event.
         * @throws Exception curator exception.
         */
        @Override
        public void childEvent(
                final CuratorFramework client,
                final PathChildrenCacheEvent event) throws Exception {
            switch (event.getType()) {
                case CHILD_REMOVED:
                    if (event.getData().getPath().endsWith(prospective)) {
                        synchronized (client) {
                            log("Leader gone.");
                            client.notifyAll();
                            electLeader();
                        }
                    }
                default:
                    break;
            }
        }
    }

    /**
     * Listener for myself.
     */
    private class PeerListener implements NodeCacheListener {

        /**
         * Some message arrived.
         *
         * @throws Exception curator exception.
         */
        @Override
        public void nodeChanged() throws Exception {
            if (me.getCurrentData() != null) {
                synchronized (CURATOR) {
                    CURATOR.notifyAll();
                }
            }
        }
    }

    /**
     * Log method to add a timestamp.
     *
     * @param log string to be printed.
     */
    private void log(final String log) {
        System.out.println(
                MILLISECONDS.format(
                        new java.util.Date(System.currentTimeMillis())
                ) + " " + log);
    }

}
