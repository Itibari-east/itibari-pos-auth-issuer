package com.itibari.auth.doc;

public final class Examples {

    private Examples() {
    }

    public static final String CREATE_USER_REQUEST =
            "{\"firstName\":\"user1\",\"lastName\":\"user2\",\"contactEmail\":\"user1@admin.com\",\"contactPhoneNumber\":\"+250700000000\",\"password\":\"Password1!\",\"confirmPassword\":\"Password1!\"}";

    public static final String CREATE_USER_OK_RESPONSE =
            "{\"message\":\"User created successfully\",\"identity\":\"user1 user2\"}";

    public static final String CREATE_TENANT_USER_WITHOUT_ASSIGNMENTS_REQUEST =
            "{\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"role\":[],\"branch\":[],\"phone\":\"+255712000001\",\"email\":\"jane.doe@example.com\"}";

    public static final String CREATE_TENANT_USER_WITH_ASSIGNMENTS_REQUEST =
            "{\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"role\":[\"9de2e878-d6cb-4d72-97c2-c0ad53cf8ca5\"],\"branch\":[\"cd48c00f-2241-467b-a214-aec6249f9805\"],\"phone\":\"+255712000001\",\"email\":\"jane.doe@example.com\"}";

    public static final String CREATE_TENANT_USER_OK_RESPONSE =
            "{\"message\":\"User created successfully\",\"identity\":\"Jane Doe\",\"publicId\":\"0bad0e09-c448-4810-a5a5-5dbe0fb436f1\"}";

    public static final String GET_USER_OK_RESPONSE =
            "{\"publicId\":\"04f677b4-dec6-43ef-af1e-398fb83a6c6e\",\"firstName\":\"Alice\",\"lastName\":\"Admin\",\"phoneNumber\":\"+250700000000\",\"email\":\"alice@example.com\",\"activeStatus\":true,\"passportUrl\":\"https://api-pos-dev.itibari.io/api/v1/pos/files/1f0e2d3c-4b5a-4968-8776-a5b4c3d2e1f0/content\"}";

    public static final String USERS_OK_RESPONSE =
            "{\"totalElements\":2,\"pageSize\":2,\"totalPages\":1,\"last\":true,\"first\":true,\"pageNumber\":0,\"content\":[{\"publicId\":\"04f677b4-dec6-43ef-af1e-398fb83a6c6e\",\"firstName\":\"Alice\",\"lastName\":\"Admin\",\"phoneNumber\":\"+250700000000\",\"email\":\"alice@example.com\",\"activeStatus\":true,\"passportUrl\":\"https://api-pos-dev.itibari.io/api/v1/pos/files/1f0e2d3c-4b5a-4968-8776-a5b4c3d2e1f0/content\"},{\"publicId\":\"0bad0e09-c448-4810-a5a5-5dbe0fb436f1\",\"firstName\":\"Bob\",\"lastName\":\"Builder\",\"phoneNumber\":\"+250700000000\",\"email\":\"bob@example.com\",\"activeStatus\":true,\"passportUrl\":null}]}";

    public static final String SESSION_USER_OK_RESPONSE =
            "{\"publicId\":\"0bad0e09-c448-4810-a5a5-5dbe0fb436f1\",\"firstName\":\"Bob\",\"lastName\":\"Builder\",\"phoneNumber\":\"+250700000000\",\"email\":\"bob@example.com\",\"activeStatus\":true,\"passportUrl\":\"https://api-pos-dev.itibari.io/api/v1/pos/files/1f0e2d3c-4b5a-4968-8776-a5b4c3d2e1f0/content\"}";

    public static final String UPDATE_USER_REQUEST =
            "{\"firstName\":\"Alice\",\"lastName\":\"Admin\",\"phoneNumber\":\"+250700000000\",\"email\":\"alice@example.com\"}";

    public static final String ASSIGN_ROLE_TO_USER_REQUEST =
            "{\"rolePublicIds\":[\"dc9fbc18-456a-4f33-aff9-b551d70d217a\",\"d54f5b2c-aabf-4991-8931-3d40f176f3e1\"]}";

    public static final String ASSIGN_ROLE_TO_USER_RESPONSE =
            "{\"message\":\"successfully assigned roles\",\"identity\":\"ROLE_ADMIN, ROLE_AUDITOR\"}";

    public static final String USER_ROLES_OK_RESPONSE =
            "{\"totalElements\":1,\"pageSize\":1,\"totalPages\":1,\"last\":true,\"first\":true,\"pageNumber\":0,\"content\":[{\"publicId\":\"47a111df-dd04-4311-a144-9a572b4bdd59\",\"name\":\"ROLE_ADMIN\"}]}";

    public static final String ALLOCATE_SHOPS_TO_USER_REQUEST =
            "{\"shopPublicIds\":[\"dc9fbc18-456a-4f33-aff9-b551d70d217a\",\"d54f5b2c-aabf-4991-8931-3d40f176f3e1\"]}";

    public static final String ALLOCATE_SHOPS_TO_USER_RESPONSE =
            "{\"message\":\"successfully allocated shops\",\"identity\":\"Alice Admin\"}";

    public static final String REMOVE_SHOP_ALLOCATION_RESPONSE =
            "{\"message\":\"successfully removed shop allocation\",\"identity\":\"Alice Admin\"}";

    public static final String USER_PRIVILEGES_OK_RESPONSE =
            "{\"totalElements\":1,\"pageSize\":1,\"totalPages\":1,\"last\":true,\"first\":true,\"pageNumber\":0,\"content\":[{\"ROLE_ADMIN\":{\"publicId\":\"47a111df-dd04-4311-a144-9a572b4bdd59\",\"MODULE_VIEWS\":[{\"publicId\":\"36237c22-7e51-4fa7-a7e4-f6304835765a\",\"VIEW_USERS\":true}]}}]}";

    public static final String FORGOT_PASSWORD_RESPONSE =
            "{\"resetToken\":\"123456\",\"expiresAt\":\"5\"}";

    public static final String RESET_PASSWORD_REQUEST =
            "{\"resetToken\":\"123456\",\"newPassword\":\"Password1!\",\"confirmPassword\":\"Password1!\"}";

    public static final String RESET_PASSWORD_RESPONSE =
            "{\"message\":\"You have successfully reset your password\"}";

    public static final String CHANGE_PASSWORD_REQUEST =
            "{\"newPassword\":\"Password1!\",\"confirmPassword\":\"Password1!\"}";

    public static final String CHANGE_PASSWORD_RESPONSE =
            "{\"message\":\"You have successfully changed your password\"}";

    public static final String CREATE_COUNTRY_REQUEST =
            "{\"isoAlpha\":\"KEN\"}";

    public static final String COUNTRY_TANZANIA_HIT =
            "{\"publicId\":\"3b7c1e5a-8d24-4f91-9a6e-2c5d8f0a1b23\",\"name\":\"Tanzania\",\"isoAlpha\":\"TZA\",\"countryCode\":\"+255\",\"isoAlphaTwo\":\"TZ\",\"currencyCode\":\"TZS\"}";

    public static final String COUNTRY_GERMANY_HIT =
            "{\"publicId\":null,\"name\":\"Germany\",\"isoAlpha\":\"DEU\",\"countryCode\":\"+49\",\"isoAlphaTwo\":\"DE\",\"currencyCode\":\"EUR\"}";

    public static final String COUNTRY_GERMANY_CREATED =
            "{\"publicId\":\"8e2a4c6b-1f03-4d85-a7c9-0b1d2e3f4a56\",\"name\":\"Germany\",\"isoAlpha\":\"DEU\",\"countryCode\":\"+49\",\"isoAlphaTwo\":\"DE\",\"currencyCode\":\"EUR\"}";

    public static final String CURRENCY_TZS_HIT =
            "{\"code\":\"TZS\",\"name\":\"Tanzanian Shilling\",\"fractionDigits\":2}";
}
