package com.tigger.closetconnectproject.Common.Exception;

/**
 * 리소스를 찾을 수 없을 때 발생하는 예외
 * HTTP 404 Not Found로 매핑됩니다.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceType;
    private final Object resourceId;

    public ResourceNotFoundException(String resourceType, Object resourceId) {
        super(String.format("%s를 찾을 수 없습니다. ID: %s", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceType = null;
        this.resourceId = null;
    }

    public String getResourceType() {
        return resourceType;
    }

    public Object getResourceId() {
        return resourceId;
    }
}
