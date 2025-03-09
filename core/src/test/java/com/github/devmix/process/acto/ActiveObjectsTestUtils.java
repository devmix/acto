package com.github.devmix.process.acto;

import lombok.experimental.UtilityClass;

/**
 * @author Sergey Grachev
 */
@UtilityClass
public final class ActiveObjectsTestUtils {

    public static boolean allWithStatus(final ActiveObjectStatus status, final ActiveObjectContext... contexts) {
        for (final var context : contexts) {
            if (context.getStatus() != status) {
                return false;
            }
        }
        return true;
    }
}
