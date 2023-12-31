# Introduction

This document details the endpoints provided by each of the node servers.

Endpoints beginning with `/internal/` are meant to be used for intra-node communication,
while endpoints beginning with `/external/` are meant to be accessed by the load balancer
or other external tools, such as the administrator tool that adds servers, though it
may also be accessed by other nodes when redirecting a write request.

All input and output is done via JSON.

# Endpoints

## /pulse

### GET

#### Request Structure

N/A

#### Responses

##### 200 - OK

###### Body

N/A

#### Use Cases

- Check if node is reachable.
- When performing READs or WRITEs, the node responsible for handling the request must select the first N reachable nodes. In order to know if the node is reachable or not, the coordinator checks its pulse.

## /internal/ring

### GET

#### Request Structure

N/A

#### Responses

##### 200 - OK

###### Body

- ring: String (ConsistentHasher.toJSON())

##### 500 - Internal Server Error

###### Body

- errorMessage: String

#### Use Cases

- Queries to seed servers to update membership view of nodes.

## /external/ring

### GET

#### Request Structure

N/A

#### Responses

##### 200 - OK

###### Body

- ring: String (ConsistentHasher.toJSON())

##### 500 - Internal Server Error

###### Body

- errorMessage: String

#### Use Cases

- When adding or removing a server, an external tool must first query the seed servers to
obtain the current ring status.

### PUT

#### Request Structure

- ring: String (ConsistentHasher.toJSON())

#### Responses

##### 201 - Created

###### Body

N/A

##### 400 - Bad Request

###### Body

- errorMessage: String

#### Use Cases

- Used by an external service to update the seed servers when a new server is added or removed.

## /internal/shopping-list/{ID}

### GET

#### Request Structure

N/A

#### Responses

##### 200 - OK

###### Body

- list: String (CRDT.toJSON())
- ring: String (ConsistentHasher.toJSON())

##### 400 - Bad Request

###### Body

- errorMessage: String

##### 404 - Not Found

###### Body

- errorMessage: String

##### 500 - Internal Server Error

###### Body

- errorMessage: String

#### Use Cases

- A node handling a **READ** requests **R** other nodes for their version of the requested
shopping list.

### PUT

#### Request Structure

- list: String (CRDT.toJSON())
- ring: String(ConsistentHasher.toJSON())

#### Responses

##### 201 - Created

###### Body

N/A

##### 400 - Bad Request

###### Body

- errorMessage: String

##### 500 - Internal Server Error

###### Body

- errorMessage: String

#### Use Cases

- A node handling a **WRITE** replicates the received information across **N-1** other nodes.

## /external/shopping-list/{ID}/{forID}

### GET

#### Request Structure

N/A

#### Responses

##### 202 - Processing

###### Body

N/A

##### 400 - Bad Request

###### Body

- errorMessage: String

##### 500 - Internal Server Error

###### Body

- errorMessage: String

#### Use Cases

- The load balancer requests a **READ** on the shopping list. `forID` is an unique identifier
randomly generated by the load balancer to identify the request. When the request has been
processed a message is sent to the load balancer, where `forID` will be used by the load
balancer to determine to what user it is supposed to redirect it to.

## /external/shopping-list/{ID}

### PUT

#### Request Structure

- for: String
- list: String (CRDT.toJSON())

#### Responses

##### 202 - Processing

###### Body

N/A

##### 400 - Bad Request

###### Body

- errorMessage: String

##### 500 - Internal Server Error

###### Body

- errorMessage: String

#### Use Cases

- The load balancer requests a **WRITE** on the shopping list. `for` is an unique identifier
  randomly generated by the load balancer to identify the request. When the request has been
  processed a message is sent to the load balancer, where `for` will be used by the load
  balancer to determine to what user it is supposed to redirect it to.
- A node receives a **WRITE** request, but it isn't responsible for the list, so it redirects
the request to an appropriate node.
