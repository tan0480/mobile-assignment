-- Gadget Mover — Supabase schema
-- Run this once in the Supabase SQL Editor (Project → SQL Editor → New query).
-- Safe to re-run: every statement is idempotent (IF NOT EXISTS / DROP ... IF EXISTS).

-- ============================================================================
-- 1. profiles
-- One row per auth.users row. Never stores a password — Supabase Auth (GoTrue)
-- owns credentials in the private auth.users table; this table is the public-
-- readable profile Postgrest actually queries/joins against.
-- ============================================================================
create table if not exists public.profiles (
    id uuid primary key references auth.users(id) on delete cascade,
    username text not null default '',
    user_id text,
    email text not null default '',
    phone_number text not null default '',
    location text not null default '',
    avatar_url text not null default '',
    rating numeric not null default 0,
    rating_count integer not null default 0,
    is_verified boolean not null default false,
    wallet_balance numeric not null default 0,
    created_at timestamptz not null default now()
);

-- Account-level settings toggles — synced/server-checkable rather than device-local, unlike
-- the dark-mode preference (a display setting, not an account fact) which stays on-device.
alter table public.profiles add column if not exists notifications_enabled boolean not null default true;
alter table public.profiles add column if not exists marketing_emails_enabled boolean not null default true;

-- `user_id` (e.g. "@cjgoh84") is the public, unique handle chosen at registration — unlike
-- `username`, the free-text display name shown throughout the app, which is allowed to repeat
-- between accounts. Left nullable (no default) rather than `not null default ''` so a unique
-- index can be added without every pre-existing row colliding on the empty string — Postgres
-- unique indexes already treat any number of NULLs as distinct, so old rows just have no handle
-- until their owner sets one.
alter table public.profiles add column if not exists user_id text;
create unique index if not exists profiles_user_id_key on public.profiles (user_id);

-- Stripe Customer id backing the saved-payment-methods feature (Profile > Payment Methods,
-- and PaymentSheet's saved-card UI at checkout). Written only by the get-or-create-stripe-customer
-- Edge Function using the service-role key — see the revoke below.
alter table public.profiles add column if not exists stripe_customer_id text;

-- False for an account created via Google Sign-In that has never set a Gadget Mover password —
-- set at insert time by handle_new_user() below (from auth.users' own provider metadata, not
-- something the client claims), and flipped true by the app once the seller sets one (see
-- AccountInfoScreen / the "create a password" prompt gating Buy/Rent/List an item). Defaults true
-- so every pre-existing email/password account is unaffected by this column's addition.
alter table public.profiles add column if not exists has_password boolean not null default true;

alter table public.profiles enable row level security;

drop policy if exists "profiles are publicly readable" on public.profiles;
create policy "profiles are publicly readable"
    on public.profiles for select
    using (true);

drop policy if exists "users can update their own profile" on public.profiles;
create policy "users can update their own profile"
    on public.profiles for update
    using (auth.uid() = id)
    with check (auth.uid() = id);

-- Column-level grants narrow that row-level policy further: wallet_balance may only change via
-- credit_wallet()/debit_wallet() (see wallet_transactions below), is_verified only by a real
-- verification flow if one is ever added, and stripe_customer_id only by the service-role
-- get-or-create-stripe-customer Edge Function — none of these should be directly settable by the
-- row's own owner, or a client could grant itself funds, a verified badge, or splice in someone
-- else's Stripe customer with one PostgREST call.
revoke update (wallet_balance, is_verified, stripe_customer_id) on public.profiles from authenticated;

-- A new auth.users row always gets a matching profiles row automatically, so
-- the app never has to (and never has RLS permission to) INSERT into
-- profiles directly — it only UPDATEs the row the trigger already created.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    -- has_password reads the real signup provider straight off auth.users' own metadata
    -- (never something the client could claim) — 'email' means a password was actually set at
    -- signup, anything else (e.g. 'google') means the account only has native-auth credentials.
    insert into public.profiles (id, email, has_password)
    values (
        new.id,
        coalesce(new.email, ''),
        coalesce(new.raw_app_meta_data->>'provider', 'email') = 'email'
    );
    return new;
end;
$$;

drop trigger if exists on_auth_user_created on auth.users;
create trigger on_auth_user_created
    after insert on auth.users
    for each row execute function public.handle_new_user();

-- ============================================================================
-- 2. products
-- `specs` carries every category-specific attribute (a phone's SoC, a PSU's
-- wattage, ...) as one jsonb blob — a `CategoryFilterState` from the app's
-- schema-driven filter model (model/filter/FilterSchema.kt) — since each of
-- the 20 product categories needs a different, much larger field set than a
-- one-column-per-field table could reasonably hold. Every other column is a
-- flat, category-agnostic attribute every listing has regardless of category.
-- `brand` also stays a flat column (denormalized from `specs`) purely so it's
-- easy to eyeball/query in the dashboard.
-- ============================================================================
create table if not exists public.products (
    id uuid primary key default gen_random_uuid(),
    seller_id uuid not null references auth.users(id) on delete cascade,
    title text not null,
    description text not null default '',
    brand text not null default '',
    category text not null,
    condition text not null,
    listing_type text not null check (listing_type in ('BUY', 'RENT', 'BOTH')),
    buy_price numeric,
    rental_price_per_day numeric,
    deposit numeric,
    specs jsonb not null default '{}'::jsonb,
    image_urls text[] not null default '{}',
    location text not null default '',
    is_featured boolean not null default false,
    has_warranty boolean not null default false,
    warranty_details text,
    fulfillment_methods text[] not null default '{}',
    meetup_locations jsonb not null default '[]'::jsonb,
    status text not null default 'AVAILABLE',
    created_at timestamptz not null default now()
);

-- Migrates an already-deployed products table (this file is re-run against
-- the live project rather than tracked through a separate migrations
-- folder) from the old flat, keyboard-shaped spec columns to the jsonb
-- `specs` column above.
alter table public.products add column if not exists specs jsonb not null default '{}'::jsonb;
alter table public.products drop column if exists switch_type;
alter table public.products drop column if exists layout;
alter table public.products drop column if exists hot_swappable;
alter table public.products drop column if exists connectivity;
alter table public.products drop column if exists battery_life_hours;
alter table public.products drop column if exists rgb_lighting;
alter table public.products drop column if exists weight_grams;
alter table public.products drop column if exists keycap_material;

-- Which handover methods ('SHIPPING'/'MEETUP') a listing supports, plus the
-- seller's declared meet-up spots (name/address/lat/lng) — read by the
-- checkout flow and the buyer-facing "Delivery / Meet-up" common filter.
alter table public.products add column if not exists fulfillment_methods text[] not null default '{}';
alter table public.products add column if not exists meetup_locations jsonb not null default '[]'::jsonb;

-- Seller-set shipping fees (null = seller doesn't offer that tier) — replaces
-- the old platform-fixed ShippingTier.fee; the checkout screen reads these
-- instead of a hardcoded amount.
alter table public.products add column if not exists standard_shipping_fee numeric;
alter table public.products add column if not exists express_shipping_fee numeric;

-- For a RENT/BOTH listing with SHIPPING enabled, the seller's own address that
-- renters should ship the item back to — a snapshot set at listing time
-- (not a live reference into the seller's address book), same rationale as
-- checkout_details' shipping* snapshot below: whoever renders it later can't
-- resolve someone else's address book by id.
alter table public.products add column if not exists return_receiver_name text;
alter table public.products add column if not exists return_phone_number text;
alter table public.products add column if not exists return_full_address text;

alter table public.products enable row level security;

drop policy if exists "products are publicly readable" on public.products;
create policy "products are publicly readable"
    on public.products for select
    using (true);

drop policy if exists "sellers can insert their own products" on public.products;
create policy "sellers can insert their own products"
    on public.products for insert
    with check (auth.uid() = seller_id);

drop policy if exists "sellers can update their own products" on public.products;
create policy "sellers can update their own products"
    on public.products for update
    using (auth.uid() = seller_id)
    with check (auth.uid() = seller_id);

drop policy if exists "sellers can delete their own products" on public.products;
create policy "sellers can delete their own products"
    on public.products for delete
    using (auth.uid() = seller_id);

-- ============================================================================
-- 3. orders
-- One row per transaction (not the app's old mirrored-row mock pattern) —
-- both buyer_id and seller_id live on the same row; "is this mine as buyer
-- or as seller" is derived client-side from the logged-in user's id.
-- ============================================================================
create table if not exists public.orders (
    id uuid primary key default gen_random_uuid(),
    buyer_id uuid not null references auth.users(id) on delete cascade,
    seller_id uuid not null references auth.users(id) on delete cascade,
    product_id uuid references public.products(id) on delete set null,
    product_title text not null default '',
    product_image text not null default '',
    order_type text not null check (order_type in ('BUY', 'RENT')),
    total_amount numeric not null,
    deposit_amount numeric,
    rental_daily_rate numeric,
    rental_days integer,
    rental_start_date date,
    rental_end_date date,
    status text not null default 'COMPLETED',
    -- Stripe PaymentIntent id. Unique so a duplicate "Pay" tap (or a retry after order-
    -- creation failed post-payment) can never create two orders for the same payment.
    payment_id text unique,
    payment_status text not null default 'PENDING',
    -- Everything else checkout added (fees, receiving/returning method, meetup/shipping
    -- address references, deposit status) — one jsonb column, same precedent as
    -- products.specs, instead of a dozen new flat columns.
    checkout_details jsonb not null default '{}'::jsonb,
    created_at timestamptz not null default now()
);

-- Migrates an already-deployed orders table (payment/checkout support added after
-- the table already existed in some projects).
alter table public.orders add column if not exists payment_id text unique;
alter table public.orders add column if not exists payment_status text not null default 'PENDING';
alter table public.orders add column if not exists checkout_details jsonb not null default '{}'::jsonb;

-- Per-viewer "delete from my history" — orders is a single shared row per transaction (not
-- mirrored per side), so a real DELETE would erase it from the other party's history too.
-- These flags let each side hide their own copy without touching the other's.
alter table public.orders add column if not exists hidden_by_buyer boolean not null default false;
alter table public.orders add column if not exists hidden_by_seller boolean not null default false;

-- Set the instant an order enters TO_REVIEW (by release_order_payout() below) — read by the
-- pg_cron sweep to auto-complete an order whose 30-day review window has lapsed.
alter table public.orders add column if not exists to_review_at timestamptz;

-- A seller must never end up as their own buyer. The client already hides Buy Now/Rent for a
-- listing's own owner (Product Detail's isOwner check, and the chat Special Price offer card),
-- but that's UI-only — this is the actual backstop, enforced at the database level regardless of
-- which insert path (today's direct client insert, or any future RPC/Edge Function) creates the
-- row, so a self-purchase order can never exist no matter how it was attempted.
alter table public.orders drop constraint if exists orders_buyer_not_seller;
alter table public.orders add constraint orders_buyer_not_seller check (buyer_id <> seller_id);

alter table public.orders enable row level security;

drop policy if exists "buyers and sellers can read their own orders" on public.orders;
create policy "buyers and sellers can read their own orders"
    on public.orders for select
    using (auth.uid() = buyer_id or auth.uid() = seller_id);

drop policy if exists "buyers can create orders for themselves" on public.orders;
create policy "buyers can create orders for themselves"
    on public.orders for insert
    with check (auth.uid() = buyer_id);

-- Explicit re-grant: an earlier revision of this schema revoked INSERT on this
-- table for a server-authoritative checkout flow that has since been reverted.
-- Re-running this file must restore direct client inserts regardless of
-- whether that revision was ever applied to a given database.
grant insert on public.orders to authenticated;

-- No generic UPDATE policy on orders — every status change goes through this
-- validated RPC instead, so a client can never scribble over price/payment
-- fields directly or skip a lifecycle step (e.g. jump straight to COMPLETED
-- without a handover). SECURITY DEFINER lets it write despite there being no
-- UPDATE policy; the role/current-status checks below are what actually gate it.
create or replace function public.advance_order_status(p_order_id uuid, p_new_status text)
returns public.orders
language plpgsql
security definer
set search_path = public
as $$
declare
    v_order public.orders%rowtype;
    v_is_buyer boolean;
    v_is_seller boolean;
    v_allowed boolean := false;
begin
    select * into v_order from public.orders where id = p_order_id;
    if not found then
        raise exception 'Order not found';
    end if;

    v_is_buyer := auth.uid() = v_order.buyer_id;
    v_is_seller := auth.uid() = v_order.seller_id;
    if not (v_is_buyer or v_is_seller) then
        raise exception 'Not authorized for this order';
    end if;

    -- Granular ship/receive/review/return-refund lifecycle. `mark_order_shipped()` below owns
    -- every transition that's a physical handover (and may carry courier data); this function
    -- owns the rest (confirm-receipt, cancel, and the return/refund resolution steps that don't
    -- need extra data). PROCESSING/READY_FOR_HANDOVER/RETURNED are no longer reachable from here
    -- (kept only so old rows still decode) — SHIPPED/RENTAL_SHIPPED now cover what those used to.
    if v_order.order_type = 'BUY' then
        v_allowed := case
            when v_order.status = 'SHIPPED' and p_new_status = 'TO_REVIEW' and v_is_buyer then true
            when v_order.status = 'RETURN_AWAITING_RECEIPT' and p_new_status = 'REFUNDED' and v_is_seller then true
            when v_order.status in ('PAID', 'SHIPPED') and p_new_status = 'CANCELLED' then true
            else false
        end;
    else
        v_allowed := case
            when v_order.status = 'RENTAL_SHIPPED' and p_new_status = 'RENTING' and v_is_buyer then true
            when v_order.status = 'RETURN_PENDING' and p_new_status = 'TO_REVIEW' and v_is_seller then true
            when v_order.status in ('PAID', 'RENTAL_SHIPPED') and p_new_status = 'CANCELLED' then true
            else false
        end;
    end if;

    if not v_allowed then
        raise exception 'Invalid status transition: % -> % for this role', v_order.status, p_new_status;
    end if;

    update public.orders set
        status = p_new_status,
        to_review_at = case when p_new_status = 'TO_REVIEW' then now() else to_review_at end
        where id = p_order_id
        returning * into v_order;

    return v_order;
end;
$$;

grant execute on function public.advance_order_status(uuid, text) to authenticated;

-- Owns every transition that's a physical handover (PAID -> SHIPPED/RENTAL_SHIPPED, RENT's
-- RENTING -> RETURN_PENDING return leg, BUY's RETURN_AWAITING_SHIP -> RETURN_AWAITING_RECEIPT
-- return/refund leg) — split from advance_order_status() because these may carry courier data.
-- p_courier/p_tracking_number are ignored for a MEETUP leg and required for a SHIPPING leg.
create or replace function public.mark_order_shipped(p_order_id uuid, p_courier text, p_tracking_number text)
returns public.orders
language plpgsql
security definer
set search_path = public
as $$
declare
    v_order public.orders%rowtype;
    v_is_buyer boolean;
    v_is_seller boolean;
    v_new_status text;
    v_leg_key text; -- 'outbound' or 'return' — which pair of checkout_details keys to write
    v_method text;
begin
    select * into v_order from public.orders where id = p_order_id;
    if not found then
        raise exception 'Order not found';
    end if;

    v_is_buyer := auth.uid() = v_order.buyer_id;
    v_is_seller := auth.uid() = v_order.seller_id;
    if not (v_is_buyer or v_is_seller) then
        raise exception 'Not authorized for this order';
    end if;

    if v_order.order_type = 'BUY' and v_order.status = 'PAID' and v_is_seller then
        v_new_status := 'SHIPPED';
        v_leg_key := 'outbound';
        v_method := v_order.checkout_details->>'receivingMethod';
    elsif v_order.order_type = 'RENT' and v_order.status = 'PAID' and v_is_seller then
        v_new_status := 'RENTAL_SHIPPED';
        v_leg_key := 'outbound';
        v_method := v_order.checkout_details->>'receivingMethod';
    elsif v_order.order_type = 'RENT' and v_order.status = 'RENTING' and v_is_buyer then
        v_new_status := 'RETURN_PENDING';
        v_leg_key := 'return';
        v_method := v_order.checkout_details->>'returningMethod';
    elsif v_order.order_type = 'BUY' and v_order.status = 'RETURN_AWAITING_SHIP' and v_is_buyer then
        v_new_status := 'RETURN_AWAITING_RECEIPT';
        v_leg_key := 'return';
        v_method := v_order.checkout_details->>'returningMethod';
    else
        raise exception 'Invalid status transition: % (%) for this role', v_order.status, v_order.order_type;
    end if;

    if v_method = 'SHIPPING' and (p_courier is null or trim(p_courier) = '' or p_tracking_number is null or trim(p_tracking_number) = '') then
        raise exception 'Courier and tracking number are required for a shipping order';
    end if;

    update public.orders set
        status = v_new_status,
        checkout_details = case
            when v_method != 'SHIPPING' then checkout_details
            when v_leg_key = 'outbound' then checkout_details
                || jsonb_build_object('outboundCourier', p_courier, 'outboundTrackingNumber', p_tracking_number)
            else checkout_details
                || jsonb_build_object('returnCourier', p_courier, 'returnTrackingNumber', p_tracking_number)
        end
        where id = p_order_id
        returning * into v_order;

    return v_order;
end;
$$;

grant execute on function public.mark_order_shipped(uuid, text, text) to authenticated;

-- Lets a prospective renter see which date ranges are already booked on a
-- product so the checkout calendar can lock them out, without exposing the
-- other bookings' buyer identity or price (the orders SELECT policy above
-- only lets a party to an order see it, so this bypasses RLS deliberately
-- via SECURITY DEFINER while returning only non-identifying columns).
create or replace function public.get_booked_rental_ranges(p_product_id uuid)
returns table(rental_start_date date, rental_end_date date, checkout_details jsonb)
language sql
security definer
set search_path = public
as $$
    -- TO_REVIEW/COMPLETED are excluded alongside CANCELLED/RETURNED (the latter now vestigial) —
    -- once a rental reaches TO_REVIEW the owner has already confirmed the item's return
    -- (RETURN_PENDING -> TO_REVIEW), so the dates must free up immediately rather than waiting
    -- out the review window.
    select rental_start_date, rental_end_date, checkout_details
    from public.orders
    where product_id = p_product_id and order_type = 'RENT'
      and status not in ('CANCELLED', 'RETURNED', 'TO_REVIEW', 'COMPLETED')
      and rental_start_date is not null;
$$;

grant execute on function public.get_booked_rental_ranges(uuid) to authenticated;

-- Marks a product SOLD right after its buyer's own BUY order is created — there's no generic
-- UPDATE policy letting a buyer touch `products` (only the seller can), so this bypasses RLS
-- deliberately via SECURITY DEFINER. product_id is derived from the validated order row, never
-- a caller-supplied argument, so a client can't mark an arbitrary listing sold; only the actual
-- buyer of an actual BUY order, for that order's own product. Idempotent via the status guard.
create or replace function public.mark_product_sold(p_order_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    v_order public.orders%rowtype;
begin
    select * into v_order from public.orders where id = p_order_id;
    if not found then
        raise exception 'Order not found';
    end if;
    if auth.uid() != v_order.buyer_id then
        raise exception 'Not authorized for this order';
    end if;
    if v_order.order_type != 'BUY' then
        raise exception 'Not a BUY order';
    end if;
    update public.products set status = 'SOLD'
        where id = v_order.product_id and status = 'AVAILABLE';
end;
$$;

grant execute on function public.mark_product_sold(uuid) to authenticated;

-- "Delete" an order from My Activities without a raw UPDATE policy (same no-generic-UPDATE
-- convention as advance_order_status) — flips only the caller's own hidden flag, never the
-- other party's, and rejects anyone who isn't actually buyer or seller on the order.
create or replace function public.hide_order_for_current_user(p_order_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
    v_order public.orders%rowtype;
begin
    select * into v_order from public.orders where id = p_order_id;
    if not found then
        raise exception 'Order not found';
    end if;

    if auth.uid() = v_order.buyer_id then
        update public.orders set hidden_by_buyer = true where id = p_order_id;
    elsif auth.uid() = v_order.seller_id then
        update public.orders set hidden_by_seller = true where id = p_order_id;
    else
        raise exception 'Not authorized for this order';
    end if;
end;
$$;

grant execute on function public.hide_order_for_current_user(uuid) to authenticated;

-- ============================================================================
-- 3b. (reverted) — this project briefly used a server-authoritative checkout
-- flow (checkout_sessions/stripe_webhook_events tables, create_checkout_session/
-- finalize_checkout_session_service/attach_checkout_payment/fail_checkout_session_service
-- functions) that has since been reverted back to the original
-- create-payment-intent/get-payment-status Edge Functions. The statements
-- below undo that flow on any database it was applied to; re-running this
-- file is safe whether or not it was ever applied.
-- ============================================================================
drop function if exists public.create_checkout_session(uuid, text, date, date, jsonb, text);
drop function if exists public.attach_checkout_payment(uuid, text);
drop function if exists public.finalize_checkout_session_service(uuid, text, text);
drop function if exists public.fail_checkout_session_service(uuid, text);
drop table if exists public.checkout_sessions;
drop table if exists public.stripe_webhook_events;
do $$
begin
    if exists (select 1 from pg_constraint where conname = 'orders_no_active_rental_overlap') then
        alter table public.orders drop constraint orders_no_active_rental_overlap;
    end if;
end;
$$;

-- ============================================================================
-- 4. messages
-- No separate "threads" table — a conversation thread is just every row
-- between the same two users, grouped client-side (see ChatRepository.threadKey)
-- rather than persisted as its own row. One thread per user pair, regardless
-- of which product(s) they've discussed.
-- ============================================================================
create table if not exists public.messages (
    id uuid primary key default gen_random_uuid(),
    sender_id uuid not null references auth.users(id) on delete cascade,
    receiver_id uuid not null references auth.users(id) on delete cascade,
    product_id uuid references public.products(id) on delete set null,
    content text not null,
    is_read boolean not null default false,
    created_at timestamptz not null default now()
);

-- Migrates an already-deployed messages table to add the read-tracking column.
alter table public.messages add column if not exists is_read boolean not null default false;

-- Migrates an already-deployed messages table to support attachment types (chat photo, shared
-- current location, shared product listing, negotiated "special price" offer) alongside plain
-- text. `metadata` is one flat jsonb payload shape covering every type (see MessageMetadata in
-- the Kotlin client) — only the fields relevant to a message's own type are populated, and the
-- jsonb keys are the literal camelCase Kotlin field names (no @SerialName overrides).
alter table public.messages add column if not exists message_type text not null default 'TEXT'
    check (message_type in ('TEXT','IMAGE','LOCATION','PRODUCT','OFFER'));
alter table public.messages add column if not exists metadata jsonb;

alter table public.messages enable row level security;

drop policy if exists "participants can read their own messages" on public.messages;
create policy "participants can read their own messages"
    on public.messages for select
    using (auth.uid() = sender_id or auth.uid() = receiver_id);

drop policy if exists "senders can send messages as themselves" on public.messages;
create policy "senders can send messages as themselves"
    on public.messages for insert
    with check (auth.uid() = sender_id);

-- Needed so a participant can mark the other side's messages as read
-- (updates `is_read`) when they open the thread.
drop policy if exists "participants can update their own messages" on public.messages;
create policy "participants can update their own messages"
    on public.messages for update
    using (auth.uid() = sender_id or auth.uid() = receiver_id)
    with check (auth.uid() = sender_id or auth.uid() = receiver_id);

-- ============================================================================
-- 4b. chat_hidden_threads
-- "Delete conversation" is per-user, not a real delete of shared message rows
-- — deleting hides everything up to `hidden_before` from just the deleting
-- user's own thread list (ChatRepository.refreshFromRemote filters them out);
-- the counterparty's copy of the conversation is untouched, and the thread
-- reappears for the deleting user if the counterparty sends a new message
-- afterward. One row per (user, counterparty) pair, re-upserted on repeat deletes.
-- ============================================================================
create table if not exists public.chat_hidden_threads (
    user_id uuid not null references auth.users(id) on delete cascade,
    counterparty_id uuid not null references auth.users(id) on delete cascade,
    hidden_before timestamptz not null default now(),
    primary key (user_id, counterparty_id)
);
alter table public.chat_hidden_threads enable row level security;
drop policy if exists "users manage their own hidden threads" on public.chat_hidden_threads;
create policy "users manage their own hidden threads"
    on public.chat_hidden_threads for all
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

-- ============================================================================
-- 5. addresses
-- One row per saved shipping/receiving address. `latitude`/`longitude` are
-- filled in when the address was picked via the Google Maps picker; both
-- stay null for an address only ever typed as plain text.
-- ============================================================================
create table if not exists public.addresses (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    label text not null default '',
    receiver_name text not null default '',
    phone_number text not null default '',
    full_address text not null default '',
    latitude double precision,
    longitude double precision,
    is_default boolean not null default false,
    created_at timestamptz not null default now()
);

alter table public.addresses enable row level security;

drop policy if exists "users can read their own addresses" on public.addresses;
create policy "users can read their own addresses"
    on public.addresses for select
    using (auth.uid() = user_id);

drop policy if exists "users can insert their own addresses" on public.addresses;
create policy "users can insert their own addresses"
    on public.addresses for insert
    with check (auth.uid() = user_id);

drop policy if exists "users can update their own addresses" on public.addresses;
create policy "users can update their own addresses"
    on public.addresses for update
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

drop policy if exists "users can delete their own addresses" on public.addresses;
create policy "users can delete their own addresses"
    on public.addresses for delete
    using (auth.uid() = user_id);

create index if not exists addresses_user_id_idx on public.addresses(user_id);

-- ============================================================================
-- 6. wallet_transactions
-- One row per deposit/withdrawal/payout/refund. `profiles.wallet_balance` is
-- the running total; this table is its per-user, RLS-scoped audit trail —
-- previously the app kept both purely in mock client state shared by every
-- account, so every user saw the same balance and history.
-- ============================================================================
create table if not exists public.wallet_transactions (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    type text not null check (type in ('DEPOSIT', 'WITHDRAWAL', 'SALE_PAYOUT', 'RENTAL_PAYOUT', 'REFUND')),
    amount numeric not null,
    description text not null default '',
    created_at timestamptz not null default now()
);

-- Widens the type check to include PURCHASE (paying for an order out of wallet balance,
-- distinct from WITHDRAWAL which leaves the app) on a database where the table already existed
-- with the old constraint.
alter table public.wallet_transactions drop constraint if exists wallet_transactions_type_check;
alter table public.wallet_transactions add constraint wallet_transactions_type_check
    check (type in ('DEPOSIT', 'WITHDRAWAL', 'PURCHASE', 'SALE_PAYOUT', 'RENTAL_PAYOUT', 'REFUND'));

-- Idempotency key for a wallet top-up: lets credit_wallet() below safely no-op a duplicate
-- confirm call for the same Stripe PaymentIntent instead of double-crediting. Null (and
-- non-unique) for every non-DEPOSIT row, which Postgres unique constraints already allow.
alter table public.wallet_transactions add column if not exists stripe_payment_intent_id text;
do $$
begin
    if not exists (
        select 1 from pg_constraint where conname = 'wallet_transactions_stripe_payment_intent_id_key'
    ) then
        alter table public.wallet_transactions
            add constraint wallet_transactions_stripe_payment_intent_id_key unique (stripe_payment_intent_id);
    end if;
end;
$$;

alter table public.wallet_transactions enable row level security;

drop policy if exists "users can read their own wallet transactions" on public.wallet_transactions;
create policy "users can read their own wallet transactions"
    on public.wallet_transactions for select
    using (auth.uid() = user_id);

-- No client INSERT policy: every row is written by credit_wallet()/debit_wallet() below
-- (security definer), never by a raw client insert — a client-writable row with an arbitrary
-- amount/type is exactly how a balance could be fabricated (see those functions' comments).
drop policy if exists "users can insert their own wallet transactions" on public.wallet_transactions;
revoke insert on public.wallet_transactions from authenticated;

-- Only service_role (the wallet-topup-confirm Edge Function, after verifying the charge with
-- Stripe directly) may ever *increase* a wallet balance — this is the actual fix for the wallet
-- being freely top-up-able by any client that could otherwise just call
-- `update profiles set wallet_balance = ...` or insert a fake DEPOSIT row itself.
create or replace function public.credit_wallet(
    p_user_id uuid,
    p_amount numeric,
    p_description text,
    p_stripe_payment_intent_id text
)
returns numeric
language plpgsql
security definer
set search_path = public
as $$
declare
    v_new_balance numeric;
begin
    -- auth.role() (Supabase's own built-in helper) rather than a hand-rolled
    -- current_setting('request.jwt.claim.role', ...) lookup, whose exact GUC shape isn't
    -- guaranteed across PostgREST versions/configs and was never actually verified against this
    -- project.
    if auth.role() is distinct from 'service_role' then
        raise exception 'Not authorized';
    end if;
    if p_amount <= 0 then
        raise exception 'Amount must be positive';
    end if;

    begin
        insert into public.wallet_transactions (user_id, type, amount, description, stripe_payment_intent_id)
        values (p_user_id, 'DEPOSIT', p_amount, p_description, p_stripe_payment_intent_id);
    exception
        when unique_violation then
            -- Already credited for this exact PaymentIntent (e.g. confirm called twice) —
            -- idempotent no-op rather than crediting twice.
            select wallet_balance into v_new_balance from public.profiles where id = p_user_id;
            return v_new_balance;
    end;

    update public.profiles set wallet_balance = wallet_balance + p_amount where id = p_user_id
        returning wallet_balance into v_new_balance;
    return v_new_balance;
end;
$$;

grant execute on function public.credit_wallet(uuid, numeric, text, text) to service_role;

-- Any authenticated user may debit their OWN wallet (a purchase paid from wallet balance, or a
-- withdrawal) without going through a server function first — unlike credit_wallet, this can
-- only ever move a user's balance toward zero and never below it, so there's no way to fabricate
-- funds through it even though it isn't service_role-gated.
create or replace function public.debit_wallet(
    p_type text,
    p_amount numeric,
    p_description text
)
returns numeric
language plpgsql
security definer
set search_path = public
as $$
declare
    v_uid uuid := auth.uid();
    v_balance numeric;
    v_new_balance numeric;
begin
    if v_uid is null then
        raise exception 'Not authorized';
    end if;
    if p_type not in ('WITHDRAWAL', 'PURCHASE') then
        raise exception 'Invalid debit type';
    end if;
    if p_amount <= 0 then
        raise exception 'Amount must be positive';
    end if;

    select wallet_balance into v_balance from public.profiles where id = v_uid;
    if v_balance is null or v_balance < p_amount then
        raise exception 'Insufficient balance';
    end if;

    insert into public.wallet_transactions (user_id, type, amount, description)
    values (v_uid, p_type, -p_amount, p_description);

    update public.profiles set wallet_balance = wallet_balance - p_amount where id = v_uid
        returning wallet_balance into v_new_balance;
    return v_new_balance;
end;
$$;

grant execute on function public.debit_wallet(text, numeric, text) to authenticated;

-- ============================================================================
-- 7. notifications
-- Rows are never inserted by the app directly — they're created server-side
-- by triggers on `messages`/`orders` inserts below, the same server-owned
-- pattern as handle_new_user() creating `profiles` rows. The app only reads
-- its own rows and flips `is_read`.
-- ============================================================================
create table if not exists public.notifications (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references auth.users(id) on delete cascade,
    type text not null,
    title text not null,
    message text not null default '',
    related_product_id uuid references public.products(id) on delete set null,
    related_sender_id uuid references auth.users(id) on delete set null,
    is_read boolean not null default false,
    created_at timestamptz not null default now()
);

alter table public.notifications enable row level security;

-- Lets the client subscribe to new rows via Supabase Realtime (see the Android app's
-- NotificationRepository.startRealtimeListening) so a system tray notification can be posted the
-- moment a trigger below inserts one — RLS above still scopes each subscriber to their own rows.
do $$
begin
    if not exists (
        select 1 from pg_publication_tables
        where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'notifications'
    ) then
        alter publication supabase_realtime add table public.notifications;
    end if;
end;
$$;

drop policy if exists "users can read their own notifications" on public.notifications;
create policy "users can read their own notifications"
    on public.notifications for select
    using (auth.uid() = user_id);

drop policy if exists "users can update their own notifications" on public.notifications;
create policy "users can update their own notifications"
    on public.notifications for update
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

-- New message -> notify the receiver. Non-TEXT messages have empty `content`, so the preview
-- is derived from `message_type`/`metadata` instead — mirrors ChatRepository.previewFor's preview text.
create or replace function public.notify_on_new_message()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    v_preview text;
begin
    v_preview := case new.message_type
        when 'IMAGE' then '📷 Photo'
        when 'LOCATION' then '📍 Location'
        when 'PRODUCT' then '🏷️ ' || coalesce(new.metadata->>'productTitle', 'Product')
        when 'OFFER' then '💰 Special price: ' || coalesce(new.metadata->>'productTitle', '')
        else left(new.content, 140)
    end;

    insert into public.notifications (user_id, type, title, message, related_product_id, related_sender_id)
    values (
        new.receiver_id,
        'NEW_MESSAGE',
        'New message',
        v_preview,
        new.product_id,
        new.sender_id
    );
    return new;
end;
$$;

drop trigger if exists on_message_created on public.messages;
create trigger on_message_created
    after insert on public.messages
    for each row execute function public.notify_on_new_message();

-- New order -> notify the seller.
create or replace function public.notify_on_new_order()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    insert into public.notifications (user_id, type, title, message, related_product_id, related_sender_id)
    values (
        new.seller_id,
        case when new.order_type = 'RENT' then 'RENTAL_REQUEST' else 'PAYMENT' end,
        case when new.order_type = 'RENT' then 'New rental request' else 'Payment confirmed' end,
        new.product_title || ' — RM' || new.total_amount::text,
        new.product_id,
        new.buyer_id
    );
    return new;
end;
$$;

drop trigger if exists on_order_created on public.orders;
create trigger on_order_created
    after insert on public.orders
    for each row execute function public.notify_on_new_order();

-- Order status changed (via advance_order_status) -> notify whichever party
-- didn't make the change.
create or replace function public.notify_on_order_status_change()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    v_recipient uuid;
begin
    if new.status = old.status then
        return new;
    end if;
    v_recipient := case when auth.uid() = new.seller_id then new.buyer_id else new.seller_id end;
    insert into public.notifications (user_id, type, title, message, related_product_id, related_sender_id)
    values (
        v_recipient,
        'ORDER_UPDATE',
        'Order status updated',
        new.product_title || ' is now "' || new.status || '"',
        new.product_id,
        auth.uid()
    );
    return new;
end;
$$;

drop trigger if exists on_order_status_changed on public.orders;
create trigger on_order_status_changed
    after update of status on public.orders
    for each row execute function public.notify_on_order_status_change();

-- If a BUY order is cancelled after mark_product_sold already ran, the listing must reopen —
-- otherwise a cancelled purchase would permanently lock a product that was never actually sold.
create or replace function public.reopen_product_on_buy_order_cancelled()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    if new.order_type = 'BUY' and new.status = 'CANCELLED' and old.status != 'CANCELLED' then
        update public.products set status = 'AVAILABLE'
            where id = new.product_id and status = 'SOLD';
    end if;
    return new;
end;
$$;

drop trigger if exists on_buy_order_cancelled on public.orders;
create trigger on_buy_order_cancelled
    after update of status on public.orders
    for each row execute function public.reopen_product_on_buy_order_cancelled();

-- A successfully returned BUY order (buyer shipped/handed the item back, seller confirmed
-- receipt) must reopen the listing too — otherwise a returned item stays permanently marked SOLD.
-- Checking old.status specifically (rather than just new.status = 'REFUNDED') matters: a "refund
-- only" resolution goes RETURN_REQUESTED -> REFUNDED directly with no physical return leg, and
-- the buyer keeps the item in that case, so that path must NOT reopen the listing.
create or replace function public.reopen_product_on_return_completed()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    if new.order_type = 'BUY' and old.status = 'RETURN_AWAITING_RECEIPT' and new.status = 'REFUNDED' then
        update public.products set status = 'AVAILABLE'
            where id = new.product_id and status = 'SOLD';
    end if;
    return new;
end;
$$;

drop trigger if exists on_return_order_refunded on public.orders;
create trigger on_return_order_refunded
    after update of status on public.orders
    for each row execute function public.reopen_product_on_return_completed();

-- Releases the seller's/owner's payout the moment an order enters TO_REVIEW (BUY: buyer just
-- confirmed receipt; RENT: owner just confirmed the *second* receipt — the item coming back —
-- never the first), releases a refund to the buyer when a return/refund dispute's physical-return
-- leg completes (RETURN_AWAITING_RECEIPT -> REFUNDED, seller just confirmed the returned item
-- arrived), and separately refunds a buyer/renter whose order gets cancelled. All three are side
-- effects of an already-authorized status change (advance_order_status), so no separate
-- client-callable RPC is needed — a client can only ever trigger this by making a transition it
-- was already allowed to make. Every refund here excludes the shipping fee — it's a real courier
-- cost already spent, unlike the platform fee, which the buyer still gets back.
create or replace function public.release_order_payout()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
    v_payout numeric;
    v_shipping_fee numeric;
    v_refund numeric;
begin
    if new.status = 'TO_REVIEW' and old.status in ('SHIPPED', 'RETURN_PENDING') then
        v_payout := new.total_amount - coalesce(new.deposit_amount, 0)
            - coalesce((new.checkout_details->>'platformFee')::numeric, 0);
        insert into public.wallet_transactions (user_id, type, amount, description)
            values (new.seller_id, case when new.order_type = 'BUY' then 'SALE_PAYOUT' else 'RENTAL_PAYOUT' end,
                     v_payout, new.product_title);
        update public.profiles set wallet_balance = wallet_balance + v_payout where id = new.seller_id;
    end if;

    if new.status = 'REFUNDED' and old.status = 'RETURN_AWAITING_RECEIPT' then
        v_shipping_fee := coalesce((new.checkout_details->>'shippingFee')::numeric, 0);
        v_refund := new.total_amount - v_shipping_fee;
        insert into public.wallet_transactions (user_id, type, amount, description)
            values (new.buyer_id, 'REFUND', v_refund, new.product_title);
        update public.profiles set wallet_balance = wallet_balance + v_refund where id = new.buyer_id;
    end if;

    if new.status = 'CANCELLED' and old.status in ('PAID', 'SHIPPED', 'RENTAL_SHIPPED') then
        v_shipping_fee := coalesce((new.checkout_details->>'shippingFee')::numeric, 0);
        v_refund := new.total_amount - v_shipping_fee;
        insert into public.wallet_transactions (user_id, type, amount, description)
            values (new.buyer_id, 'REFUND', v_refund, new.product_title);
        update public.profiles set wallet_balance = wallet_balance + v_refund where id = new.buyer_id;
    end if;

    return new;
end;
$$;

drop trigger if exists on_order_payout_released on public.orders;
create trigger on_order_payout_released
    after update of status on public.orders
    for each row execute function public.release_order_payout();

-- ============================================================================
-- 9. saved_items
-- A buyer's wishlist/favorites — one row per (user, product) they've saved.
-- ============================================================================
create table if not exists public.saved_items (
    user_id uuid not null references auth.users(id) on delete cascade,
    product_id uuid not null references public.products(id) on delete cascade,
    created_at timestamptz not null default now(),
    primary key (user_id, product_id)
);
alter table public.saved_items enable row level security;
drop policy if exists "users manage their own saved items" on public.saved_items;
create policy "users manage their own saved items"
    on public.saved_items for all
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

-- ============================================================================
-- 10. browse_history
-- One row per (user, product) they've viewed — viewed_at is bumped (not
-- duplicated) on repeat views, so it doubles as "most recently viewed" order.
-- ============================================================================
create table if not exists public.browse_history (
    user_id uuid not null references auth.users(id) on delete cascade,
    product_id uuid not null references public.products(id) on delete cascade,
    viewed_at timestamptz not null default now(),
    primary key (user_id, product_id)
);
alter table public.browse_history enable row level security;
drop policy if exists "users manage their own browse history" on public.browse_history;
create policy "users manage their own browse history"
    on public.browse_history for all
    using (auth.uid() = user_id)
    with check (auth.uid() = user_id);

-- ============================================================================
-- 11. reviews
-- One review per completed order, left by the buyer about the seller. No
-- generic INSERT policy — submit_review below validates the order actually
-- belongs to the caller as buyer and is COMPLETED before inserting, and the
-- trigger after it keeps profiles.rating/rating_count in sync automatically.
-- ============================================================================
create table if not exists public.reviews (
    id uuid primary key default gen_random_uuid(),
    order_id uuid not null unique references public.orders(id) on delete cascade,
    product_id uuid references public.products(id) on delete set null,
    reviewer_id uuid not null references auth.users(id) on delete cascade,
    seller_id uuid not null references auth.users(id) on delete cascade,
    rating smallint not null check (rating between 1 and 5),
    comment text not null default '',
    created_at timestamptz not null default now()
);
alter table public.reviews enable row level security;
drop policy if exists "reviews are publicly readable" on public.reviews;
create policy "reviews are publicly readable"
    on public.reviews for select
    using (true);

create or replace function public.submit_review(p_order_id uuid, p_rating smallint, p_comment text)
returns public.reviews
language plpgsql
security definer
set search_path = public
as $$
declare
    v_order public.orders%rowtype;
    v_review public.reviews%rowtype;
begin
    select * into v_order from public.orders where id = p_order_id;
    if not found then
        raise exception 'Order not found';
    end if;
    if auth.uid() != v_order.buyer_id then
        raise exception 'Only the buyer may review this order';
    end if;
    if v_order.status != 'TO_REVIEW' then
        raise exception 'Order is not awaiting review';
    end if;
    if exists (select 1 from public.reviews where order_id = p_order_id) then
        raise exception 'This order has already been reviewed';
    end if;

    insert into public.reviews (order_id, product_id, reviewer_id, seller_id, rating, comment)
    values (p_order_id, v_order.product_id, auth.uid(), v_order.seller_id, p_rating, p_comment)
    returning * into v_review;

    -- Leaving a review immediately completes the order rather than waiting out the rest of the
    -- 30-day window — the auto_complete_reviewed_orders cron job below only needs to catch the
    -- orders where the buyer never reviewed at all.
    update public.orders set status = 'COMPLETED' where id = p_order_id;

    return v_review;
end;
$$;

grant execute on function public.submit_review(uuid, smallint, text) to authenticated;

create or replace function public.recompute_seller_rating()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
    update public.profiles set
        rating = (select coalesce(avg(rating), 0) from public.reviews where seller_id = NEW.seller_id),
        rating_count = (select count(*) from public.reviews where seller_id = NEW.seller_id)
    where id = NEW.seller_id;
    return NEW;
end;
$$;

drop trigger if exists on_review_inserted on public.reviews;
create trigger on_review_inserted
    after insert on public.reviews
    for each row execute function public.recompute_seller_rating();

-- An order that's sat in TO_REVIEW for over 30 days without a review auto-completes. Real
-- pg_cron (not a lazy on-read check) so it fires even if nobody opens the app. If `create
-- extension` errors on hosted Supabase, enable pg_cron once via Dashboard -> Database ->
-- Extensions, then re-run this file.
create extension if not exists pg_cron with schema extensions;

select cron.unschedule(jobid) from cron.job where jobname = 'auto_complete_reviewed_orders';

select cron.schedule(
    'auto_complete_reviewed_orders',
    '0 3 * * *',
    $sql$
        update public.orders set status = 'COMPLETED'
        where status = 'TO_REVIEW' and to_review_at < now() - interval '30 days';
    $sql$
);

-- ============================================================================
-- 12. return_requests
-- The buyer-initiated return/refund dispute workflow — BUY orders only. Up to 2 rows per order
-- (attempt_number 1 and 2); a 3rd is rejected server-side by submit_return_request. No generic
-- INSERT/UPDATE policy — both RPCs below are SECURITY DEFINER and also drive the parent order's
-- own status, same no-generic-UPDATE convention as `orders` itself.
-- ============================================================================
create table if not exists public.return_requests (
    id uuid primary key default gen_random_uuid(),
    order_id uuid not null references public.orders(id) on delete cascade,
    requester_id uuid not null references auth.users(id) on delete cascade,
    attempt_number integer not null default 1,
    request_type text not null check (request_type in ('RETURN', 'REFUND')),
    reason_code text not null,
    reason_other_text text not null default '',
    refund_amount numeric,
    return_method text,
    description text not null default '',
    photo_urls text[] not null default '{}',
    status text not null default 'PENDING' check (status in ('PENDING', 'ACCEPTED', 'REJECTED')),
    rejection_reason text,
    created_at timestamptz not null default now(),
    decided_at timestamptz
);

-- The buyer's suggested meet-up spot, shown to the seller as context on the decide screen — not
-- authoritative; decide_return_request() below lets the seller pick the actual final location.
alter table public.return_requests add column if not exists meetup_location jsonb;

alter table public.return_requests enable row level security;

drop policy if exists "order parties can read return requests" on public.return_requests;
create policy "order parties can read return requests"
    on public.return_requests for select
    using (exists (
        select 1 from public.orders o where o.id = order_id
          and (auth.uid() = o.buyer_id or auth.uid() = o.seller_id)
    ));

drop function if exists public.submit_return_request(uuid, text, text, text, numeric, text, text, text[]);
create or replace function public.submit_return_request(
    p_order_id uuid,
    p_type text,
    p_reason_code text,
    p_reason_other text,
    p_refund_amount numeric,
    p_return_method text,
    p_description text,
    p_photo_urls text[],
    p_meetup_location jsonb
)
returns public.return_requests
language plpgsql
security definer
set search_path = public
as $$
declare
    v_order public.orders%rowtype;
    v_attempt_count integer;
    v_request public.return_requests%rowtype;
begin
    select * into v_order from public.orders where id = p_order_id;
    if not found then
        raise exception 'Order not found';
    end if;
    if auth.uid() != v_order.buyer_id then
        raise exception 'Only the buyer may request a return/refund';
    end if;
    if v_order.status != 'SHIPPED' then
        raise exception 'Return/refund can only be requested before you confirm receipt';
    end if;
    if p_type not in ('RETURN', 'REFUND') then
        raise exception 'Invalid request type';
    end if;

    select count(*) into v_attempt_count from public.return_requests where order_id = p_order_id;
    if v_attempt_count >= 2 then
        raise exception 'You have already used both return/refund attempts for this order — please contact customer support';
    end if;

    -- Shipping fee is never refundable — it's a real courier cost already spent — so the cap
    -- excludes it, unlike the platform fee which the buyer can still get back.
    if p_type = 'REFUND' and (
        p_refund_amount is null or p_refund_amount <= 0
        or p_refund_amount > (v_order.total_amount - coalesce((v_order.checkout_details->>'shippingFee')::numeric, 0))
    ) then
        raise exception 'Refund amount must be between 0 and the order total minus shipping';
    end if;

    -- No longer required to match one of the product's own declared meet-up spots — the buyer can
    -- suggest a fresh location just for this return, since it may not match where they'd meet for
    -- the original handover at all.
    if p_type = 'RETURN' and p_return_method = 'MEETUP' and (p_meetup_location is null or coalesce(p_meetup_location->>'name', '') = '') then
        raise exception 'Please pick a meet-up location';
    end if;

    insert into public.return_requests (
        order_id, requester_id, attempt_number, request_type, reason_code, reason_other_text,
        refund_amount, return_method, description, photo_urls, meetup_location
    ) values (
        p_order_id, auth.uid(), v_attempt_count + 1, p_type, p_reason_code, coalesce(p_reason_other, ''),
        case when p_type = 'REFUND' then p_refund_amount else null end,
        case when p_type = 'RETURN' then p_return_method else null end,
        coalesce(p_description, ''), coalesce(p_photo_urls, '{}'),
        case when p_type = 'RETURN' and p_return_method = 'MEETUP' then p_meetup_location else null end
    ) returning * into v_request;

    update public.orders set status = 'RETURN_REQUESTED' where id = p_order_id;

    return v_request;
end;
$$;

grant execute on function public.submit_return_request(uuid, text, text, text, numeric, text, text, text[], jsonb) to authenticated;

drop function if exists public.decide_return_request(uuid, boolean, text);
create or replace function public.decide_return_request(
    p_request_id uuid,
    p_accept boolean,
    p_rejection_reason text,
    p_final_return_method text default null,
    p_final_meetup_location jsonb default null,
    p_final_return_receiver_name text default null,
    p_final_return_phone_number text default null,
    p_final_return_full_address text default null
)
returns public.return_requests
language plpgsql
security definer
set search_path = public
as $$
declare
    v_request public.return_requests%rowtype;
    v_order public.orders%rowtype;
begin
    select * into v_request from public.return_requests where id = p_request_id;
    if not found then
        raise exception 'Return request not found';
    end if;
    select * into v_order from public.orders where id = v_request.order_id;
    if auth.uid() != v_order.seller_id then
        raise exception 'Only the seller may decide this request';
    end if;
    if v_request.status != 'PENDING' then
        raise exception 'This request has already been decided';
    end if;

    if not p_accept then
        update public.return_requests set status = 'REJECTED', rejection_reason = p_rejection_reason, decided_at = now()
            where id = p_request_id returning * into v_request;
        -- Back to the normal receive step — the buyer can still confirm receipt normally, or
        -- file a 2nd attempt (submit_return_request's own count check enforces the cap).
        update public.orders set status = 'SHIPPED' where id = v_request.order_id;
        return v_request;
    end if;

    update public.return_requests set status = 'ACCEPTED', decided_at = now()
        where id = p_request_id returning * into v_request;

    if v_request.request_type = 'REFUND' then
        insert into public.wallet_transactions (user_id, type, amount, description)
            values (v_request.requester_id, 'REFUND', v_request.refund_amount, v_order.product_title);
        update public.profiles set wallet_balance = wallet_balance + v_request.refund_amount where id = v_request.requester_id;
        update public.orders set status = 'REFUNDED' where id = v_request.order_id;
    else
        -- RETURN: the seller picks the actual return logistics here (independent of whatever the
        -- buyer suggested at submission) — mirrors how a rental's own return leg is always known
        -- up front. No payout yet — the buyer still has to ship/hand the item back, and the
        -- release_order_payout() trigger only refunds once the seller confirms it arrived
        -- (RETURN_AWAITING_RECEIPT -> REFUNDED, driven by mark_order_shipped + advance_order_status).
        if p_final_return_method not in ('MEETUP', 'SHIPPING') then
            raise exception 'Please choose how you want the item returned to you';
        end if;

        -- No longer required to match one of the product's own declared meet-up spots — the
        -- seller may pick a fresh location just for this return (see submit_return_request's
        -- own matching relaxation for the buyer's suggestion).
        if p_final_return_method = 'MEETUP' then
            if p_final_meetup_location is null or coalesce(p_final_meetup_location->>'name', '') = '' then
                raise exception 'Please pick a meet-up location';
            end if;
            update public.orders set
                status = 'RETURN_AWAITING_SHIP',
                checkout_details = checkout_details || jsonb_build_object(
                    'returningMethod', 'MEETUP',
                    'returningMeetup', p_final_meetup_location
                )
                where id = v_request.order_id;
        else
            if coalesce(trim(p_final_return_receiver_name), '') = ''
                or coalesce(trim(p_final_return_phone_number), '') = ''
                or coalesce(trim(p_final_return_full_address), '') = '' then
                raise exception 'Please pick a return address';
            end if;
            update public.orders set
                status = 'RETURN_AWAITING_SHIP',
                checkout_details = checkout_details || jsonb_build_object(
                    'returningMethod', 'SHIPPING',
                    'returnReceiverName', p_final_return_receiver_name,
                    'returnPhoneNumber', p_final_return_phone_number,
                    'returnFullAddress', p_final_return_full_address
                )
                where id = v_request.order_id;
        end if;
    end if;

    return v_request;
end;
$$;

grant execute on function public.decide_return_request(uuid, boolean, text, text, jsonb, text, text, text) to authenticated;

create index if not exists return_requests_order_id_idx on public.return_requests(order_id);

-- ============================================================================
-- 8. Storage: product-images bucket
-- Listing photos, uploaded by the seller during the listing wizard's Photos
-- step. Public bucket (product pages are publicly browsable), but only an
-- authenticated user can upload, and only into their own `{uid}/...` folder
-- — the app uploads to `{sellerId}/{productId}/{index}.jpg`.
-- ============================================================================
insert into storage.buckets (id, name, public)
values ('product-images', 'product-images', true)
on conflict (id) do nothing;

drop policy if exists "product images are publicly readable" on storage.objects;
create policy "product images are publicly readable"
    on storage.objects for select
    using (bucket_id = 'product-images');

drop policy if exists "authenticated users can upload their own product images" on storage.objects;
create policy "authenticated users can upload their own product images"
    on storage.objects for insert
    with check (bucket_id = 'product-images' and auth.uid()::text = (storage.foldername(name))[1]);

drop policy if exists "sellers can delete their own product images" on storage.objects;
create policy "sellers can delete their own product images"
    on storage.objects for delete
    using (bucket_id = 'product-images' and auth.uid()::text = (storage.foldername(name))[1]);

-- ============================================================================
-- 9. Storage: chat-images bucket
-- Photos sent as chat attachments. Same shape as product-images: public
-- bucket, upload restricted to the sender's own `{uid}/...` folder — the app
-- uploads to `{senderId}/{uuid}.jpg`.
-- ============================================================================
insert into storage.buckets (id, name, public)
values ('chat-images', 'chat-images', true)
on conflict (id) do nothing;

drop policy if exists "chat images are publicly readable" on storage.objects;
create policy "chat images are publicly readable"
    on storage.objects for select
    using (bucket_id = 'chat-images');

drop policy if exists "senders can upload their own chat images" on storage.objects;
create policy "senders can upload their own chat images"
    on storage.objects for insert
    with check (bucket_id = 'chat-images' and auth.uid()::text = (storage.foldername(name))[1]);

-- ============================================================================
-- 13. Storage: return-request-photos bucket
-- Photos a buyer attaches to a return/refund request, uploaded to
-- `{orderId}/{index}.jpg`. Not public (private disputes) — only the order's two parties can
-- read them, and only the buyer can upload (matches submit_return_request's own buyer-only gate).
-- ============================================================================
insert into storage.buckets (id, name, public)
values ('return-request-photos', 'return-request-photos', false)
on conflict (id) do nothing;

drop policy if exists "order parties can read return photos" on storage.objects;
create policy "order parties can read return photos"
    on storage.objects for select
    using (bucket_id = 'return-request-photos' and exists (
        select 1 from public.orders o where o.id::text = (storage.foldername(name))[1]
          and (auth.uid() = o.buyer_id or auth.uid() = o.seller_id)
    ));

drop policy if exists "buyers can upload their own return photos" on storage.objects;
create policy "buyers can upload their own return photos"
    on storage.objects for insert
    with check (bucket_id = 'return-request-photos' and exists (
        select 1 from public.orders o where o.id::text = (storage.foldername(name))[1] and auth.uid() = o.buyer_id
    ));

-- ============================================================================
-- Helpful indexes
-- ============================================================================
create index if not exists products_seller_id_idx on public.products(seller_id);
create index if not exists products_status_idx on public.products(status);
create index if not exists orders_buyer_id_idx on public.orders(buyer_id);
create index if not exists orders_seller_id_idx on public.orders(seller_id);
create index if not exists messages_sender_receiver_idx on public.messages(sender_id, receiver_id);
create index if not exists messages_receiver_is_read_idx on public.messages(receiver_id, is_read);
create index if not exists notifications_user_id_idx on public.notifications(user_id);
create index if not exists wallet_transactions_user_id_idx on public.wallet_transactions(user_id);
create index if not exists saved_items_user_id_idx on public.saved_items(user_id);
create index if not exists browse_history_user_id_viewed_at_idx on public.browse_history(user_id, viewed_at desc);
create index if not exists reviews_seller_id_idx on public.reviews(seller_id);
