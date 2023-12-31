package org.C2.utils;

public class ConsistentHashingParameters {

    /**
     * Minimum number of responses required for a 'READ' operation to be considered successful.
     */
    public static final Integer R = 3;

    /**
     * Minimum number of responses required for a 'WRITE' operation to be considered successful.
     */
    public static final Integer W = 2;

    /**
     * Number of servers in which the data is replicated.
     */
    public static final Integer N = 4;

    /**
     * Maximum number of nodes that can be reached when performing READ or WRITE requests.
     */
    public static final Integer PriorityListLength = 5;
}
