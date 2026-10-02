CREATE TABLE public.tenants (
                                id UUID PRIMARY KEY,
                                name VARCHAR(255) NOT NULL,
                                slug VARCHAR(100) NOT NULL UNIQUE,
                                schema_name VARCHAR(150) NOT NULL UNIQUE,
                                status VARCHAR(30) NOT NULL,
                                created_at TIMESTAMP WITH TIME ZONE NOT NULL,
                                updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);