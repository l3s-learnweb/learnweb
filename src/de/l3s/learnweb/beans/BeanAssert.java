package de.l3s.learnweb.beans;

import java.util.function.Supplier;

import de.l3s.learnweb.exceptions.BadRequestHttpException;
import de.l3s.learnweb.exceptions.ForbiddenHttpException;
import de.l3s.learnweb.exceptions.GoneHttpException;
import de.l3s.learnweb.exceptions.NotFoundHttpException;
import de.l3s.learnweb.exceptions.UnauthorizedHttpException;
import de.l3s.learnweb.group.Group;
import de.l3s.util.Deletable;

/**
 * This asserts can be used in `viewAction` to validate data,
 * if failed it will throw an error which prevent further processing and will show an error page.
 *
 * All methods should accept {@code true} as valid (no error) input and {@code false} as invalid input which triggers exception.
 */
public final class BeanAssert {

    public static final Supplier<NotFoundHttpException> NOT_FOUND = () -> new NotFoundHttpException("error_pages.not_found_object_description");
    public static final Supplier<NotFoundHttpException> NOT_FOUND_GROUP = () -> new NotFoundHttpException("error_pages.not_found_group_description");

    /**
     * @see BeanAssert#validate(Object, String)
     */
    public static void validate(final Object object) {
        validate(object != null, null);
    }

    /**
     * @param object If the given object is {@code null} throws an error and displays '400 Bad Request' error page
     * @param message is shown instead of default error description
     */
    public static void validate(final Object object, final String message) {
        validate(object != null, message);
    }

    /**
     * @see BeanAssert#validate(boolean, String)
     */
    public static void validate(final boolean isValid) {
        validate(isValid, null);
    }

    /**
     * @param isValid if {@code false} throws an error which prevents further processing and displays '400 Bad Request' error page
     * @param message is shown instead of default error description
     */
    public static void validate(final boolean isValid, final String message) {
        if (!isValid) {
            throw new BadRequestHttpException(message);
        }
    }

    /**
     * @see BeanAssert#isFound(Object, String)
     */
    public static void isFound(final Group object) {
        isFound(object != null, "error_pages.not_found_group_description");
    }

    /**
     * @see BeanAssert#isFound(Object, String)
     */
    public static void isFound(final Object object) {
        isFound(object != null, "error_pages.not_found_object_description");
    }

    /**
     * @param object If the given object is {@code null} throws an error and displays '400 Not Found' error page
     * @param message is shown instead of default error description
     */
    public static void isFound(final Object object, final String message) {
        isFound(object != null, message);
    }

    /**
     * @see BeanAssert#isFound(Object, String)
     */
    public static void isFound(final boolean isFound) {
        isFound(isFound, null);
    }

    /**
     * @param isFound if {@code false} throws an error which prevents further processing and displays '400 Not Found' error page
     * @param message is shown instead of default error description
     */
    public static void isFound(final boolean isFound, final String message) {
        if (!isFound) {
            throw new NotFoundHttpException(message);
        }
    }

    /**
     * @see BeanAssert#notDeleted(Deletable, String)
     */
    public static void notDeleted(final Deletable object) {
        notDeleted(object, null);
    }

    /**
     * @param object If the given object has deleted state throws an error which prevents further processing and displays '410 Gone' error page
     * @param message is shown instead of default error description
     */
    public static void notDeleted(final Deletable object, final String message) {
        if (object.isDeleted()) {
            throw new GoneHttpException(message);
        }
    }

    /**
     * @see BeanAssert#hasPermission(boolean, String)
     */
    public static void hasPermission(final boolean hasPermission) {
        hasPermission(hasPermission, null);
    }

    /**
     * @param hasPermission if {@code false} throws an error which prevents further processing and displays '403 Forbidden' error page
     * @param message is shown instead of default error description
     */
    public static void hasPermission(final boolean hasPermission, final String message) {
        if (!hasPermission) {
            throw new ForbiddenHttpException(message);
        }
    }

    /**
     * @param object If the given object (User) is {@code null} throws an error
     * @see BeanAssert#authorized(boolean)
     */
    public static void authorized(final Object object) {
        authorized(object != null);
    }

    /**
     * @param isAuthorized if {@code false} throws an error which prevents further processing and redirects to login page
     */
    public static void authorized(final boolean isAuthorized) {
        if (!isAuthorized) {
            throw new UnauthorizedHttpException();
        }
    }
}
