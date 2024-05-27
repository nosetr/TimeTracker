# Simple TimeTracker with Firebase Auth, Firestore and Spring WebFlux

## Overview

**TimeTracker** is a simple web application for tracking and managing work hours. The application uses Firebase Authentication for user management, Firestore for data storage, and Spring WebFlux for reactive programming. A scheduled task updates the top 10 users with the best ratings daily.

## Features

- **User registration and authentication** with Firebase Auth
- **Storing and managing work hours** in Firestore
- **Reactive API** with Spring WebFlux
- **Daily update of top 10 users** based on ratings
- **Prevent multiple votes** from the same user

## Installation

1. **Clone the repository:**

```sh
 	git clone https://github.com/YourUsername/TimeTracker.git
 	cd TimeTracker
```

2. **Configure Firebase:**

- Create a Firebase project and set up Authentication and Firestore.
- Download the google-services.json file and place it in the root directory of the project.

3. **Set up environment variables:**

Create a .env file in the root directory of the project with the following variables:

```env
	FIREBASE_KEY=[WEB-API-KEY from project]
	FIREBASE_USER_ID=[user_id_by_firebase]
	FIREBASE_EMAIL=[user_email_by_firebase]
	FIREBASE_PASSWORD=[user_password_by_firebase]
```

4. **Create an application properties file:**

Create a file application.properties under src/main/resources with the necessary configurations for Firebase and Firestore.

5. **Install dependencies and start the project:**

```sh
	./mvnw clean install
	./mvnw spring-boot:run
```

## API Endpoints

- Register a user:

```http
	POST /api/register
```

- Save a work time record:

```http
	POST /api/record
```

- Get work time records by week:

```http
	GET /api/by-week/{year}/{week}
```

- Add a rating to a work time record:

```http
	POST /api/record/{timeRecordId}
```

## Scheduler

A scheduler runs daily at 01:00 AM and performs the following actions:

- Delete old top 10 users
- Retrieve new top 10 users based on ratings
- Save the new top 10 users in Firestore

## Testing

To test the application, ensure the environment variables are set in your test environment and then run the tests:

```sh
./mvnw test
```

Example for setting environment variables in a test environment:

```sh
export FIREBASE_KEY=[WEB-API-KEY from project]
export FIREBASE_USER_ID=[user_id_by_firebase]
export FIREBASE_EMAIL=[user_email_by_firebase]
export FIREBASE_PASSWORD=[user_password_by_firebase]
```

## Contribution

Contributions are welcome! Please create a pull request or open an issue to suggest improvements or report bugs.