// Supabase Edge Function: create-setup-intent
//
// Backs the Payment Methods page's "Add Card" flow — saves a card to the caller's Stripe
// Customer with no charge, via PaymentSheet.presentWithSetupIntent(client_secret, ...) on the
// Android side. Requires stripe_customer_id to already exist (the app calls
// get-or-create-stripe-customer first to get both the customer id and the ephemeral key
// PaymentSheet's CustomerConfiguration needs — this function only needs the id).
//
// Deploy with: supabase functions deploy create-setup-intent

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

    const setupIntent = await stripe.setupIntents.create({
      customer: customerId,
      metadata: { supabase_user_id: user.id },
    });

    return new Response(
      JSON.stringify({ client_secret: setupIntent.client_secret }),
      { headers: { "Content-Type": "application/json" } },
    );
  } catch (error) {
    return new Response(JSON.stringify({ error: (error as Error).message }), { status: 500 });
  }
});
