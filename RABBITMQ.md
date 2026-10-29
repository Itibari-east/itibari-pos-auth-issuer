# POS RabbitMQ contracts and rollout

Queue, exchange, and routing names live in `messaging/RabbitMessaging.java`, not YAML.
Each repository is independently built: wire names shared by a producer and consumer
must match. JSON uses stable fields, not a Java package name as the wire contract.

| Caller / publisher | Destination | Exchange | Queue / routing key | Behavior |
|---|---|---|---|---|
| Invoice sales lifecycle | Finance (`accounting-service` in Kubernetes) | `pos.invoice` | queue `pos.finance.invoice.sales-events`, key `pos.invoice.sales-events` | Durable event, transactional outbox/inbox |
| Invoice loyalty earned | Commercials | `pos.commercials` | queue `pos.commercials.loyalty.earn`, key `loyalty.earn` | Durable event, transactional outbox/inbox |
| Auth password setup/reset | Notification | `pos.notifications` | `pos.notifications.send` | Durable outbox, notification delivery retries |
| Invoice stock deduct/restore, including batches | Inventory | `pos.rpc` | `pos.inventory.rpc` | Authenticated request/reply |
| Invoice loyalty redemption | Commercials | `pos.rpc` | `pos.commercials.rpc` | Authenticated request/reply |
| HR create child user / assign roles | Authorization | `pos.rpc` | `pos.authorization.rpc` | Authenticated request/reply |
| File inventory report pages | Inventory | `pos.rpc` | `pos.inventory.rpc` | Authenticated request/reply |

HR, inventory, finance, commercials, file, invoice, and auth also have a typed
`NotificationCommandPublisher` for future notification calls. It confirms a send
or raises an error; callers needing atomic database-and-message persistence should
use an outbox as invoice and auth do. No additional recipients or notifications
are invented by this migration.

## Existing operations

Invoice retains its sales lifecycle payload, with a new immutable `eventId`.
Finance handles the original seven journal event types: order created/cancelled,
invoice created/updated/completed/cancelled, and sales return created. Other
lifecycle types (quotation, proforma, and cancelled-invoice reversal) retain the
previous no-journal behavior; new financial business rules need explicit handlers.

Loyalty earnings become eventually consistent: sales completion records an event
and the commercials ledger updates asynchronously. Redemption still returns the
result before the sale proceeds. Stock mutations, HR account creation and role
assignment, and report pages retain their existing response contracts over AMQP.
Remaining HTTP calls (pricing, customer/bank lookups, promos, directories, licensing,
file metadata, etc.) are unchanged.

## Reliability and authentication

- Invoice and auth persist events in their business database transaction. A
  serialized dispatcher sends committed events with persistent delivery, publisher
  confirms, and mandatory routing. Failure leaves the event pending for retry.
- Deploy both ends before sending traffic. Queue declarations are mirrored at
  producers where needed, so messages can wait for an offline consumer.
- Finance and commercials record `eventId` in an inbox in the same transaction as
  the domain write. Duplicate delivery does not repeat that committed write.
  Finance has a single active consumer and prefetch one to preserve queue order.
- Async delivery failures get three attempts, then a durable `.dead` queue via
  `<exchange>.dead`. Inspect and repair before replay. A dead-lettered earlier event
  can require reconciliation before later events for that document are processed.
- Notification is at least once: provider acceptance and a database/broker ack are
  not atomic, so a crash can still duplicate an email/SMS. Invalid notification
  payloads are rejected to the dead-letter route.
- RPC requests carry the caller's signed JWT. Receivers validate it with their
  existing decoder, establish the existing security context, validate request
  bodies, and call only allowlisted controller methods. Tenant checks remain in
  those controllers/services. Never publish arbitrary caller-supplied RPC methods.
- RPC uses a 30-second deadline and queue TTL, no automatic client retry. Mutation
  results are stored with the domain write and reused on redelivery of the same
  request ID. Reusing an ID with another caller or payload is rejected. Read-only
  report queries do not store results. Security context is restored after handling.
- A timeout during an in-flight mutation means its outcome may be unknown; inspect
  the destination state before retrying. RabbitMQ does not make cross-service stock,
  sale, or account changes one atomic transaction. A new user request gets a new ID.
- Broker access is service-trusted. Use namespace-local credentials; do not expose
  AMQP publicly. Async event payloads carry tenant IDs, not a user's bearer token.

## Configuration

All services use `SPRING_RABBITMQ_HOST`, `SPRING_RABBITMQ_PORT`,
`SPRING_RABBITMQ_USERNAME`, and `SPRING_RABBITMQ_PASSWORD`.
`MESSAGING_RABBITMQ_ENABLED` enables the new services; notification retains
`NOTIFICATION_RABBITMQ_ENABLED`. Both are true only in the dev CI shared config.
When messaging is disabled, the selected RPC clients fall back to their existing
HTTP clients, while event publishing/consumption is disabled.

Schema migrations are additive, via Liquibase: invoice/auth outboxes, finance and
commercials inboxes, and inventory/commercials/auth RPC result tables. Retain inbox
IDs and RPC results for the redelivery/reconciliation window. Periodically remove
old published outbox rows; auth outbox payloads contain password email content and
should have a short retention period. Never remove pending rows as a cleanup step.

## Deploy dev

1. Deploy RabbitMQ and create `rabbitmq-credentials` (`username`, `password`) in dev.
2. Publish and deploy notification, finance/accounting, inventory, and commercials
   images first. Publish/deploy auth next, then invoice, HR, and file. Each image
   pipeline updates its CI overlay; ensure that new image tags actually reach dev.
3. Sync CI shared config and affected Applications. The deployment manifests inject
   RabbitMQ credentials into all eight services. Kafka-disable variables on the old
   invoice/accounting Deployments remain only to keep old images safe during rollout;
   the new Java applications have no Kafka dependency and ignore those variables.
4. Check migrations and rollouts; verify queue consumers in the RabbitMQ UI. Enable
   notification replicas/provider credentials if it is still scaled to zero.
5. Exercise one sale → journal, earned loyalty → ledger, stock command, HR account
   creation, password email, and file report with a real tenant JWT. Confirm that
   dead-letter queues stay empty and outbox rows obtain `published_at`.

Staging/prod remain disabled until their own brokers and credentials are provisioned.
For rollback, disable messaging in shared config and restart callers/consumers, or
restore prior images. Pending events remain in the database/broker; preserve them
for reconciliation. Do not drop the new tables or purge queues during rollback.

## Adding an event or request

Add static wire names and a `Subscription` entry in the destination catalog; mirror
that contract in the producer catalog. Add the typed payload and listener, with
idempotent handling for domain writes. Use the outbox for durable business events.
For RPC, add an explicit operation to the caller's allowlist and receiver registry;
read operations return data, mutation operations use the result store. Add tests
for the new payload/routing and failure behavior. Rebuild the affected services;
no exchange/queue YAML changes are needed.
