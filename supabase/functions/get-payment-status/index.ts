// Supabase Edge Function: get-payment-status
//
// Lets the app verify a PaymentIntent actually succeeded — server-side, against Stripe's own
// records — before CheckoutViewModel ever creates an order. Never trust a bare
// PaymentSheetResult.Completed callback on its own; that only means the user finished the sheet,
// not that Stripe confirmed the charge. Called via
// `supabase.functions.invoke("get-payment-status", ...)`.
//
// Deploy with: supabase functions deploy get-payment-status

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

    // Only ever report the status of a payment this same user created.
    if (paymentIntent.metadata?.supabase_user_id !== user.id) {
      return new Response(JSON.stringify({ error: "Forbidden" }), { status: 403 });
    }

    return new Response(
      JSON.stringify({ status: paymentIntent.status }),
      { headers: { "Content-Type": "application/json" } },
    );
  } catch (error) {
    return new Response(JSON.stringify({ error: (error as Error).message }), { status: 500 });
  }
});
