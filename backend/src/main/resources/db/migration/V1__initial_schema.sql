
CREATE TABLE teams (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(100) NOT NULL UNIQUE,
                       description TEXT,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE categories (
                            id BIGSERIAL PRIMARY KEY,
                            name VARCHAR(100) NOT NULL UNIQUE,
                            description TEXT,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,

                       employee_code VARCHAR(30) UNIQUE,

                       name VARCHAR(150) NOT NULL,

                       email VARCHAR(255) NOT NULL UNIQUE,

                       password VARCHAR(255) NOT NULL,

                       role VARCHAR(30) NOT NULL,

                       status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

                       team_id BIGINT,

                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT fk_user_team
                           FOREIGN KEY (team_id)
                               REFERENCES teams(id)
                               ON DELETE SET NULL
);


CREATE TABLE tickets (
                         id BIGSERIAL PRIMARY KEY,

                         ticket_number VARCHAR(30) NOT NULL UNIQUE,

                         title VARCHAR(255) NOT NULL,

                         description TEXT NOT NULL,

                         priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',

                         status VARCHAR(20) NOT NULL DEFAULT 'OPEN',

                         created_by BIGINT NOT NULL,

                         assigned_to BIGINT,

                         category_id BIGINT NOT NULL,

                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                         resolved_at TIMESTAMP,

                         closed_at TIMESTAMP,

                         CONSTRAINT fk_ticket_creator
                             FOREIGN KEY (created_by)
                                 REFERENCES users(id)
                                 ON DELETE CASCADE,

                         CONSTRAINT fk_ticket_assignee
                             FOREIGN KEY (assigned_to)
                                 REFERENCES users(id)
                                 ON DELETE SET NULL,

                         CONSTRAINT fk_ticket_category
                             FOREIGN KEY (category_id)
                                 REFERENCES categories(id)
);