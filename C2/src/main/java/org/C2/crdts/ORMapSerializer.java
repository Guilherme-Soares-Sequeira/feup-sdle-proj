package org.C2.crdts;

import com.fasterxml.jackson.core.*;
import org.json.JSONException;
import org.json.JSONObject;
import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import netscape.javascript.JSObject;

public class ORMapSerializer {

    private JSONObject ormapJSON;
    public ORMapSerializer() {
        this.ormapJSON = new JSONObject();
    }

    public JSONObject serialize(ORMap map) throws JsonProcessingException, JSONException {
        this.ormapJSON.put("id", map.id());
        //TODO: serialize the rest of the map

        return this.ormapJSON;
    }

    /* Note: keeping this for reference, but it's not used in the code.
    ORMap:
        - Map (String, CCounter) map
          - CCounter:
            - DotKernel dorkernel:
              - Map (Dot, Integer) dotMap
                - Dot:
                  - String replicaID
                  - Integer: sequenceNumber
                - DotContext context:
                  - Map (String, Integer) causalContext
                  - Set (Dot) dotcloud
                    - Dot:
                      - String replicaID
                      - Integer: sequenceNumber
              - DotContext context:
                 - Map (String, Integer) causalContext
                 - Set (Dot)
                   - Dot:
                     - String replicaID
                     - Integer: sequenceNumber
            - String id

        - DotContext context:
          - Map (String, Integer) causalContext
          - Set (Dot)
           - Dot:
            - String replicaID
            - Integer: sequenceNumber

        - String id
    */


}
