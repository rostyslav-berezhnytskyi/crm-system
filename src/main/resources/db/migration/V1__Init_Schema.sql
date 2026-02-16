-- 1. Create Users Table
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       email VARCHAR(255) UNIQUE,
                       role VARCHAR(50) NOT NULL
);

-- 2. Create Projects Table
CREATE TABLE projects (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          description TEXT,
                          active BOOLEAN NOT NULL DEFAULT TRUE,
                          created_date TIMESTAMP NOT NULL
);

-- 3. Create Transactions Table
CREATE TABLE transactions (
                              id BIGSERIAL PRIMARY KEY,
                              type VARCHAR(50) NOT NULL,
                              amount NUMERIC(19, 2) NOT NULL, -- BigDecimal maps to Numeric
                              category VARCHAR(50) NOT NULL,
                              payment_method VARCHAR(50) NOT NULL,
                              description VARCHAR(500),
                              date TIMESTAMP NOT NULL,
                              receipt_image_url VARCHAR(255),
                              item_image_url VARCHAR(255),

    -- Foreign Keys
                              user_id BIGINT NOT NULL REFERENCES users(id),
                              project_id BIGINT NOT NULL REFERENCES projects(id)
);