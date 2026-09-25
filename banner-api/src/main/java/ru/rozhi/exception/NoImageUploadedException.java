package ru.rozhi.exception;

public class NoImageUploadedException extends BadRequestException {

    public NoImageUploadedException(String objectKey) {
        super("NO_IMAGE_UPLOADED", "Object does not exist: " + objectKey);
    }
}