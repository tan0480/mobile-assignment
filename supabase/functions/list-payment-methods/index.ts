// Supabase Edge Function: list-payment-methods
//
// Backs the Payment Methods page's list — the page never talks to Stripe directly (only the
// Android app's secret-free publishable key exists client-side), so this is the only source of
// truth for "what cards does this user have saved". Returns an empty list rather than an error
// when the caller has no Stripe customer yet (nothing saved), matching PaymentMethodsScreen's
// existing "No payment methods yet" empty state.
//
// Deploy with: supabase functions deploy list-payment-methods

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
      return new Response(
        JSON.stringify({ payment_methods: [] }),
        { headers: { "Content-Type": "application/json" } },
      );
    }

    const [methods, customer] = await Promise.all([
      stripe.paymentMethods.list({ customer: customerId, type: "card" }),
      stripe.customers.retrieve(customerId),
    ]);

    const defaultId = customer.deleted
      ? null
      : (customer.invoice_settings?.default_payment_method as string | null);

    const paymentMethods = methods.data.map((pm) => ({
      id: pm.id,
      brand: pm.card?.brand ?? "card",
      last4: pm.card?.last4 ?? "0000",
      exp_month: pm.card?.exp_month ?? 0,
      exp_year: pm.card?.exp_year ?? 0,
      is_default: pm.id === defaultId,
    }));

    return new Response(
      JSON.stringify({ payment_methods: paymentMethods }),
      { headers: { "Content-Type": "application/json" } },
    );
  } catch (error) {
    return new Response(JSON.stringify({ error: (error as Error).message }), { status: 500 });
  }
});
