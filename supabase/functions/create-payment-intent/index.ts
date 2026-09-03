// Supabase Edge Function: create-payment-intent
//
// The only place in this project that holds the Stripe *secret* key — it lives in this
// function's own secret (`supabase secrets set STRIPE_SECRET_KEY=sk_test_...`), never in the
// Android app or committed to source. Called by CheckoutRepository.createPaymentIntent() via
// `supabase.functions.invoke("create-payment-intent", ...)`, which automatically forwards the
// calling user's session as the Authorization header — verified below before creating anything.
//
// Deploy with: supabase functions deploy create-payment-intent

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

    // SUPABASE_URL / SUPABASE_ANON_KEY are auto-injected into every Edge Function's
    // environment — no manual secret needed for these two, only STRIPE_SECRET_KEY.
    const supabase = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_ANON_KEY") ?? "",
      { global: { headers: { Authorization: authHeader } } },
    );
    const { data: { user }, error: userError } = await supabase.auth.getUser();
    if (userError || !user) {
      return new Response(JSON.stringify({ error: "Unauthorized" }), { status: 401 });
    }

    const { amount, currency, customer_id } = await req.json();
    if (!amount || typeof amount !== "number" || amount <= 0) {
      return new Response(JSON.stringify({ error: "Invalid amount" }), { status: 400 });
    }

    // customer_id is client-supplied (from the app's own prior get-or-create-stripe-customer
    // call), so it's checked against the caller's own profile row rather than trusted outright —
    // otherwise a crafted request could attach this payment (and the saved card that results
    // from setup_future_usage below) to a different user's Stripe customer.
    let customerId: string | undefined;
    if (customer_id) {
      const { data: profile, error: profileError } = await supabase
        .from("profiles")
        .select("stripe_customer_id")
        .eq("id", user.id)
        .single();
      if (profileError) {
        return new Response(JSON.stringify({ error: profileError.message }), { status: 500 });
      }
      if (profile?.stripe_customer_id !== customer_id) {
        return new Response(JSON.stringify({ error: "Forbidden" }), { status: 403 });
      }
      customerId = customer_id;
    }

    // amount is already in the smallest currency unit (sen) — the app computes it as
    // round(RM total * 100) before calling this function. setup_future_usage attaches the card
    // used here to the customer for reuse once payment succeeds, which is what makes it show up
    // afterwards in list-payment-methods / the Payment Methods page.
    const paymentIntent = await stripe.paymentIntents.create({
      amount: Math.round(amount),
      currency: currency ?? "myr",
      metadata: { supabase_user_id: user.id },
      automatic_payment_methods: { enabled: true },
      ...(customerId ? { customer: customerId, setup_future_usage: "on_session" } : {}),
    });

    return new Response(
      JSON.stringify({
        client_secret: paymentIntent.client_secret,
        payment_intent_id: paymentIntent.id,
      }),
      { headers: { "Content-Type": "application/json" } },
    );
  } catch (error) {
    return new Response(JSON.stringify({ error: (error as Error).message }), { status: 500 });
  }
});
