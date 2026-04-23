# Paiement Stripe

Le paiement colis passe par Stripe Checkout.

## Flux

1. Le front cree le colis avec le statut `PAYMENTPENDING`.
2. La page paiement appelle `POST /packages/v1/payments/stripe/checkout-session?packageID=...`.
3. Le backend cree une session Stripe Checkout et enregistre un `PackagePayment`.
4. Le front redirige l'utilisateur vers l'URL Stripe.
5. Stripe appelle le webhook `POST /packages/v1/payments/stripe/webhook`.
6. Seul le webhook signe Stripe passe le colis de `PAYMENTPENDING` a `NEW`.

La page de retour Stripe affiche seulement un message. Elle ne confirme pas le paiement.

## Configuration

Variables attendues cote packages-service:

```properties
PACKAGES_STRIPE_SECRET_KEY=sk_test_...
PACKAGES_STRIPE_WEBHOOK_SECRET=whsec_...
PACKAGES_STRIPE_CURRENCY=eur
```

Webhook Stripe a declarer:

```text
https://api.quickdelivery.fr/packages/v1/payments/stripe/webhook
```

Evenements utiles:

```text
checkout.session.completed
checkout.session.async_payment_succeeded
checkout.session.async_payment_failed
checkout.session.expired
```

En local, utiliser Stripe CLI pour forwarder le webhook vers le gateway ou directement vers packages-service.
