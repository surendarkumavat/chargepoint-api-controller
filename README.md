# api-controller

# Challenge: Asynchronous Service Communication

## Introduction

At the API layer, a key principle is to protect underlying services from
overload while ensuring optimal performance, even under high-traffic
conditions. We employ an asynchronous queueing mechanism for
communication with internal services to achieve this.

------------------------------------------------------------------------

## Use Case: Starting a Charging Session

A driver attempts to start a charging session at a specific station by
sending an API request to a REST endpoint.

The request includes:

- **Station Identifier**: A UUID (UUIDv4)
- **Driver Identifier (Token)**:
    - Length: 20--80 characters
    - Allowed characters:
        - Uppercase letters (`A-Z`)
        - Lowercase letters (`a-z`)
        - Digits (`0-9`)
        - Hyphen (`-`)
        - Period (`.`)
        - Underscore (`_`)
        - Tilde (`~`)
- **Callback URL**: A valid HTTP or HTTPS endpoint where the final
  decision will be sent

------------------------------------------------------------------------

## Request Flow

1. The API controller receives the request.
2. The input is validated.
3. The API immediately responds with an acknowledgment.
4. The controller asynchronously forwards the following data via a
   queueing mechanism:
    - Station ID
    - Driver token
    - Callback URL
5. The internal authorization service:
    - Checks an Access Control List (ACL)
    - Makes a decision
6. The decision is persisted for debugging purposes.
7. The decision result is sent to the provided callback URL.

------------------------------------------------------------------------

## API Acknowledgment Response

``` json
{
  "status": "accepted",
  "message": "Request is being processed asynchronously. The result will be sent to the provided callback URL."
}
```

------------------------------------------------------------------------

## Callback Payload Example

``` json
{
  "station_id": "123e4567-e89b-12d3-a456-426614174000",
  "driver_token": "validDriverToken123",
  "status": "allowed"
}
```

### Possible `status` values:

- `allowed`
- `not_allowed`
- `unknown`
- `invalid`

The client determines the outcome based on this callback response.

------------------------------------------------------------------------

## Constraints

- The internal authorization service:
    - Exposes only an HTTP/REST interface
    - Must NOT be invoked synchronously by the API controller
- If the authorization service does not respond within a predefined
  timeout:
    - The driver token defaults to `unknown`
- The callback URL must be a valid HTTP/HTTPS endpoint

------------------------------------------------------------------------

## Boundaries

- The API controller:
    - Is NOT responsible for rate-limiting
    - Is NOT responsible for authenticating/authorizing incoming HTTP
      requests
- Station identifiers (UUIDv4) are assumed to be valid and known

------------------------------------------------------------------------

## Implementation Notes

- Allowed implementation languages:
    - PHP
    - Python
    - Kotlin
- Deliverables:
    - Git repository link or archive file
    - Instructions to run the application locally
    - Instructions to run the test suite
    - Documentation for the public API endpoint
    - Supplementary materials (notes, diagrams, scaling
      considerations) are encouraged

------------------------------------------------------------------------

## Time Expectation

The recommended time to complete this task is **5 hours**. This reflects
the expected level of complexity.

## Building & Running

To build or run the project, use one of the following tasks:

| Task                                    | Description                                                          |
|-----------------------------------------|----------------------------------------------------------------------|
| `./gradlew test`                        | Run the tests                                                        |
| `./gradlew build`                       | Build everything                                                     |
| `./gradlew buildFatJar`                 | Build an executable JAR of the server with all dependencies included |
| `./gradlew buildImage`                  | Build the docker image to use with the fat JAR                       |
| `./gradlew publishImageToLocalRegistry` | Publish the docker image locally                                     |
| `./gradlew run`                         | Run the server                                                       |
| `./gradlew runDocker`                   | Run using the local docker image                                     |

If the server starts successfully, you'll see the following output:

```
2024-12-04 14:32:45.584 [main] INFO  Application - Application started in 0.303 seconds.
2024-12-04 14:32:45.682 [main] INFO  Application - Responding at http://0.0.0.0:8080
```

## Implementation
### Public API Details

Initiate Charging Session API: POST http://localhost:8080/charging-sessions

Expected Body:
```
{
  "station_id": "123e4567-e89b-12d3-a456-426614174000",
  "driver_token": "validDriverToken1234",
  "callback_url": "http://localhost:8080/callback/#/~123" 
}
```

Expected Response: HTTP 202
```
{
    "status": "accepted",
    "message": "Request is being processed asynchronously. The result will be sent to the provided callback URL."
}
```

## Work Notes
### Technology Selection

Need to use one of:
 - PHP - No experience
 - Python - Past Experience with REST API development using Flask, Apache, mod-wsgi. Multi-processing using container, JIT and routine parallelization using numba. Not relevant for current assignment. Need to learn on Queuing and Pooled Parallelization.
 - Kotlin - No experience. Similarity with Java. But new concepts for parallelization. But ecosystem should be similar. Selecting. Using ktor framework since chargepoint uses it and at first glance seems similar to Boot

### Assigment:
 - Given: An Internal ACL based Auth Service
 - Implement: A public api controller 
    - With: Task Queueing and Async API Invocation
 - Returning: Charging Session Authorization using callback url
    - And: Decision persistence in DB for debugging

### Implementation Approach
v1: Synchronous flow

User Agent -> (Api Controller: Controller -> Service -> (Auth Service, DB))

v2: Daemon Process dispatching requests to Auth Service

User Agent -> (Api Controller: Controller -> Service -> (Worker -> (Auth Service, DB))

#### General Approach:
1. Request Lands on Contorller
2. Request Validation in Controller (We wanted Open API Generated code to take care of this. Unfortunately it doesn't so we implement our own)
3. Model to Dto. 
   - Dto contains an additonal correlationId field to be used for dispatching to worker
3. Service Call with Dto
4. Check if a request is already queued by looking up in DB with accepted state with (station id, driver id, "accepted")
   - if exists Return
   - else
4. Create DB Record for request using request correlation id (to be used for updating later)
5. Dispatch Job to worker
   - Custom CoRoutine Scope of type Service Job and Dispatcher.IO
6. Once scheduled:
   1. Hit Auth Service API
   2. TimeOut scenario handled with request timeout configurations for HttpClient
   3. Update correlation id record with decision or unknown in case of error
   4. Hit callback

### Tasks:
#### Select Project Structure
Options:
 - gradle multi module
 - ktor multi module
 - single module

 => Since the use case is of an api controller, gradle multi module seems irrelevant. Also, we want to target microservices architecture as it is the De-facto standard for REST interaction
 => Selecting ktor multi module as this api controller may need to offer similar functionality for other use cases.

#### API Approach
API/Contract First all the way!!

Since we define the API first, let's try to use Code Generators for Boiler Plate API interface and Model Code
Yes!! OpenAPI Codegen Available for ktor. Catch -> Only targets ktor v2. Let's try to use it

An excruciating hour late, generated Controller/Route and API Code is useless. Also, server models do no have @SerializeName for cutomizing json property representation. Server code completely useless!

Client code seems usable with models generated as expected.

#### Data Store
SQL vs NoSql -> This use case seems quite simple. we can use either but the approach we selected needs to update the request on completion. So choosing SQL. Specifically in memory H2 DB

#### DAO
DB interaction is quite simple. Since we are learning, let's try to use ORM abstraction.

Exposed seems to be available. ktor generator and ktor server documentation has a neat guide helping us get started. The only catch is although v1 is available, example code still uses 0.6xx. Tried to upgarde to lates but example code no longer works and there a quite a lot of adjustements needed. So falling back to 0.6 for quick start.
