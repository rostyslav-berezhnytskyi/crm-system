CREATE TABLE calendar_events (
                                 id BIGSERIAL PRIMARY KEY,
                                 title VARCHAR(100) NOT NULL,
                                 description VARCHAR(500),
                                 start_date TIMESTAMP NOT NULL,
                                 end_date TIMESTAMP,
                                 color VARCHAR(20)
);