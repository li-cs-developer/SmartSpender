package com.smartspender.common.exception;

/**
 * A domain rule was violated in a way the user can correct. Maps to HTTP 400.
 *
 * Contrast with IllegalArgumentException (also 400 but no code): this class
 * carries a stable error code the frontend can translate.
 */
public class BusinessRuleException extends BusinessException {

    public BusinessRuleException(String code, String message) {
        super(code, message);
    }

    // ---- Factory methods for the codes we use today ----

    public static BusinessRuleException categoryNameTaken(String name) {
        return new BusinessRuleException(
            "CATEGORY_NAME_TAKEN",
            "A category named '" + name + "' already exists"
        );
    }

    public static BusinessRuleException categoryColorTaken(String color) {
        return new BusinessRuleException(
            "CATEGORY_COLOR_TAKEN",
            "Color " + color + " is already used by another category"
        );
    }

    public static BusinessRuleException amountTooLarge() {
        return new BusinessRuleException(
            "AMOUNT_TOO_LARGE",
            "Amount exceeds the maximum allowed (999,999,999,999.99)"
        );
    }

    public static BusinessRuleException categoryNotOwned(Long categoryId) {
        return new BusinessRuleException(
            "CATEGORY_NOT_OWNED",
            "Category " + categoryId + " does not belong to this user"
        );
    }

    public static BusinessRuleException categoryInUse() {
        return new BusinessRuleException(
            "CATEGORY_IN_USE",
            "This category has transactions. Reassign or delete them first."
        );
    }
}