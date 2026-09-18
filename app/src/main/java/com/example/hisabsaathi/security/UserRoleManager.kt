package com.example.hisabsaathi.security

enum class UserRole(val code: String, val displayName: String, val description: String) {
    ADMIN("admin", "Admin", "Full access to all modules, financial exports, settings, and role management."),
    MANAGER("manager", "Manager", "Access to customer ledger, transactions, and attendance. Restricted from business settings."),
    EMPLOYEE("employee", "Employee", "Restricted access to record daily attendance and add transactions. No delete or config access.")
}

enum class Permission {
    MANAGE_BUSINESS_SETTINGS,
    MANAGE_USER_ROLES,
    EXPORT_FINANCIAL_DATA,
    DELETE_TRANSACTIONS,
    EDIT_CUSTOMERS,
    RECORD_ATTENDANCE,
    VIEW_DASHBOARD_STATS
}

object UserRoleManager {

    fun normalizeRole(roleStr: String?): UserRole {
        return when (roleStr?.lowercase()?.trim()) {
            "admin" -> UserRole.ADMIN
            "manager" -> UserRole.MANAGER
            "employee" -> UserRole.EMPLOYEE
            else -> UserRole.ADMIN // Default fallback for main account owner
        }
    }

    fun hasPermission(roleStr: String?, permission: Permission): Boolean {
        val role = normalizeRole(roleStr)
        return when (role) {
            UserRole.ADMIN -> true // Admin has full access to everything
            UserRole.MANAGER -> {
                when (permission) {
                    Permission.MANAGE_BUSINESS_SETTINGS -> false
                    Permission.MANAGE_USER_ROLES -> false
                    Permission.EXPORT_FINANCIAL_DATA -> true
                    Permission.DELETE_TRANSACTIONS -> false // Managers cannot delete audit records
                    Permission.EDIT_CUSTOMERS -> true
                    Permission.RECORD_ATTENDANCE -> true
                    Permission.VIEW_DASHBOARD_STATS -> true
                }
            }
            UserRole.EMPLOYEE -> {
                when (permission) {
                    Permission.MANAGE_BUSINESS_SETTINGS -> false
                    Permission.MANAGE_USER_ROLES -> false
                    Permission.EXPORT_FINANCIAL_DATA -> false
                    Permission.DELETE_TRANSACTIONS -> false
                    Permission.EDIT_CUSTOMERS -> false // View only
                    Permission.RECORD_ATTENDANCE -> true
                    Permission.VIEW_DASHBOARD_STATS -> false
                }
            }
        }
    }

    fun verifyServerSidePermission(roleStr: String?, permission: Permission) {
        if (!hasPermission(roleStr, permission)) {
            val role = normalizeRole(roleStr)
            throw SecurityException("Server-Side Access Denied: Role '${role.displayName}' does not have permission for $permission")
        }
    }
}
