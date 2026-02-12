# api-controller

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
 - Bondaries: no request Auth and throttling at controller level

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

#### Async Processing
First glance: Need to Use Co-routines. ktor also uses coroutines for request processing that is all methods called in a request flow need to be suspend

Need something akin to ExecutorService in Java with Unbounded Queue.

Custom Coroutine Scopes bound to IO Dispatchers(Backing Threads) and type Service Job seem to be the answer. ktor also has its own coroutine scopes. Some are bound to applicaiton, some to request and there is a global scope. global (from experience everywhere else) is strongly discouraged.

On to the queue, we need to ensure that the requests are not rate-limited. so won't use bounded. can use some blocking queues from java world. However found Channel abstraction that is built exactly for this. By name, seems irrelevant but came across it again and again in various forums and threads. So finally looked at the API and was convinced about it being key to our case

### Testing
#### Unit
Service class went through mutiple iterations, first to get a working code, next to make it DI capable, and finally while writing cases to make it easier to test by accepting the mocks. Also, makes our class design more consistent and aligned to standards
Now all we wanted was mockito. Checked, mockito available but not kotlin native. simple google search for mock kotlin takes us to mockk. And that's what we need.

2 simple unit test cases to test the Service class:
1. Test happy flow by mocking api client and introspecting client call to confirm the right status is submitted to client.
   - Turns out mocking client is not that easy. Recommendation everywhere is to wrap it and mock the wrapper. Also, made code simpler for client calls.
2. Test error flow by throwing exception on mocked method call of client and expect unknown status in callback call from wrapper

#### Manual
Tested by hitting api-controller public api using talend API tester.
Scenarios:
1. Happy Case: Got 200. Realized this was not the right status code for async. changed to 202. Checked logs and found follwing:
   - Callback (Using HttpClient) failing with json parsing error. installed content negotiation plugin to httpclient
   - Auth Service Call (Using OpenAPI generated code) failing with similar error.
   - Not strainghtforward to inject our own httplient into generated client.
   - But it does provide us with lambda to configure client engine and client config
   - Created a common engine config for both the calls and externalized the timeout configs to application.yaml
   - callbacks and auth service calls still fail due to mock urls configured but that error is expected. Need to further test using Integration Test
2. Invalid input: sent an invalid json. Got an error response with text error.
   - Updated Status Pages plugin configuration to add generic throwable handling with proper body structure
   - Added handling for BadRequestException to send 400 bad request
   - Found out that there was no scaffolding validation code/annotation generated with model classes based on API spec like in java
   - Implemented manual validation for the input fields using regex for driver, exception handling for url validation
 
#### Integration
ktor documentation page has very good examples of testing api server calls. Taking that as reference, starting with test cases 

@TODO

### Scaling Considerations
Conceptually, api-controller service for hitting auth service can be scaled in 2 ways as and when more capacity becomes available on the auth service:
1. Vertical -> increase auth service parallel request configuration per api-controller instance by updating in application.yaml. These configurations may be further externalized to be more on-demand.
2. Horizontally -> In case, we reach upper limit per instance, we can horizontally scale by having more instances of api-controller. (Ofcource DB will have to be a proper DB and not the in-memory one we are using to simulate DB interaction currently)

We can even have a scaling rule for auth service based on hit load and hit load we can control exclusively from api-controller in the above 2 ways