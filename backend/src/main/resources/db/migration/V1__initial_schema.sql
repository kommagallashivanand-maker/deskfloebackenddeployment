CREATE TABLE teams (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       name VARCHAR(100) NOT NULL UNIQUE,
                       description TEXT,
                       status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE categories (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            name VARCHAR(100) NOT NULL UNIQUE,
                            description TEXT,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                       employee_code VARCHAR(30) UNIQUE,

                       name VARCHAR(150) NOT NULL,

                       email VARCHAR(255) NOT NULL UNIQUE,

                       password VARCHAR(255) NOT NULL,

                       role VARCHAR(30) NOT NULL,

                       status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

                       team_id UUID,

                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

                       CONSTRAINT fk_user_team
                           FOREIGN KEY (team_id)
                               REFERENCES teams(id)
                               ON DELETE SET NULL
);


CREATE TABLE tickets (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

                         ticket_number VARCHAR(30) NOT NULL UNIQUE,

                         title VARCHAR(255) NOT NULL,

                         description TEXT NOT NULL,

                         priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',

                         status VARCHAR(20) NOT NULL DEFAULT 'OPEN',

                         created_by UUID NOT NULL,

                         assigned_to UUID,

                         category_id UUID NOT NULL,

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