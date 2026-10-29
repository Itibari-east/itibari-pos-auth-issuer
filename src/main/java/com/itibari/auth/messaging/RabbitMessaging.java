package com.itibari.auth.messaging;

/** Wire contract: keep names in sync with the destination service's catalog. */
public final class RabbitMessaging {
    private RabbitMessaging() {}
    public static final class Exchanges {
        public static final String NOTIFICATIONS = "pos.notifications";
        public static final String INVOICE = "pos.invoice";
        private Exchanges() {}
    }
    public static final class Queues {
        public static final String SEND_NOTIFICATION = "pos.notifications.send";
        public static final String FINANCE_SALES = "pos.finance.invoice.sales-events";
        private Queues() {}
    }
    public static final class RoutingKeys {
        public static final String SEND_NOTIFICATION = "pos.notifications.send";
        public static final String SALES_EVENTS = "pos.invoice.sales-events";
        private RoutingKeys() {}
    }
    public static final class Rpc {
        public static final String EXCHANGE = "pos.rpc";
        public static final String INVENTORY = "pos.inventory.rpc";
        public static final String AUTHORIZATION = "pos.authorization.rpc";
        public static final String COMMERCIALS = "pos.commercials.rpc";
        private Rpc() {}
    }
    public enum Subscription {
        SEND_NOTIFICATION(Exchanges.NOTIFICATIONS, Queues.SEND_NOTIFICATION, RoutingKeys.SEND_NOTIFICATION);
        private final String exchange;
        private final String queue;
        private final String routingKey;
        Subscription(String exchange, String queue, String routingKey) {
            this.exchange = exchange;
            this.queue = queue;
            this.routingKey = routingKey;
        }
        public String exchange() { return exchange; }
        public String queue() { return queue; }
        public String routingKey() { return routingKey; }
        public String deadExchange() { return exchange + ".dead"; }
        public String deadQueue() { return queue + ".dead"; }
    }
}
