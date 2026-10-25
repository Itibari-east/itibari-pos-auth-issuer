package com.itibari.auth.access;

import java.util.List;

public final class AccessCatalog {

    public static final String OWNER_ROLE = "Owner";
    public static final String CASHIER_ROLE = "Cashier";
    public static final String STOREKEEPER_ROLE = "Storekeeper";
    public static final String ACCOUNTANT_ROLE = "Accountant";
    public static final String HR_ROLE = "HR";

    public static final List<GroupSpec> GROUPS = List.of(
            new GroupSpec("DASHBOARD", List.of("VIEW_DASHBOARD")),
            new GroupSpec("SALES", List.of("VIEW_SALES", "CREATE_INVOICE", "CREATE_QUOTATION", "CREATE_PROFORMA",
                    "VIEW_RETURNS")),
            new GroupSpec("POS", List.of("VIEW_POS")),
            new GroupSpec("PRODUCTS", List.of("VIEW_PRODUCTS", "MANAGE_PRODUCTS", "MANAGE_PRICE_GROUPS")),
            new GroupSpec("CUSTOMERS", List.of("VIEW_CUSTOMERS", "MANAGE_CUSTOMERS")),
            new GroupSpec("INVENTORY", List.of("VIEW_INVENTORY", "MANAGE_STOCK")),
            new GroupSpec("PROCUREMENT", List.of("VIEW_PROCUREMENT", "MANAGE_PURCHASE_ORDERS", "MANAGE_GRN")),
            new GroupSpec("ACCOUNTING", List.of("VIEW_ACCOUNTING", "MANAGE_JOURNALS", "VIEW_REPORTS")),
            new GroupSpec("STAFF", List.of("VIEW_STAFF", "MANAGE_STAFF", "MANAGE_PAYROLL")),
            new GroupSpec("SETTINGS", List.of("VIEW_SETTINGS", "MANAGE_SETTINGS")),
            new GroupSpec("USER_MANAGEMENT", List.of("VIEW_USERS", "MANAGE_USERS", "MANAGE_ROLES"))
    );

    public static final List<RoleSpec> ROLES = List.of(
            new RoleSpec(OWNER_ROLE, "Full access to all shop modules", groupNames()),
            new RoleSpec(CASHIER_ROLE, "Point of sale, invoices, and customers",
                    List.of("POS", "SALES", "CUSTOMERS", "DASHBOARD")),
            new RoleSpec(STOREKEEPER_ROLE, "Stock, products, and procurement",
                    List.of("INVENTORY", "PROCUREMENT", "PRODUCTS", "DASHBOARD")),
            new RoleSpec(ACCOUNTANT_ROLE, "Accounting and financial reports",
                    List.of("ACCOUNTING", "DASHBOARD", "SALES")),
            new RoleSpec(HR_ROLE, "Staff, attendance, and payroll",
                    List.of("STAFF", "DASHBOARD"))
    );

    private AccessCatalog() {
    }

    public static List<String> groupNames() {
        return GROUPS.stream().map(GroupSpec::name).toList();
    }

    public record GroupSpec(String name, List<String> privileges) {
    }

    public record RoleSpec(String name, String description, List<String> groupNames) {
    }
}
