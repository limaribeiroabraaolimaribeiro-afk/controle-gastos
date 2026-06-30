-- ============================================================
-- Controle de Gastos PRO — Schema Supabase
-- Adicionar tabelas do Leitor Bancário Android
-- NÃO quebra tabelas existentes
-- Seguro para rodar mais de uma vez (idempotente)
-- ============================================================

-- ------------------------------------------------------------
-- TABELA: bank_notification_imports
-- Armazena detecções de notificações bancárias do app Android
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS bank_notification_imports (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    type               text NOT NULL CHECK (type IN ('entrada', 'saida')),
    amount             numeric NOT NULL CHECK (amount > 0),
    category           text,
    bank_name          text,
    app_package        text,
    notification_time  timestamptz,
    description        text,
    raw_hash           text NOT NULL,
    status             text NOT NULL DEFAULT 'pending'
                           CHECK (status IN ('pending', 'confirmed', 'ignored', 'auto_confirmed')),
    confidence_score   int DEFAULT 0 CHECK (confidence_score BETWEEN 0 AND 100),
    created_at         timestamptz DEFAULT now(),
    updated_at         timestamptz DEFAULT now()
);

-- Índice único para evitar duplicidade de notificações por usuário
CREATE UNIQUE INDEX IF NOT EXISTS idx_bank_imports_user_hash
    ON bank_notification_imports(user_id, raw_hash);

-- Índice para consultas por status
CREATE INDEX IF NOT EXISTS idx_bank_imports_status
    ON bank_notification_imports(user_id, status);

-- Índice para consultas por data
CREATE INDEX IF NOT EXISTS idx_bank_imports_time
    ON bank_notification_imports(user_id, notification_time DESC);

-- ------------------------------------------------------------
-- FUNÇÃO: atualiza updated_at automaticamente
-- CREATE OR REPLACE é idempotente — sem problema
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger em bank_notification_imports
DROP TRIGGER IF EXISTS bank_imports_updated_at ON bank_notification_imports;
CREATE TRIGGER bank_imports_updated_at
    BEFORE UPDATE ON bank_notification_imports
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ------------------------------------------------------------
-- RLS: bank_notification_imports
-- Cada usuário só acessa seus próprios dados
-- ------------------------------------------------------------
ALTER TABLE bank_notification_imports ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "bank_imports_select_own" ON bank_notification_imports;
CREATE POLICY "bank_imports_select_own"
    ON bank_notification_imports FOR SELECT
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "bank_imports_insert_own" ON bank_notification_imports;
CREATE POLICY "bank_imports_insert_own"
    ON bank_notification_imports FOR INSERT
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "bank_imports_update_own" ON bank_notification_imports;
CREATE POLICY "bank_imports_update_own"
    ON bank_notification_imports FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "bank_imports_delete_own" ON bank_notification_imports;
CREATE POLICY "bank_imports_delete_own"
    ON bank_notification_imports FOR DELETE
    USING (auth.uid() = user_id);

-- ------------------------------------------------------------
-- TABELA: income_entries
-- Entradas/receitas detectadas pelo leitor bancário ou
-- adicionadas manualmente no PWA
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS income_entries (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    mes            text NOT NULL,      -- formato: "YYYY-MM"
    descricao      text,
    categoria      text DEFAULT 'Entrada',
    valor          numeric NOT NULL CHECK (valor > 0),
    data           date,
    origem         text DEFAULT 'manual'
                       CHECK (origem IN ('manual', 'notificacao_banco', 'pwa')),
    bank_import_id uuid REFERENCES bank_notification_imports(id) ON DELETE SET NULL,
    created_at     timestamptz DEFAULT now(),
    updated_at     timestamptz DEFAULT now()
);

-- Índices para consultas por mês e usuário
CREATE INDEX IF NOT EXISTS idx_income_entries_mes
    ON income_entries(user_id, mes);

CREATE INDEX IF NOT EXISTS idx_income_entries_data
    ON income_entries(user_id, data DESC);

-- Trigger em income_entries
DROP TRIGGER IF EXISTS income_entries_updated_at ON income_entries;
CREATE TRIGGER income_entries_updated_at
    BEFORE UPDATE ON income_entries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ------------------------------------------------------------
-- RLS: income_entries
-- ------------------------------------------------------------
ALTER TABLE income_entries ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "income_select_own" ON income_entries;
CREATE POLICY "income_select_own"
    ON income_entries FOR SELECT
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "income_insert_own" ON income_entries;
CREATE POLICY "income_insert_own"
    ON income_entries FOR INSERT
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "income_update_own" ON income_entries;
CREATE POLICY "income_update_own"
    ON income_entries FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "income_delete_own" ON income_entries;
CREATE POLICY "income_delete_own"
    ON income_entries FOR DELETE
    USING (auth.uid() = user_id);

-- ------------------------------------------------------------
-- TABELA: expenses (cria se não existir)
-- Gastos vinculados a importações bancárias
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS expenses (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    mes            text NOT NULL,
    descricao      text,
    categoria      text DEFAULT 'Outros',
    valor          numeric NOT NULL CHECK (valor > 0),
    data           date,
    origem         text DEFAULT 'manual',
    bank_import_id uuid REFERENCES bank_notification_imports(id) ON DELETE SET NULL,
    created_at     timestamptz DEFAULT now(),
    updated_at     timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_expenses_mes
    ON expenses(user_id, mes);

-- Trigger em expenses
DROP TRIGGER IF EXISTS expenses_updated_at ON expenses;
CREATE TRIGGER expenses_updated_at
    BEFORE UPDATE ON expenses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ------------------------------------------------------------
-- RLS: expenses
-- ------------------------------------------------------------
ALTER TABLE expenses ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "expenses_select_own" ON expenses;
CREATE POLICY "expenses_select_own"
    ON expenses FOR SELECT
    USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "expenses_insert_own" ON expenses;
CREATE POLICY "expenses_insert_own"
    ON expenses FOR INSERT
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "expenses_update_own" ON expenses;
CREATE POLICY "expenses_update_own"
    ON expenses FOR UPDATE
    USING (auth.uid() = user_id)
    WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "expenses_delete_own" ON expenses;
CREATE POLICY "expenses_delete_own"
    ON expenses FOR DELETE
    USING (auth.uid() = user_id);

-- ------------------------------------------------------------
-- VIEW auxiliar: resumo mensal de importações por usuário
-- CREATE OR REPLACE é idempotente
-- ------------------------------------------------------------
CREATE OR REPLACE VIEW bank_imports_summary AS
SELECT
    user_id,
    LEFT(COALESCE(notification_time::text, created_at::text), 7) AS mes,
    COUNT(*) FILTER (WHERE status = 'pending')                              AS total_pending,
    COUNT(*) FILTER (WHERE status IN ('confirmed', 'auto_confirmed'))       AS total_confirmed,
    COUNT(*) FILTER (WHERE status = 'ignored')                              AS total_ignored,
    SUM(amount) FILTER (WHERE type = 'entrada'
        AND status IN ('confirmed', 'auto_confirmed'))                      AS total_entradas,
    SUM(amount) FILTER (WHERE type = 'saida'
        AND status IN ('confirmed', 'auto_confirmed'))                      AS total_saidas,
    COUNT(*)                                                                AS total
FROM bank_notification_imports
GROUP BY user_id, LEFT(COALESCE(notification_time::text, created_at::text), 7);
