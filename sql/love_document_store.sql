create table public.love_document_store
(
    id        uuid default uuid_generate_v4() not null
        primary key,
    content   text,
    metadata  json,
    embedding vector(1536)
);

alter table public.love_document_store
    owner to postgres;

create index spring_ai_vector_index
    on public.love_document_store using hnsw (embedding public.vector_cosine_ops);

