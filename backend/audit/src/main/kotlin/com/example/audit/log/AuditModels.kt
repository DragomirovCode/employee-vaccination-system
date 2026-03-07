package com.example.audit.log

enum class AuditAction {
    CREATE,
    UPDATE,
    DELETE,
}

enum class AuditEntityType {
    USER,
    USER_ROLE,
    DEPARTMENT,
    EMPLOYEE,
    VACCINE,
    DISEASE,
    VACCINE_DISEASE,
    VACCINATION,
    DOCUMENT,
}
