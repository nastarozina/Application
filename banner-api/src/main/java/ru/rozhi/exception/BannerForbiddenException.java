package ru.rozhi.exception;

public class BannerForbiddenException extends ForbiddenException {
    public BannerForbiddenException(String bannerId) {
        super("BANNER_FORBIDDEN", "Access to banner " +  bannerId + " is forbidden.");
    }
}
