![изображение](https://github.com/user-attachments/assets/2060faf4-2583-4fc7-b298-3e82ef863590)

# Mobile Development: Calculator Application

This repository contains the implementation of a **Calculator Application** developed as part of the "Mobile Development" course. Each laboratory work
 introduces new functionality to the application, starting from basic calculator features and gradually integrating advanced capabilities like Firebase integration, data fetching, and secure authorization.

---

## Table of Contents

1. [Laboratory Work 1: Common Functionality](#laboratory-work-1-common-functionality)
2. [Laboratory Work 2: Platform API Integration](#laboratory-work-2-platform-api-integration)
3. [Laboratory Work 3: Fetching Data](#laboratory-work-3-fetching-data)
4. [Laboratory Work 4: Pass Key Authorization](#laboratory-work-4-pass-key-authorization)

---

## Laboratory Work 1: Common Functionality

**Objective:** Implement the core functionality of the calculator application, including the user interface and basic arithmetic operations.

**Features:**
- Design and implement the calculator UI (buttons, display, etc.).
- Add support for basic arithmetic operations: addition, subtraction, multiplication, and division.
- Handle edge cases (e.g., division by zero, invalid inputs).
- Ensure responsive and user-friendly design.

---

## Laboratory Work 2: Platform API Integration

**Objective:** Implement various functionalities using the mobile operating system's API to enhance the application's capabilities.

**Features:**
- Integrate interesting inputs (e.g., touch events, gestures).
- Add widgets for quick access to calculator features.
- Use the device's camera for advanced functionality (e.g., scanning math problems).
- Optimize performance and enhance user experience using platform-specific APIs.
  
---

## Laboratory Work 3: Fetching Data

**Objective:** Enable communication with external data sources using Firebase and implement push notifications.

**Features:**
- Connect the app to **Firebase Cloud Platform**.
- Retrieve theme customization parameters from the cloud and apply them in the app.
- Save and load calculation history to/from Firebase Firestore or Realtime Database.
- Implement **Push Notifications** to keep users updated with relevant information.

---

## Laboratory Work 4: Pass Key Authorization

**Objective:** Implement a secure Pass Key authorization feature to protect sensitive parts of the application.

**Features:**
- Initialize Pass Key during app setup.
- Validate the entered Pass Key and grant access upon successful validation.
- Handle incorrect or forgotten Pass Key scenarios.
- Provide an option for users to reset or change their Pass Key.
- Securely store the Pass Key and protect it against unauthorized access.
- Integrate **Biometric Authentication** (e.g., fingerprint or face recognition) for enhanced security.

---

## How to Use This Repository

1. Clone the repository:
   ```bash
   git clone https://github.com/Sayrexxx/Mobile-Apps-semester6.git
   ```
1. Open the project in your IDE (e.g., Android Studio).
1. Set up Firebase:
    - Create a Firebase project at Firebase Console.
    - Add the google-services.json file to your Android project.
    - Configure Firebase dependencies in build.gradle.
    - Run the app on an emulator or physical device.

---

## Technologies

- Programming Language: Kotlin (Android)
- Platform: Android
- Firebase Services:
  - Firebase Authentication (for user login/sign-up).
  - Firebase Realtime Database/Firestore (for storing calculation history).
  - Firebase Cloud Messaging (for notifications, if needed).
- Additional Libraries:
  - Retrofit (for API communication).
  - Biometric API (for secure authentication).
