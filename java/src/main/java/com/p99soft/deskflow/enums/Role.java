package com.p99soft.deskflow.enums;

/**
 * Represents the roles available to users in the DeskFlow system.
 * <ul>
 *   <li>{@link #EMPLOYEE} – A regular employee who can raise support tickets.</li>
 *   <li>{@link #AGENT}    – A support agent who handles and resolves tickets.</li>
 *   <li>{@link #ADMIN}    – A system administrator with full access.</li>
 * </ul>
 */
public enum Role {
    EMPLOYEE,
    AGENT,
    ADMIN
}
