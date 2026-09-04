// Supabase Edge Function: detach-payment-method
//
// Backs the Payment Methods page's "Remove" action — stripe.paymentMethods.detach permanently
// unlinks the card from the customer (Stripe keeps no way to re-attach it; the user would need to
// re-enter the card). Same ownership check as set-default-payment-method so one user can't detach
// another user's saved card by guessing its id.
//
// Deploy with: supabase functions deploy detach-payment-method

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

    const { payment_method_id } = await req.json();
    if (!payment_method_id || typeof payment_method_id !== "string") {
      return new Response(JSON.stringify({ error: "Missing payment_method_id" }), { status: 400 });
    }

    const { data: profile, error: profileError } = await supabase
      .from("profiles")
      .select("stripe_customer_id")
      .eq("id", user.id)
      .single();
    if (profileError) {
      return new Response(JSON.stringify({ error: profileError.message }), { status: 500 });
    }

    const customerId = profile?.stripe_customer_id as string | null;
    if (!customerId) {
      return new Response(JSON.stringify({ error: "No Stripe customer on file" }), { status: 400 });
    }

    const method = await stripe.paymentMethods.retrieve(payment_method_id);
    if (method.customer !== customerId) {
      return new Response(JSON.stringify({ error: "Forbidden" }), { status: 403 });
    }

    await stripe.paymentMethods.detach(payment_method_id);

    return new Response(JSON.stringify({ ok: true }), { headers: { "Content-Type": "application/json" } });
  } catch (error) {
    return new Response(JSON.stringify({ error: (error as Error).message }), { status: 500 });
  }
});
