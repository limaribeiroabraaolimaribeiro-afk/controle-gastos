-- ============================================================
-- Controle de Gastos PRO -- Schema Supabase Completo
-- Idempotente: pode rodar mais de uma vez sem erro
-- ============================================================

-- ------------------------------------------------------------
-- GRANTS DE SCHEMA (necessario para authenticated acessar tabelas)
-- Supabase exige GRANT antes das policies funcionarem
-- ------------------------------------------------------------
GRANT USAGE ON SCHEMA public TO anon, authenticated, service_role;

-- ------------------------------------------------------------
-- FUNCAO: atualiza updated_at automaticamente
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================
-- 1. PROFILES
-- ============================================================
CREATE TABLE IF NOT EXISTS profiles (
    id           uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email        text,
    display_name text,
    avatar_url   text,
    theme_preset text DEFAULT 'default',
    accent_color text DEFAULT '#569cff',
    theme_mode   text DEFAULT 'dark' CHECK (theme_mode IN ('dark', 'light')),
    created_at   timestamptz DEFAULT now(),
    updated_at   timestamptz DEFAULT now()
);

DROP TRIGGER IF EXISTS profiles_updated_at ON profiles;
CREATE TRIGGER profiles_updated_at
    BEFORE UPDATE ON profiles
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE profiles ENABLE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, UPDATE, DELETE ON profiles TO authenticated;

DROP POLICY IF EXISTS "profiles_select_own" ON profiles;
CREATE POLICY "profiles_select_own"
    ON profiles FOR SELECT USING (auth.uid() = id);

DROP POLICY IF EXISTS "profiles_insert_own" ON profiles;
CREATE POLICY "profiles_insert_own"
    ON profiles FOR INSERT WITH CHECK (auth.uid() = id);

DROP POLICY IF EXISTS "profiles_update_own" ON profiles;
CREATE POLICY "profiles_update_own"
    ON profiles FOR UPDATE USING (auth.uid() = id) WITH CHECK (auth.uid() = id);

DROP POLICY IF EXISTS "profiles_delete_own" ON profiles;
CREATE POLICY "profiles_delete_own"
    ON profiles FOR DELETE USING (auth.uid() = id);

-- Trigger: cria perfil automaticamente ao registrar usuario
CREATE OR REPLACE FUNCTION handle_new_user()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO public.profiles (id, email)
    VALUES (NEW.id, NEW.email)
    ON CONFLICT (id) DO NOTHING;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
    AFTER INSERT ON auth.users
    FOR EACH ROW EXECUTE FUNCTION handle_new_user();

-- ============================================================
-- 2. MONTHLY_SETTINGS
-- ============================================================
CREATE TABLE IF NOT EXISTS monthly_settings (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    mes        text NOT NULL,
    salary     numeric DEFAULT 0,
    meta       numeric DEFAULT 0,
    notes      text,
    created_at timestamptz DEFAULT now(),
    updated_at timestamptz DEFAULT now()
);

-- Garante colunas salary e meta em tabelas criadas antes desta versao do schema
ALTER TABLE monthly_settings ADD COLUMN IF NOT EXISTS salary numeric DEFAULT 0;
ALTER TABLE monthly_settings ADD COLUMN IF NOT EXISTS meta   numeric DEFAULT 0;
ALTER TABLE monthly_settings ADD COLUMN IF NOT EXISTS notes  text;

CREATE UNIQUE INDEX IF NOT EXISTS idx_monthly_settings_user_mes
    ON monthly_settings(user_id, mes);

DROP TRIGGER IF EXISTS monthly_settings_updated_at ON monthly_settings;
CREATE TRIGGER monthly_settings_updated_at
    BEFORE UPDATE ON monthly_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE monthly_settings ENABLE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, UPDATE, DELETE ON monthly_settings TO authenticated;

DROP POLICY IF EXISTS "monthly_settings_select_own" ON monthly_settings;
CREATE POLICY "monthly_settings_select_own"
    ON monthly_settings FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "monthly_settings_insert_own" ON monthly_settings;
CREATE POLICY "monthly_settings_insert_own"
    ON monthly_settings FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "monthly_settings_update_own" ON monthly_settings;
CREATE POLICY "monthly_settings_update_own"
    ON monthly_settings FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "monthly_settings_delete_own" ON monthly_settings;
CREATE POLICY "monthly_settings_delete_own"
    ON monthly_settings FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- 3. CATEGORIES
-- ============================================================
CREATE TABLE IF NOT EXISTS categories (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    nome       text NOT NULL,
    sort_order integer DEFAULT 0,
    created_at timestamptz DEFAULT now(),
    updated_at timestamptz DEFAULT now()
);

-- Compatibilidade com tabelas criadas sem sort_order
ALTER TABLE categories ADD COLUMN IF NOT EXISTS sort_order integer DEFAULT 0;

CREATE UNIQUE INDEX IF NOT EXISTS idx_categories_user_nome
    ON categories(user_id, lower(nome));

DROP TRIGGER IF EXISTS categories_updated_at ON categories;
CREATE TRIGGER categories_updated_at
    BEFORE UPDATE ON categories
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE categories ENABLE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, UPDATE, DELETE ON categories TO authenticated;

DROP POLICY IF EXISTS "categories_select_own" ON categories;
CREATE POLICY "categories_select_own"
    ON categories FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "categories_insert_own" ON categories;
CREATE POLICY "categories_insert_own"
    ON categories FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "categories_update_own" ON categories;
CREATE POLICY "categories_update_own"
    ON categories FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "categories_delete_own" ON categories;
CREATE POLICY "categories_delete_own"
    ON categories FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- 4. FIXED_EXPENSES
-- ============================================================
CREATE TABLE IF NOT EXISTS fixed_expenses (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    descricao  text NOT NULL,
    categoria  text DEFAULT 'Outros',
    valor      numeric NOT NULL CHECK (valor > 0),
    sort_order integer DEFAULT 0,
    ativo      boolean DEFAULT true,
    created_at timestamptz DEFAULT now(),
    updated_at timestamptz DEFAULT now()
);

-- Compatibilidade com tabelas criadas sem sort_order
ALTER TABLE fixed_expenses ADD COLUMN IF NOT EXISTS sort_order integer DEFAULT 0;
ALTER TABLE fixed_expenses ADD COLUMN IF NOT EXISTS ativo     boolean DEFAULT true;

DROP TRIGGER IF EXISTS fixed_expenses_updated_at ON fixed_expenses;
CREATE TRIGGER fixed_expenses_updated_at
    BEFORE UPDATE ON fixed_expenses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE fixed_expenses ENABLE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, UPDATE, DELETE ON fixed_expenses TO authenticated;

DROP POLICY IF EXISTS "fixed_expenses_select_own" ON fixed_expenses;
CREATE POLICY "fixed_expenses_select_own"
    ON fixed_expenses FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "fixed_expenses_insert_own" ON fixed_expenses;
CREATE POLICY "fixed_expenses_insert_own"
    ON fixed_expenses FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "fixed_expenses_update_own" ON fixed_expenses;
CREATE POLICY "fixed_expenses_update_own"
    ON fixed_expenses FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "fixed_expenses_delete_own" ON fixed_expenses;
CREATE POLICY "fixed_expenses_delete_own"
    ON fixed_expenses FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- 5. FIXED_INVESTMENTS
-- ============================================================
CREATE TABLE IF NOT EXISTS fixed_investments (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    nome        text NOT NULL,
    meta_final  numeric DEFAULT 0,
    prazo_meses int DEFAULT 12,
    mensal      numeric DEFAULT 0,
    dia_pagar   int DEFAULT 0,
    ativo       boolean DEFAULT true,
    created_at  timestamptz DEFAULT now(),
    updated_at  timestamptz DEFAULT now()
);

DROP TRIGGER IF EXISTS fixed_investments_updated_at ON fixed_investments;
CREATE TRIGGER fixed_investments_updated_at
    BEFORE UPDATE ON fixed_investments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE fixed_investments ENABLE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, UPDATE, DELETE ON fixed_investments TO authenticated;

DROP POLICY IF EXISTS "fixed_investments_select_own" ON fixed_investments;
CREATE POLICY "fixed_investments_select_own"
    ON fixed_investments FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "fixed_investments_insert_own" ON fixed_investments;
CREATE POLICY "fixed_investments_insert_own"
    ON fixed_investments FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "fixed_investments_update_own" ON fixed_investments;
CREATE POLICY "fixed_investments_update_own"
    ON fixed_investments FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "fixed_investments_delete_own" ON fixed_investments;
CREATE POLICY "fixed_investments_delete_own"
    ON fixed_investments FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- 6. SAVED_AMOUNTS
-- ============================================================
CREATE TABLE IF NOT EXISTS saved_amounts (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    mes           text NOT NULL,
    valor         numeric NOT NULL CHECK (valor > 0),
    descricao     text,
    data          date DEFAULT CURRENT_DATE,
    source        text DEFAULT 'manual',
    investment_id uuid REFERENCES fixed_investments(id) ON DELETE SET NULL,
    created_at    timestamptz DEFAULT now(),
    updated_at    timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_saved_amounts_user_mes
    ON saved_amounts(user_id, mes);

CREATE INDEX IF NOT EXISTS idx_saved_amounts_investment
    ON saved_amounts(investment_id);

DROP TRIGGER IF EXISTS saved_amounts_updated_at ON saved_amounts;
CREATE TRIGGER saved_amounts_updated_at
    BEFORE UPDATE ON saved_amounts
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE saved_amounts ENABLE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, UPDATE, DELETE ON saved_amounts TO authenticated;

DROP POLICY IF EXISTS "saved_amounts_select_own" ON saved_amounts;
CREATE POLICY "saved_amounts_select_own"
    ON saved_amounts FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "saved_amounts_insert_own" ON saved_amounts;
CREATE POLICY "saved_amounts_insert_own"
    ON saved_amounts FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "saved_amounts_update_own" ON saved_amounts;
CREATE POLICY "saved_amounts_update_own"
    ON saved_amounts FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "saved_amounts_delete_own" ON saved_amounts;
CREATE POLICY "saved_amounts_delete_own"
    ON saved_amounts FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- 7. EXPENSES
-- Gastos variaveis por mes
-- Colunas alinhadas com o payload do app:
--   user_id, mes, descricao, categoria, valor, data, status, pago
-- ============================================================
CREATE TABLE IF NOT EXISTS expenses (
    id         uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    mes        text NOT NULL,
    descricao  text,
    categoria  text DEFAULT 'Outros',
    valor      numeric NOT NULL CHECK (valor > 0),
    data       date,
    status     text DEFAULT 'ativo',
    pago       boolean DEFAULT false,
    created_at timestamptz DEFAULT now(),
    updated_at timestamptz DEFAULT now()
);

-- Colunas adicionadas em versao anterior (manter compatibilidade se ja existirem)
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS paid      boolean DEFAULT false;
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS is_fixo   boolean DEFAULT false;
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS origem    text DEFAULT 'manual';
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS status    text DEFAULT 'ativo';
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS pago      boolean DEFAULT false;
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS bank_import_id uuid;

CREATE INDEX IF NOT EXISTS idx_expenses_user_mes
    ON expenses(user_id, mes);

CREATE INDEX IF NOT EXISTS idx_expenses_data
    ON expenses(user_id, data DESC);

DROP TRIGGER IF EXISTS expenses_updated_at ON expenses;
CREATE TRIGGER expenses_updated_at
    BEFORE UPDATE ON expenses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE expenses ENABLE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, UPDATE, DELETE ON expenses TO authenticated;

DROP POLICY IF EXISTS "expenses_select_own" ON expenses;
CREATE POLICY "expenses_select_own"
    ON expenses FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "expenses_insert_own" ON expenses;
CREATE POLICY "expenses_insert_own"
    ON expenses FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "expenses_update_own" ON expenses;
CREATE POLICY "expenses_update_own"
    ON expenses FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "expenses_delete_own" ON expenses;
CREATE POLICY "expenses_delete_own"
    ON expenses FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- 8. HISTORY
-- ============================================================
CREATE TABLE IF NOT EXISTS history (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    mes          text NOT NULL,
    salary       numeric DEFAULT 0,
    meta         numeric DEFAULT 0,
    total_gastos numeric DEFAULT 0,
    total_saved  numeric DEFAULT 0,
    saldo        numeric DEFAULT 0,
    snapshot     jsonb,
    created_at   timestamptz DEFAULT now(),
    updated_at   timestamptz DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_history_user_mes
    ON history(user_id, mes);

DROP TRIGGER IF EXISTS history_updated_at ON history;
CREATE TRIGGER history_updated_at
    BEFORE UPDATE ON history
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE history ENABLE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, UPDATE, DELETE ON history TO authenticated;

DROP POLICY IF EXISTS "history_select_own" ON history;
CREATE POLICY "history_select_own"
    ON history FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "history_insert_own" ON history;
CREATE POLICY "history_insert_own"
    ON history FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "history_update_own" ON history;
CREATE POLICY "history_update_own"
    ON history FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "history_delete_own" ON history;
CREATE POLICY "history_delete_own"
    ON history FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- 9. APP_BACKUPS
-- ============================================================
CREATE TABLE IF NOT EXISTS app_backups (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    label       text,
    payload     jsonb NOT NULL,
    app_version text,
    created_at  timestamptz DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_app_backups_user
    ON app_backups(user_id, created_at DESC);

ALTER TABLE app_backups ENABLE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, UPDATE, DELETE ON app_backups TO authenticated;

DROP POLICY IF EXISTS "app_backups_select_own" ON app_backups;
CREATE POLICY "app_backups_select_own"
    ON app_backups FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "app_backups_insert_own" ON app_backups;
CREATE POLICY "app_backups_insert_own"
    ON app_backups FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "app_backups_delete_own" ON app_backups;
CREATE POLICY "app_backups_delete_own"
    ON app_backups FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- 10. BANK_NOTIFICATION_IMPORTS
-- ============================================================
CREATE TABLE IF NOT EXISTS bank_notification_imports (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    type              text NOT NULL CHECK (type IN ('entrada', 'saida')),
    amount            numeric NOT NULL CHECK (amount > 0),
    category          text,
    bank_name         text,
    app_package       text,
    notification_time timestamptz,
    description       text,
    raw_hash          text NOT NULL,
    status            text NOT NULL DEFAULT 'pending'
                          CHECK (status IN ('pending', 'confirmed', 'ignored', 'auto_confirmed')),
    confidence_score  int DEFAULT 0 CHECK (confidence_score BETWEEN 0 AND 100),
    created_at        timestamptz DEFAULT now(),
    updated_at        timestamptz DEFAULT now()
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_bank_imports_user_hash
    ON bank_notification_imports(user_id, raw_hash);

CREATE INDEX IF NOT EXISTS idx_bank_imports_status
    ON bank_notification_imports(user_id, status);

CREATE INDEX IF NOT EXISTS idx_bank_imports_time
    ON bank_notification_imports(user_id, notification_time DESC);

DROP TRIGGER IF EXISTS bank_imports_updated_at ON bank_notification_imports;
CREATE TRIGGER bank_imports_updated_at
    BEFORE UPDATE ON bank_notification_imports
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE bank_notification_imports ENABLE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, UPDATE, DELETE ON bank_notification_imports TO authenticated;

DROP POLICY IF EXISTS "bank_imports_select_own" ON bank_notification_imports;
CREATE POLICY "bank_imports_select_own"
    ON bank_notification_imports FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "bank_imports_insert_own" ON bank_notification_imports;
CREATE POLICY "bank_imports_insert_own"
    ON bank_notification_imports FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "bank_imports_update_own" ON bank_notification_imports;
CREATE POLICY "bank_imports_update_own"
    ON bank_notification_imports FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "bank_imports_delete_own" ON bank_notification_imports;
CREATE POLICY "bank_imports_delete_own"
    ON bank_notification_imports FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- 11. INCOME_ENTRIES
-- ============================================================
CREATE TABLE IF NOT EXISTS income_entries (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    mes            text NOT NULL,
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

CREATE INDEX IF NOT EXISTS idx_income_entries_mes
    ON income_entries(user_id, mes);

CREATE INDEX IF NOT EXISTS idx_income_entries_data
    ON income_entries(user_id, data DESC);

DROP TRIGGER IF EXISTS income_entries_updated_at ON income_entries;
CREATE TRIGGER income_entries_updated_at
    BEFORE UPDATE ON income_entries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE income_entries ENABLE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, UPDATE, DELETE ON income_entries TO authenticated;

DROP POLICY IF EXISTS "income_select_own" ON income_entries;
CREATE POLICY "income_select_own"
    ON income_entries FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "income_insert_own" ON income_entries;
CREATE POLICY "income_insert_own"
    ON income_entries FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "income_update_own" ON income_entries;
CREATE POLICY "income_update_own"
    ON income_entries FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "income_delete_own" ON income_entries;
CREATE POLICY "income_delete_own"
    ON income_entries FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- VIEW: resumo mensal de importacoes Android
-- ============================================================
CREATE OR REPLACE VIEW bank_imports_summary AS
SELECT
    user_id,
    LEFT(COALESCE(notification_time::text, created_at::text), 7) AS mes,
    COUNT(*) FILTER (WHERE status = 'pending')                        AS total_pending,
    COUNT(*) FILTER (WHERE status IN ('confirmed', 'auto_confirmed')) AS total_confirmed,
    COUNT(*) FILTER (WHERE status = 'ignored')                        AS total_ignored,
    SUM(amount) FILTER (WHERE type = 'entrada'
        AND status IN ('confirmed', 'auto_confirmed'))                AS total_entradas,
    SUM(amount) FILTER (WHERE type = 'saida'
        AND status IN ('confirmed', 'auto_confirmed'))                AS total_saidas,
    COUNT(*)                                                          AS total
FROM bank_notification_imports
GROUP BY user_id, LEFT(COALESCE(notification_time::text, created_at::text), 7);

-- ============================================================
-- 12. APP_SETTINGS
-- Preferencias do app por usuario (tema, voz, moeda etc.)
-- UNIQUE (user_id) garante apenas uma linha por usuario
-- ============================================================
CREATE TABLE IF NOT EXISTS app_settings (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id        uuid NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    theme          text DEFAULT 'dark'    CHECK (theme IN ('dark', 'light')),
    accent_color   text DEFAULT '#569cff',
    theme_preset   text DEFAULT 'default',
    voice_enabled  boolean DEFAULT false,
    currency       text DEFAULT 'BRL',
    ai_enabled     boolean DEFAULT true,
    reader_enabled boolean DEFAULT false,
    created_at     timestamptz DEFAULT now(),
    updated_at     timestamptz DEFAULT now(),
    CONSTRAINT app_settings_user_unique UNIQUE (user_id)
);

DROP TRIGGER IF EXISTS app_settings_updated_at ON app_settings;
CREATE TRIGGER app_settings_updated_at
    BEFORE UPDATE ON app_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE app_settings ENABLE ROW LEVEL SECURITY;

GRANT SELECT, INSERT, UPDATE, DELETE ON app_settings TO authenticated;

DROP POLICY IF EXISTS "app_settings_select_own" ON app_settings;
CREATE POLICY "app_settings_select_own"
    ON app_settings FOR SELECT USING (auth.uid() = user_id);

DROP POLICY IF EXISTS "app_settings_insert_own" ON app_settings;
CREATE POLICY "app_settings_insert_own"
    ON app_settings FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "app_settings_update_own" ON app_settings;
CREATE POLICY "app_settings_update_own"
    ON app_settings FOR UPDATE USING (auth.uid() = user_id) WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS "app_settings_delete_own" ON app_settings;
CREATE POLICY "app_settings_delete_own"
    ON app_settings FOR DELETE USING (auth.uid() = user_id);

-- ============================================================
-- Colunas extras: history (metricas adicionais)
-- ============================================================
ALTER TABLE history ADD COLUMN IF NOT EXISTS total_pago     numeric DEFAULT 0;
ALTER TABLE history ADD COLUMN IF NOT EXISTS total_pendente numeric DEFAULT 0;
ALTER TABLE history ADD COLUMN IF NOT EXISTS guardado       numeric DEFAULT 0;
ALTER TABLE history ADD COLUMN IF NOT EXISTS sobra          numeric DEFAULT 0;
ALTER TABLE history ADD COLUMN IF NOT EXISTS saude          numeric DEFAULT 0;

-- ============================================================
-- GRANTS FINAIS: sequences e view
-- ============================================================
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO authenticated;
GRANT SELECT ON bank_imports_summary TO authenticated;
