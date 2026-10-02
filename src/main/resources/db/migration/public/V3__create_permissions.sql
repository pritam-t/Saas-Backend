CREATE TABLE public.permissions (
                                    id UUID PRIMARY KEY,
                                    code VARCHAR(100) NOT NULL,
                                    description VARCHAR(255) NOT NULL,

                                    CONSTRAINT uk_permissions_code
                                        UNIQUE (code)
);