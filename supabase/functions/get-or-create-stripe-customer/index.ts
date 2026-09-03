// Supabase Edge Function: get-or-create-stripe-customer
//
// Every payment-methods-related flow (Checkout's PaymentSheet, the Payment Methods page's list/
// add/remove/set-default) starts here: looks up the caller's `profiles.stripe_customer_id`,
// creates a Stripe Customer the first time one is needed, and always mints a fresh ephemeral key
// (ephemeral keys are meant to be short-lived and re-minted per client session, never stored).
//
// Writing stripe_customer_id back to profiles needs a service-role client — the anon-key client
// (used for everything else in this function) can't, since `revoke update (stripe_customer_id)
// on public.profiles from authenticated` in schema.sql blocks it even for the row's own owner.
// SUPABASE_SERVICE_ROLE_KEY must be set via `supabase secrets set` — this is the only Edge
// Function in the project that needs it.
//
// Deploy with: supabase functions deploy get-or-create-stripe-customer

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

    let customerId = profile?.stripe_customer_id as string | null;
    if (!customerId) {
      const customer = await stripe.customers.create({
        email: user.email ?? undefined,
        metadata: { supabase_user_id: user.id },
      });
      customerId = customer.id;

      const serviceClient = createClient(
        Deno.env.get("SUPABASE_URL") ?? "",
        Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "",
      );
      const { error: writeError } = await serviceClient
        .from("profiles")
        .update({ stripe_customer_id: customerId })
        .eq("id", user.id);
      if (writeError) {
        return new Response(JSON.stringify({ error: writeError.message }), { status: 500 });
      }
    }

    const ephemeralKey = await stripe.ephemeralKeys.create(
      { customer: customerId },
      { apiVersion: "2024-06-20" },
    );

    return new Response(
      JSON.stringify({ customer_id: customerId, ephemeral_key: ephemeralKey.secret }),
      { headers: { "Content-Type": "application/json" } },
    );
  } catch (error) {
    return new Response(JSON.stringify({ error: (error as Error).message }), { status: 500 });
  }
});
