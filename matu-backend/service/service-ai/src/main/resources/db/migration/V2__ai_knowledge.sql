-- RAG knowledge store (pgvector).
--
-- pgvector 0.8.0.2 is allow-listed on this Aliyun RDS instance and the service
-- account owns the database, so CREATE EXTENSION succeeds without superuser.
-- The dimension (1024) must match ai.rag.embedding-dimension in the Nacos config
-- and the actual output size of qwen3.7-text-embedding.
CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS ai_knowledge (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(128) NOT NULL,
    title TEXT,
    content TEXT NOT NULL,
    embedding vector(1024),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ai_knowledge_tenant_idx ON ai_knowledge(tenant_id);
