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

## Book Management 

These functionalities enable administrators or authorized users to manage **books** available in the store.

---

### Add Book
* **Purpose**: Creates new book entries in the database.
* **Details**: Involves capturing information such as **title**, **description**, **short description**, **ISBN**, **price**, **publication year**, and linking to existing **authors** and **categories**. Includes validation to ensure data integrity (e.g., ensuring the ISBN is unique and properly formatted).

### Update Book
* **Purpose**: Modifies details of an existing book.
* **Details**: Takes a **book ID** and updated data as input. Allows administrators to correct typos, change the price, update the description, or modify the author/category associations.

### Delete Book
* **Purpose**: Removes an existing book from the store's inventory.
* **Details**: Takes a **book ID** as input. May include checks to ensure the book isn't tied to active orders or reviews before deletion.

### Get Book by (Title, Category, ID, ISBN)
* **Purpose**: Provides various methods for retrieving book information for display or internal use.
* **Details**:
    * **By Title**: Searches and retrieves books matching a given title (can support partial or full matches).
    * **By Category**: Retrieves all books belonging to a specific category or subcategory.
    * **By ID**: Retrieves a single, specific book using its unique identifier.
    * **By ISBN**: Retrieves a single book using its unique International Standard Book Number.

---

## Order Management

These functionalities handle the creation and management of customer orders.

### Create Empty Order
* **Purpose**: Initializes a new order record without any products yet.
* **Details**: Sets an initial **status** (e.g., "pending"), associates it with a user (if logged in), and generates a unique **order ID**.

### Create Order and Fill it with Products
* **Purpose**: Adds products to an existing (or newly created) order.
* **Details**: Associates **product IDs** and **quantities** with the order. The system calculates the **total price** and should manage **stock levels** by decrementing them as products are added to an order. The order status typically transitions to "created" or "processing."
