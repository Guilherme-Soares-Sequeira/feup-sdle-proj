package org.C2.utils;

import org.C2.crdts.ORMap;

public class FetchListInfo {
    private final ORMap list;
    private final RequestStatus status;

    public FetchListInfo(RequestStatus status) {
        this.status = status;
        this.list = null;
    }

    public FetchListInfo(RequestStatus status, ORMap list) {
        this.status = status;
        this.list = list;
    }

    public ORMap getListJson() {
        return list;
    }

    public RequestStatus getStatus() {
        return status;
    }
}
