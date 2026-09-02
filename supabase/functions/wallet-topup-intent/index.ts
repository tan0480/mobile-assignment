// Supabase Edge Function: wallet-topup-intent
//
// Step 1 of "Add Funds": creates a Stripe PaymentIntent for topping up the caller's own wallet.
// Tagged with `metadata.purpose = "wallet_topup"` so wallet-topup-confirm can tell this apart
// from a regular checkout PaymentIntent (see create-payment-intent) and refuse to credit the
// wallet for one that wasn't actually meant as a top-up.
//
// Deploy with: supabase functions deploy wallet-topup-intent

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

    const { amount, currency } = await req.json();
    if (!amount || typeof amount !== "number" || amount <= 0) {
      return new Response(JSON.stringify({ error: "Invalid amount" }), { status: 400 });
    }

    const paymentIntent = await stripe.paymentIntents.create({
      amount: Math.round(amount),
      currency: currency ?? "myr",
      metadata: { supabase_user_id: user.id, purpose: "wallet_topup" },
      automatic_payment_methods: { enabled: true },
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
