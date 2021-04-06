/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marchiori.curatorzab.configuration;

import java.text.SimpleDateFormat;

/**
 * Configurations for the Proof of Concept, use something better in production.
 *
 * @author Matteo Marchiori
 */
public final class Configuration {

    /**
     * Useless contructor.
     */
    private Configuration() {
    }

    /**
     * Default local ZooKeeper address.
     */
    public static final String ZOOKEEPER_INSTANCE = "localhost:2181";

    /**
     * ZooKeeper session timeout for client.
     */
    public static final int SESSION_TIMEOUT = 1000;

    /**
     * Root node for the Proof of Concept.
     */
    public static final String ELECTION_NAMESPACE = "/leader-election";

    /**
     * Retries of connection to ZooKeeper for curator.
     */
    public static final int MAX_RETRIES = 5;

    /**
     * Format for timestamp in logs.
     */
    public static final SimpleDateFormat MILLISECONDS
            = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Minimum timeout before retry ZAB algorithm.
     */
    public static final int MIN_TIMEOUT = 10;

    /**
     * Maximum timeout before retry ZAB algorithm.
     */
    public static final int MAX_TIMEOUT = 15;

    /**
     * Older node started in ZooKeeper.
     */
    public static final int OLDER_NODE = 0;

    /**
     * Minimum number of nodes to do all of the ZAB phases.
     */
    public static final int MIN_CLUSTER = 3;

    /**
     * Nodes used from ZooKeeper to manage locks.
     */
    public static final int LOCK_NODES = 2;

    /**
     * Delay.
     */
    public static final int DELAY = 100;
}
