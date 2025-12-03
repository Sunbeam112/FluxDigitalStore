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

---

## User Management (AuthenticationController)

These functionalities cover user account creation, authentication, and password recovery, managed by the `/auth/v1` endpoints.

### User Registration (`POST /register`)
* **Purpose**: Allows new users to create an account within the online store.
* **Details**: Collects essential user information (e.g., username/email, password). Includes **data validation** and **password hashing**. The account is initially in a pending/unverified state.
    * **Status Codes**: **201** (Created), **409** (User already exists), **500** (Email service failure), **400** (Validation errors).

### Email Verification (`POST /verify`)
* **Purpose**: Confirms the user's ownership of the registered email address.
* **Details**: Verifies a **unique, time-limited token** received via email. Successful verification activates the user's account.
    * **Status Codes**: **200** (OK), **409** (Token invalid/expired).

### Login (`POST /login`)
* **Purpose**: Authenticates returning users.
* **Details**: Users provide credentials. Upon successful authentication, a **JSON Web Token (JWT)** is issued.
    * **Unverified User Handling**: If a login is successful but the user is unverified, a **403 Forbidden** status is returned with a failure reason (`USER_NOT_VERIFIED`), and a new verification email may be sent.
    * **Status Codes**: **200** (OK with JWT), **403** (User not verified), **400** (Authentication failure), **500** (Email service failure).

### Forgot Password (`POST /forgot_password`)
* **Purpose**: Initiates the password recovery process.
* **Details**: Sends a **unique, time-limited Reset Password Token (RPT)** to the user's registered email.
    * **Status Codes**: **200** (OK), **409** (Email not verified or reset cooldown), **404** (User not found), **500** (Email service failure).

### Reset Password (`POST /reset_password`)
* **Purpose**: Allows users to set a new password using the RPT.
* **Details**: The system validates the RPT and the new password, updates the hashed password, and invalidates the token.
    * **Status Codes**: **200** (OK), **400** (Invalid token or password validation failed), **409** (Email not verified), **500** (Password change failed).

### Get User Info (`GET /me`)
* **Purpose**: Retrieves details for the currently authenticated user.
* **Details**: Provides the user's **ID**, **email**, **email verification status**, and a list of their **roles** (e.g., `ROLE_USER`, `ROLE_ADMIN`).
    * **Status Codes**: **200** (OK with user info), **401** (Unauthorized if no user is authenticated).

---

## Book Management (BookController)

These functionalities enable administrators or authorized users to manage **books** available in the store, managed by the `/books` endpoints.

### Add Book (`POST /create`)
* **Purpose**: Creates new book entries in the database.
* **Details**: Captures information like **title**, **ISBN**, **price**, **publication year**, and links to **authors** and **categories**. Includes validation for data integrity.
    * **Status Codes**: **201** (Created with BookDTO), **409** (Book already exists, e.g., duplicate ISBN), **500** (Internal server error).

### Update Book (`PUT /update`)
* **Purpose**: Modifies details of an existing book by its ID.
* **Details**: Takes a **book ID** and updated data. Allows correction of details like price, description, or associations.
    * **Status Codes**: **200** (OK with updated BookDTO), **404** (Book not found), **500** (Internal server error).

### Delete Book (`DELETE /delete`)
* **Purpose**: Removes an existing book from the store's inventory by its ID.
* **Details**: Takes a **book ID** as input.
    * **Status Codes**: **200** (OK), **404** (Book not found).

### Get All Books (`GET /get_all`)
* **Purpose**: Retrieves all books with **pagination** support.
* **Details**: Returns a paged list of books. Supports `page`, `size`, and `sort` parameters.
    * **Status Codes**: **200** (OK with Page of BookDTOs), **204** (No Content if page is empty).

### Get Book by ID (`GET /search_by_id`)
* **Purpose**: Retrieves a single, specific book using its unique identifier.
* **Details**: Searches by a required `id` request parameter.
    * **Status Codes**: **200** (OK with BookDTO), **204** (No Content if not found), **500** (Internal server error).

### Combined Book Search (`GET /search`)
* **Purpose**: Provides a flexible, paginated search for books based on various criteria.
* **Details**: Uses a `BookFilter` object to allow searching by criteria such as **title**, **category**, **author**, **ISBN**, etc. Supports pagination.
    * **Status Codes**: **200** (OK with Page of BookDTOs), **204** (No Content if no matches found).

---

## Order Management (OrderController)

These functionalities handle the creation, retrieval, and dispatch of customer orders, managed by the `/order` endpoints.

### Create and Process Order (`POST /create`)
* **Purpose**: Handles the complete checkout process, including validation, payment, and stock reservation.
* **Details**: Takes **order items** (product IDs and quantities) and **delivery details**. The process involves:
    1.  Validation of request and product existence.
    2.  User and Address assignment.
    3.  Payment processing.
    4.  Stock reservation (decrementing stock).
    5.  Persisting the order.
* **Status Codes**: **201** (Created with OrderDTO), **400** (Invalid product ID, address error, or empty order), **401** (User not authenticated), **402** (Payment failed), **409** (Insufficient stock), **500** (Internal error).

### Get Orders for Current User (`GET /`)
* **Purpose**: Retrieves the list of all orders associated with the currently authenticated user.
* **Details**: Returns a list of `OrderDTO`s for the user identified by the current session/JWT.
* **Status Codes**: **200** (OK with List of OrderDTOs), **401** (User not authenticated).

### Dispatch Order (Admin/Owner) (`POST /dispatch/{orderId}`)
* **Purpose**: Initiates the internal process to dispatch a completed order.
* **Details**: Takes an `orderId`. This is a secured operation: only the **order owner** or a user with the **ADMIN** role can execute it. Updates the order status (e.g., to "Dispatched").
* **Status Codes**: **200** (OK with updated OrderDTO), **403** (Forbidden/Unauthorized access), **404** (Order not found), **400** (Order status not valid for dispatch or stock inconsistency), **500** (Internal error).
