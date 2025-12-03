-- *** 1. AUTHOR INSERTS (Total 25 Unique Authors) ***
-- Includes the 3 original, 5 requested, and 17 added to ensure every book has a unique author ID.

INSERT INTO author (name)
VALUES ('Stephen King'),      -- ID 1
       ('Agatha Christie'),   -- ID 2
       ('Mary Shelley'),      -- ID 3
       ('J.R.R. Tolkien'),    -- ID 4
       ('Jane Austen'),       -- ID 5
       ('George Orwell'),     -- ID 6
       ('Toni Morrison'),     -- ID 7
       ('Neil Gaiman'),       -- ID 8
       ('William Gibson'),    -- ID 9
       ('Daphne du Maurier'), -- ID 10
       ('Umberto Eco'),       -- ID 11
       ('Stieg Larsson'),     -- ID 12
       ('Donna Tartt'),       -- ID 13
       ('Philip K. Dick'),    -- ID 14
       ('Octavia Butler'),    -- ID 15
       ('Oscar Wilde'),       -- ID 16
       ('Charles Dickens'),   -- ID 17
       ('Thomas Hardy'),      -- ID 18
       ('Khaled Hosseini'),   -- ID 19
       ('Richard Dawkins'),   -- ID 20
       ('Michelle Obama'),    -- ID 21
       ('Shirley Jackson'),   -- ID 22
       ('Delia Owens'),       -- ID 23
       ('Bram Stoker'),       -- ID 24
       ('T.S. Eliot');
-- ID 25


-- *** 2. CATEGORY INSERTS (Total 13 Categories) ***

INSERT INTO category (name, description)
VALUES ('Horror', 'Books designed to frighten or disgust.'),
       ('Mystery', 'Novels focused on solving a crime or puzzle.'),
       ('Gothic Fiction', 'Literature combining elements of fear, death, and romance.'),
       ('Fantasy', 'Involves magic, supernatural elements, and often a secondary world.'),
       ('Young Adult', 'Fiction aimed at readers aged 12 to 18.'),
       ('Dystopian', 'A story about an imagined oppressive society.'),
       ('Historical Fiction', 'A fictional story set during a real historical period.'),
       ('Romance', 'Focuses on the romantic relationship between two main characters.'),
       ('Science Fiction', 'Speculative fiction dealing with imaginative concepts like future science and technology.'),
       ('Thriller', 'A fast-paced genre characterized by suspense and excitement.'),
       ('Literary Fiction', 'Focuses on style, character, and theme over plot.'),
       ('Biography', 'An account of someone''s life written by someone else.'),
       ('Poetry', 'A collection of writings in verse or rhythm.');


-- *** 3. BOOK INSERTS (Total 33 Books, Split into 3 statements) ***

-- Insert Block 1 (Books 1-12)
INSERT INTO book (title, isbn, price, publication_year, description, short_description, category, subcategory,
                  url_photo)
VALUES ('The Shining', '9780385121675', 15.99, 1977,
        'A gripping psychological horror novel set in an isolated hotel during winter.',
        'Horror classic by Stephen King.', 'Fiction', 'Psychological Horror', 'http://example.com/theshining.jpg'),
       ('And Then There Were None', '9780062073488', 12.50, 1939,
        'Ten strangers are lured to an isolated island and killed one by one.',
        'The best-selling mystery novel of all time.', 'Fiction', 'Whodunit', 'http://example.com/atwwn.jpg'),
       ('Frankenstein', '9780141439471', 9.99, 1818, 'The story of Victor Frankenstein and his creation.',
        'A classic of both horror and gothic literature.', 'Fiction', 'Gothic', 'http://example.com/frankenstein.jpg'),
       ('1984', '9780451524935', 11.50, 1949, 'A chilling look at a totalitarian future.', 'Orwellian masterpiece.',
        'Fiction', 'Political Dystopia', 'url4'),
       ('The Lord of the Rings', '9780618260235', 25.00, 1954, 'The quest to destroy the One Ring.',
        'Epic high fantasy.', 'Fiction', 'High Fantasy', 'url5'),
       ('Beloved', '9781400033423', 14.99, 1987, 'A story of a former slave haunted by a ghost.',
        'A powerful work of literary fiction.', 'Fiction', 'Magical Realism', 'url6'),
       ('Neverwhere', '9780380789016', 13.50, 1996, 'A fantasy story set in hidden "London Below".',
        'Urban fantasy adventure.', 'Fiction', 'Urban Fantasy', 'url7'),
       ('Neuromancer', '9780441569595', 12.00, 1984, 'A washed-up hacker is hired for one last job.',
        'Cyberpunk defining novel.', 'Fiction', 'Cyberpunk', 'url8'),
       ('Pride and Prejudice', '9780141439518', 9.99, 1813, 'The story of the Bennet sisters in 19th-century England.',
        'A classic romantic novel.', 'Fiction', 'Regency', 'url9'),
       ('Sula', '9780307278278', 13.00, 1973, 'The life of two friends in a black community in Ohio.',
        'A deeply moving literary novel.', 'Fiction', 'African American Lit', 'url10'),
       ('The Hobbit', '9780345339685', 10.99, 1937, 'Bilbo Baggins is hired as a burglar on an unexpected journey.',
        'A foundational children''s fantasy book.', 'Fiction', 'Children''s Fantasy', 'url11'),
       ('Rebecca', '9780380731329', 11.50, 1938, 'A newlywed is haunted by her husband''s first wife.',
        'A gothic mystery/romance.', 'Fiction', 'Gothic Romance', 'url12');

-- Insert Block 2 (Books 13-24)
INSERT INTO book (title, isbn, price, publication_year, description, short_description, category, subcategory,
                  url_photo)
VALUES ('The Name of the Rose', '9780156008498', 15.00, 1980,
        'A monk investigates a series of murders in a 14th-century monastery.',
        'A historical and philosophical mystery.', 'Fiction', 'Historical Mystery', 'url13'),
       ('The Stand', '9781101967118', 18.99, 1978, 'A post-apocalyptic battle between good and evil after a plague.',
        'King''s epic horror/fantasy.', 'Fiction', 'Apocalyptic Horror', 'url14'),
       ('The Girl with the Dragon Tattoo', '9780307269757', 14.50, 2005,
        'A journalist and a hacker investigate a 40-year-old disappearance.', 'A gripping Swedish thriller.', 'Fiction',
        'Nordic Noir', 'url15'),
       ('The Secret History', '9780679736400', 14.50, 1992,
        'A group of classics students in Vermont grapple with murder.', 'A dark academic mystery.', 'Fiction',
        'Academic Mystery', 'url16'),
       ('Murder on the Orient Express', '9780062073495', 12.00, 1934,
        'Hercule Poirot must solve a murder on a stalled train.', 'A classic closed-room mystery.', 'Fiction',
        'Classic Whodunit', 'url17'),
       ('A Scanner Darkly', '9780547525363', 10.99, 1977, 'An undercover narcotics agent gets addicted to a drug.',
        'A paranoia-fueled sci-fi novel.', 'Fiction', 'Drug Culture Sci-Fi', 'url18'),
       ('Sense and Sensibility', '9780141439498', 9.50, 1811, 'Two sisters navigate societal expectations and love.',
        'A quintessential Austen romance.', 'Fiction', 'Regency Romance', 'url19'),
       ('Stardust', '9780062015501', 12.99, 1999, 'A young man enters a magical realm to retrieve a fallen star.',
        'A whimsical fairy tale for adults.', 'Fiction', 'Fantasy Romance', 'url20'),
       ('The Graveyard Book', '9780060530921', 11.99, 2008, 'A boy is raised by ghosts in a graveyard.',
        'A Newbery Medal winning YA fantasy.', 'Fiction', 'YA Fantasy', 'url21'),
       ('Kindred', '9780807083693', 13.99, 1979,
        'A young black woman is transported between the present and a pre-Civil War plantation.',
        'A seminal work of Afrofuturism.', 'Fiction', 'Afrofuturism', 'url22'),
       ('The Picture of Dorian Gray', '9780141439570', 10.50, 1890,
        'A man sells his soul to keep his youth and beauty.', 'A gothic philosophical novel.', 'Fiction',
        'Decadent Movement', 'url23'),
       ('A Tale of Two Cities', '9780141439600', 9.50, 1859, 'The French Revolution and the lives of two men.',
        'A classic historical novel.', 'Fiction', 'Historical', 'url24');

-- Insert Block 3 (Books 25-33)
INSERT INTO book (title, isbn, price, publication_year, description, short_description, category, subcategory,
                  url_photo)
VALUES ('Tess of the d''Urbervilles', '9780141439593', 10.00, 1891, 'The tragic life of a country girl.',
        'A tragic classic.', 'Fiction', 'Victorian', 'url25'),
       ('The Kite Runner', '9781594480003', 14.50, 2003, 'A story of two friends in Afghanistan across decades.',
        'A powerful contemporary novel.', 'Fiction', 'Contemporary', 'url26'),
       ('The God Delusion', '9780618680009', 16.99, 2006, 'A critique of religion.', 'A best-selling non-fiction book.',
        'Non-Fiction', 'Atheism', 'url27'),
       ('Becoming', '9781524763138', 18.00, 2018, 'The memoir of Michelle Obama.', 'A best-selling biography.',
        'Non-Fiction', 'Memoir', 'url28'),
       ('The Haunting of Hill House', '9780143039983', 11.00, 1959,
        'A scientific investigation of a haunted mansion goes wrong.', 'A definitive haunted house story.', 'Fiction',
        'Haunted House', 'url29'),
       ('Where the Crawdads Sing', '9780735219090', 15.99, 2018,
        'A girl who grew up in the marsh is accused of murder.', 'A mystery and coming-of-age story.', 'Fiction',
        'Southern Gothic', 'url30'),
       ('The Ocean at the End of the Lane', '9780062272895', 13.99, 2013,
        'A man remembers his childhood encounter with a magical family.', 'A short, poignant fantasy novel.', 'Fiction',
        'Magical Realism', 'url31'),
       ('Dracula', '9780141439501', 10.50, 1897, 'The definitive vampire novel.', 'A classic gothic horror.', 'Fiction',
        'Vampire', 'url32'),
       ('The Waste Land', '9780156030352', 8.99, 1922, 'A highly influential modernist poem.',
        'A landmark work of 20th-century poetry.', 'Poetry', 'Modernist', 'url33');


-- *** 4. JOIN TABLE: BOOK-AUTHORS (Updated for Multiple Authors) ***
-- Original Mappings (33 total):
-- (1, 1), (2, 2), (3, 3), (4, 6), (5, 4), (6, 7), (7, 8), (8, 9), (9, 5), (10, 7), (11, 4), (12, 10), (13, 11), (14, 1), (15, 12), (16, 13), (17, 2), (18, 14), (19, 5), (20, 8), (21, 8), (22, 15), (23, 16), (24, 17), (25, 18), (26, 19), (27, 20), (28, 21), (29, 22), (30, 23), (31, 8), (32, 24), (33, 25)

INSERT INTO book_authors (book_id, author_id)
VALUES
    -- Original Mappings
    (1, 1),   -- The Shining: Stephen King
    (2, 2),   -- And Then There Were None: Agatha Christie
    (3, 3),   -- Frankenstein: Mary Shelley
    (4, 6),   -- 1984: George Orwell
    (5, 4),   -- The Lord of the Rings: J.R.R. Tolkien
    (6, 7),   -- Beloved: Toni Morrison
    (7, 8),   -- Neverwhere: Neil Gaiman
    (8, 9),   -- Neuromancer: William Gibson
    (9, 5),   -- Pride and Prejudice: Jane Austen
    (10, 7),  -- Sula: Toni Morrison
    (11, 4),  -- The Hobbit: J.R.R. Tolkien
    (12, 10), -- Rebecca: Daphne du Maurier
    (13, 11), -- The Name of the Rose: Umberto Eco
    (14, 1),  -- The Stand: Stephen King
    (15, 12), -- The Girl with the Dragon Tattoo: Stieg Larsson
    (16, 13), -- The Secret History: Donna Tartt
    (17, 2),  -- Murder on the Orient Express: Agatha Christie
    (18, 14), -- A Scanner Darkly: Philip K. Dick
    (19, 5),  -- Sense and Sensibility: Jane Austen
    (20, 8),  -- Stardust: Neil Gaiman
    (21, 8),  -- The Graveyard Book: Neil Gaiman
    (22, 15), -- Kindred: Octavia Butler
    (23, 16), -- The Picture of Dorian Gray: Oscar Wilde
    (24, 17), -- A Tale of Two Cities: Charles Dickens
    (25, 18), -- Tess of the d'Urbervilles: Thomas Hardy
    (26, 19), -- The Kite Runner: Khaled Hosseini
    (27, 20), -- The God Delusion: Richard Dawkins
    (28, 21), -- Becoming: Michelle Obama
    (29, 22), -- The Haunting of Hill House: Shirley Jackson
    (30, 23), -- Where the Crawdads Sing: Delia Owens
    (31, 8),  -- The Ocean at the End of the Lane: Neil Gaiman
    (32, 24), -- Dracula: Bram Stoker
    (33, 25), -- The Waste Land: T.S. Eliot

    -- NEW CO-AUTHOR MAPPINGS (Books with multiple authors):
    (1, 14),  -- The Shining (1) now co-authored by Philip K. Dick (14) - (Just for example data)
    (2, 6),   -- And Then There Were None (2) now co-authored by George Orwell (6) - (Just for example data)
    (5, 9),   -- The Lord of the Rings (5) now co-authored by William Gibson (9) - (Just for example data)
    (7, 12),  -- Neverwhere (7) now co-authored by Stieg Larsson (12) - (Just for example data)
    (13, 16), -- The Name of the Rose (13) now co-authored by Oscar Wilde (16)
    (15, 1),  -- The Girl with the Dragon Tattoo (15) now co-authored by Stephen King (1)
    (20, 4),  -- Stardust (20) co-authored by J.R.R. Tolkien (4) and Neil Gaiman (8) (Already had Gaiman)
    (21, 15), -- The Graveyard Book (21) co-authored by Octavia Butler (15) and Neil Gaiman (8) (Already had Gaiman)
    (27, 13);
-- The God Delusion (27) now co-authored by Donna Tartt (13)

-- *** 5. JOIN TABLE: BOOK-CATEGORIES (Multiple categories per book) ***

INSERT INTO book_categories (book_id, category_id)
VALUES (1, 1),
       (2, 2),
       (3, 3),
       (3, 1),
       (4, 6),
       (5, 4),
       (6, 11),
       (7, 4),
       (8, 9),
       (9, 8),
       (9, 11),
       (10, 11),
       (11, 4),
       (11, 5),
       (12, 2),
       (12, 3),
       (13, 7),
       (13, 2),
       (14, 1),
       (14, 4),
       (15, 10),
       (15, 2),
       (16, 2),
       (16, 11),
       (17, 2),
       (18, 9),
       (19, 8),
       (20, 4),
       (20, 8),
       (21, 5),
       (21, 4),
       (22, 9),
       (22, 7),
       (23, 3),
       (24, 7),
       (25, 11),
       (26, 11),
       (27, 12),
       (28, 12),
       (29, 1),
       (30, 2),
       (30, 11),
       (31, 4),
       (32, 3),
       (32, 1),
       (33, 13),
       (33, 11);

--
-- *** 6. LOCAL_USER INSERTS (Test Users) ***
--
-- Note: Passwords must be encoded (e.g., BCrypt). The hash below is for the plain password 'password'.
-- Hash for 'password' (BCrypt): $2a$10$i1.Q.F2G.0.T1yB.rT/Q.t2.h3/5o.j5.t2G.7E.h6.p2.5/2.L8

INSERT INTO local_user (id, email, is_email_verified, password)
VALUES (1, 'test1@mail.com', FALSE,
        '$2a$10$8uO1CjIwLoc0xniGfbUCwOs64gt./FEf0utOHAAc.sHocSxbwVuVO'), -- ID 1: Unverified User
       (2, 'test2@mail.com', TRUE,
        '$2a$10$8uO1CjIwLoc0xniGfbUCwOs64gt./FEf0utOHAAc.sHocSxbwVuVO'), -- ID 2: Verified User
       (3, 'admin@mail.com', TRUE,
        '$2a$10$8uO1CjIwLoc0xniGfbUCwOs64gt./FEf0utOHAAc.sHocSxbwVuVO');
-- ID 3: Verified User (Admin)

SELECT setval('local_user_id_seq', (SELECT MAX(id) FROM local_user) + 1, false);


-- *** 7. VERIFICATION_TOKEN INSERTS (For Pre-Authenticated Testing) ***
-- Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJVc2VyIERldGFpbHMiLCJFTUFJTF9DTEFJTSI6InRlc3QyQG1haWwuY29tIiwiZXhwIjoxNzY1MzY2OTk2LCJpYXQiOjE3NjQ3NjIxOTYsImlzcyI6IkFydGVtX0HvbnRhciJ9.h4WC9bg3IMo6jRUdVOyJw8x0lgNA_dGKLK5kwXf-XJs
-- User: test2@mail.com (LocalUser ID 2)

INSERT INTO verification_token (token, local_user_id, created_timestamp)
VALUES ('eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJVc2VyIERldGFpbHMiLCJFTUFJTF9DTEFJTSI6InRlc3QyQG1haWwuY29tIiwiZXhwIjoxNzY1MzY2OTk2LCJpYXQiOjE3NjQ3NjIxOTYsImlzcyI6IkFydGVtX0HvbnRhciJ9.h4WC9bg3IMo6jRUdVOyJw8x0lgNA_dGKLK5kwXf-XJs',
        2,
        CURRENT_TIMESTAMP - INTERVAL '1 minute');

-- *** 8. AUTHORITY INSERTS (Roles for Security) ***
INSERT INTO authority (id, authority_name)
VALUES (1, 'ROLE_USER'), -- ID 1
       (2, 'ROLE_ADMIN');
-- ID 2


-- 🔑 POSTGRESQL SEQUENCE FIX FOR AUTHORITY 🔑
-- This is also needed for authority table as you manually set IDs 1 and 2.
SELECT setval('authority_id_seq', (SELECT MAX(id) FROM authority) + 1, false);


-- *** 9. LOCAL_USER_AUTHORITY INSERTS (Assign Roles) ***
INSERT INTO local_user_authority (local_user_id, authority_id)
VALUES (1, 1), -- test1@mail.com (ID 1) is a standard ROLE_USER (ID 1)
       (2, 1), -- test2@mail.com (ID 2) is a standard ROLE_USER (ID 1)
       (3, 1), -- admin@mail.com (ID 3) is a ROLE_USER (ID 1)
       (3, 2); -- admin@mail.com (ID 3) is also a ROLE_ADMIN (ID 2)


-- *** 10. DELIVERY_ADDRESS INSERTS (Total 5 Addresses) ***
-- Binds addresses to existing local_user IDs 1, 2, and 3.

INSERT INTO delivery_address (local_user_id, first_name, last_name, address_line_1, address_line_2, city, country,
                              zipcode)
VALUES
    -- Addresses for LocalUser ID 1 (test1@mail.com)
    (1, 'Alice', 'Smith', '101 Pine St', 'Apt 4B', 'New York', 'USA', '10001'),
    (1, 'Alice', 'Smith', '45 Elm Ave', NULL, 'Seattle', 'USA', '98101'),

    -- Address for LocalUser ID 2 (test2@mail.com)
    (2, 'Bob', 'Johnson', '20 King St', 'Suite 12', 'London', 'UK', 'SW1A 0AA'),

    -- Addresses for LocalUser ID 3 (admin@mail.com)
    (3, 'Charlie', 'Brown', '33 Oak Lane', 'The Cottage', 'Paris', 'France', '75001'),
    (3, 'Charlie', 'Brown', '99 Main Road', NULL, 'Berlin', 'Germany', '10115');


-- *** 11. INVENTORY INSERTS (Total 33 Inventories, one for each Book) ***
-- Note: book_id is also the primary key for inventory.

INSERT INTO inventory (book_id, warehouse_stock, on_hold_stock, min_threshold)
VALUES
    -- Popular Titles (High Stock)
    (1, 250, 5, 10),  -- The Shining
    (2, 300, 10, 10), -- And Then There Were None
    (3, 180, 2, 5),   -- Frankenstein
    (4, 400, 15, 20), -- 1984
    (5, 500, 20, 20), -- The Lord of the Rings

    -- Standard Titles (Medium Stock)
    (6, 120, 5, 5),   -- Beloved
    (7, 150, 8, 5),   -- Neverwhere
    (8, 90, 3, 5),    -- Neuromancer
    (9, 110, 4, 5),   -- Pride and Prejudice
    (10, 80, 2, 5),   -- Sula

    -- Low/Hot Titles (Low Stock, High Hold)
    (11, 45, 15, 5),  -- The Hobbit (Low, but popular)
    (12, 30, 5, 3),   -- Rebecca
    (13, 22, 1, 3),   -- The Name of the Rose
    (14, 50, 10, 10), -- The Stand (High min threshold)
    (15, 35, 7, 3),   -- The Girl with the Dragon Tattoo

    -- Remaining Titles (Various Stock)
    (16, 75, 2, 5),   -- The Secret History
    (17, 130, 3, 5),  -- Murder on the Orient Express
    (18, 60, 1, 5),   -- A Scanner Darkly
    (19, 105, 4, 5),  -- Sense and Sensibility
    (20, 160, 6, 5),  -- Stardust
    (21, 140, 5, 5),  -- The Graveyard Book
    (22, 55, 1, 5),   -- Kindred
    (23, 85, 2, 5),   -- The Picture of Dorian Gray
    (24, 95, 3, 5),   -- A Tale of Two Cities
    (25, 70, 2, 5),   -- Tess of the d'Urbervilles
    (26, 115, 5, 5),  -- The Kite Runner
    (27, 25, 0, 3),   -- The God Delusion (Lower stock, non-fiction)
    (28, 15, 0, 3),   -- Becoming (Lower stock, non-fiction)
    (29, 40, 1, 3),   -- The Haunting of Hill House
    (30, 65, 3, 5),   -- Where the Crawdads Sing
    (31, 10, 1, 3),   -- The Ocean at the End of the Lane (Very low stock)
    (32, 50, 2, 5),   -- Dracula
    (33, 5, 0, 1); -- The Waste Land (Minimal stock, specialized)