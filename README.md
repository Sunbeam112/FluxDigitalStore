# FluxDigitalStore - Online Store Backend Application on Java Spring

---

## Technical Stack & Infrastructure

### Database
* **Type**: **PostgreSQL**
* **Details**: A powerful, open-source relational database used for persistent storage of all application data, including user information, product details, and orders.

### Security
* **Password Hashing**: Passwords are securely hashed using an **BCrypt** to protect user credentials.
* **Authentication/Authorization**: Implemented using **Spring Boot Security**.
    * **JWT (JSON Web Tokens)**: Used for stateless authentication after successful login, enabling secure access to protected endpoints. Users with unverified emails are restricted from accessing certain functionalities, such as their order history.
    * **Reset Password Tokens (RPT)**: Dedicated tokens for secure password reset workflows.

### Email Protocol
* **Client**: **Fake-SMTP**
* **Details**: Utilized for simulating email sending during development and testing phases, ensuring email-related functionalities (like email verification and password reset) work correctly without sending actual emails.

### Frontend Integration
* **Technology**: **React**
* **Details**: The backend application serves as an API for a separate **React front-end project**, handling all data processing, business logic, and database interactions.

---

## User Management

These functionalities cover user account creation, authentication, and password recovery.

### User Registration
* **Purpose**: Allows new users to create an account within the online store.
* **Details**: Collects essential user information (e.g., username/email, password). Includes **data validation** for inputs and **password hashing** using an **EncryptionService** for robust security. Accounts are typically set to a pending/unverified state initially.

### Email Verification
* **Purpose**: Confirms the user's ownership of the registered email address.
* **Details**: A **unique, time-limited token** is generated and sent to the user's email via **Fake-SMTP** (for development/testing). Clicking a link containing this token activates the user's account, preventing fraudulent registrations.

### Login
* **Purpose**: Authenticates returning users.
* **Details**: Users provide **credentials** (username/email and password). The system verifies these against stored hashed passwords. Upon successful authentication, a **JSON Web Token (JWT)** is issued. This JWT is used for **Spring Boot Security** to maintain the user's logged-in state and grant access to protected resources (e.g., accessing order history requires a valid JWT and a verified email).

### Forgot Password
* **Purpose**: Assists users who have forgotten their password.
* **Details**: Users submit their registered email. A **unique, time-limited Reset Password Token (RPT)** is generated and sent to that email address via **Fake-SMTP**, enabling a secure password reset process.

### Reset Password
* **Purpose**: Allows users to set a new password after initiating a "forgot password" request.
* **Details**: Users utilize the **Reset Password Token (RPT)** received via email to access a secure endpoint where they can input and confirm their new password. The system then **updates their hashed password** using the **EncryptionService** and invalidates the used RPT.

---

## Product Management

These functionalities enable administrators or authorized users to manage products available in the store.

### Add Product
* **Purpose**: Creates new product entries in the database.
* **Details**: Involves capturing information such as **product name**, **description**, **price**, **category**, and **stock quantity**. Includes validation to ensure data integrity.

### Delete Product
* **Purpose**: Removes an existing product from the store's inventory.
* **Details**: Takes a **product ID** as input. May include checks to ensure the product isn't tied to active orders before deletion.

### Get Product by (Name, Category, ID)
* **Purpose**: Provides various methods for retrieving product information.
* **Details**:
    * **By Name**: Searches and retrieves products matching a given name (can support partial matches).
    * **By Category**: Retrieves all products belonging to a specific category.
    * **By ID**: Retrieves a single, specific product using its unique identifier.

### Parse and Export Products to CSV
* **Purpose**: Utility for data management and reporting.
* **Details**: Generates a **CSV (Comma Separated Values)** file containing selected or all product data. This is useful for inventory tracking, analysis, or transferring data to other systems. 

---

## Order Management

These functionalities handle the creation and management of customer orders.

### Create Empty Order
* **Purpose**: Initializes a new order record without any products yet.
* **Details**: Sets an initial **status** (e.g., "pending"), associates it with a user (if logged in), and generates a unique **order ID**.

### Create Order and Fill it with Products
* **Purpose**: Adds products to an existing (or newly created) order.
* **Details**: Associates **product IDs** and **quantities** with the order. The system calculates the **total price** and should manage **stock levels** by decrementing them as products are added to an order. The order status typically transitions to "created" or "processing."
