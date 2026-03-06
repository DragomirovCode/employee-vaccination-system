package com.example.audit.log

enum class AuditAction {
    CREATE,
    UPDATE,
    DELETE,
}

enum class AuditEntityType {
    VACCINATION,
    DOCUMENT,
}
