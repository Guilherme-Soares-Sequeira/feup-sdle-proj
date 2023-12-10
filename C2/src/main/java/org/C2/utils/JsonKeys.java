package org.C2.utils;

public class JsonKeys {

    /**
     * Used for passing an errorMessage.
     */
    public static final String errorMessage = "errorMessage";

    /**
     * Used for passing a ConsistentHasher.toJSON().
     */
    public static final String ring = "ring";

    /**
     * Used for passing a shopping list's JSON representation in a String.
     */
    public static final String list = "list";

    /**
     * Used for passing information which will be used to send the information back to the LoadBalancer.
     */
    public static final String forId = "for";

    /**
     * Used by the load balancer to inform the client of what' the status of a request when polled.
     */
    public static final String status = "status";

    /**
     * Used when informing LoadBalancer whether a WRITE operation failed or not.
     */
    public static final String error = "error";

}
