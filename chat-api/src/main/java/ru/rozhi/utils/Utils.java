package ru.rozhi.utils;

public class Utils {

    public static ChatUsers getChatUsers(
            String initiatorUserId,
            String targetUserId
    ) {
        int compareResult = initiatorUserId.compareTo(targetUserId);
        String userLow, userHigh;
        if (compareResult > 0) {
            userLow = targetUserId;
            userHigh = initiatorUserId;
        } else if (compareResult < 0) {
            userLow = initiatorUserId;
            userHigh = targetUserId;
        } else {
            throw new IllegalArgumentException("Users are identical");
        }

        return new ChatUsers(userLow, userHigh);
    }

    public record ChatUsers(
            String userLow,
            String userHigh
    ) {}
}
