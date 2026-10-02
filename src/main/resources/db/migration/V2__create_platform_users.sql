CREATE TABLE public.platform_users (
                                       id UUID PRIMARY KEY,
                                       email VARCHAR(255) NOT NULL UNIQUE,
                                       password_hash VARCHAR(255) NOT NULL,
                                       status VARCHAR(30) NOT NULL,
                                       created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                       updated_at TIMESTAMP WITH TIME ZONE NOT NULL,

                                       CONSTRAINT chk_platform_user_status
                                           CHECK (status IN ('ACTIVE', 'DISABLED'))
);