CREATE TABLE public.tenants (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL,
    schema_name VARCHAR(100) NOT NULL,
    status      VARCHAR(30)  NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT uk_tenants_name        UNIQUE (name),
    CONSTRAINT uk_tenants_slug        UNIQUE (slug),
    CONSTRAINT uk_tenants_schema_name UNIQUE (schema_name)
);
