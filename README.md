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

