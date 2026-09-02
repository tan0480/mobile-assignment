// Supabase Edge Function: wallet-topup-confirm
//
// Step 2 of "Add Funds": re-verifies a PaymentIntent succeeded directly against Stripe's own
// records (never trusting a bare PaymentSheetResult.Completed callback — see get-payment-status'
// same rationale), then credits the wallet server-side via the `credit_wallet` Postgres function
// using the service-role key. This is the ONLY path in the app that can ever increase a wallet
// balance — WalletRepository.debit() can only ever decrease one, and there is no client-callable
// "credit" RPC at all (see schema.sql's wallet_transactions section) — so a modified client can't
// grant itself funds no matter what it tells this app.
//
// Deploy with: supabase functions deploy wallet-topup-confirm

import Stripe from "npm:stripe@17.5.0";
import { createClient } from "npm:@supabase/supabase-js@2.45.4";

const stripe = new Stripe(Deno.env.get("STRIPE_SECRET_KEY") ?? "", {
  apiVersion: "2024-06-20",
});

Deno.serve(async (req) => {
  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(JSON.stringify({ error: "Missing Authorization header" }), { status: 401 });
    }

    const supabase = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_ANON_KEY") ?? "",
      { global: { headers: { Authorization: authHeader } } },
    );
    const { data: { user }, error: userError } = await supabase.auth.getUser();
    if (userError || !user) {
      return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 });
    }

    const { payment_intent_id } = await req.json();
    if (!payment_intent_id || typeof payment_intent_id !== "string") {
      return new Response(JSON.stringify({ error: "Missing payment_intent_id" }), { status: 400 });
    }

    const paymentIntent = await stripe.paymentIntents.retrieve(payment_intent_id);

    if (paymentIntent.metadata?.supabase_user_id !== user.id) {
      return new Response(JSON.stringify({ error: "Forbidden" }), { status: 403 });
    }
    if (paymentIntent.metadata?.purpose !== "wallet_topup") {
      return new Response(JSON.stringify({ error: "Not a wallet top-up payment" }), { status: 400 });
    }
    if (paymentIntent.status !== "succeeded") {
      return new Response(JSON.stringify({ error: `Payment not completed (status: ${paymentIntent.status})` }), { status: 400 });
    }

    // Service-role client, separate from the user-scoped one above — credit_wallet() checks the
    // caller's JWT role is exactly 'service_role', which only this key satisfies.
    const supabaseAdmin = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "",
    );
    const { data: newBalance, error: creditError } = await supabaseAdmin.rpc("credit_wallet", {
      p_user_id: user.id,
      p_amount: paymentIntent.amount / 100,
      p_description: "Added funds via card",
      p_stripe_payment_intent_id: paymentIntent.id,
    });
    if (creditError) {
      return new Response(JSON.stringify({ error: creditError.message }), { status: 500 });
    }

    return new Response(
      JSON.stringify({ new_balance: newBalance }),
      { headers: { "Content-Type": "application/json" } },
    );
  } catch (error) {
    return new Response(JSON.stringify({ error: (error as Error).message }), { status: 500 });
  }
});
