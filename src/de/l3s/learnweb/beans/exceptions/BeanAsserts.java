package de.l3s.learnweb.beans.exceptions;

/**
 * This asserts can be used in `viewAction` to validate data,
 * if failed it will throw an error which prevent further processing and will show an error page.
 *
 * All methods should accept {@code true} as valid (no error) input and {@code false} as invalid input which triggers exception.
 */
@SuppressWarnings("NonBooleanMethodNameMayNotStartWithQuestion")
public final class BeanAsserts {

    /**
     * @see BeanAsserts#validateNotNull(Object, String)
     */
    public static void validateNotNull(final Object object) {
        validate(object != null, null);
    }

    /**
     * @param object If the given object is {@code null} throws an error and displays '400 Bad Request' error page
     * @param message an optional message, which shows instead of default error description
     */
    public static void validateNotNull(final Object object, final String message) {
        validate(object != null, message);
    }

    /**
     * @see BeanAsserts#validate(boolean, String)
     */
    public static void validate(final boolean isValid) {
        validate(isValid, null);
    }

    /**
     * @param isValid if {@code false} throws an error which prevents further processing and displays '400 Bad Request' error page
     * @param message an optional message, which shows instead of default error description
     */
    public static void validate(final boolean isValid, final String message) {
        if (!isValid) {
            throw new BadRequestBeanException(message);
        }
    }

    /**
     * @see BeanAsserts#found(boolean, String)
     */
    public static void found(final boolean isFound) {
        found(isFound, null);
    }

    /**
     * TODO: better name, maybe: isFound, exists, canBeShown, notFoundIfFalse
     *
     * @param isFound if {@code false} throws an error which prevents further processing and displays '400 Not Found' error page
     * @param message an optional message, which shows instead of default error description
     */
    public static void found(final boolean isFound, final String message) {
        if (!isFound) {
            throw new NotFoundBeanException(message);
        }
    }

    /**
     * @see BeanAsserts#hasPermission(boolean, String)
     */
    public static void hasPermission(final boolean hasPermission) {
        hasPermission(hasPermission, null);
    }

    /**
     * @param hasPermission if {@code false} throws an error which prevents further processing and displays '403 Forbidden' error page
     * @param message an optional message, which shows instead of default error description
     */
    public static void hasPermission(final boolean hasPermission, final String message) {
        if (!hasPermission) {
            throw new ForbiddenBeanException(message);
        }
    }

    /**
     * @param user If the given object (User) is {@code null} throws an error
     * @see BeanAsserts#authorized(boolean, String)
     */
    public static void authorized(final Object user) {
        authorized(user != null, null);
    }

    /**
     * @see BeanAsserts#authorized(boolean, String)
     */
    public static void authorized(final boolean isAuthorized) {
        authorized(isAuthorized, null);
    }

    /**
     * @param isAuthorized if {@code false} throws an error which prevents further processing and redirects to login page
     * @param message an optional message, which not actually shows anywhere because of redirect to login page :/
     */
    public static void authorized(final boolean isAuthorized, final String message) {
        if (!isAuthorized) {
            throw new UnauthorizedBeanException(message);
        }
    }
}
